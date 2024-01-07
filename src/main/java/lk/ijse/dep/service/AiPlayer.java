package lk.ijse.dep.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AiPlayer extends Player {
    public AiPlayer(Board board) {
        super(board);
    }

    //---------This is the method that makes the AI player's move on the board.---------
    @Override
    // ---------Create MCTS instance and start the MCTS algorithm.---------

    public void movePiece(int col) {
/*        do {
            col = (int) Math.floor(Math.random() * 6.0D);
        } while (!this.board.isLegalMove(col));*/

        Mcts mcts = new Mcts(board.getBoardImpl());

        col = mcts.startMCTS();  // Update the board with AI's move.

        this.board.updateMove(col, Piece.GREEN); // Notify the UI about the move.
        this.board.getBoardUI().update(col, false); // Check if there's a winner or if it's a draw.
        Winner winner = this.board.findWinner();
        if (winner.getWinningPiece() != Piece.EMPTY) {
            this.board.getBoardUI().notifyWinner(winner);
        }
        else if (!this.board.existLegalMoves()) {
            this.board.getBoardUI().notifyWinner(new Winner(Piece.EMPTY));
        }
    }

    static class Mcts {

        // ---------Initialization and attributes like AI's piece, Human's piece, etc.---------
        // ---------The primary method that starts the MCTS algorithm.---------
        private final BoardImpl board;
        private final Piece AiID = Piece.GREEN;
        private final Piece HumanID = Piece.BLUE;
        public Mcts(BoardImpl board) {
            this.board = board;
        }
        public int startMCTS() {

            int count = 0;
            Node tree = new Node(board);

            while (count < 4000) {
                count++;
                Node promisingNode = selectPromisingN(tree);
                Node selected = promisingNode;

                if (selected.board.getStatus()) {
                    selected = expandNodeAndReturnRanNum(promisingNode);
                }

                Piece resultPiece = simulateLightPlayout(selected);
                backPropagation(resultPiece, selected);
            }

            Node best = tree.getMaxScoreChild();
            return best.board.cols;
        }

        // ---------Backpropagation step of the MCTS algorithm.---------
        private void backPropagation(Piece resultPiece, Node selected) {
            Node node = selected;

            while (node != null){
                node.visits++;
                if (node.board.piece == resultPiece){
                    node.score++;
                }
                node = node.parent;
            }
        }

        // ---------Simulates a light playout to determine the result of a game---------
        private Piece simulateLightPlayout(Node promisingNode) {
            Node node = new Node(promisingNode.board);
            node.parent = promisingNode.parent;
            Winner winner = node.board.findWinner();

            if (winner.getWinningPiece() == Piece.BLUE){
                node.parent.score=Integer.MIN_VALUE;
                return node.board.findWinner().getWinningPiece();
            }

            while (node.board.getStatus()){
                BoardImpl nextMove = node.board.getRandomLeagalNextMove();
                Node child = new Node(nextMove);
                child.parent = node;
                node.addNewChild(child);
                node = child;
            }
            return node.board.findWinner().getWinningPiece();
        }

        // ---------Expands a node in the tree and returns a random child node.---------
        private Node expandNodeAndReturnRanNum(Node node) {
            Node result = node;
            BoardImpl board = node.board;
            List<BoardImpl> legalMoves = board.getAllLegalNextMoves();

            for (int i = 0; i < legalMoves.size(); i++) {
                BoardImpl move = legalMoves.get(i);
                Node child = new Node(move);
                child.parent = node;
                node.addNewChild(child);
                result = child;
            }
            int random = Board.RANDOM_GENERATOR.nextInt(node.children.size());
            return node.children.get(random);
        }
        // ---------Selects a promising node using UCT values.---------
        private Node selectPromisingN(Node tree) {
            Node node = tree;
            while (node.children.size() != 0){
                node = UCT.findBestNodeWithUCT(node);
            }
            return node;
        }
    }
    // Represents a node in the MCTS tree.
    static class Node {
        public BoardImpl board;
        public int visits;
        public int score;
        List<Node> children = new ArrayList<>();
        Node parent = null;

        public Node(BoardImpl board) {
            this.board = board;
        }

        Node getMaxScoreChild() {
            Node result = children.get(0);
            for (int i = 1; i < children.size(); i++) {
                if (children.get(i).score > result.score) {
                    result = children.get(i);
                }
            }
            return result;
        }
        void addNewChild(Node node) {
            children.add(node);
        }

    }

    static class UCT {
        // ---------Utility class for the UCT (Upper Confidence Bound for Trees) calculation---------
        public static double findUCTValue(
                int totalVisit, double nodeWinScore, int nodeVisit) {
            if (nodeVisit == 0) {
                return Integer.MAX_VALUE;
            }
            return ((double) nodeWinScore / (double) nodeVisit) + 1.41 * Math.sqrt(Math.log(totalVisit) / (double) nodeVisit);
        }

        // ---------Finds the best node based on UCT values.---------
        public static Node findBestNodeWithUCT(Node node) {
            int parentVisit = node.visits;
            return Collections.max(node.children, Comparator.comparing(c -> findUCTValue(parentVisit, c.score, c.visits)));
        }
    }
}