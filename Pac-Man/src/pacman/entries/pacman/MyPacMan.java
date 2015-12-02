package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.controllers.examples.StarterGhosts;
import java.util.*;

//import static pacman.game.Constants.DELAY;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getAction() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.pacman.mypackage).
 */
public class MyPacMan extends Controller<MOVE>
{
	// move and last move
	private MOVE myMove=MOVE.NEUTRAL;
	private MOVE lastMove = MOVE.NEUTRAL;
	
	// alpha beta stuff
	// tree depth
	private final double MAX_DEPTH = 19;
	// evaluation function stuff
	// add a penalty for changing directions (aka moving right after moving left)
	private final double changeDirectionPenalty = 10;
	private final double dotDistFactor = 0.2;
	private final double ghostDistFactor = 0.1;
	private final double ghostMaxRange = 5;
	private final double loseScore = -5000;
	private final double winScore = 5000;
        
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
		newState.advanceGame(eachMove, new StarterGhosts().getMove());
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
            return bestMove;	
	}
        
        // best move for pacman
	private double alphaBetaMaxValue(Game state, MOVE previousMove, double alpha, double beta, double depth){
            if (depth < 1){
                return alphaBetaEvaluate(state);
            }
		
            double value = Double.NEGATIVE_INFINITY;
            List<MOVE> moves = Arrays.asList(state.getPossibleMoves(state.getPacmanCurrentNodeIndex()));
            moves.remove(previousMove.opposite());
            for (MOVE eachMove: moves){
            	Game newState = state.copy();
            	newState.advanceGame(eachMove, new StarterGhosts().getMove());
		value = Math.max(value, alphaBetaMinValue(newState, eachMove, alpha, beta, depth - 1));
		if (value < beta)
                    alpha = Math.max(alpha, value);
		else 
                    break;
		}
            return value;	
	}
	
	// worst move for pacman
	// write a proper min please
	private double alphaBetaMinValue(Game state, MOVE previousMove, double alpha, double beta, double depth){
            return alphaBetaEvaluate(state);
        }
	
        // SHITTY make a better evaluation LOL
	private double alphaBetaEvaluate(Game state){
            double score = 0;
            int p = state.getPacmanCurrentNodeIndex();
		
            double minDistance = Double.POSITIVE_INFINITY;
            for (int q: state.getActivePillsIndices()){
		double distance = state.getManhattanDistance(p, q);
		if (distance < minDistance){
                    minDistance = distance;
		}
            }
            score -= minDistance * dotDistFactor;
			
            score += averageGhostDistance(state) * ghostDistFactor;
            score -= numberOfGhostsInRange(state);
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
        
        // Extremely dumb as depth-limited nor iterative deepening is being used
        private MOVE depthFirst(Game game, long timeDue)
        {
                int bestEval = 0;
                MOVE bestMove = myMove;
                Stack<Game> states = new Stack();
                Stack<MOVE> direction = new Stack();
                
                int current = game.getPacmanCurrentNodeIndex();
                MOVE next[] = game.getPossibleMoves(current);
                for (MOVE eachMove : next)
                {
                        Game newState = game.copy();
                        newState.advanceGame(eachMove, new StarterGhosts().getMove());
                        states.push(newState);
                        direction.push(eachMove);
                }
                
                while (!states.isEmpty() && System.currentTimeMillis() < timeDue)
                {
                        Game curState = states.pop();
                        MOVE startingMove = direction.pop();
                        current = curState.getPacmanCurrentNodeIndex();
                        next = curState.getPossibleMoves(current);
                        
                        int evaluation = eval(curState);
                        if (evaluation > bestEval)
                        {
                                bestEval = evaluation;
                                bestMove = startingMove;
                        }
                        
                        for (MOVE eachMove : next)
                        {
                                Game newState = curState.copy();
                                newState.advanceGame(eachMove, new StarterGhosts().getMove());
                                states.push(newState);
                                direction.push(startingMove);
                        }
                        
                }
                
                return bestMove;
        }
        
        private MOVE breadthFirst(Game game, long timeDue)
        {
                int bestEval = 0;
                MOVE bestMove = myMove;
                Queue<Game> states = new LinkedList<Game>();
                Queue<MOVE> direction = new LinkedList<MOVE>();
                
                int current = game.getPacmanCurrentNodeIndex();
                MOVE next[] = game.getPossibleMoves(current);
                for (MOVE eachMove : next)
                {
                        Game newState = game.copy();
                        newState.advanceGame(eachMove, new StarterGhosts().getMove());
                        states.add(newState);
                        direction.add(eachMove);
                }
                
                while (!states.isEmpty() && System.currentTimeMillis() < timeDue)
                {
                        Game curState = states.remove();
                        MOVE startingMove = direction.remove();
                        current = curState.getPacmanCurrentNodeIndex();
                        next = curState.getPossibleMoves(current);
                        
                        int evaluation = eval(curState);
                        if (evaluation > bestEval)
                        {
                                bestEval = evaluation;
                                bestMove = startingMove;
                        }
                        
                        for (MOVE eachMove : next)
                        {
                                Game newState = curState.copy();
                                newState.advanceGame(eachMove, new StarterGhosts().getMove());
                                states.add(newState);
                                direction.add(startingMove);
                        }
                        
                }
                
                return bestMove;
        }
        
        private int eval(Game game)
        {
                return game.getScore() + game.getPacmanNumberOfLivesRemaining() * 1000;
        }
        
        
	public MOVE getMove(Game game, long timeDue) 
	{
		//Place your game logic here to play the game as Ms Pac-Man
		
		//return breadthFirst(game, timeDue);
                return alphaBetaPruning(game, timeDue);
	}
}