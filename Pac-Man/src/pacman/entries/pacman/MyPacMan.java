package pacman.entries.pacman;

import pacman.controllers.Controller;
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
	private MOVE myMove=MOVE.NEUTRAL;
        
        private int eval(Game game)
        {
                return game.getScore() + game.getPacmanNumberOfLivesRemaining() * 1000;
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
        
	public MOVE getMove(Game game, long timeDue) 
	{
		//Place your game logic here to play the game as Ms Pac-Man
		
		return breadthFirst(game, timeDue);
	}
}