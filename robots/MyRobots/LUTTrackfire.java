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
		
	2016-11-15
		- added rewards for dying and winning 
		- added "onHitBullet" event and "Hit" event to give rewards and to let TF learn. 
		- added two more actions... not sure if any of this makes sense though.. it feels very random. 
		- if gets hit by bullet, it should move away! 
	New updates
	
	2:33 pm 
		- execute plan from LUTplan.xlsx
		- update scannedRobot(); 
		- update onHitBullet() event. 
		- update actions
		
	3:51 pm. 
		- reward system 
			-positiveReward for hitting an enemy
			-negativeReward for getting hit. 
			-negativeTerminalReward
			-positiveTerminalReward
	1:51 am
		- added states  not really working. 
		
	2016-11-18 - j
		- Implementing some weird useless shit to reminicse about embedded - read at ur own peril
		- write exports, add config lines in current txt
		- change run fxn
 */

package MyRobots;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

import mainInterface.LUTInterface;
import robocode.AdvancedRobot;
import robocode.BattleEndedEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.DeathEvent;
import robocode.HitWallEvent;
import robocode.RobocodeFileOutputStream;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;

public class LUTTrackfire extends AdvancedRobot implements LUTInterface{
	/*
	 * SAV Change Rules:
	 * 1. update STATEACTION VARIABLES
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
    private static final double alpha = 0.2;                //to what extent the newly acquired information will override the old information.
    private static final double gamma = 0.8;                //importance of future rewards
    private static final double epsilon = 0.0; 				//degree of exploration 
    
    //policy:either greedy or exploratory or SARSA 
    private static final int greedy = 0;
    private static final int exploratory = 1;
    private static final int SARSA = 2;
    
    private static final short CONFIGMASK_ZEROLUT  = 				0x0001;
    private static final short CONFIGMASK_VERIFYSETTINGSAVAIL = 	0x4000;
    private static final short CONFIGMASK_FILE_LUTTrackfire =		0x0010;
    private static final short CONFIGMASK_FILE_WinLose = 			0x0020;
    
    private static final int importDATA_SUCCESS = 					0x00;
    private static final int importDATA_ERROR_0x01 = 				0x01;
    private static final int importDATA_ERROR_0x02 = 				0x02;
    private static final int importDATA_ERROR_0x03 = 				0x03;
    private static final int importLUTDATA_ERROR_0x04 =				0x04;
    private static final int importWINLOSE_ERROR_0x05 =				0x05;
    
    String strLUT = "LUTTrackfire.dat";
    String strWinLose = "winlose.dat";
    String strTest = "stringTest.dat";
    
    /**
	 * STATEACTION VARIABLES for stateAction ceilings.
	 */
    private static final int num_actions = 36; 

    private static final int enemyBearingFromGun_states = 3; 					// bearingFromGun < 3, bearingFromGun > 3
    private static final int offensiveFiringDirectionalBehaviour_actions = 1;	// not implemented yet
    private static final int offensiveFiringStrengthBehaviour_actions = 1;		// not implemented yet
    private static final int enemyDistance_states = 3;							//distance < 33, 33 < distance < 66, 66 < distance < 75, 75 < distance < 100
    private static final int myEnergy_states = 3;								//energy < 33, 33 < distance < 66, 66 < distance < 75, 75 < distance < 100
   
    /**
     * FLAGS AND COUNTS
     */
    
    //debug flag.
    static private boolean debug = false;  
    static private boolean debug_doAction = true;
    
    // Flag used for functions importLUTData and exportLUTData. Assists in preventing overwrite.
    static private boolean flag_preventMultipleLUTDataImports = false; 
    
    // printout error flag
    static private int flag_error = 0;

//    //Flag used if user desires to zero LUT at the next battle. 
//    static private boolean zeroLUT = false; 
    
    /**
     *  OTHER GLOBALS
     */
    

    // LUT table configuration information, stored in the first line of .dat
    static private short configDatFile = 0; 
    
    // LUT table stored in memory.
    private static double [][][][][][] roboLUT //?Joey: should these be static private? is there a difference? 
        = new double
        [num_actions]
        [enemyBearingFromGun_states]
        [offensiveFiringDirectionalBehaviour_actions]
        [offensiveFiringStrengthBehaviour_actions]
        [enemyDistance_states]
        [myEnergy_states];
    
    // Dimensions of LUT table, used for iterations.
    private static int[] roboLUTDimensions = {
        num_actions, 
        enemyBearingFromGun_states,
        offensiveFiringDirectionalBehaviour_actions,
        offensiveFiringStrengthBehaviour_actions,
        enemyDistance_states,
        myEnergy_states};
    
    // Stores current reward for action.
    private double reward = 0.0; //only one reward variable to brief both offensive and defensive maneuvers
    
    // Stores current and previous stateAction vectors.
    private int currentStateActionVector[] = new int [roboLUTDimensions.length];
    private int prevStateActionVector[]    = new int [roboLUTDimensions.length]; 
     
    //variables used for getMax.
    private int [] arrAllMaxActions = new int [num_actions]; //array for storing all actions with maxqval
    private int actionChosenForQValMax = 0; //stores the chosen currSAV with maxqval before policy
    private double qValMax = 0.0; // stores the maximum currSAV QMax

    //chosen policy. greedy or exploratory (or SARSA). 
    private static int policy = exploratory;
    
    //enemy information
    private double enemyDistance = 0.0;
    private double enemyBearingFromRadar = 0.0;
    private double enemyBearingFromGun = 0.0;
    private double enemyBearingFromHeading = 0.0;
    private double enemyEnergy = 0.0;
    
    //my information
    private double myHeading = 0.0; 
    
    private int totalFights = 0;
    private int[] battleResults = new int [20000];
	
    
    

    
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
        
        // Import data. ->Change imported filename here<-
        
        flag_error = importData("strTest.dat");
        if( flag_error != importDATA_SUCCESS) {
        	out.println("ERROR: " + flag_error);
        }
        
        importWinLose();
        
        //set gun and radar for robot turn
        setAdjustGunForRobotTurn(true);
    	setAdjustRadarForGunTurn(true);	
    	setAdjustRadarForRobotTurn(true);

    	// infinite loop
        for(;;){
        	setTurnRadarRight(45);
    		execute();
        }
         
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
        flag_preventMultipleLUTDataImports = exportLUTData(flag_preventMultipleLUTDataImports);    
    }
    
    /**
     * @name: 		onDeath
     * @purpose: 	1. 	Exports LUT data from memory to .dat file, which stores Qvalues 
     * 				   	linearly. Exporting will occur only once per fight, either during 
     * 				   	death or fight end.
     * 				2.  Sets a terminal reward of -100 
     * @param:		1.	DeathEvent class from Robot
     * @return:		n
     */
    public void onDeath(DeathEvent event){
        flag_preventMultipleLUTDataImports = exportLUTData(flag_preventMultipleLUTDataImports);
        exportWinLose(0);
        reward -=100; 
        learningLoop(); //?Joey: why is learningLOop called here? for terminal reward?
    }
    
    /**
     * @name: 		onWin
     * @purpose: 	1. 	Exports LUT data from memory to .dat file, which stores Qvalues 
     * 				   	linearly. Exporting will occur only once per fight, either during 
     * 				   	death or fight end.
     * 				2.  Sets a terminal reward of +100 
     * @param:		1.	WinEvent class from Robot
     * @return:		n
     */    
	public void onWin(WinEvent e) {
		flag_preventMultipleLUTDataImports = exportLUTData(flag_preventMultipleLUTDataImports);
		exportWinLose(1);
		reward +=100; 
		learningLoop(); //?Joey: why is learningLoop called here?
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
		enemyBearingFromRadar = getHeading() + event.getBearing() - getRadarHeading();
		enemyBearingFromGun = getHeading() + event.getBearing() - getGunHeading();
		enemyBearingFromHeading = event.getBearing();
		enemyDistance = event.getDistance(); 
//		out.println("enemyDistance " + enemyDistance);
		enemyEnergy = event.getEnergy(); 
    	learningLoop();
    }

	/**
	* @name: 		onBulletMissed
	* @purpose: 	1. Updates reward. -10 if bullet misses enemy
	* @param:		1. HItBulletEvent class from Robot
	* @return:		n
	*/      
    public void onBulletMissed(BulletMissedEvent event){
    	reward -= 5;    	
    }
    
	/**
	* @name: 		onBulletHit
	* @purpose: 	1. Updates reward. +30 if bullet hits enemy
	* 				2. Update the values of heading and energy of my robot 
	* @param:		1. HItBulletEvent class from Robot
	* @return:		n
	*/     
    public void onBulletHit(BulletHitEvent e){
    	reward += 50; 
		myHeading = getHeading(); 
		enemyEnergy = e.getEnergy(); 
		learningLoop(); //?Joey: why is learningLoop called here
    }
    
    /**
     * @name: 		onHitWall
     * @purpose: 	1. Updates reward. -10
     * 				2. Updates heading and energy levels. 
     * @param:		1. HitWallEvent class from Robot
     * @return:		n
     */   
    public void onHitWall(HitWallEvent e) {
    	reward -= 10;	
    	myHeading = getHeading(); 
    }
    
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
  //      	resetReward();
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
        if (debug_doAction || debug) {
      	  out.println("currentStateActionVector" + Arrays.toString(currentStateActionVector));
        }
        //Dimension 1: input: bearingFromGun
    		currentStateActionVector[1] = (int)(getHeading() / 45);
    		if (currentStateActionVector[1] < 3){
    			currentStateActionVector[1] = 0;     		
    		}
    		else if (currentStateActionVector[1] > 3){
    			currentStateActionVector[1] = 1; 
    		}
    		else{
    			currentStateActionVector[1] = 2; 
    		}
    	//Dimension 2: input: offensiveFiringDirectionalBehaviour_actions
    		currentStateActionVector[2] = 0; 
    	//Dimension 3: input: offensiveFiringStrengthBehaviour_actions
    		currentStateActionVector[3] = 0  ;
    	//Dimension 4: input: enemyDistance_states
    		currentStateActionVector[4] = (int) enemyDistance;
    		if (currentStateActionVector[4] < 33){
    			currentStateActionVector[4] = 0; 
    		}
    		else if (currentStateActionVector[4] >= 33 && currentStateActionVector[4]  < 66){
    			currentStateActionVector[4] = 1; 
    		}
    		else if (currentStateActionVector[4] > 66){
    			currentStateActionVector[4] = 2; 
    		}
    		//Dimension 5: input: Energy_states
    		currentStateActionVector[5] = (int) enemyEnergy;
    		if (currentStateActionVector[5] < 33){
    			currentStateActionVector[5] = 0; 
    		}
    		else if (currentStateActionVector[5] >= 33 && currentStateActionVector[5]  < 66){
    			currentStateActionVector[5] = 1; 
    		}
    		else if (currentStateActionVector[5] > 66){
    			currentStateActionVector[5] = 2; 
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
            indexQVal = roboLUT[i][currentStateActionVector[1]][currentStateActionVector[2]][currentStateActionVector[3]][currentStateActionVector[4]][currentStateActionVector[5]];
            
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
        						 [prevStateActionVector[3]]
        						 [prevStateActionVector[4]]
        						 [prevStateActionVector[5]];
        
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
        
        double selectRandom = 0;
        
        roboLUT[prevStateActionVector[0]]
         	   [prevStateActionVector[1]]
         	   [prevStateActionVector[2]]
         	   [prevStateActionVector[3]]
         	   [prevStateActionVector[4]]
         	   [prevStateActionVector[5]]
         					  = prevQVal;
        
        if (debug) {
	        out.println("prev " + Arrays.toString(prevStateActionVector));
	        out.println("prevQVal" +  roboLUT[prevStateActionVector[0]][prevStateActionVector[1]][prevStateActionVector[2]][prevStateActionVector[3]][prevStateActionVector[4]][prevStateActionVector[5]]);
        }
        
        //Choosing next action based on policy.
        valueRandom = (int)(Math.random()*(num_actions));
     
//        /* used for exploratory */
//        if (policy == SARSA) {
//        	currentStateActionVector[0] = valueRandom;
//        }
//        
//        else if(policy == exploratory) {
//        	currentStateActionVector[0] = (Math.random() > epsilon ? actionChosenForQValMax : valueRandom);
//        }
//        
//        else{ 
//        	currentStateActionVector[0] = actionChosenForQValMax;
//        }
        
        //Choosing next action based on policy.
        valueRandom = (int)(Math.random()*(num_actions));
     
        if (policy == exploratory) {
        	currentStateActionVector[0] = valueRandom;
        }
        else if (policy == SARSA){ 
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
    	
    	setTurnRadarRight(normalRelativeAngleDegrees(enemyBearingFromRadar));
    	
    	//maneuver behaviour (chase-offensive/defensive)
    	if ((currentStateActionVector[0])%4 == 0) {
    		setTurnRight(enemyBearingFromHeading);
    		setAhead(50);
    	}
    	else if((currentStateActionVector[0])%4 == 1){
    		setTurnRight(enemyBearingFromHeading);
    		setAhead(-50);
    	}
    	else if((currentStateActionVector[0])%4 == 2){
    		setTurnRight(normalRelativeAngleDegrees(enemyBearingFromHeading - 90));
    		setAhead(50);
    	}
    	else if((currentStateActionVector[0])%4 == 3){
    		setTurnRight(normalRelativeAngleDegrees(enemyBearingFromHeading - 90));
    		setAhead(-50);
    	}
    	
    	if ( ((currentStateActionVector[0])/4) %3 == 0){
    		setFire(1);
    	}
    	else if ( ((currentStateActionVector[0])/4) %3 == 1){
    		setFire(2);
    	}
    	else if ( ((currentStateActionVector[0])/4) %3 == 2){
    		setFire(3);
    	}
    	
    	//firing behaviour (to counter defensive behaviour)
    	if ((currentStateActionVector[0])/12 == 0){
    		setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun));
    	}
    	
    	else if ((currentStateActionVector[0])/12 == 1){
    		setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun + 20));
    	}
    	
    	else if ((currentStateActionVector[0])/12 == 2){
    		setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun - 20));
    	}
      
      
    	scan();
    	execute();
     
      if (debug_doAction || debug) {
    	  out.println("currentStateActionVector" + Arrays.toString(currentStateActionVector));
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
     * @name:		importData
     * @author:		partly written in sittingduckbot
     * @purpose: 	1. Imports data from .dat file. 
     * @param: 		1. stringname of file desired to be written
     * 				also uses:
     * 				1. bool flag_preventMultipleLUTDataImports, static flag for preventing multiple imports by multiple instances of robot (hopefully?).
     * @return:		1. int importLUTDATA success/error;
     */
    public int importData(String strImported){
        if (flag_preventMultipleLUTDataImports == false) {
            try {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(getDataFile(strImported)));
                    configDatFile = (short)Integer.parseInt(reader.readLine());
                    if ((configDatFile & CONFIGMASK_VERIFYSETTINGSAVAIL) != CONFIGMASK_VERIFYSETTINGSAVAIL) {
                    	
                    	out.println("Import aborted (file not configured properly)");
                    	
                    	return importDATA_ERROR_0x03;
                    }
                    else {
                    	// different commands between files
                    	if ((configDatFile & CONFIGMASK_FILE_LUTTrackfire) == CONFIGMASK_FILE_LUTTrackfire) {
                    		if (strImported != "LUTTrackfire.dat") {
                    				if (debug) {
                    					out.println ("Import aborted (Imported wrong file - file was labelled LUTTrackfire.dat)");
                    				}
                    			return importLUTDATA_ERROR_0x04;
                    		}
	                    	if ((configDatFile & CONFIGMASK_ZEROLUT) == CONFIGMASK_ZEROLUT) {
	                    		out.println("zeroLUT");
	    	                    for (int p0 = 0; p0 < roboLUTDimensions[0]; p0++) {
	    	                        for (int p1 = 0; p1 < roboLUTDimensions[1]; p1++) {
	    	                        	for(int p2 = 0; p2 < roboLUTDimensions[2]; p2++){
	    	                        		for (int p3 = 0; p3 < roboLUTDimensions[3]; p3++) {
	    		                        		for (int p4 = 0; p4 < roboLUTDimensions[4]; p4++) {
	    	                        				for (int p5 = 0; p5 < roboLUTDimensions[5]; p5++) {
	    	                        					roboLUT[p0][p1][p2][p3][p4][p5] = 0;
	    	                        				}
	                            				}
	                            			}
	    	                        	}
	    	                        }
	    	                    }
	                    	} // end of configmask_zeroLUT for LUTTrackfire
	                    	else {
	                    		for (int p0 = 0; p0 < roboLUTDimensions[0]; p0++) {
	    	                        for (int p1 = 0; p1 < roboLUTDimensions[1]; p1++) {
	    	                        	for (int p2 = 0; p2 < roboLUTDimensions[2]; p2++) {
	    	                        		for (int p3 = 0; p3 < roboLUTDimensions[3]; p3++) {
	    	                        			for (int p4 = 0; p4 < roboLUTDimensions[4]; p4++) {
	    	                        				for (int p5 = 0; p5 < roboLUTDimensions[5]; p5++) {
	    	                        					roboLUT[p0][p1][p2][p3][p4][p5] = Double.parseDouble(reader.readLine());
	    	                        				}
	    	                        			}
	    	                        		}
	    	                        	}
	    	                        }
	    	                    } // end of data extraction for LUTTrackfire
	                    		if (debug) {
	                            	out.println("Imported LUT data");
	                            }
	                    	}
                    	} // end of LUTTrackfire
                    	else if((configDatFile & CONFIGMASK_FILE_WinLose) == CONFIGMASK_FILE_WinLose) {
                    		if (strImported != "winlose.dat") {
                    			if (debug) {
                    				out.println ("Import aborted (Imported wrong file - file was labelled winlose.dat)");
                    			}
                    			return importWINLOSE_ERROR_0x05;
                    		}
                    		totalFights = Integer.parseInt(reader.readLine());
                        	for (int i = 0; i < battleResults.length; i++){
                        		if (i < totalFights) {
                        			battleResults[i] = Integer.parseInt(reader.readLine());
                        		}
                        		else {
                        			battleResults[i] = 0;
                        		}
                        	}
                    	} // end of WinLose
                    	
                    	//write code for new file uses here. 
                    	//also change the string being called 
                    	//ctr+f: Import data. ->Change imported filename here<- 
                    	else{}
                    }
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }
            } 
            catch (IOException e) {
            	if (debug) {
            		out.println("Something done fucked up (Error0x01 error in file reading)");
            	}
                return importDATA_ERROR_0x01;
            } 
            catch (NumberFormatException e) {
                if (debug) {
                	out.println("Something done fucked up (Error0x02 error in int conversion)");
                }
                return importDATA_ERROR_0x02;
            }
        }
       
        flag_preventMultipleLUTDataImports = true; //sets flag to prevent multiple instances of robot importing(and exporting) from same .dat (and causing weird interactions(?)).
        return importDATA_SUCCESS;
    }
    
    /**
     * @name: 		exportData()
     * @author: 	partially written in robocode's sittingduckbot
     * @param: 		uses:
     * 				1. bool flag_preventMultipleLUTDataImports, static flag for preventing multiple imports
     * 				
     */

    public void exportData() {
    	return;
    }
    /**
     * @deprecated - use exportData
     * @name:		exportLUTData
     * @author:		Mostly from sittingduckbot
     * @purpose: 	1. Exports local LUT from memory to .dat file.
     * 					A. Configuration of the .dat file is written in the first line 
     * @param: 		1. repeatFlag
     * @return:		1. repeatFlag
     */
    public boolean exportLUTData(boolean repeatFlag){
        
        if (flag_preventMultipleLUTDataImports == true) {
            out.println("wewhat");
            PrintStream w = null;
            try {
                w = new PrintStream(new RobocodeFileOutputStream(getDataFile("LUTTrackfire.dat")));
                for (int p0 = 0; p0 < roboLUTDimensions[0]; p0++) {
                    for (int p1 = 0; p1 < roboLUTDimensions[1]; p1++) {
                    	for(int p2 = 0; p2 < roboLUTDimensions[2]; p2++){
                    		for (int p3 = 0; p3 < roboLUTDimensions[3]; p3++) {
                    			for (int p4 = 0; p4 < roboLUTDimensions[4]; p4++) {
                    				for (int p5 = 0; p5 < roboLUTDimensions[5]; p5++) {
                    					w.println(roboLUT[p0][p1][p2][p3][p4][p5]);
                    				}
                				}
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
        
        flag_preventMultipleLUTDataImports = false;
        return repeatFlag;
    }
    
    /**
     * @deprecated - use importData
     * @name: importWinLose
     */
    public void importWinLose(){
    	try {
            BufferedReader reader = null;
            try {
            	reader = new BufferedReader(new FileReader(getDataFile("winlose.dat")));
            	totalFights = Integer.parseInt(reader.readLine());
            	for (int i = 0; i < battleResults.length; i++){
            		if (i < totalFights) {
            			battleResults[i] = Integer.parseInt(reader.readLine());
            		}
            		else {
            			battleResults[i] = 0;
            		}
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
    	
    public void exportWinLose(int winLose){  
    	PrintStream w = null;
    	try {
			w = new PrintStream(new RobocodeFileOutputStream(getDataFile("winlose.dat")));
			w.println(totalFights+1);
        	for (int i = 0; i < totalFights; i++){
    			w.println(battleResults[i]);
        	}
    			w.println(winLose);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            if (w != null) {
                w.close();
            }
		}
    	
    	
    }
    /* Methods in LUTInterface not being used */
	@Override
	public double[] outputForward(double[] X, boolean flag, int numTrial) {
		return null;
	}
	@Override
	public double train(double[] X, double argValue, double[] Ycalc, boolean flag, int numTrial) {
		return 0;
	}
	@Override
	public void save(double error, int numTrial, PrintStream saveFile, boolean flag) {
	}
	@Override
	public void load(String argFileName) throws IOException {
	}
	@Override
	public void initialiseLUT() {
	}
}