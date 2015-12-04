package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.controllers.examples.AggressiveGhosts;
import java.util.*;
import java.io.*;

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
	
    String filename = "replay.txt";
    ArrayList<replayData> historicalData = understand(filename);

		tree decisionMaking = setupTree(historicalData);
    
    // alpha beta stuff
    // tree depth
    private final double MAX_DEPTH_AB = 10;
    
    // evaluation function stuff
    // add a penalty for changing directions (aka moving right after moving left)
    private final double changeDirectionPenalty = 10;
    private final double ghostMaxRange = 5;
    
    //Evolution/genetic stuff
    private final double MAX_DEPTH_EVOLUTION = 20;
    double evolutionRandomizerChance = .20;
    double geneticRandomizerChance = .10;
    int evolutionExpand = 3; // Decides how many children there will be 
    int geneticExpand = 3;
        
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
            else if (simulatedAnnealingAcceptanceProbability(game) > Math.random()*100){
                myMove = eachMove;
                currentEval = evalScore;
            }
        }
        return myMove;
    }
        
    // acceptance probability
    private double simulatedAnnealingAcceptanceProbability(Game game){
        return Math.max((-game.getCurrentLevelTime())/20 + 20, 0);
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
            moveScore = alphaBetaMinValue(newState, eachMove, alpha, beta, MAX_DEPTH_AB - 1);
	
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
        
        double L = Double.NEGATIVE_INFINITY;
        double U = Double.POSITIVE_INFINITY;
        
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
    
    private MOVE breadthFirst(Game game, int maxDepth, long timeDue) {
        double bestEval = Double.NEGATIVE_INFINITY;
        MOVE bestMove = myMove;
        Queue<Node> states = new LinkedList();

        int current = game.getPacmanCurrentNodeIndex();
        MOVE next[] = game.getPossibleMoves(current);
        for (MOVE eachMove : next) {
            //if (eachMove.opposite() == lastMove) continue;
            Game newState = game.copy();
            newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
            states.add(new Node(newState, eachMove, eachMove, 1, 0));
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

            if (curState.depth < maxDepth) {
                for (MOVE eachMove : next) {
                    if (eachMove == curState.previousMove.opposite()) {
                        continue;
                    }

                    Game newState = curState.game.copy();
                    newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
                    states.add(new Node(newState, curState.initialMove, eachMove, curState.depth + 1, 0));
                }
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

    private MOVE iterativeDeepening(Game game, int iterDepth, int maxDepth, long timeDue) {
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
        int cumDepth = 0;
        while (cumDepth < maxDepth && System.currentTimeMillis() < timeDue - 5) {
            Node curState = states.pop();
            current = curState.game.getPacmanCurrentNodeIndex();
            next = curState.game.getPossibleMoves(current);

            double evaluation = eval(curState.game);
            if (evaluation > bestEval) {
                bestEval = evaluation;
                bestMove = curState.initialMove;
            }

            if (curState.depth < iterDepth) {
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
                cumDepth += iterDepth;
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
    
    private MOVE aStar(Game game, int maxDepth, long timeDue) {
        double bestEval = Double.NEGATIVE_INFINITY;
        MOVE bestMove = myMove;

        PriorityQueue<Node> states = new PriorityQueue(1, new aStarCompare());

        int current = game.getPacmanCurrentNodeIndex();
        MOVE next[] = game.getPossibleMoves(current);
        for (MOVE eachMove : next) {
            //if (eachMove.opposite() == lastMove) continue;
            Game newState = game.copy();
            newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
            states.add(new Node(newState, eachMove, eachMove, 1, eval(newState) - newState.getNumberOfActivePowerPills() * 100));
        }

        while (!states.isEmpty() && System.currentTimeMillis() < timeDue - 20) {
            Node curState = states.poll();
            current = curState.game.getPacmanCurrentNodeIndex();
            next = curState.game.getPossibleMoves(current);

            if (curState.aStarValue > bestEval) {
                bestEval = curState.aStarValue;
                bestMove = curState.initialMove;
            }

            if (curState.depth < maxDepth) {
                for (MOVE eachMove : next) {
                    if (eachMove == curState.previousMove.opposite()) {
                        continue;
                    }

                    Game newState = curState.game.copy();
                    newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
                    states.add(new Node(newState, curState.initialMove, eachMove, curState.depth + 1, eval(newState) - newState.getNumberOfActivePowerPills() * 100));
                }
            }
        }
        lastMove = bestMove;
        return bestMove;

    }
    
    private MOVE evolution(Game game, long timeDue){
        double bestEval = Double.NEGATIVE_INFINITY;
        double worstEval = Double.NEGATIVE_INFINITY;
        int indexOfBest = 0;
        int indexOfWorst = 0;
        
        int current = game.getPacmanCurrentNodeIndex();
        MOVE next[] = game.getPossibleMoves(current);
        
        //Number of action sequences determined by initial number of possible moves
        //Begin creating action sequences
        ArrayList<ArrayList<MOVE>> actionSequences = new ArrayList<ArrayList<MOVE>>(next.length * evolutionExpand);
        for(int x = 0; x < next.length * evolutionExpand; x++)
        {
            ArrayList<MOVE> currentSequence = new ArrayList<MOVE>();
            currentSequence.add(next[x/evolutionExpand]);
            Game currentGame = game.copy();
            currentGame.advanceGame(currentSequence.get(0), new AggressiveGhosts().getMove());
            int tempCurrent;
            MOVE tempNext[];
            
            while(currentSequence.size() < MAX_DEPTH_EVOLUTION)
            {
                tempCurrent = currentGame.getPacmanCurrentNodeIndex();
                tempNext = currentGame.getPossibleMoves(tempCurrent);
                MOVE chosenMove = tempNext[(int)(Math.random() * tempNext.length)];
                currentSequence.add(chosenMove);
                currentGame.advanceGame(chosenMove, new AggressiveGhosts().getMove());
            }
            actionSequences.add(currentSequence);
            double currentEval = eval(currentGame);
            if(currentEval > bestEval)
            {
                bestEval = currentEval;
                indexOfBest = x;
            }
            if (currentEval < worstEval)
            {
                worstEval = currentEval;
                indexOfWorst = x;
            }
            
        }
        
        //Now evaluate and evolve the action sequences
        ArrayList<MOVE> bestSequence = actionSequences.get(indexOfBest);
        bestSequence = actionSequences.get(indexOfBest);
        while(System.currentTimeMillis() < timeDue - 5)
        {
            //Evaluate best and worst
            ArrayList<Game> results = new ArrayList<Game>(actionSequences.size());
            
            //Assign the best sequence if time runs out
            bestEval = Double.NEGATIVE_INFINITY;
            worstEval = Double.POSITIVE_INFINITY;
            indexOfBest = 0;
            indexOfWorst = 0;
            
            //Best and worst action sequence has been decided
            //Now to evolve. Evolution will keep the best unmodified
            //It will kill the worst and replace with a modified version of best
           
            for(int x = 0; x < actionSequences.size(); x++)
            {
                Game currentGame = game;
                ArrayList<MOVE> currentSequence = actionSequences.get(x);
                currentGame.advanceGame(currentSequence.get(0), new AggressiveGhosts().getMove());
                if(x == indexOfWorst)
                {
                    ArrayList<MOVE> newSequence = new ArrayList<MOVE>(currentSequence);
                }
                if(x != indexOfBest)
                {
                    for(int indexOfMove = 1; indexOfMove < currentSequence.size(); indexOfMove++)
                    {
                        int tempCurrent;
                        MOVE tempNext[];
                        currentGame.advanceGame(currentSequence.get(0), new AggressiveGhosts().getMove());
                        if(Math.random() < evolutionRandomizerChance)
                        {
                            tempCurrent = currentGame.getPacmanCurrentNodeIndex();
                            tempNext = currentGame.getPossibleMoves(tempCurrent);
                            MOVE chosenMove = tempNext[(int)(Math.random() * tempNext.length)];
                            currentSequence.set(indexOfMove, chosenMove);
                        }
                    }
                }
                double currentEval = eval(currentGame);
                if(currentEval > bestEval) 
                {
                    bestEval = currentEval;
                    indexOfBest = x;
                }
                if (currentEval < worstEval)
                {
                    worstEval = currentEval;
                    indexOfWorst = x;
                }
            }
            bestSequence = actionSequences.get(indexOfBest);
            
        }
        
        return bestSequence.get(0);
        
    }
    
    private MOVE genetic(Game game, long timeDue){
        MOVE bestMove = myMove;
        
        
        int current = game.getPacmanCurrentNodeIndex();
        MOVE next[] = game.getPossibleMoves(current);
        
        //Number of action sequences determined by initial number of possible moves
        //Begin creating action sequences
        ArrayList<ArrayList<MOVE>> actionSequences = new ArrayList<ArrayList<MOVE>>(next.length * geneticExpand);
        for(int x = 0; x < next.length * geneticExpand; x++)
        {
            actionSequences.add(new ArrayList<MOVE>());
            actionSequences.get(x).add(next[x/geneticExpand]);
        }
        
        //Fill the action sequences with random moves
        for(int x = 0; x < actionSequences.size(); x++)
        {
            //Game newState = game.copy();
            ArrayList<MOVE> currentSequence = actionSequences.get(x);
            //newState.advanceGame(currentSequence.get(0),new AggressiveGhosts().getMove());
            
            while(currentSequence.size() < MAX_DEPTH_EVOLUTION)
            {
                MOVE chosenMove = MOVE.values()[(int)(Math.random() * 5)];
                currentSequence.add(chosenMove);
            }
            
            x++;
        }
        
        
        //Now evaluate and evolve the action sequences
        ArrayList<MOVE> bestSequence = actionSequences.get(0);
        while(System.currentTimeMillis() < timeDue)
        {
            //Evaluate best and worst
            ArrayList<Game> results = new ArrayList<Game>(actionSequences.size());
            double bestEval = Double.NEGATIVE_INFINITY;
            double worstEval = Double.POSITIVE_INFINITY;
            int indexOfBest = 0;
            int indexOfWorst = 0;
            for(int x = 0; x < actionSequences.size(); x++)
            {
                Game currentGame = game;
                ArrayList<MOVE> currentSequence = actionSequences.get(x);
                for(MOVE currentMove : currentSequence)
                {
                    current = game.getPacmanCurrentNodeIndex();
                    next = game.getPossibleMoves(current);
                    boolean isInMoveSet = false;
                    for(MOVE nextMoves : next)
                    {
                        if(nextMoves == currentMove)
                        {
                            isInMoveSet = true;
                        }
                    }
                    
                    if(isInMoveSet)
                    {
                        currentGame.advanceGame(currentMove, new AggressiveGhosts().getMove());
                    }
                    else //use neutral if the current move isn't usable
                    {
                        currentGame.advanceGame(MOVE.NEUTRAL, new AggressiveGhosts().getMove());
                    }
                }
                
                double currentEval = eval(currentGame);
                if(currentEval > bestEval)
                {
                    bestEval = currentEval;
                    indexOfBest = x;
                }
                if (currentEval < worstEval)
                {
                    worstEval = currentEval;
                    indexOfWorst = x;
                }
            }
             
            //Assign the best sequence if time runs out
            bestSequence = actionSequences.get(indexOfBest);
            
            
            
             if(indexOfWorst == indexOfBest) //lazy error checking pls never happen
                break;
             
            //Best and worst action sequence has been decided
            //Now to evolve genetic style
             //The worst will be killed and replaced by a modified version the best
             //All of the rests will have their top halves replaced by the best
             //Then all but the best will be randomized slightly
            ArrayList<MOVE> geneticModifier = new ArrayList<MOVE>(bestSequence.subList(bestSequence.size()/2, bestSequence.size()));
            for(int x = 0; x < actionSequences.size(); x++)
            {
                ArrayList<MOVE> currentSequence = actionSequences.get(x);
                if(x == indexOfWorst)
                {
                    currentSequence = new ArrayList<MOVE>(bestSequence);
                }
                if(x != indexOfBest)
                {
                    currentSequence.subList(currentSequence.size()/2, currentSequence.size()).clear();
                    currentSequence.addAll(geneticModifier);
                    for(int indexOfMove = 1; indexOfMove < currentSequence.size(); indexOfMove++)
                    {
                        if(Math.random() < geneticRandomizerChance)
                        {
                            MOVE chosenMove = MOVE.values()[(int)(Math.random() * 5)];
                            currentSequence.set(indexOfMove, chosenMove);
                        }
                    }
                }
            }
            
        }
        
        current = game.getPacmanCurrentNodeIndex();
        next = game.getPossibleMoves(current);
        boolean isInMoveSet = false;
        for(MOVE nextMoves : next)
        {
            if(nextMoves == bestSequence.get(0));
            {
                isInMoveSet = true;
            }
        }

        if(isInMoveSet)
        {
            return bestSequence.get(0);
        }
        else //use neutral if the current move isn't usable
        {
            return MOVE.NEUTRAL;
        }
    }
    
    private static class replayCompare implements Comparator<replayData> {
        public int compare(replayData r1, replayData r2) {
            if (r1.averageDistance < r2.averageDistance) return -1;
            if (r2.averageDistance < r1.averageDistance) return 1;
            return 0;
        }
    }



    class replayData {
        int pacmanPosition;
        MOVE pacmanMove;
        int[] ghostPosition;
        MOVE[] ghostMove;
        double averageDistance;
        boolean moveAway;
        MOVE[] possibleMoves;
        MOVE closestEnemyMove = MOVE.NEUTRAL;
        
        
        public replayData(String setting) {
            String[] values = setting.split(",");
            
            pacmanPosition = Integer.parseInt(values[5]);
            pacmanMove = MOVE.valueOf(values[6]);
            ghostPosition = new int[]{Integer.parseInt(values[9]), Integer.parseInt(values[13]), 
                Integer.parseInt(values[17]), Integer.parseInt(values[21])};
            ghostMove = new MOVE[]{MOVE.valueOf(values[12]), MOVE.valueOf(values[16]), 
                MOVE.valueOf(values[20]), MOVE.valueOf(values[24])};
            
            Game state = new Game(0);
            state.setGameState(setting);
            
            averageDistance = 0;
            double closestEnemyDistance = Double.POSITIVE_INFINITY;
            for (int i = 0; i < ghostPosition.length; i++) {
                double tempDistance = state.getShortestPathDistance(pacmanPosition, ghostPosition[i]);
                if (tempDistance != -1 && tempDistance < closestEnemyDistance) {
                    closestEnemyDistance = tempDistance;
                    closestEnemyMove = ghostMove[i];
                }
                averageDistance += state.getShortestPathDistance(pacmanPosition, ghostPosition[i]);
            }
            averageDistance /= ghostPosition.length;
            
            state.advanceGame(pacmanMove, new AggressiveGhosts().getMove());
            
            double newAverageDistance = 0;
            for (int i = 0; i < ghostPosition.length; i++) {
                newAverageDistance += state.getShortestPathDistance(pacmanPosition, ghostPosition[i]);
            }
            newAverageDistance /= ghostPosition.length;
            
            if (newAverageDistance < averageDistance) {
                moveAway = true;
            } else {
                moveAway = false;
            }
            
            possibleMoves = state.getPossibleMoves(pacmanPosition);
            
            
        }
    }
    
    private ArrayList<replayData> understand(String filename) {
        ArrayList<String> replay = new ArrayList<String>();
		
        try {         	
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));	 
            String input = br.readLine();		
            
            while(input!=null) {
            	if (!input.equals("")) replay.add(input);
            	input=br.readLine();	
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
        
        ArrayList<replayData> history = new ArrayList();
        
        for (int i = 0; i < replay.size(); i++) {
            String data = replay.get(i);
            
            history.add(new replayData(data));
        }
        Collections.sort(history, new replayCompare());
        
        return history;
    }
    
    private int kNNSearch(int first, int last, double key) {
        if (first >= last) {
            if (last < 0) return first;
            if (first != 0) return first - 1;
            return first;
        }
        int mid = (first + last) / 2;
        if (historicalData.get(mid).averageDistance == key) {
            return mid;
        } else if (historicalData.get(mid).averageDistance > key) {
            return kNNSearch(first, mid - 1, key);
        } else {
            return kNNSearch(mid + 1, last, key);
        }
    }
    
    private MOVE kNN(Game game, int k, long timeDue) {
        MOVE bestMove = myMove;
        
        int current = game.getPacmanCurrentNodeIndex();
        
        double averageDistance = averageGhostDistance(game);
        
        int positionBottom = kNNSearch(0, historicalData.size(), averageDistance);
        int positionTop = positionBottom + 1;
        k--;
        int totalCount = k / 2;
        while (totalCount > 0 && k > 0) {
            if (positionBottom > 0) {
                if (historicalData.get(positionBottom).moveAway) {
                    totalCount--;
                }
                positionBottom--;
                k--;
                if (k <= 0) break;
            }
            
            if (positionTop < historicalData.size()) {
                if (historicalData.get(positionTop).moveAway) {
                    totalCount--;
                }
                positionTop++;
                k--;
            }
        }
        
        MOVE[] next = game.getPossibleMoves(current);
        if (totalCount > 0) {
            double closestAway = Double.POSITIVE_INFINITY;
            for (int index : game.getActivePillsIndices()) {
                double distanceAway = game.getShortestPathDistance(game.getPacmanCurrentNodeIndex(), index);
                if (distanceAway < closestAway) closestAway = distanceAway;
            }
            
            for (MOVE eachMove : next) {
                Game newState = game.copy();
                newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
                // if near to pill good;
                double nextClosestAway = Double.POSITIVE_INFINITY;
                    for (int index : newState.getActivePillsIndices()) {
                        double distanceAway = newState.getShortestPathDistance(newState.getPacmanCurrentNodeIndex(), index);
                        if (distanceAway < closestAway) nextClosestAway = distanceAway;
                    }
                    if (nextClosestAway <= closestAway) bestMove = eachMove;
            } 
        } else {
            for (MOVE eachMove : next) {
                Game newState = game.copy();
                newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
                if (averageGhostDistance(newState) < averageDistance) {
                    bestMove = eachMove;
                }
            }
        }
        
        lastMove = bestMove;
        return bestMove;

    }  
    private double eval(Game state) {
        if (state.gameOver()) return -9999;
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
                    score += 8000 * closeGhosts / distanceAway;
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
        score += state.getScore() * 8;
        score += state.getTotalTime() * 20;
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

    class tree {
        int[][][] scores; 
        
        public tree() {
            scores = new int[11][4][4];
        }
        
        int getIndexMax(int[] arr, boolean flag1, boolean flag2, boolean flag3, boolean flag4){
            int index = 0;
            int max = -1;
            if (flag1) {
                if (arr[0] > max) {
                    max = arr[0];
                    index = 0;
                }
            }
            if (flag2) {
                if (arr[1] > max) {
                    max = arr[1];
                    index = 1;
                }
            }
            if (flag3) {
                if (arr[2] > max) {
                    max = arr[2];
                    index = 2;
                }
            }
            if (flag4) {
                if (arr[3] > max) {
                    max = arr[3];
                    index = 3;
                }
            }
            return index;
        }
        
        public MOVE makeDecision(Game state, MOVE closestDir) {
            int dir = 0;
            if (closestDir == MOVE.LEFT) dir = 0;
            if (closestDir == MOVE.DOWN) dir = 1;
            if (closestDir == MOVE.RIGHT) dir = 2;
            if (closestDir == MOVE.UP) dir = 3;
            
            int decision = 0;
            Set<MOVE> moves = new HashSet();
            for (MOVE eachMove : state.getPossibleMoves(state.getPacmanCurrentNodeIndex())) {
                moves.add(eachMove);
            }
            
            if (moves.size() == 4) {
                decision = getIndexMax(scores[0][dir], true, true, true, true);
            } else if (moves.size() == 3) {
                if (!moves.contains(MOVE.DOWN)) {
                    decision = getIndexMax(scores[1][dir], true, false, true, true);
                } else if (!moves.contains(MOVE.RIGHT)) {
                    decision = getIndexMax(scores[2][dir], true, true, false, true);
                } else if (!moves.contains(MOVE.UP)) {
                    decision = getIndexMax(scores[3][dir], true, true, true, false);
                } else {
                    decision = getIndexMax(scores[4][dir], false, true, true, true);
                }
            } else {
                if (moves.contains(MOVE.DOWN)) {
                    if (moves.contains(MOVE.RIGHT)) {
                        decision = getIndexMax(scores[5][dir], false, true, true, false);
                    } else if (moves.contains(MOVE.UP)) {
                        decision = getIndexMax(scores[6][dir], false, true, false, true);
                    } else {
                        decision = getIndexMax(scores[7][dir], true, true, false, false);
                    }
                } else if (moves.contains(MOVE.RIGHT)) {
                    if (moves.contains(MOVE.UP)) {
                        decision = getIndexMax(scores[8][dir], false, false, true, true);
                    } else {
                        decision = getIndexMax(scores[9][dir], true, false, true, false);
                    }
                } else {
                    decision = getIndexMax(scores[10][dir], true, false, false, true);
                }
            }
            
            if (decision == 0) return MOVE.LEFT;
            if (decision == 1) return MOVE.DOWN;
            if (decision == 2) return MOVE.RIGHT;
            return MOVE.UP;
        }
        
        public void addCondition(Set<MOVE> moves, MOVE closestDir, MOVE pacmanMove) {
            int dir = 0;
            if (closestDir == MOVE.LEFT) dir = 0;
            if (closestDir == MOVE.DOWN) dir = 1;
            if (closestDir == MOVE.RIGHT) dir = 2;
            if (closestDir == MOVE.UP) dir = 3;
            
            int move = 0;
            if (pacmanMove == MOVE.LEFT) move = 0;
            if (pacmanMove == MOVE.DOWN) move = 1;
            if (pacmanMove == MOVE.RIGHT) move = 2;
            if (pacmanMove == MOVE.UP) move = 3;
            
            if (moves.size() == 4) {
                scores[0][dir][move]++;
            } else if (moves.size() == 3) {
                if (!moves.contains(MOVE.DOWN)) {
                    scores[1][dir][move]++;
                } else if (!moves.contains(MOVE.RIGHT)) {
                    scores[2][dir][move]++;
                } else if (!moves.contains(MOVE.UP)) {
                    scores[3][dir][move]++;
                } else {
                    scores[4][dir][move]++;
                }
            } else {
                if (moves.contains(MOVE.DOWN)) {
                    if (moves.contains(MOVE.RIGHT)) {
                        scores[5][dir][move]++;
                    } else if (moves.contains(MOVE.UP)) {
                        scores[6][dir][move]++;
                    } else {
                        scores[7][dir][move]++;
                    }
                } else if (moves.contains(MOVE.RIGHT)) {
                    if (moves.contains(MOVE.UP)) {
                        scores[8][dir][move]++;
                    } else {
                        scores[9][dir][move]++;
                    }
                } else {
                    scores[10][dir][move]++;
                }
            }
        }
    }
    
    public tree setupTree(ArrayList<replayData> data) {
        tree finalTree = new tree();
        for (int i = 0; i < data.size(); i++) {
            replayData cur = data.get(i);
            Set<MOVE> possibleMoves = new HashSet();
            
            for (MOVE eachMove : cur.possibleMoves) {
                possibleMoves.add(eachMove);
            }
            finalTree.addCondition(possibleMoves, cur.closestEnemyMove, cur.pacmanMove);
        }
        
        return finalTree;
    }
    
    private MOVE decisionTree(Game game, long timeDue) {
        MOVE bestMove = myMove;
        
        int current = game.getPacmanCurrentNodeIndex();
        
        double distanceAway = Double.POSITIVE_INFINITY;
        MOVE ghostDir = MOVE.NEUTRAL;
        for (GHOST g : GHOST.values()) {
            double distTemp = game.getShortestPathDistance(current, game.getGhostCurrentNodeIndex(g));
            if (distTemp != -1 && distTemp < distanceAway) {
                distanceAway = distTemp;
                ghostDir = game.getGhostLastMoveMade(g);
            }
        }
        
        bestMove = decisionMaking.makeDecision(game, ghostDir);
        
        lastMove = bestMove;
        return lastMove;
    }
    
    class Ratio{
        int success;
        int failure;
        
        public Ratio() {
            success = 0;
            failure = 0;
        } 
        
        public double getRatio() {
            return (double) success / ((double) success + (double) failure);
        }
    }
    
    private MOVE monteBFS(Game game, int maxJunction, long timeDue) {
        int current = game.getPacmanCurrentNodeIndex();
        MOVE next[] = game.getPossibleMoves(current);
        if (!game.isJunction(current)) {
            for (MOVE eachMove : next) {
                if (eachMove != lastMove && eachMove != lastMove.opposite()) {
                    lastMove = eachMove;
                    return lastMove;
                }
            }
            return lastMove;
        }
        
        double currentEval = eval(game);
        Queue<Node> states = new LinkedList();
        HashMap<MOVE, Ratio> possibleMoves = new HashMap();
        
        for (MOVE eachMove : next) {
            possibleMoves.put(eachMove, new Ratio());
            Game newState = game.copy();
            newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
            while (!newState.isJunction(newState.getPacmanCurrentNodeIndex())) {
                MOVE[] nextNext = newState.getPossibleMoves(newState.getPacmanCurrentNodeIndex());
                for (MOVE eachNextMove : nextNext) {
                    if (eachNextMove != newState.getPacmanLastMoveMade().opposite()) {
                        newState.advanceGame(eachNextMove, new AggressiveGhosts().getMove());
                        break;
                    }
                }
            }
            states.add(new Node(newState, eachMove, newState.getPacmanLastMoveMade(), 1, 0));
        }

        while (!states.isEmpty() && System.currentTimeMillis() < timeDue - 5) {
            Node curState = states.remove();
            current = curState.game.getPacmanCurrentNodeIndex();
            next = curState.game.getPossibleMoves(current);

            double evaluation = eval(curState.game);
            if (evaluation > currentEval) {
                possibleMoves.get(curState.initialMove).success++;
            } else {
                possibleMoves.get(curState.initialMove).failure++;
            }

            if (curState.depth < maxJunction) {
                for (MOVE eachMove : next) {
                    Game newState = curState.game.copy();
                    newState.advanceGame(eachMove, new AggressiveGhosts().getMove());
                    while (!newState.isJunction(newState.getPacmanCurrentNodeIndex())) {
                        MOVE[] nextNext = newState.getPossibleMoves(newState.getPacmanCurrentNodeIndex());
                        for (MOVE eachNextMove : nextNext) {
                            if (eachNextMove != newState.getPacmanLastMoveMade().opposite()) {
                                newState.advanceGame(eachNextMove, new AggressiveGhosts().getMove());
                                break;
                            }
                        }
                    }
                    states.add(new Node(newState, curState.initialMove, newState.getPacmanLastMoveMade(), curState.depth + 1, 0));
                }
            }
        }
        
        Iterator<MOVE> keySetIterator = possibleMoves.keySet().iterator();
        double goodRatio = -1; 
        while (keySetIterator.hasNext()) {
            MOVE key = keySetIterator.next();
            double newRatio = possibleMoves.get(key).getRatio();
            if (newRatio > goodRatio) {
                goodRatio = newRatio;
                lastMove = key;
            }
        }
        return lastMove;
    }
        
    public MOVE getMove(Game game, long timeDue) {
        //Place your game logic here to play the game as Ms Pac-Man
        
        //return breadthFirst(game, 50, timeDue);
        //return depthFirst(game, 50, timeDue);
        //return iterativeDeepening(game, 5, 50, timeDue);
        //return aStar(game, 50, timeDue);
        //return hillClimber(game, timeDue);
        //return simulatedAnnealing(game, timeDue);
        return evolution(game, timeDue);
        //return genetic(game, timeDue);
        //return alphaBetaPruning(game, timeDue);
        //return kNN(game, 10, timeDue);
        //return decisionTree(game, timeDue);
        //return monteBFS(game, 5, timeDue);
    }
}
