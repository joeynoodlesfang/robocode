/** Plan of attack - have multiple battle plans. 
 *  Two states and one action [offensive - trackfire, defensive -moveLeft, adjustAngleGun, action]
	Exploratory 	- (1) search for enemy, if find enemy give highest reward. That way, go back to this state
	Battle Plan 1.  - (1) Found enemy, attack . If hit enemy, award high reward. If miss enemy, award low reward. If get hit, negative reward
	Battle Plan 2. 	- (1) Found enemy, check distance. If close, fire hard, if far, move closer. 
	
	Joey: What sort of inputs?
	e.getBearing  - these two combine for bearingFromGun
	getGunHeading -
	getHeading
	getDistance
	
	Worklog
	2016-11-14
		- currently working on mimicking TrackFire's attacking abilities
		in particular, variables (done), onScannedRobot() (done), generateCurrentStateVector(), doAction()
		
		- implement reward system
		
		- implement simple defensive strategy (eg: Fire.java moves when hit)
		
	6:52 pm - Andy
		- added two more discretized levels for energy and distance: generateCurrentStateVector() (done)
		- added action for doAction()
		
 */

package MyRobots;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

import robocode.AdvancedRobot;
import robocode.BattleEndedEvent;
import robocode.DeathEvent;
import robocode.HitWallEvent;
import robocode.RobocodeFileOutputStream;
import robocode.ScannedRobotEvent;

public class LUTTrackfire extends AdvancedRobot{
	/*
	 * SAV Change Rules:
	 * 1. update STATEACTION VARIABLE
	 * 2. update roboLUT initialization
	 * 3. update roboLUTDimensions
	 * 4. If adding or deleting states: Change for loops in import and export functions 
	 *    (twice in import including zeroLUT loop, once in export)
	 * 5. Update the corresponding dimension in generateCurrentStateVector()
	 * 6. if adding or deleting states: In getMax(), update the roboLUT access in the maxQ for loop.
	 * 7. if adding or deleting states: In calcNewPrevQVal(), update the roboLUT access.
	 * 8. if adding or deleting states: In updateLUT(), update roboLUT access as well as debug
	 * 9. if adding or deleting actions: In doAction(), edit accordingly.
	 */
	/**
	 * FINALS (defines)
	 */
	 //variables for the q-function. Robot will NOT change learning pattern midfight.
    private static final double alpha = 0.1;                 //to what extent the newly acquired information will override the old information.
    private static final double gamma = 0.8;                 //importance of future rewards
    
    //policy: Greedy if == 1, Exploratory if 0.
    private static final int greedy = 0;
    private static final int exploratory = 1;
    
    /**
	 * STATEACTION VARIABLES for stateAction ceilings.
	 */
    private static final int num_actions = 1; 
//    private static final int defensive_states = 1; //Joey: trying to implement inputs that might make ur NN easier later
//    private static final int offensive_states = 1; 
    private static final int enemyBearingFromGun_states = 2; // bearingFromGun < 3, bearingFromGun > 3
    private static final int enemyDistance_states = 3;		//distance < 33, 33 < distance < 66, 66 < distance < 75, 75 < distance < 100
    private static final int enemyEnergy_states = 3;		//energy < 33, 33 < distance < 66, 66 < distance < 75, 75 < distance < 100
    
    // LUT table stored in memory.
    private static double[][][][] roboLUT 
        = new double
        [num_actions]
        [enemyBearingFromGun_states]
        [enemyDistance_states]
        [enemyEnergy_states];
    
    // Dimensions of LUT table, used for iterations.
    private static int[] roboLUTDimensions = {
            num_actions, 
            enemyBearingFromGun_states,
            enemyDistance_states,
            enemyEnergy_states};
    
    // Stores current reward for action.
    private double reward = 0.0; //only one reward variable to brief offensive and defensive maneuvers
    
    // Stores current and previous stateAction vectors.
    private int currentStateActionVector[] = new int [roboLUTDimensions.length];
    private int prevStateActionVector[] = new int [roboLUTDimensions.length]; 
     
    //variables used for getMax.
    private int [] arrAllMaxActions = new int [num_actions]; //array for storing all actions with maxqval
    private int actionChosenForQValMax = 0; //stores the chosen currSAV with maxqval before policy
    private double qValMax = 0.0; // stores the maximum currSAV QMax

    //chosen policy. greedy or exploratory.
    private static int policy = greedy;
    
    //enemy information
    private double enemyBearingFromGun = 0.0;
    private double enemyDistance = 0.0;
    private double enemyEnergy = 0.0;
    
    
    /**
     * FLAGS AND COUNTS
     */
    
    //debug flag.
    static private boolean debug = false;
    
    // Flag used for functions importLUTData and exportLUTData. Assists in preventing overwrite.
    private boolean repeatFlag_importexportLUTData = false; 
    
    //Flag used if user desires to zero LUT at the next battle. 
    static private boolean zeroLUT = true;

    //@@@@@@@@@@@@@@@ RUN & EVENT CLASS FUNCTIONS @@@@@@@@@@@@@@@@@    
    
    /**
     * @name: 		run
     * @purpose:	1. Initializes robot colour
     * 				2. Imports LUT data from file into local memory array.
     * 				4. Runs LUT-based learning code. See fxn learningLoop for details. 
     * @param:		n
     * @return:		n
     */
    public void run() {
        
        // Sets Robot Colors.
        setColors();
        
        if (debug) {
        	out.println("I have been a dodger duck (robot entered run)"); 
        }
        
        // Import dat.
        repeatFlag_importexportLUTData = importLUTData(repeatFlag_importexportLUTData);
        
        learningLoop();  
    }
    /**
     * @name: 		onBattleEnded
     * @purpose: 	1. 	Exports LUT data from memory to .dat file, which stores Qvalues 
     * 				   	linearly. Exporting will occur only once per fight, either during 
     * 				   	death or fight end.
     * @param:		1.	BattleEndedEvent class from Robot
     * @return:		n
     */
    public void onBattleEnded(BattleEndedEvent event){
        repeatFlag_importexportLUTData = exportLUTData(repeatFlag_importexportLUTData);
    }
    
    /**
     * @name: 		onDeath
     * @purpose: 	1. 	Exports LUT data from memory to .dat file, which stores Qvalues 
     * 				   	linearly. Exporting will occur only once per fight, either during 
     * 				   	death or fight end.
     * @param:		1.	DeathEvent class from Robot
     * @return:		n
     */
    public void onDeath(DeathEvent event){
        repeatFlag_importexportLUTData = exportLUTData(repeatFlag_importexportLUTData);
    }

    /**
     * @name:		onScannedRobot
     * @purpose:	1. determine enemy bearing and distance
     * @param:		ScannedRobotEvent event
     * @return:		none, but updates:
     * 				1. getGunBearing
     * 				2. enemyDistance
     */
    public void onScannedRobot(ScannedRobotEvent event){
    	enemyBearingFromGun = normalRelativeAngleDegrees(event.getBearing() + getHeading() - getGunHeading());
    	enemyDistance = event.getDistance();
    	enemyEnergy = event.getEnergy();
    	reward += 20;
    	learningLoop(); 
    	
    }
//    /**
//     * @name: 		onHitWall
//     * @purpose: 	1. Updates reward. -10
//     * 				2. Invoke LearningLoop.
//     * @param:		1. HitWallEvent class from Robot
//     * @return:		n
//     */   
//    public void onHitWall(HitWallEvent e) {
//    	if (debug) {
//    		System.out.println("HIT WALL " + Arrays.toString(currentStateActionVector));
//    	}
//        
//    	reward -= 10;	
//        learningLoop();
//    }
    //@@@@@@@@@@@@@@@ OTHER INVOKED CLASS FUNCTIONS @@@@@@@@@@@@@@@@@
    
    /** 
     * @name: 		setColors()
     * @purpose: 	Sets robot colour.  
     * @param: 		none
     * @return: 	none
     */
    public void setColors() {
        setBodyColor(Color.pink);
        setGunColor(Color.green);
        setRadarColor(Color.blue);
        setScanColor(Color.white);
        setBulletColor(Color.red);
    }
    
    /**
     * @name:		learningLoop
     * @purpose:	perform continuous reinforcement learning.
     * 				Reinforcement learning involves following steps:
     * 				1. Store the last state and action (currentSAV -> prevSAV)
     * 				2. Use information from the environment to determine the current state.
     * 				(3. punish robot for not changing state [not in original qfunction])
     * 				4. Perform QFunction (detailed further in function).
     * 				5. Perform chosen action.
     * 				6. Reset rewards. IE: all events affect reward only once unless specified.
     * 				7. Repeat step 1-6. 
     * @param:		n
     * @return:		n
     */
    public void learningLoop(){
    	
    	while (true) {
        	copyCurrentSAVIntoPrevSAV();
        	generateCurrentStateVector();
        	qFunction(); 
        	doAction(); 
        	resetReward();
        }
    }
    
    /**
     * @name:		copyCurrentSAVIntoPrevSAV
     * @purpose:	Copies array currentStateActionVector into array prevStateActionVector
     * @param:		n, but uses:
     * 				1. currentStateActionVector
     * @return:		n
     */
    public void copyCurrentSAVIntoPrevSAV(){
    	for (int i = 0; i < prevStateActionVector.length; i++) {
    		prevStateActionVector[i] = currentStateActionVector[i];
    	}
    }
    
    /**
     * @name: 		generateCurrentStateVector - discretize states here. 
     * @purpose: 	1. gets state values from battlefield. 
     * 				2. discretize. 
     * 				3. Update array of current stateAction vector.  
     * @param: 		n, but uses:
     * 				1. bearingFromGun
     * @return: 	none
     */
    public void generateCurrentStateVector(){
        //Dimension 1: input: bearingFromGun
    	if (enemyBearingFromGun < 3) {
    		currentStateActionVector[1] = 0;
    	}
    	else {
    		currentStateActionVector[1] = 1;
    	}
    	//Dimension 2: input: enemyDistance 
    	if (enemyDistance <= 33){
    		currentStateActionVector[2] = 0;
    	}
    	else if (enemyDistance > 33 && enemyDistance <= 66 ){
    		currentStateActionVector[2] = 1;
    	}
    	else if (enemyDistance > 66 && enemyDistance <= 100 ){
    		currentStateActionVector[2] = 2;
    	}   	
    	//Dimension 3: input: enemyEnergy
    	if (enemyEnergy <= 33){
    		currentStateActionVector[3] = 0;
    	}
    	else if (enemyEnergy > 33 && enemyEnergy <= 66 ){
    		currentStateActionVector[3] = 1;
    	}
    	else if (enemyEnergy > 66 && enemyEnergy <= 100 ){
    		currentStateActionVector[3] = 2;
    	}          
    }
    /**
     * @name:		qFunction
     * @purpose: 	1. Obtain the action in current state with the highest q-value, 
     * 				   and its associated q-value. 
     * 				2. Calculate new prev q-value. 
     * 				3. Update prevSAV with this q-value in LUT. 
     * @param: 		none, but uses:
     * 				1.	double reward 
     * 				2.	int currentStateVector[] already discretized (size numStates)
     * 				3.	double[].. LUT table, 
     * 				4.	int [] currentStateActionVector. 
     * @return: 	n
     */
    public void qFunction(){
                                    //call the GetMax function below to get maximum of all actions in that state
       getMax(); 
       double prevQVal = calcNewPrevQVal();
       updateLUT(prevQVal);
       
    }

    /**
     * @name:		getMax()
     * @purpose: 	1. Obtain the action in current state with the highest q-value, 
     * 				   and its associated q-value. 
	 *					a. Start current max Q value at lower than obtainable value.
	 *					b. Cycle through all actions in current SAV, recording max q-values.
	 *						i. if indexQVal > currMax:
	 *							(1) Update currMax
	 *							(2) Set numMaxActions = 1.
	 *							(3) Store the action index into arrAllMaxActions[numMaxActions-1]
	 *						ii. if indexQVal == currMax:
	 *							(1) numMaxActions++
	 *							(2) Store the action index into arrAllMaxActions[numMaxActions-1]
	 *						iii. if indexQVal < currMax:
	 *							ignore.
	 *					c. record chosen action. If multiple actions with max q-values, 
	 *					   randomize chosen action.
	 *						i. if numMaxActions > 1, 
	 *						   randomly select between 0 and numMaxActions - 1. The randomed 
	 *						   number will correspond to the array location of the chosen
	 *						   action in arrAllMaxActions. 
	 *						ii. actionChosenForQValMax = arrAllMaxActions[randomed number]
	 *					d. record associated q-value.
     * @param: 		none, but uses:
     * 				1.	current SAV[].
     * 				2.	roboLUT 
     * 				3.	currQArrayMax(Joey: can we get replace this) 
     * @return: 	n
     */
    public void getMax() {
        double currMax = -100.0;
        double indexQVal = 0.0;
        int numMaxActions = 0;
        int randMaxAction = 0;

        if (debug) {
        	out.println("@getMax()");
        	out.println("reward " + reward);
        	out.println("Cycling actions in currSAV for maxQ: ");
        }   
        
        for (int i = 0; i < num_actions; i++){
            indexQVal = roboLUT[i][currentStateActionVector[1]][currentStateActionVector[2]][currentStateActionVector[3]];
            
            if (indexQVal > currMax){
            	currMax = indexQVal;
            	numMaxActions = 1;
            	arrAllMaxActions[numMaxActions-1] = i;
            }
            else if (indexQVal == currMax){
            	numMaxActions++;
            	arrAllMaxActions[numMaxActions-1] = i;
            }
            
            if (debug) {
            	out.print(i + ": " + indexQVal + "  ");
            }
        } 
        
        if (numMaxActions > 1) {
        	randMaxAction = (int)(Math.random()*(numMaxActions)); //math.random randoms btwn 0.0 and 0.999. Add 1 to avoid truncation after typecasting to int.
        	
        	if (debug) {
            	System.out.println("randMaxAction " + randMaxAction + " numMaxActions " + numMaxActions);
            }
        }
        
        actionChosenForQValMax = arrAllMaxActions[randMaxAction];
        qValMax = currMax;
        
        if (debug) {
        	System.out.println("Action Chosen: " + actionChosenForQValMax  + " qVal: " + qValMax);
        }
    }
    
    /**
     * @name		calcNewPrevQVal
     * @purpose		1. Calculate the new prev q-value based on Qvalue function.
     * @param		n, but uses:
     * 				1. qValMax
     * @return		prevQVal
     */
    public double calcNewPrevQVal(){

        double prevQVal = roboLUT[prevStateActionVector[0]]
        						 [prevStateActionVector[1]]
        						 [prevStateActionVector[2]]		 
        						 [prevStateActionVector[3]];
        
        prevQVal += alpha*(reward + gamma*qValMax - prevQVal);
        
        return prevQVal;
        
    }
    
    /**
     * @name:		updateLUT
     * @purpose:	1. Update prevSAV in LUT with the calculated prevQVal.
     * 				2. Update curr state with correct action based on policy (greedy or exploratory).
     * @param:		1. prevQVal
     * @return:		n
     */
    public void updateLUT(double prevQVal){
        int valueRandom = 0;
        
        roboLUT[prevStateActionVector[0]]
         	   [prevStateActionVector[1]]
         	   [prevStateActionVector[2]]
         	   [prevStateActionVector[3]]
         					  = prevQVal;
        
        if (debug) {
	        out.println("prev " + Arrays.toString(prevStateActionVector));
	        out.println("prevQVal" +  roboLUT[prevStateActionVector[0]][prevStateActionVector[1]][prevStateActionVector[2]][prevStateActionVector[3]]);
        }
        
        //Choosing next action based on policy.
        valueRandom = (int)(Math.random()*(num_actions));
     
        if (policy == exploratory) {
        	currentStateActionVector[0] = valueRandom;
        }
        else{ 
        	currentStateActionVector[0] = actionChosenForQValMax;
        }
    }
    
    /**
     * @name:		doAction
     * @purpose: 	Converts state Action vector into action by reading currentSAV[0]
     * @param: 		n, but uses:
     * 				1. Array currentSAV.
     * @return:		n
     */
    public void doAction(){
    	
      if (currentStateActionVector[0] == 0) {
          fire(2); 
      }

    }
    
	/**
     * @name:		resetReward
     * @purpose: 	Resets reward to 0.
     * @param: 		n, but uses:
     * 				1. reward
     * @return:		n
     */
    public void resetReward(){
        
        reward = 0;
        
    }
    /**
     * @name:		importLUTData
     * @author:		95% sittingduckbot
     * @purpose: 	1. Imports LUT data from .dat file. 
     * @param: 		1. repeatFlag
     * @return:		1. repeatFlag
     */
    public boolean importLUTData(boolean repeatFlag){
        if (repeatFlag == false) {
            try {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(getDataFile("LUTTrackfire.dat")));
                    for (int p0 = 0; p0 < roboLUTDimensions[0]; p0++) {
                        for (int p1 = 0; p1 < roboLUTDimensions[1]; p1++) {
                        	for (int p2 = 0; p2 < roboLUTDimensions[2]; p2++) {
                        		for (int p3 = 0; p3 < roboLUTDimensions[2]; p3++) {
                        		roboLUT[p0][p1][p2][p3] = Double.parseDouble(reader.readLine());
                        		}
                        	}
                        }
                    }
                    // zeroes the LUT.
                    if (zeroLUT) {
	                    for (int p0 = 0; p0 < roboLUTDimensions[0]; p0++) {
	                        for (int p1 = 0; p1 < roboLUTDimensions[1]; p1++) {
	                        	for(int p2 = 0; p2 < roboLUTDimensions[2]; p2++){
	                        		for (int p3 = 0; p3 < roboLUTDimensions[2]; p3++) {
	                            		roboLUT[p0][p1][p2][p3] = 0;
	                            	}
	                        	}
	                        }
	                    }
                    }
                    if (debug) {
                    	out.println("Imported LUT data");
                    }
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }
            } 
            catch (IOException e) {
                // Error0x01: error in file reading
                out.println("Something done fucked up (Error0x01 error in file reading)");
            } 
            catch (NumberFormatException e) {
                // Error0x02: error in int conversion
                out.println("Something done fucked up (Error0x02 error in int conversion)");
            }
        }
        
        repeatFlag = true;
        return repeatFlag;
    }
    
    /**
     * @name:		exportLUTData
     * @author:		99% sittingduckbot
     * @purpose: 	1. Exports local LUT from memory to .dat file. 
     * @param: 		1. repeatFlag
     * @return:		1. repeatFlag
     */
    public boolean exportLUTData(boolean repeatFlag){
        
        if (repeatFlag == true) {
            out.println("wewhat");
            PrintStream w = null;
            try {
                w = new PrintStream(new RobocodeFileOutputStream(getDataFile("LUTTrackfire.dat")));
    
                for (int p0 = 0; p0 < roboLUTDimensions[0]; p0++) {
                    for (int p1 = 0; p1 < roboLUTDimensions[1]; p1++) {
                    	for(int p2 = 0; p2 < roboLUTDimensions[2]; p2++){
                    		for (int p3 = 0; p3 < roboLUTDimensions[2]; p3++) {
                    			w.println(roboLUT[p0][p1][p2][p3]);
                    		}
                    	}
                    }
                }
                
                // PrintStreams don't throw IOExceptions during prints, they simply set a flag.... so check it here.
                if (w.checkError()) {
                    //Error 0x03: cannot write
                    out.println("Something done fucked up (Error0x03 cannot write)");
                }
            } catch (IOException e) {
                out.println("IOException trying to write: ");
                e.printStackTrace(out); //Joey: lol no idea what this means
            } finally {
                if (w != null) {
                    w.close();
                }
            }
        }      
        
        repeatFlag = false;
        return repeatFlag;
    }
    
}