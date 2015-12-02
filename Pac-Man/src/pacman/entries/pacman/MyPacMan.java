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
	private final double MAX_DEPTH = 5;
	// evaluation function stuff
	// add a penalty for changing directions (aka moving right after moving left)
	private final double changeDirectionPenalty = 10;
	private final double ghostMaxRange = 5;
        private final double L = -5000;
        private final double U = 5000;
        
        public MOVE hillClimber(Game game, long timeDue){
            double currentEval = Double.NEGATIVE_INFINITY;
            for (MOVE eachMove : game.getPossibleMoves(game.getPacmanCurrentNodeIndex())){
                Game newState = game.copy();
                newState.advanceGame(eachMove, new StarterGhosts().getMove());
                if (eval(newState) > currentEval){
                    myMove = eachMove;
                    currentEval = eval2(newState);
                }
            }
            return myMove;
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
            System.out.println(bestMove);
            return bestMove;	
	}
        
        // best move for pacman
	private double alphaBetaMaxValue(Game state, MOVE previousMove, double alpha, double beta, double depth){
            if (depth < 1){
                return eval2(state);
            }
		
            double value = Double.NEGATIVE_INFINITY;
            List<MOVE> moves = new ArrayList<MOVE>(Arrays.asList(state.getPossibleMoves(state.getPacmanCurrentNodeIndex())));
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
            if (depth < 1){
                return eval2(state);
            }
            List<MOVE> ghostMoves = new ArrayList<>();
            for (GHOST g : GHOST.values()) {
                ghostMoves.add(state.getGhostLastMoveMade(g));
            }
            double N = ghostMoves.size();
            double A = N * (alpha - U) + U;
            double B = N * (beta - L) + L;
            double vsum = 0;
            for (MOVE eachMove : ghostMoves)
            {
                double AX = Math.max(A, L);
                double BX = Math.min(B, U);
			
		Game newState = state.copy();
                newState.advanceGame(eachMove, new StarterGhosts().getMove());
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
        
        // SHITTY make a better evaluation LOL
	private double eval2(Game state){
            double score = state.getScore();
            int p = state.getPacmanCurrentNodeIndex();
		
            double minDistance = Double.POSITIVE_INFINITY;
            for (int q: state.getActivePillsIndices()){
		double distance = state.getManhattanDistance(p, q);
		if (distance < minDistance){
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
        
	public MOVE getMove(Game game, long timeDue) 
	{
		//Place your game logic here to play the game as Ms Pac-Man
		
		//return breadthFirst(game, timeDue);
                return hillClimber(game, timeDue);
	}
}