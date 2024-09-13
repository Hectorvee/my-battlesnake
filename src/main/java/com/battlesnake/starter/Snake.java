package com.battlesnake.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.get;

/**
 * This is a simple Battlesnake server written in Java.
 * 
 * For instructions see
 * https://github.com/BattlesnakeOfficial/starter-snake-java/README.md
 */
public class Snake {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Handler HANDLER = new Handler();
    private static final Logger LOG = LoggerFactory.getLogger(Snake.class);
    private static final int FOOD = 1;
    private static final int SNAKE_HEAD = 2;
    private static final int SNAKE_BODY = 3;
    private static final int MY_SNAKE_HEAD = 5;
    private static final int MY_SNAKE_BODY = 6;

    /**
     * Main entry point.
     *
     * @param args are ignored.
     */
    public static void main(String[] args) {
        String port = System.getProperty("PORT");
        if (port == null) {
            LOG.info("Using default port: {}", port);
            port = "8080";
        } else {
            LOG.info("Found system provided port: {}", port);
        }
        port(Integer.parseInt(port));
        get("/", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/start", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/move", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/end", HANDLER::process, JSON_MAPPER::writeValueAsString);
    }

    /**
     * Handler class for dealing with the routes set up in the main method.
     */
    public static class Handler {

        /**
         * For the start/end request
         */
        private static final Map<String, String> EMPTY = new HashMap<>();

        /**
         * Generic processor that prints out the request and response from the methods.
         *
         * @param req
         * @param res
         * @return
         */
        public Map<String, String> process(Request req, Response res) {
            try {
                JsonNode parsedRequest = JSON_MAPPER.readTree(req.body());
                String uri = req.uri();
                LOG.info("{} called with: {}", uri, req.body());
                Map<String, String> snakeResponse;
                if (uri.equals("/")) {
                    snakeResponse = index();
                } else if (uri.equals("/start")) {
                    snakeResponse = start(parsedRequest);
                } else if (uri.equals("/move")) {
                    snakeResponse = move(parsedRequest);
                } else if (uri.equals("/end")) {
                    snakeResponse = end(parsedRequest);
                } else {
                    throw new IllegalAccessError("Strange call made to the snake: " + uri);
                }

                LOG.info("Responding with: {}", JSON_MAPPER.writeValueAsString(snakeResponse));

                return snakeResponse;
            } catch (JsonProcessingException e) {
                LOG.warn("Something went wrong!", e);
                return null;
            }
        }

        /**
         * This method is called everytime your Battlesnake is entered into a game.
         * 
         * Use this method to decide how your Battlesnake is going to look on the board.
         *
         * @return a response back to the engine containing the Battlesnake setup
         *         values.
         */
        public Map<String, String> index() {
            Map<String, String> response = new HashMap<>();
            response.put("apiversion", "1");
            response.put("hectorvee", "");
            response.put("color", "#229799");
            response.put("head", "default"); // TODO: Personalize
            response.put("tail", "default"); // TODO: Personalize
            return response;
        }

        /**
         * This method is called everytime your Battlesnake is entered into a game.
         * 
         * Use this method to decide how your Battlesnake is going to look on the board.
         *
         * @param startRequest a JSON data map containing the information about the game
         *                     that is about to be played.
         * @return responses back to the engine are ignored.
         */
        public Map<String, String> start(JsonNode startRequest) {
            LOG.info("START");
            return EMPTY;
        }

        /**
         * This method is called on every turn of a game. It's how your snake decides
         * where to move.
         * 
         * Use the information in 'moveRequest' to decide your next move. The
         * 'moveRequest' variable can be interacted with as
         * com.fasterxml.jackson.databind.JsonNode, and contains all of the information
         * about the Battlesnake board for each move of the game.
         * 
         * For a full example of 'json', see
         * https://docs.battlesnake.com/references/api/sample-move-request
         *
         * @param moveRequest JsonNode of all Game Board data as received from the
         *                    Battlesnake Engine.
         * @return a Map<String,String> response back to the engine the single move to
         *         make. One of "up", "down", "left" or "right".
         */
        public Map<String, String> move(JsonNode moveRequest) {

            try {
                LOG.info("Data: {}", JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(moveRequest));
            } catch (JsonProcessingException e) {
                LOG.error("Error parsing payload", e);
            }

            /*
             * Example how to retrieve data from the request payload:
             * 
             * String gameId = moveRequest.get("game").get("id").asText();
             * 
             * int height = moveRequest.get("board").get("height").asInt();
             * 
             */

            JsonNode head = moveRequest.get("you").get("head");
            JsonNode body = moveRequest.get("you").get("body");

            ArrayList<String> possibleMoves = new ArrayList<>(Arrays.asList("up", "down", "left", "right"));

            // Don't allow your Battlesnake to move back in on its own neck
            avoidMyNeck(head, body, possibleMoves);

            int row = moveRequest.get("board").get("height").asInt();
            int col = moveRequest.get("board").get("width").asInt();
            int[][] board = initializeBoard(row, col, moveRequest);
            // Print the board
//            for (int[] r: board) {
//                for (int c: r) {
//                    System.out.print(c + " ");
//                }
//                System.out.println();
//            }

            // TODO: Using information from 'moveRequest', find the edges of the board and don't let your
            //  Battlesnake move beyond them board_height = ? board_width = ?
            avoidTheWalls(moveRequest, possibleMoves, row, col);

            // TODO Using information from 'moveRequest', don't let your Battlesnake pick a move that would
            //  hit its own body

            // TODO: Using information from 'moveRequest', don't let your Battlesnake pick a move that would
            //  collide with another Battlesnake

            // TODO: Using information from 'moveRequest', make your Battlesnake move towards a piece of food
            //  on the board

            // Choose a random direction to move in
            final int choice = new Random().nextInt(possibleMoves.size());
            final String move = possibleMoves.get(choice);

            LOG.info("MOVE {}", move);

            Map<String, String> response = new HashMap<>();
            response.put("move", move);
            return response;
        }

        /**
         * Remove the 'neck' direction from the list of possible moves
         * 
         * @param head          JsonNode of the head position e.g. {"x": 0, "y": 0}
         * @param body          JsonNode of x/y coordinates for every segment of a
         *                      Battlesnake. e.g. [ {"x": 0, "y": 0}, {"x": 1, "y": 0},
         *                      {"x": 2, "y": 0} ]
         * @param possibleMoves ArrayList of String. Moves to pick from.
         */
        public void avoidMyNeck(JsonNode head, JsonNode body, ArrayList<String> possibleMoves) {
            JsonNode neck = body.get(1);

            if (neck.get("x").asInt() < head.get("x").asInt()) {
                possibleMoves.remove("left");
            } else if (neck.get("x").asInt() > head.get("x").asInt()) {
                possibleMoves.remove("right");
            } else if (neck.get("y").asInt() < head.get("y").asInt()) {
                possibleMoves.remove("down");
            } else if (neck.get("y").asInt() > head.get("y").asInt()) {
                possibleMoves.remove("up");
            }
        }

        /**
         * This method is called when a game your Battlesnake was in ends.
         * 
         * It is purely for informational purposes, you don't have to make any decisions
         * here.
         *
         * @param endRequest a map containing the JSON sent to this snake. Use this data
         *                   to know which game has ended
         * @return responses back to the engine are ignored.
         */
        public Map<String, String> end(JsonNode endRequest) {
            LOG.info("END");
            return EMPTY;
        }
    }

    /**
     * Initialize the board with food and snake positions
     *
     * @param row         the height of the board
     * @param col         the width of the board
     * @param moveRequest the JSON data map containing the information about the game
     *                    that is about to be played.
     * @return a 2D array representing the board
     */
    private static int[][] initializeBoard(int row, int col, JsonNode moveRequest) {
        int[][] board = new int[row][col];
        JsonNode food = moveRequest.get("board").get("food");   // food is an array of objects
        for (JsonNode f : food) {
            board[f.get("y").asInt()][f.get("x").asInt()] = FOOD;
        }

        JsonNode snakes = moveRequest.get("board").get("snakes");   // snakes is an array of objects
        String mySnakeId = moveRequest.get("you").get("id").asText();
        for (JsonNode snake: snakes) {
            int headValue = snake.get("id").asText().equals(mySnakeId) ? MY_SNAKE_HEAD : SNAKE_HEAD;
            int bodyValue = snake.get("id").asText().equals(mySnakeId) ? MY_SNAKE_BODY : SNAKE_BODY;

            board[snake.get("head").get("y").asInt()][snake.get("head").get("x").asInt()] = headValue;
            for (JsonNode body: snake.get("body")) {
                board[body.get("y").asInt()][body.get("x").asInt()] = bodyValue;
            }
        }

        return board;
    }

    /**
     * Avoid the walls
     *
     * @param board        the 2D array representing the board
     * @param moveRequest  the JSON data map containing the information about the game
     *                     that is about to be played.
     * @param possibleMoves the list of possible moves
     * @param row          the height of the board
     * @param col          the width of the board
     */
    private static void avoidTheWalls(JsonNode moveRequest, ArrayList<String> possibleMoves, int row, int col) {

        int xSnakeHead = moveRequest.get("you").get("head").get("x").asInt();
        int ySnakeHead = moveRequest.get("you").get("head").get("y").asInt();

        if (xSnakeHead+1>col) {
            possibleMoves.remove("right");
        }

        if (xSnakeHead-1<col) {
            possibleMoves.remove("left");
        }

        if (ySnakeHead+1>row) {
            possibleMoves.remove("down");
        }

        if (ySnakeHead+1<row) {
            possibleMoves.remove("up");
        }
    }

}
