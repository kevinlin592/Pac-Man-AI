package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.controllers.examples.AggressiveGhosts;
import java.util.*;

//import static pacman.game.Constants.DELAY;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getAction() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.pacman.mypackage).
 */
public class MyPacMan extends Controller<MOVE>
{
    
    class Node {
        Game game;
        MOVE initialMove;
        MOVE previousMove;
        int depth;
        double aStarValue;

        public Node(Game g, MOVE i, MOVE p) {
            game = g;
            initialMove = i;
            previousMove = p;
            depth = 0;
            aStarValue = 0;
        }

        public Node(Game g, MOVE i, MOVE p, int d, double a) {
            game = g;
            initialMove = i;
            previousMove = p;
            depth = d;
            aStarValue = a;
        }
    }

    
    // move and last move
    private MOVE myMove=MOVE.NEUTRAL;
    private MOVE lastMove = MOVE.NEUTRAL;
	
    // alpha beta stuff
    // tree depth
    private final double MAX_DEPTH = 5;
    // evaluation function stuff
    // add a penalty for changing directions (aka moving right after moving left)
    private final double changeDirectionPenalty = 10;
    private final double ghostMaxRange = 5;
    private final double L = -5000;
    private final double U = 5000;
        
    // can get stuck in local maxima
    public MOVE hillClimber(Game game, long timeDue){
        double currentEval = Double.NEGATIVE_INFINITY;
        for (MOVE eachMove : game.getPossibleMoves(game.getPacmanCurrentNodeIndex())){
            Game newState = game.copy();
            newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
            if (eval(newState) > currentEval){
                myMove = eachMove;
                currentEval = eval(newState);
            }
        }
        return myMove;
    }
        
    // same as hill climber but has an acceptance probability
    public MOVE simulatedAnnealing(Game game, long timeDue){
        double currentEval = -5000;
          
        for (MOVE eachMove : game.getPossibleMoves(game.getPacmanCurrentNodeIndex())){
            Game newState = game.copy();
            newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
            double evalScore = eval(newState);
              
            // if it's better take it
            if (evalScore > currentEval) {
                myMove = eachMove;
                currentEval = evalScore;
            } 
            // if its not better, well take it anyways according to an acceptance probablity so you can escape local maxima
            else if (simulatedAnnealingAcceptanceProbability(currentEval, evalScore, game) < Math.random()){
                myMove = eachMove;
                currentEval = evalScore;
            }
        }
        return myMove;
    }
        
    // TODO: probably rewrite this, the probability doesn't feel right
    private double simulatedAnnealingAcceptanceProbability(double score, double newScore, Game game){
        return Math.exp(((newScore - score)*10) / game.getScore());
    } 
        
    public MOVE alphaBetaPruning(Game game, long timeDue){
        // initializing
        MOVE bestMove = MOVE.NEUTRAL;
        double bestScore = Double.NEGATIVE_INFINITY;
        // alpha beta initializing shit
        double alpha = Double.NEGATIVE_INFINITY;
        double beta = Double.POSITIVE_INFINITY;

        for (MOVE eachMove : game.getPossibleMoves(game.getPacmanCurrentNodeIndex())){
            double moveScore = 0;
            Game newState = game.copy();
            newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
            moveScore = alphaBetaMinValue(newState, eachMove, alpha, beta, MAX_DEPTH - 1);
	
            if (lastMove == eachMove.opposite())
                moveScore -= changeDirectionPenalty;
			
            if (moveScore > bestScore){
                bestScore = moveScore;
                bestMove = eachMove;
            }
			
            if (moveScore < beta)
                alpha = Math.max(alpha, moveScore);
            else
                break;
        }
        lastMove = bestMove;
        System.out.println(bestMove);
        return bestMove;	
    }
        
    // best move for pacman
    private double alphaBetaMaxValue(Game state, MOVE previousMove, double alpha, double beta, double depth){
        if (depth < 1){
            return eval(state);
        }
		
        double value = Double.NEGATIVE_INFINITY;
        List<MOVE> moves = new ArrayList<MOVE>(Arrays.asList(state.getPossibleMoves(state.getPacmanCurrentNodeIndex())));
        moves.remove(previousMove.opposite());
        for (MOVE eachMove: moves){
            Game newState = state.copy();
            newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
            value = Math.max(value, alphaBetaMinValue(newState, eachMove, alpha, beta, depth - 1));
            if (value < beta)
                alpha = Math.max(alpha, value);
            else 
                break;
        }
        return value;	
    }
	
    // worst move for pacman
    private double alphaBetaMinValue(Game state, MOVE previousMove, double alpha, double beta, double depth){
        if (depth < 1){
            return eval(state);
        }
        List<MOVE> ghostMoves = new ArrayList<>();
        for (GHOST g : GHOST.values()) {
            ghostMoves.add(state.getGhostLastMoveMade(g));
        }
        double N = ghostMoves.size();
        double A = N * (alpha - U) + U;
        double B = N * (beta - L) + L;
        double vsum = 0;
        for (MOVE eachMove : ghostMoves) {
            double AX = Math.max(A, L);
            double BX = Math.min(B, U);
	
            Game newState = state.copy();
            newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
            double v =  alphaBetaMaxValue(newState, previousMove, AX, BX, depth - 1);
								  
            if (v <= A)
                return alpha;
            if (v >= B)
                return beta;
            vsum += v;
            A += U - v;
            B += L - v;
            
        }
        return (vsum / N);
    }
        
    private MOVE breadthFirst(Game game, long timeDue) {
        double bestEval = Double.NEGATIVE_INFINITY;
        MOVE bestMove = myMove;
        Queue<Node> states = new LinkedList<Node>();

        int current = game.getPacmanCurrentNodeIndex();
        MOVE next[] = game.getPossibleMoves(current);
        for (MOVE eachMove : next) {
            //if (eachMove.opposite() == lastMove) continue;
            Game newState = game.copy();
            newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
            states.add(new Node(newState, eachMove, eachMove));
        }

        while (!states.isEmpty() && System.currentTimeMillis() < timeDue - 5) {
            Node curState = states.remove();
            current = curState.game.getPacmanCurrentNodeIndex();
            next = curState.game.getPossibleMoves(current);

            double evaluation = eval(curState.game);
            if (evaluation > bestEval) {
                bestEval = evaluation;
                bestMove = curState.initialMove;
            }

            for (MOVE eachMove : next) {
                if (eachMove == curState.previousMove.opposite()) {
                    continue;
                }

                Game newState = curState.game.copy();
                newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
                states.add(new Node(newState, curState.initialMove, eachMove, curState.depth + 1, 0));
            }
        }
        lastMove = bestMove;
        return bestMove;

    }
        
    private MOVE depthFirst(Game game, int maxDepth, long timeDue) {
        double bestEval = Double.NEGATIVE_INFINITY;
        MOVE bestMove = myMove;
        Stack<Node> states = new Stack();

        int current = game.getPacmanCurrentNodeIndex();
        MOVE next[] = game.getPossibleMoves(current);
        for (MOVE eachMove : next) {
            //if (eachMove.opposite() == lastMove) continue;
            Game newState = game.copy();
            newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
            states.push(new Node(newState, eachMove, eachMove, 1, 0));
        }

        while (!states.isEmpty() && System.currentTimeMillis() < timeDue - 5) {
            Node curState = states.pop();
            current = curState.game.getPacmanCurrentNodeIndex();
            next = curState.game.getPossibleMoves(current);

            double evaluation = eval(curState.game);
            if (evaluation > bestEval) {
                bestEval = evaluation;
                bestMove = curState.initialMove;
            }

            if (curState.depth < maxDepth) {
                for (MOVE eachMove : next) {
                    if (eachMove == curState.previousMove.opposite()) {
                        continue;
                    }

                    Game newState = curState.game.copy();
                    newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
                    states.push(new Node(newState, curState.initialMove, eachMove, curState.depth + 1, 0));
                }
            }
        }
        lastMove = bestMove;
        return bestMove;

    }

    private MOVE iterativeDeepening(Game game, int maxDepth, long timeDue) {
        double bestEval = Double.NEGATIVE_INFINITY;
        MOVE bestMove = myMove;
        Stack<Node> states = new Stack();

        int current = game.getPacmanCurrentNodeIndex();
        MOVE next[] = game.getPossibleMoves(current);
        for (MOVE eachMove : next) {
            //if (eachMove.opposite() == lastMove) continue;
            Game newState = game.copy();
            newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
            states.push(new Node(newState, eachMove, eachMove, 1, 0));
        }

        Stack<Node> newStatesss = new Stack();
        int total = 0;
        while (System.currentTimeMillis() < timeDue - 5) {
            Node curState = states.pop();
            current = curState.game.getPacmanCurrentNodeIndex();
            next = curState.game.getPossibleMoves(current);

            double evaluation = eval(curState.game);
            if (evaluation > bestEval) {
                bestEval = evaluation;
                bestMove = curState.initialMove;
            }

            if (curState.depth < maxDepth) {
                for (MOVE eachMove : next) {
                    if (eachMove == curState.previousMove.opposite()) {
                        continue;
                    }

                    Game newState = curState.game.copy();
                    newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
                    states.push(new Node(newState, curState.initialMove, eachMove, curState.depth + 1, 0));
                }
            } else {
                for (MOVE eachMove : next) {
                    if (eachMove == curState.previousMove.opposite()) {
                        continue;
                    }

                    Game newState = curState.game.copy();
                    newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
                    newStatesss.push(new Node(newState, curState.initialMove, eachMove, 1, 0));
                }
            }
            if (states.isEmpty()) {
                total++;
                states = newStatesss;
                newStatesss = new Stack();
            }
        }
        lastMove = bestMove;
        return bestMove;

    }
    
    private static class aStarCompare implements Comparator<Node> {
        public int compare(Node n1, Node n2) {
            if (n1.aStarValue > n2.aStarValue) return -1;
            if (n2.aStarValue > n1.aStarValue) return 1;
            return 0;
        }
    }
    
    private MOVE aStar(Game game, long timeDue) {
        double bestEval = Double.NEGATIVE_INFINITY;
        MOVE bestMove = myMove;
        
        PriorityQueue<Node> states = new PriorityQueue<Node>(1, new aStarCompare());

        int current = game.getPacmanCurrentNodeIndex();
        MOVE next[] = game.getPossibleMoves(current);
        for (MOVE eachMove : next) {
            //if (eachMove.opposite() == lastMove) continue;
            Game newState = game.copy();
            newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
            states.add(new Node(newState, eachMove, eachMove, 0, eval(newState)));
        }
        
        while (!states.isEmpty() && System.currentTimeMillis() < timeDue - 20) {
            Node curState = states.poll();
            current = curState.game.getPacmanCurrentNodeIndex();
            next = curState.game.getPossibleMoves(current);

            if (curState.aStarValue > bestEval) {
                bestEval = curState.aStarValue;
                bestMove = curState.initialMove;
            }

            for (MOVE eachMove : next) {
                if (eachMove == curState.previousMove.opposite()) {
                    continue;
                }

                Game newState = curState.game.copy();
                newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
                states.add(new Node(newState, curState.initialMove, eachMove, 0, eval(newState)));
            }
        }
        lastMove = bestMove;
        return bestMove;

    }
    
        
    /*
    private double eval(Game state){
        double score = state.getScore();
        int p = state.getPacmanCurrentNodeIndex();
		
        double minDistance = Double.POSITIVE_INFINITY;
        for (int q: state.getActivePillsIndices()){
            double distance = state.getManhattanDistance(p, q);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        score -= minDistance;
        for (GHOST g: GHOST.values()){
            if (state.isGhostEdible(g)){
                score += 10;
            }
        }
        score += averageGhostDistance(state) * 0.1;
        score -= numberOfGhostsInRange(state);
            
        score += state.getPacmanNumberOfLivesRemaining() * 1000;
        return score;
    }*/
    
    private double eval(Game state) {
        double score = 0;
        score += state.getCurrentLevel() * 5000;
        score += state.getPacmanNumberOfLivesRemaining() * 5000;
        int closeGhosts = 0;
        for (GHOST g : GHOST.values()) {
            if (state.isGhostEdible(g)) continue;
            double distanceAway = state.getShortestPathDistance(state.getPacmanCurrentNodeIndex(), state.getGhostCurrentNodeIndex(g));
            if (distanceAway == -1) continue;
            if (distanceAway < 10) {
                score -= 1000 * (5 / distanceAway);
                closeGhosts++;
            } else if (distanceAway < 20) {
                score -= 1000 * (2 / distanceAway);
            }
        }
        if (closeGhosts > 0) {
            for (int index : state.getActivePowerPillsIndices()) {
                double distanceAway = state.getShortestPathDistance(state.getPacmanCurrentNodeIndex(), index);
                if (distanceAway < 100) {
                    score += 4000 * closeGhosts / distanceAway;
                }
            }
        }
        score -= state.getNumberOfActivePills() * 10;
        score += state.getNumberOfActivePowerPills() * 800;
        double closestAway = Double.POSITIVE_INFINITY;
        for (int index : state.getActivePillsIndices()) {
            double distanceAway = state.getShortestPathDistance(state.getPacmanCurrentNodeIndex(), index);
            if (distanceAway < closestAway) closestAway = distanceAway;
        }
        score += 100 / closestAway;
        score += state.getScore() * 5;
        //System.out.println(score);
        return score;
    }
	
    private double averageGhostDistance(Game state){
        double sumDistance = 0;
        for (GHOST g : GHOST.values()) {
            double d = state.getManhattanDistance(state.getPacmanCurrentNodeIndex(), state.getGhostCurrentNodeIndex(g));
            sumDistance += d;
        }
        return (sumDistance/GHOST.values().length);
    }
	
    private int numberOfGhostsInRange(Game state){
        int numberInRange = 0; 
        for (GHOST g : GHOST.values()) {
            double d = state.getManhattanDistance(state.getPacmanCurrentNodeIndex(), state.getGhostCurrentNodeIndex(g));
            if (d < ghostMaxRange)
                numberInRange++;
        }
        return numberInRange;
    }
        
    public MOVE getMove(Game game, long timeDue) {
	//Place your game logic here to play the game as Ms Pac-Man
	
        //return breadthFirst(game, timeDue);
        //return depthFirst(game, 80, timeDue);
        return iterativeDeepening(game, 5, timeDue);
        //return aStar(game, timeDue);
        //return simulatedAnnealing(game, timeDue);
    }
}