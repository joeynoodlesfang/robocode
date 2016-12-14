/*
	
	december 12, 2016
		- implement NN basic online training structure. 

 */

package MyRobots;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import robocode.AdvancedRobot;
import robocode.BattleEndedEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.DeathEvent;
import robocode.HitWallEvent;
import robocode.RobocodeFileOutputStream;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;

public class NN2_LUTMimic extends AdvancedRobot{
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
	 //variables for the q-function. Robot will NOT change learning pattern mid-fight.
    private static final double alpha = 0.5;                //to what extent the newly acquired information will override the old information.
    private static final double gamma = 0.5;                //importance of future rewards
    private static final double epsilon = 0.05; 				//degree of exploration 
    
    //policy:either greedy or exploratory or SARSA 
    private static final int greedy = 0;
    private static final int exploratory = 1;
    private static final int SARSA = 2;
    
    // _ _ _ _  _ _ _ _  _ _ _ _  _ _ _ _ 
    //   MSB		filename		LSB
    // verificatn                 file-specific settings
    //
    // Config settings:
    //     				stringTest: 16400 (0x4010)
    // 					strLUT:		16416 (0x4020), zeroLUT = 16417 (0x4021)
    //     				WL:			16448 (0x4040)
    /* 
     * Notes: 
     * short - size of int. 
     * CONFIGMASK - */
    private static final short CONFIGMASK_ZEROLUT  = 				0x0001;
    private static final short CONFIGMASK_VERIFYSETTINGSAVAIL = 	0x4000;
    private static final short CONFIGMASK_FILETYPE_stringTest =		0x0010;
    private static final short CONFIGMASK_FILETYPE_LUTTrackfire =	0x0020;
    private static final short CONFIGMASK_FILETYPE_WinLose = 		0x0040;
    
    private static final int SUCCESS_importData = 					0x00;
    private static final int SUCCESS_exportData = 					0x00;
    private static final int SUCCESS_importDataWeights =			0x00;
    private static final int SUCCESS_exportDataWeights =			0x00;
    
    private static final int ERROR_1_import_IOException = 				1;
    private static final int ERROR_2_import_typeConversionOrBlank = 	2;
    private static final int ERROR_3_import_verification = 				3;
    private static final int ERROR_4_import_wrongFileName_stringTest =	4;
    private static final int ERROR_5_import_wrongFileName_WL =			5;
    private static final int ERROR_6_export_cannotWrite =				6;
    private static final int ERROR_7_export_IOException =				7;
    private static final int ERROR_8_import_dump =						8;
    private static final int ERROR_9_export_dump =						9;
    private static final int ERROR_10_export_mismatchedStringName =		10;
    private static final int ERROR_11_import_wrongFileName_LUT = 		11;
    private static final int ERROR_12_importWeights_IOException = 12;
    private static final int ERROR_13_importWeights_typeConversionOrBlank = 13;
    private static final int ERROR_14_exportWeights_cannotWrite_NNWeights_inputToHidden = 14;
    private static final int ERROR_15_exportWeights_cannotWrite_NNWeights_hiddenToOutput = 15;
    private static final int ERROR_16_exportWeights_IOException = 16;
    private static final int ERROR_17 = 17;
    private static final int ERROR_18 = 18;
    
    
    //strings used for importing or extracting files 
    String strStringTest = "stringTest.dat";    
    String strLUT = "LUTTrackfire.dat";
    String strWL = "winlose.dat";
    String strSA = "stateAction.dat"; 
    
    /**
	 * STATEACTION VARIABLES for stateAction ceilings.
	 * FOR NN
	 */
    //currently NN needs to forward propagate 4 * 2 * 3 = 24
    private static final int input_action0_moveReferringToEnemy_possibilities = 4;
    //0ahead50, 0ahead-50, -90ahead50, -90ahead-50
    
    private static final int input_action1_fire_possibilities = 2;
    //1, 3
    
    private static final int input_action2_fireDirection_possibilities = 3;
    //-10deg, 0, 10deg
    
    private static final int numActions = 3;
    
    private static final int input_state0_myPos_possibilities = 5;
    //center, left, right, top, bottom (cannot be undiscretized) 
    private static final int input_state1_myHeading_originalPossilibities = 4;
    //0-89deg, 90-179, 180-269, 270-359
    private static final int input_state2_enemyEnergy_originalPossibilities = 2;
    //>30, <30
    private static final int input_state3_enemyDistance_originalPossibilities = 3;
    //<150, <350, >=350
    private static final int input_state4_enemyDirection_originalPossibilities = 3;
    //head-on (still (abs <30 || >150), 
    //left (<0 relative dir w/ positive velo || >0 with negative velo), 
    //right (<0 dir w/ negative velo || >0 with positive velo)
    

    private static final int numStates = 5;
    
    private static final int numInputBias = 0;
    private static final int numHiddenBias = 1;
    private static final int numHiddenNeuron = 4;
    private static final int numInputsTotal = ( numInputBias + numActions + numStates );
    private static final int numHiddensTotal = ( numHiddenBias + numHiddenNeuron );
    private static final int numOutputsTotal = 1;

    /**
     * FLAGS AND COUNTS
     */
    
    //debug flags.
    static private boolean debug = false;  
    static private boolean debug_doAction = false;
    static private boolean debug_import = false;
    static private boolean debug_export = false;
    
    
    // Flag used for functions importData and exportData.
    // primary role is to maintain 1 import -> at most 1 export
    // secondary goals: Assists in preventing overwrite, and protection against wrong file entries.
    // False == inaccessible to write-to-file commands.
    private boolean flag_stringTestImported = false;
    private boolean flag_LUTImported = false;
    private boolean flag_WLImported = false;
    private boolean flag_weightsImported = false;
    
    private static boolean flag_useOfflineTraining = true;
    // printout error flag - initialized to 0, which is no error.
    static private int flag_error = 0;

//    //Flag used if user desires to zero LUT at the next battle. 
//    static private boolean zeroLUT = false; 

    /**
     *  OTHER GLOBALS
     */
    
    // weights connecting between input and hidden layers.
    private static double[][] NNWeights_inputToHidden 
        = new double
        [numInputsTotal]
        [numHiddensTotal] //no weights go from inputs to hidden bias.
        ;
    
    // weights connecting
    private static double[][] NNWeights_hiddenToOutput
    	= new double
    	[numHiddensTotal]
    	[numOutputsTotal]
    	;
    
    
    
    // LUT table configuration information, stored in the first line of .dat
    private short fileSettings_default = 0;
    private short fileSettings_stringTest = 0;
    private short fileSettings_LUT = 0; 
    private short fileSettings_WL = 0;
    private short fileSettings_SA = 0; 
    

    // Stores current reward for action.
    
    private double reward = 0.0; //only one reward variable to brief both offensive and defensive maneuvers
    private int energyDiffCurr = 0;
    private int energyDiffPrev = 0;
    

    // Stores current and previous stateAction vectors.
    //State vector (no actions) where copy currentSV to prevSV 
    private double currentStateActionVector[] = new double [numInputsTotal];
    private double prevStateActionVector[]    = new double [numInputsTotal]; 

    private double currentNetQVal = 0.0;
    private double previousNetQVal= 0.0; 
    
    //array to store the q values from net. 
    private double [][][] qFromNet = new double [input_action0_moveReferringToEnemy_possibilities][input_action1_fire_possibilities][input_action2_fireDirection_possibilities];

	
    //variables used for getMax.
    int num_actions = 24; 
    private int [] arrAllMaxActions = new int [num_actions]; //array for storing all actions with maxqval
    private int actionChosenForQValMax = 0; //stores the chosen currSAV with maxqval before policy
    private double qValMax = 0.0; // stores the maximum currSAV QMax

    //chosen policy. greedy or exploratory or SARSA 
    private static int policy = exploratory; 

    //enemy information
    private int enemyDistance = 0;
    private int enemyHeadingRelative = 0;
    private int enemyHeadingRelativeAbs = 0;
    private int enemyVelocity = 0;    
    private double enemyBearingFromRadar = 0.0;
    private double enemyBearingFromGun = 0.0;
    private double enemyBearingFromHeading = 0.0;
    private int enemyEnergy = 0;
    
    //my information
    private int myHeading = 0; 
    private int myEnergy = 0;
    private int myPosX = 0;
    private int myPosY = 0;
    
    //general information
    private int tick = 0;
    
    
    private int totalFights = 0;
    private int[] battleResults = new int [520000];
    private int currentBattleResult = 0;
	

    /** Neural net stuff 
     * 
     * */

    
    /*Initiate variables */
		
	double lRate = 0.05; 			//learning rate
	double momentum = 0.2;  	//value of momentum 
	boolean stopError = false; 	//if flag == false, then stop loop, else continue 
	int maxEpoch = 1000; 	//if reach maximum number of Epochs, stop loop. 
	
	// initialize arrays 
	double [][] vPast 	= new double[numInputsTotal][numHiddensTotal];			// Input to Hidden weights for Past.
	double [][] wPast 	= new double[numHiddensTotal][numOutputsTotal];    		// Hidden to Output weights for Past.
	double [][] vNext	= new double[numInputsTotal][numHiddensTotal];	
	double [][] wNext 	= new double[numHiddensTotal][numOutputsTotal];    		// Hidden to Output weights.
	double [][] deltaV = new double [numInputsTotal][numHiddensTotal];		// Change in Input to Hidden weights
	double [][] deltaW = new double [numHiddensTotal][numOutputsTotal]; 	// Change in Hidden to Output weights
	double [] Z_in = new double[numHiddensTotal]; 		// Array to store Z[j] before being activate
	double [] Z    = new double[numHiddensTotal];		// Array to store values of Z 
	double [] Y_in = new double[numOutputsTotal];		// Array to store Y[k] before being activated
	double [] Y	   = new double[numOutputsTotal];		// Array to store values of Y  
	double [] delta_out = new double[numOutputsTotal];
	double [] delta_hidden = new double[numHiddensTotal];
	boolean flagActivation = false;  
	private final int bias = 1; 

    
    //@@@@@@@@@@@@@@@ RUN & EVENT CLASS FUNCTIONS @@@@@@@@@@@@@@@@@    
    
    /**
     * @name: 		run
     * @purpose:	1. Initializes robot colour
     * 				2. Imports LUT data from file into local memory array.
     * 				4. Runs LUT-based learning code. See fxn learningLoop for details.
     * @brief:		To import desired file name, simply write Stringvar of filename or "filenamehere.dat" as param of importData. 
     * @brief:		To ZeroLUT, add 1 to top line in .dat file (or change 16416 to 16417)
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
        
        flag_error = importData(strWL);
        if( flag_error != SUCCESS_importData) {
        	out.println("ERROR @run WL: " + flag_error);
        }
        
        flag_error = importDataWeights();
        if(flag_error != SUCCESS_importData) {
        	out.println("ERROR @run weights: " + flag_error);
        }
        

            
        //set gun and radar for robot turn separate gun, radar and robot (robocode properties). 
        setAdjustGunForRobotTurn(true);
    	setAdjustRadarForGunTurn(true);	
    	setAdjustRadarForRobotTurn(true);

    	// anything in infinite loop is initial behaviour of robot
        for(;;){
        	setTurnRadarRight(20);
    		execute();					//from "AdvancedRobot" to allow parallel commands. 
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
        flag_error = exportDataWeights();				//strLUT = "LUTTrackfire.dat"
        if(flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onBattleEnded weights: " + flag_error); //only one to export due to no learningloop(), but fileSettings_
        	//LUT is 0'd, causing error 9 (export_dump)
        }
        
        flag_error = exportData(strWL);					//"strWL" = winLose.dat
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onBattleEnded WL: " + flag_error);
        }
        
//        flag_error =  exportData(strSA); 
//        flag_error = saveData(strSA); 
//        if(flag_error != SUCCESS_exportData) {
//        	out.println("ERROR @onBattleEnded: " + flag_error); //only one to export due to no learningloop(), but fileSettings_
//        	//LUT is 0'd, causing error 9 (export_dump)
//        }
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
    	currentBattleResult = 0;    					//global variable. 
        flag_error = exportDataWeights();
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onDeath weights: " + flag_error);
        }
        
        flag_error = exportData(strWL);					//"strWL" = winLose.dat
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onDeath WL: " + flag_error);
        }
//        flag_error =  exportData(strSA); 
//        flag_error = saveData(strSA); 
//        if( flag_error != SUCCESS_exportData) {
//        	out.println("ERROR @onDeath: " + flag_error);
//        }
    }
    
    /**
     * @name: 		onWin
     * @purpose: 	1. 	Exports LUT data from memory to .dat file, which stores Qvalues 
     * 				   	linearly. Exporting will occur only once per fight, either during 
     * 				   	death or fight end.
     * 				2.  Sets a terminal reward of +100 
     * @param:		1.	WinEvent class from Robot
     * @notes: 		can not call learningLoop() in onWin or onDeath because these are final events. 
     * @return:		n
     */    
	public void onWin(WinEvent e) {
    	currentBattleResult = 1;
    	
        flag_error = exportDataWeights();
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onWin weights: " + flag_error);
        }
        
        flag_error = exportData(strWL);
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onWin WL: " + flag_error);
        }

//        flag_error = saveData(strSA); 
//        if( flag_error != SUCCESS_exportData) {
//        	out.println("ERROR @onDeath: " + flag_error);
//        }
	}
	

	/**
     * @name:		onScannedRobot
     * @purpose:	1. determine enemy bearing and distance
     * 				2. call learningloop to update the LUT
     * @param:		ScannedRobotEvent event
     * @return:		none, but updates:
     * 				1. getGunBearing
     * 				2. enemyDistance
     */

	public void onScannedRobot(ScannedRobotEvent event){
		myHeading = (int)getHeading();
		enemyHeadingRelative = (int)normalRelativeAngleDegrees(event.getHeading() - getGunHeading());
		enemyHeadingRelativeAbs = Math.abs(enemyHeadingRelative);
		enemyVelocity = (int)event.getVelocity();
		myPosX = (int)getX();
		myPosY = (int)getY();
		enemyBearingFromRadar = (double)myHeading + event.getBearing() - getRadarHeading();
		enemyBearingFromGun = (double)myHeading + event.getBearing() - getGunHeading();
		enemyBearingFromHeading = event.getBearing();
		enemyDistance = (int)event.getDistance(); 
		enemyEnergy = (int)event.getEnergy();
		myEnergy = (int)getEnergy();
		tick = (int)getTime();
//		out.println("Time is" + event.getTime());
    	learning();
    }

	/* 
	 * If want to emphasize a certain event, then call the event and add external reward. 
	 * */
//	/**
//	* @name: 		onBulletMissed
//	* @purpose: 	1. Updates reward. -10 if bullet misses enemy
//	* @param:		1. HItBulletEvent class from Robot
//	* @return:		n
//	*/      
//    public void onBulletMissed(BulletMissedEvent event){
////    	reward += -5;    
////    	learningLoop(); 
////    	out.println("Missed Bullet" + reward);
//    }
    
//	/**
//	* @name: 		onBulletHit
//	* @purpose: 	1. Updates reward. +30 if bullet hits enemy
//	* 				2. Update the values of heading and energy of my robot 
//	* @param:		1. HItBulletEvent class from Robot
//	* @return:		n
//	*/     
//    public void onBulletHit(BulletHitEvent e){
//    	reward += 5; 
////    	out.println("Hit Bullet" + reward);
//    }
//    
//    /**
//     * @name: 		onHitWall
//     * @purpose: 	1. Updates reward. -10
//     * 				2. Updates heading and energy levels. 
//     * @param:		1. HitWallEvent class from Robot
//     * @return:		n
//     */   
//    public void onHitWall(HitWallEvent e) {
//    	reward = -5; 
////    	out.println("Hit Wall" + reward);
//    }
    
//    /**
//     * @name: 		onHitByBullet
//     * @purpose: 	1. Updates reward. -10
//     * 				2. Updates heading and energy levels. 
//     * @param:		1. HitWallEvent class from Robot
//     * @return:		n
//     */   
////    public void onHitByBullet(HitByBulletEvent e) {
////    	reward += -5;
////    //	learningLoop();
////    }   
    
//    /**
//     * @name: 		onHitRobot
//     * @purpose: 	1. Updates reward. -10
//     * 				2. Updates heading and energy levels. 
//     * @param:		1. HitWallEvent class from Robot
//     * @return:		n
//     */   
////    public void onHitRobot(HitRobotEvent e) {
////    	reward = -1;
////    //	learningLoop();
////    }  
//	
	
	
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
     * @name:		learning
     * @purpose:	perform continuous reinforcement learning.
     * 				Reinforcement learning involves following steps:
     * 				1. Store the last state and action (currentSAV -> prevSAV)
     * 				2. Use information from the environment to determine the current state.
     * 				(3. punish robot for not changing state [not in original qfunction])
     * 				4. Perform QFunction (detailed further in function).
     * 				5. Reset rewards. IE: all events affect reward only once unless specified.
     * 				6. Perform chosen action. 
     * @param:		n
     * @return:		n
     */
    /* Training online: 
     *  step (1) - need a vector of just states "copyCurrentSV into prev SV". 
        step (2) - get weights array - neural net do forwardpropagation for each action in the "CurrentSV"  , remembering all outputs "Y" in an array
        step (3) - choose maximum "Y" from array 
        step (4) - call qFunction() below using prevSAV as qOld and qNew is the SAV with max Y (chosen in step (3)) 
        		   - with return being the qOld_(new) 
        step (5) - with prevSAV (inputs) and qOld_new (correct target)  and qOld (calculated output), run backpropagation & save weights 
        step (6) - save error for graph
        step (7) - repeat steps 1-6 using saved weights from backpropagation to feed into NN for step (2)  
     */
    public void learning() {
    	if (tick%4 == 0) {
             calculateReward();
             copyCurrentSVIntoPrevSV();
             generateCurrentStateVector();
             getQfromNet(); 
             qFunction();
             resetReward();
             doAction();
    	}

        else {
            setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun));
        }

        setTurnRadarRight(normalRelativeAngleDegrees(enemyBearingFromRadar));
        scan();
        execute();

    }


	/**
     * @name:		calculateReward
     * @purpose:	calculates reward based on change in energy difference of robots.
     * @param:		none
     * @return:		none
     */
    public void calculateReward(){
    	energyDiffPrev = energyDiffCurr;
    	energyDiffCurr = myEnergy - enemyEnergy;
    	reward += energyDiffCurr - energyDiffPrev;  	
    }
    
    /**
     * @name:		copyCurrentSAVIntoPrevSAV
     * @purpose:	Copies array currentStateActionVector into array prevStateActionVector
     * @param:		n, but uses:
     * 				1. currentStateActionVector
     * @return:		n
     */
    public void copyCurrentSVIntoPrevSV(){
    	for (int i = 0; i < currentStateActionVector.length; i++) {
    		prevStateActionVector[i] = currentStateActionVector[i];
    	}
    	previousNetQVal = currentNetQVal; 
    }
    
    /**
     * @name: 		generateCurrentStateVector - discretize states here. 
     * @purpose: 	1. gets state values from battlefield. 
     * 				2. discretize. 
     * 				3. Update array of current stateAction vector.  
     * @param: 		n
     * @return: 	none
     * currentStateVector positions [0][1][2] are all the actions. 
     */
    public void generateCurrentStateVector(){
    	//INPUTS 0, 1 and 2 are ACTION
        
    	//Dimension 1 - private static final int input_state0_myPos_possibilities = 5;
    	if (  (myPosX<=50)  &&  ( (myPosX <= myPosY) || (myPosX <= (600-myPosY)) )  ){					//left
    		currentStateActionVector[3] = 1;						
    	}
    	else if (  (myPosX>=750)  &&  ( ((800-myPosX) <= myPosY) || ((800-myPosX) <= (600-myPosY)) )  ){		//right
    		currentStateActionVector[3] = 2;						
    	}
    	else if (myPosY<=50) {		//top 
    		currentStateActionVector[3] = 3;
    	}
    	else if (myPosY>=550) {		//bottom				
    		currentStateActionVector[3] = 4;
    	}
    	else {
    		currentStateActionVector[3] = 0; 
    	}

    	//Dimension 3 - private static final int input_state1_myHeading_originalPossilibities = 4;
    	currentStateActionVector[4] = myHeading*(4/360);			//to normalize. 
    	
    	//Dimension 4 - enemyEnergy
    	if (enemyEnergy < 30){
    		currentStateActionVector[5] = enemyEnergy/60;
    	}
    	
    	else if (enemyEnergy >= 30){
    		currentStateActionVector[5] =((enemyEnergy-30)/70)+0.5;
    	}
    	//Dimension 5:  //<150, <350, >=350(to1000)
    	currentStateActionVector[6] = enemyDistance;
    	if (enemyDistance < 150){
    		currentStateActionVector[6] = enemyDistance/100;
    	}
    	
    	else if (enemyDistance <= 350){
    		currentStateActionVector[6] =((enemyDistance-150)/200);
    	}
    	else if (enemyDistance > 350){
    		currentStateActionVector[6] = (enemyDistance/2000); 
    	}
    	
		//Dimension 5: is enemy moving right, left, or within the angle of my gun?
		//requires mygunheading, enemyheading, enemyvelocity
    	if ((enemyHeadingRelativeAbs < 30) || (enemyHeadingRelativeAbs > 150) || (enemyVelocity == 0)) {
			currentStateActionVector[7] = 0; //within angle of gun
		}
		else if ( ((enemyHeadingRelative < 0)&&(enemyVelocity > 0)) || ((enemyHeadingRelative > 0)&&(enemyVelocity < 0)) ) {
			currentStateActionVector[7] = 1; //enemy moving left
		}
		else if ( ((enemyHeadingRelative < 0)&&(enemyVelocity < 0)) || ((enemyHeadingRelative > 0)&&(enemyVelocity > 0)) ){
			currentStateActionVector[7] = 2; //enemy moving right
		}
    	out.println("currentStateVector " + Arrays.toString(currentStateActionVector));
    }
 
    /** 
     * @name:		getQfromNet
     * @input: 		currentStateVector 
     * @purpose: 	For each state in "stateVector", call forwardPropagation to generate the output.  
     * @purpose: 	1. Obtain the action in current state with the highest q-value FROM the outputarray "Yout" of the neural net. 
     * @return: 	not sure yet
     */

	public void getQfromNet() {
		//need to get the Ycalc from all the states
		for (int i = 0; i < input_action0_moveReferringToEnemy_possibilities; i++){
			for (int j = 0; j < input_action1_fire_possibilities; j++){
				for(int k = 0; k < input_action2_fireDirection_possibilities; k++){
					double Ycalc = forwardProp(currentStateActionVector, flagActivation);
					//error is here
					qFromNet[i][j][k] = Ycalc; 
				}
			}
		}
		out.println("YCalc " + Arrays.deepToString(qFromNet));
	}
	
	
	/** function for forwardpropagation
	 * @purpose: does forwardPropagation on the inputs from the robot. 
	 * @return: an array of Y values for all the state pairs. 
	 **/
    public double forwardProp(double [] currentStateVector, boolean flag) {
		for (int j = 1; j < numHiddenNeuron; j++){
			double sumIn = 0.0; 
			for (int i= 0; i < numInputsTotal; i++){	
				sumIn += currentStateVector[i]*NNWeights_inputToHidden[i][j]; 
			}
			Z_in[j] = sumIn; 									//save z_in[0] for the bias hidden unit. 
			Z_in[0] = bias; 									//set z_in[0] = bias 
			if (flag == true){
				Z[j] = binaryActivation(Z_in[j]); 
				Z[0] = Z_in[0];
			}
			else{
				Z[j] = bipolarActivation(Z_in[j]); 
				Z[0] = Z_in[0];
			}
		}
		for (int k = 0; k < numOutputsTotal; k++){
			double sumOut = 0.0; 
			for (int j= 0; j < numHiddensTotal; j++){	
				sumOut += Z[j]*NNWeights_hiddenToOutput[j][k]; 
			}
			Y_in[k] = sumOut; 	
			if (flag == true)
				Y[k] = binaryActivation(Y_in[k]); 
			else
				Y[k] = bipolarActivation(Y_in[k]);				
		}		
		return Y[0]; 
	}
    
    /**
     * @name:		qFunction
     * @purpose: 	1. Obtain the action in current state with the highest q-value FROM the outputarray "Yout" of the neural net. 
     * 				2. The q value is the maximum "Y" from array
     * 				3. currentNetQVal is assigned prevQVal 
     * 				4. Ycalc is previousNetQ
     * 			    5. run runBackProp with the input X as currentStateActionVector, qExpected as currentNetQ, Ycalc is prevNetQVal 
     * @param: 		none, but uses:
     * 				1.	double reward 
     * 				2.	int currentStateVector[] already discretized (size numStates)
     * 				3.	double[].. LUT table, 
     * 				4.	int [] currentStateActionVector. 
     * @return: 	n
     */
    public void qFunction(){
       getMax(); 
       currentNetQVal =  calcNewPrevQVal();
//      
       //currentStateActionVector = X inputs, prevQVal (double) is the target, qNew, 
       double[] Ycalc = new double [1]; 			//because backProp takes in a vector for Ycalc (which is qprevious). 
       Ycalc[0] = previousNetQVal;
       double expectedYVal = currentNetQVal; 
//       out.println("expectedYVal " + expectedYVal);
//       out.println("Ycalc " + Arrays.toString(Ycalc));
       runBackProp(currentStateActionVector, expectedYVal, Ycalc, flagActivation); 
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
	 *							(3) Store the (now 3 dimension) action index into arrAllMaxActions[numMaxActions-1]
	 *						ii. if indexQVal == currMax:
	 *							(1) numMaxActions++
	 *							(2) Store the (now 3 dimension) action index into arrAllMaxActions[numMaxActions-1]
	 *						iii. if indexQVal < currMax:
	 *							ignore.
	 *					c. record chosen action. If multiple actions with max q-values, randomize chosen action.
	 *						i. if numMaxActions > 1, 
	 *						   randomly select between 0 and numMaxActions - 1. The randomed 
	 *						   number will correspond to the array location of the chosen
	 *						   action in arrAllMaxActions. 
	 *						ii. actionChosenForQValMax = arrAllMaxActions[randomed number]
	 *					d. record associated q-value.
     * @param: 		none, but uses:
     * 				1.	current SAV[].
     * 				2.	roboLUT 
     * 				3.	currQArrayMax
     * @return: 	n
     */
    public void getMax() {
    	double currMax = qFromNet[0][0][0];  
        double qVal = 0.0;
        int numMaxActions = 0;
        int randMaxAction = 0;
//        out.println("qFromNet.length " + qFromNet.length); 
    	for (int i = 0; i < qFromNet.length; i++){
		    for (int j = 0; j < qFromNet[0].length; j++){
		    	for (int k = 0; k < qFromNet[0][0].length; k++){
		    		//qFromNet[i][j][k] is a value
//		    		out.println("qFromNet" + qFromNet[i][j][k]);
		    		if (qFromNet[i][j][k] > currMax){
		    			currMax = qFromNet[i][j][k];
		            	numMaxActions = 1;
		            	arrAllMaxActions[numMaxActions-1] = (i+1)*(j+1)*(k+1)-1;		//all possible combinations (i*j*k)
		            }
		            else if (qVal == currMax){
		            	numMaxActions++;
//		            	out.println("arrAllMaxActions[numMaxActions-1] " + arrAllMaxActions[numMaxActions-1]); 
		            	arrAllMaxActions[numMaxActions-1] = (i+1)*(j+1)*(k+1)-1;
		            }
		            
		            if (debug) {
		            	out.print(i + ": " + qVal + "  ");
		            }
		    	}
    		}
    	}
        qValMax = currMax;
        
        if (numMaxActions > 1) {
        	randMaxAction = (int)(Math.random()*(numMaxActions)); //math.random randoms btwn 0.0 and 0.999. Add 1 to avoid truncation after typecasting to int.
        	
        	if (debug) {
            	System.out.println("randMaxAction " + randMaxAction + " numMaxActions " + numMaxActions);
            }
        }
        
        actionChosenForQValMax = arrAllMaxActions[randMaxAction];

        
        if (debug) {
        	System.out.println("Action Chosen: " + actionChosenForQValMax  + " qVal: " + qValMax);
        }
//        System.out.println("Action Chosen: " + actionChosenForQValMax  + " qVal: " + qValMax);
    }
    
    /**
     * @name		calcNewPrevQVal
     * @purpose		1. Calculate the new prev q-value based on Qvalue function.
     * @param		n, but uses:
     * 				1. qValMax
     * @return		prevQVal
     */
    public double calcNewPrevQVal(){
    	currentNetQVal +=  alpha*(reward + gamma*qValMax - previousNetQVal);
    	out.println("currentNetQVal " + currentNetQVal);
    	return currentNetQVal;
    }
    
    /**
     * @name:		runbackProp
     * @purpose:	1. Update prevSAV in LUT with the calculated prevQVal.
     * 				2. Update curr state with correct action based on policy (greedy or exploratory).
     * @param:		1. prevQVal
     * @return:		n
     */
    public void runBackProp(double [] X, double Yreal, double[] Ycalc, boolean flag) {
//		System.out.println("z_in " + Arrays.toString(Z_in));
//		System.out.println("Y_in " + Arrays.toString(Y_in));	
		for (int k = 0; k <numOutputsTotal; k++){
//			delta_out[k]  =  (Yreal - Ycalc[k])*customActivationDerivation(Y_in[k],maxQ, minQ);
			if (flag == true){
				delta_out[k] = (Yreal - Ycalc[k])*binaryDerivative(Y_in[k]); 
			}
			else{
				delta_out[k] = (Yreal - Ycalc[k])*bipolarDerivative(Y_in[k]);	
			}
//			System.out.println("\n");
//			System.out.println("delta " + delta_out[k]);
			for (int j = 0; j < numHiddensTotal; j++){
//				System.out.println("wPast[j][k] " + wPast[j][k]);
//				System.out.println("NNWeights_hiddenToOutput[j][k] " + NNWeights_hiddenToOutput[j][k]);
				deltaW[j][k] = alpha*delta_out[k]*Z[j];
				wNext[j][k] = NNWeights_hiddenToOutput[j][k] + deltaW[j][k] + momentum*(NNWeights_hiddenToOutput[j][k] - wPast[j][k]); 
				wPast[j][k] = NNWeights_hiddenToOutput[j][k]; 
				NNWeights_hiddenToOutput[j][k] = wNext[j][k]; 
//				System.out.println("wPast[j][k] " + wPast[j][k]);
//				System.out.println("NNWeights_hiddenToOutput[j][k] " + NNWeights_hiddenToOutput[j][k]);
//				System.out.println("wNext[j][k] " + wNext[j][k]);
			}
		}
		
		//for hidden layer
		for (int j = 0; j < numHiddensTotal; j++){
			double sumDeltaInputs = 0.0;
			for (int k = 0;  k < numOutputsTotal; k++){
				sumDeltaInputs += delta_out[k]*NNWeights_hiddenToOutput[j][k];
				if (flag == true){
					 delta_hidden[j] = sumDeltaInputs*binaryDerivative(Z_in[j]); 
				}
				else{
					delta_hidden[j] = sumDeltaInputs*bipolarDerivative(Z_in[j]);	
				}
			}
			for (int i = 0; i< numInputsTotal; i++){
//				System.out.println("vPast[i][j] " + vPast[i][j]);
//				System.out.println("NNWeights_inputToHidden[i][j] " + NNWeights_inputToHidden[i][j]);
				deltaV[i][j] = alpha*delta_hidden[j]*X[i];
				vNext[i][j]  = NNWeights_inputToHidden[i][j] + deltaV[i][j] + momentum*(NNWeights_inputToHidden[i][j] - vPast[i][j]); 
				vPast[i][j] = NNWeights_inputToHidden[i][j]; 
				NNWeights_inputToHidden[i][j] = vNext[i][j]; 
//				System.out.println("vPast[i][j] " + vPast[i][j]);
//				System.out.println("NNWeights_inputToHidden[i][j] " + NNWeights_inputToHidden[i][j]);
//				System.out.println("vNext[i][j] " + vNext[i][j]);
			}
		}
		//Step 9 - Calculate local error. 
		double error = 0.0;
		for (int k = 0; k < numOutputsTotal; k++){ 
			error = 0.5*(java.lang.Math.pow((Yreal - Ycalc[k]), 2)); 
		}
		//saveWeights
		
//		saveFile(NNWeights_inputToHidden, NNWeights_hiddenToOutput);
//		File saveErrors = new File ("C:\\Users\\Andrea\\github\\robocode\\robots\\MyRobots\\NN2_LUTMimic.data\\backPropError.txt"); 
//		PrintStream saveLocalError = null;
//		
//		try {
//			saveLocalError = new PrintStream( new FileOutputStream(saveErrors));
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//		saveLocalError.println(error);
//		saveLocalError.close(); 
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
     * @name:		doAction
     * @purpose: 	Converts state Action vector into action by reading currentSAV[0]
     * @param: 		n, but uses:
     * 				1. Array currentSAV.
     * @return:		n
     */
    	
    public void doAction(){
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
    	
    	if ( ((currentStateActionVector[0])/4) %2 == 0){
    		setFire(1);
    	}
    	else if ( ((currentStateActionVector[0])/4) %2 == 1){
    		setFire(3);
    	}
    	
    	//firing behaviour (to counter defensive behaviour)
    	if ((currentStateActionVector[0])/8 == 0){
    		setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun));
    	}
    	
    	else if ((currentStateActionVector[0])/8 == 1){
    		setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun + 10));
    	}
    	
    	else if ((currentStateActionVector[0])/8 == 2){
    		setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun - 10));
    	}
    	

//    	out.println("currentStateActionVector" + Arrays.toString(currentStateActionVector));     
      if (debug_doAction || debug) {
    	  out.println("currentStateActionVector" + Arrays.toString(currentStateActionVector));
      }
    }
    
    /**
     * @name:		importDataWeights
     * @author:		partly written in sittingduckbot
     * @purpose:	to extract neural net weights stored in finalHiddenWeights.txt 
     * 				and finalOuterWeights.txt into arrays NNWeights_inputToHidden[][]
     * 				and NNWeights_hiddenToOutput[][], respectively.
     * @param:		n, but uses these globals:
     * 				NNWeights_inputToHidden[][]
     * 				NNWeights_hiddenToOutput[][]
     * @return:		n
     */
    public int importDataWeights() {
    	if (flag_weightsImported == false) {
	    	try {
	        	BufferedReader reader = null;
	        	BufferedReader reader2 = null;
	            try {
	            	if (flag_useOfflineTraining) {
	            		reader = new BufferedReader(new FileReader(getDataFile("inToHiddenWeights_OfflineTraining.txt")));
	            	}
	            	else {
	            		reader = new BufferedReader(new FileReader(getDataFile("finalHiddenWeights.txt")));
	            	}
	            	for (int i = 0; i < numInputsTotal; i++) {
	            		for (int j = 0; j < numHiddenNeuron; j++) {
	            			NNWeights_inputToHidden[i][j] = Double.parseDouble(reader.readLine());
		                }
	            	}
	            } 
	            finally {
	                if (reader != null) {
	                    reader.close();
	                }
	            }
	            
	            try {
	            	if (flag_useOfflineTraining) {
	            		reader2 = new BufferedReader(new FileReader(getDataFile("hiddenToOutWeights_OfflineTraining.txt")));
	            	}
	            	else {
	            		reader2 = new BufferedReader(new FileReader(getDataFile("finalOuterWeights.txt")));
	            	}
	            	for (int i = 0; i < numHiddensTotal; i++) {
	            		for (int j = 0; j < numOutputsTotal; j++) {
	            			NNWeights_hiddenToOutput[i][j] = Double.parseDouble(reader.readLine());
		                }
	            	}
	            } 
	            finally {
	                if (reader2 != null) {
	                    reader2.close();
	                }
	            }
	        } 
	        //exception to catch when file is unreadable
	        catch (IOException e) {
	            return ERROR_12_importWeights_IOException;
	        } 
	        // type of exception where there is a wrong number format (type is wrong or blank)  
	        catch (NumberFormatException e) {
	            return ERROR_13_importWeights_typeConversionOrBlank;
	        }
	    	flag_weightsImported = true;
	    	if (flag_useOfflineTraining) {
	    		flag_useOfflineTraining = false;
	    	}
	    	return SUCCESS_importDataWeights;
    	}
    	
    	else {
    		return ERROR_17;
    	}
    }
    
    /**
     * @name: 		exportDataWeight
     * @author: 	mostly sittingduckbot
     * @purpose: 	1. stores the weights back into finalHiddenWeights.txt, 
     * 				finalOuterWeights.txt from data NNWeights_inputToHidden[][]
     * 				and NNWeights_hiddenToOutput[][], respectively.
     * 
     */
    public int exportDataWeights() {
    	if(flag_weightsImported == true) {
			PrintStream w1 = null;
			PrintStream w2 = null;
	    	try {
	    		w1 = new PrintStream(new RobocodeFileOutputStream(getDataFile("finalHiddenWeights.txt")));
	    		if (w1.checkError()) {
	                //Error 0x03: cannot write
	            	if (debug_export || debug) {
	            		out.println("Something done fucked up (Error 14 cannot write)");
	            	}
	            	return ERROR_14_exportWeights_cannotWrite_NNWeights_inputToHidden;
	            	//TODO here
	    		}
	    		 
	    		for (int i = 0; i < numInputsTotal; i++) {
	         		for (int j = 0; j < numHiddenNeuron; j++) {
	         			w1.println(NNWeights_inputToHidden[i][j]);
	                }
	         	}
	    		
	    		w2 = new PrintStream(new RobocodeFileOutputStream(getDataFile("finalOuterWeights.txt")));
	    		if (w2.checkError()) {
	                //Error 0x03: cannot write
	            	if (debug_export || debug) {
	            		out.println("Something done fucked up (Error 15 cannot write)");
	            	}
	            	return ERROR_15_exportWeights_cannotWrite_NNWeights_hiddenToOutput;
	    		 }
	    		 
	    		for (int i = 0; i < numHiddensTotal; i++) {
	         		for (int j = 0; j < numOutputsTotal; j++) {
	         			w2.println(NNWeights_hiddenToOutput[i][j]);
	                }
	         	}
	    	}
	    	catch (IOException e) {
	    		if (debug_export || debug) {
	    			out.println("IOException trying to write: ");
	    		}
	            e.printStackTrace(out); //Joey: lol no idea what this means
	            return ERROR_16_exportWeights_IOException;
	        } 
	        finally {
	            if (w1 != null) {
	                w1.close();
	            }
	            
	            if (w2 != null) {
	                w2.close();
	            }
	        }      
	    	flag_weightsImported = false;
	    	return SUCCESS_exportDataWeights;
    	}
    	else {
    		return ERROR_18;
    	}
    }
    
    
    /**
     * @name:		importData
     * @author:		partly written in sittingduckbot
     * @purpose: 	1. Imports data from file, depending on file name. 
     * @brief:		ONLY A SINGLE FILE CAN BE IMPORTED AT A TIME.
     * 				Class BufferReader(java.io.) is instantiated to read file with filename using 
     * 				code obtained mainly from SittingDuck.java. The first line of the .dat file is
     * 				extracted to obtain the file settings, which includes the contents the file
     * 				expects. A set of slightly varying extraction sequences are performed 
     * 				based on file.
     * 
     * 				Config settings:
     * 				stringTest: 16400 (0x4010)
     * 				strLUT:		16416 (0x4020), zeroLUT = 16417 (0x4021)
     * 				WL:			16448 (0x4040)
     * 				strSA: 		? 
     * 				
     * @param: 		1. stringname of file desired to be written. The fxn currently accepts 3(three) 
     * 				files: LUTTrackfire.dat, winlose.dat, and stringTest.dat. Any other string 
     * 				name used for file name will be flagged as erroneous.
     * 				also uses:
     * 				1. bool flag_LUTImported, static flag for preventing multiple imports by multiple instances of robot (hopefully?).
     * @return:		1. int importLUTDATA success/error;
     */
    public int importData(String strName){
    	if (debug_import || debug) {
    		out.println("@importData: at beginning of fxn");
    		out.println("printing fileSettings: ");
    		out.println("fileSettings_default: " + fileSettings_default);
    		out.println("fileSettings_stringTest: " + fileSettings_stringTest);
    		out.println("fileSettings_LUT: " + fileSettings_LUT);
    		out.println("fileSettings_WL: "+ fileSettings_WL); 
    	}
    	
        try {
        	BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(getDataFile(strName)));
                fileSettings_default = (short)Integer.parseInt(reader.readLine());			//reads first line of code to obtain what is in "fileSettings_default"
                
                if (debug_import || debug) {
            		out.println("extracted fileSettings into default: ");
            		out.println("fileSettings_default: " + fileSettings_default);
            	}
                // CONFIGMASK_VERIFYSETTINGSAVAIL = 0x4000
                // & is bit-wise "and" to compare every single bit of CONFIGMASK with fileSettings_default
                // to ensure that a first line exists. 
                if ((fileSettings_default & CONFIGMASK_VERIFYSETTINGSAVAIL) != CONFIGMASK_VERIFYSETTINGSAVAIL) {
                	if (debug_import || debug) {
                		out.println("Import aborted (file not configured properly)");
                	}
                	
                	return ERROR_3_import_verification;
                }
                else {
                	//this if prevents accidentally importing from wrong file by matching coded filename with settings in read file.
                	//flag prevents multiple imports and data overwrite since array is static
                	if ( ((fileSettings_default & CONFIGMASK_FILETYPE_stringTest) == CONFIGMASK_FILETYPE_stringTest)
                		&& (flag_stringTestImported == false) )
                	{
                		if (strName != "stringTest.dat") {
            				if (debug_import || debug) {
            					out.println ("Import aborted (Imported wrong file - file declared stringTest.dat in settings)");
            				}
                			return ERROR_4_import_wrongFileName_stringTest;
                		}
                		
                		fileSettings_stringTest = fileSettings_default;
                		flag_stringTestImported = true;
                	}
                	
                	// this if prevents accidentally importing from wrong file by matching coded filename with settings in read file.
                	//flag prevents multiple file imports (mostly for preventing export bugs)
                	
                	else if( ((fileSettings_default & CONFIGMASK_FILETYPE_WinLose) == CONFIGMASK_FILETYPE_WinLose) && (flag_WLImported == false) ) {
                		if (strName != "winlose.dat") {
                			if (debug_import || debug) {
                				out.println ("Import aborted (Imported wrong file - file was labelled winlose.dat)");
                			}
                			return ERROR_5_import_wrongFileName_WL; //error 5 - coder mislabel during coding
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
                    	fileSettings_WL = fileSettings_default;
                    	flag_WLImported = true;
                	} // end of WinLose
                	
                	//write code for new file uses here. 
                	//also change the string being called 
                	//ctr+f: Import data. ->Change imported filename here<- 
                	
                	//file is undefined - so returns error 8
                	else {
                		if (debug_import || debug) {
                    		out.println("error 8:");
                    		out.println("fileSettings_default: " + fileSettings_default);
                    		out.println("fileSettings_stringTest: " + fileSettings_stringTest);
                    		out.println("fileSettings_LUT: " + fileSettings_LUT);
                    		out.println("fileSettings_WL: "+ fileSettings_WL);
                    		out.println("CONFIGMASK_FILETYPE_LUTTrackfire|verisett: " + (CONFIGMASK_FILETYPE_LUTTrackfire | CONFIGMASK_VERIFYSETTINGSAVAIL));
                    		out.println("CONFIGMASK_FILETYPE_WinLose|versett: " + (CONFIGMASK_FILETYPE_WinLose | CONFIGMASK_VERIFYSETTINGSAVAIL));
                    		out.println("flag_LUTImported: " + flag_LUTImported);
                    		out.println("fileSettings_default & CONFIGMASK_ZEROLUT: " + (fileSettings_default & CONFIGMASK_FILETYPE_LUTTrackfire));
                    		out.println("CONFIGMASK_FILETYPE_LUTTrackfire: " + CONFIGMASK_FILETYPE_LUTTrackfire);
                    	}
                		return ERROR_8_import_dump; //error 8 - missed settings/file dump.
                	}
                }
            } 
            finally {
                if (reader != null) {
                    reader.close();
                }
            }
        } 
        //exception to catch when file is unreadable
        catch (IOException e) {
        	if (debug_import || debug) {
        		out.println("Something done fucked up (Error0x01 error in file reading)");
        	}
            return ERROR_1_import_IOException;
        } 
        // type of exception where there is a wrong number format (type is wrong or blank)  
        catch (NumberFormatException e) {
            if (debug_import || debug) {
            	out.println("Something done fucked up (Error0x02 error in type conversion - check class throw for more details)");
            }
            return ERROR_2_import_typeConversionOrBlank;
        }
       
    	if (debug_import || debug) {
    		out.println("end of fxn fileSettings check (succeeded):");
    		out.println("fileSettings_default: " + fileSettings_default);
    		out.println("fileSettings_stringTest: " + fileSettings_stringTest);
    		out.println("fileSettings_LUT: " + fileSettings_LUT);
    		out.println("fileSettings_WL: "+ fileSettings_WL);
    	}
        return SUCCESS_importData;
    }
    
    /**
     * @name: 		exportData()
     * @author: 	partially written in robocode's sittingduckbot
     * @purpose:	exports stored file data into strName.
     * @brief:		Export is done once per file.
     * 				1. the fxn contains multiple if-scopes, each for a file.
     * 				the first checks (stringName matches scope's target) 
     * 								 && (fileSettings_target is set) 
     *                               && (flag_preventMultipleFile is true)
     *              2. write into file
     *              	a. write fileSettings_target
     *              	b. write data
     *              3. flip flag_preventMultipleFile
     *              
     *              Config settings:
     * 				stringTest: 16400 (0x4010)
     * 				strLUT:		16416 (0x4020), zeroLUT = 16417 (0x4021)
     * 				WL:			16448 (0x4040)
     * 
     * @param: 		1. string of file name
     * 				and uses:
     * 				1. bool flag_LUTImported, static flag for preventing multiple imports
     * 				
     */

    public int exportData(String strName) {
    	if (debug_export || debug) {
    		out.println("@exportData: beginning");
    		out.println("printing fileSettings: ");
    		out.println("fileSettings_default: " + fileSettings_default);
    		out.println("fileSettings_stringTest: " + fileSettings_stringTest);
    		out.println("fileSettings_LUT: " + fileSettings_LUT);
    		out.println("fileSettings_WL: "+ fileSettings_WL);
    	}
    	
    	//this condition prevents wrong file from being accidentally deleted due to access by printstream.
    	if(  ( (strName == strStringTest) && (fileSettings_stringTest > 0) && (flag_stringTestImported == true) ) 
    	  || ( (strName == strLUT) && (fileSettings_LUT > 0) && (flag_LUTImported == true) ) 
    	  || ( (strName == strWL) && (fileSettings_WL > 0) && (flag_WLImported == true) )){
	    	
    		PrintStream w = null;
	        
	        try {
	            w = new PrintStream(new RobocodeFileOutputStream(getDataFile(strName)));
	            // different commands between files
	            if (w.checkError()) {
	                //Error 0x03: cannot write
	            	if (debug_export || debug) {
	            		out.println("Something done fucked up (Error 6 cannot write)");
	            	}
	            	return ERROR_6_export_cannotWrite;
	            }
	            
	            //if scope for exporting files to stringTest
	            if ( (strName == strStringTest) && (fileSettings_stringTest > 0) && (flag_stringTestImported == true) ) {
	            	
	            	//debug
	            	if (debug_export || debug) {
	            		out.println("writing into strStringTest");
	            	}
	            	
	            	w.println(fileSettings_stringTest);
	            	flag_stringTestImported = false;
	            	
	            } //end of testString

	            //winlose
	            else if ( (strName == strWL) && (fileSettings_WL > 0) && (flag_WLImported == true) ){
	            	if (debug_export || debug) {
	            		out.println("writing into winLose");
	            	}
	            	w.println(fileSettings_WL);
	            	w.println(totalFights+1);
	            	for (int i = 0; i < totalFights; i++){
	        			w.println(battleResults[i]);
	            	}
	        			w.println(currentBattleResult);
	            	flag_WLImported = false;
	            }// end winLose
	            
	            
	            /* to add new files for exporting data
	             * such as saveData
	             */
	                        
	            
	            else {
	            	if (debug_export || debug) {
	            		out.println("error 9");
	            		
	            	}
	            	return ERROR_9_export_dump;
	            }
	        }
	        
	        //OC: PrintStreams don't throw IOExceptions during prints, they simply set a flag.... so check it here.
	        catch (IOException e) {
	    		if (debug_export || debug) {
	    			out.println("IOException trying to write: ");
	    		}
	            e.printStackTrace(out); //Joey: lol no idea what this means
	            return ERROR_7_export_IOException;
	        } 
	        finally {
	            if (w != null) {
	                w.close();
	            }
	        }      
	        if (debug_export || debug) {
	        	out.println("(succeeded export)");
	        }
	        return SUCCESS_exportData;
    	}
    	
    	//this should prevent wiping INDIRECTLY if import error. If import was successful, then config flag was set.
    	//goal is to prevent accidentally wiping irrelevant file
    	else {
    		return ERROR_10_export_mismatchedStringName;
    	}
    }

    
//    public int saveData(String strName) {
//    	PrintStream w = null;
//        try {
//            w = new PrintStream(new RobocodeFileOutputStream(getDataFile(strName)));
////        	out.println("writing into strSA");
//        	//DEBUG
//        	if (debug_export || debug) {
//        		out.println("writing into strSA");
//        	}
//            for (int p0 = 0; p0 < roboLUTDimensions[0]; p0++) {
//                for (int p1 = 0; p1 < roboLUTDimensions[1]; p1++) {
//                	for (int p2 = 0; p2 < roboLUTDimensions[2]; p2++) {
//                		for (int p3 = 0; p3 < roboLUTDimensions[3]; p3++) {
//                			for (int p4 = 0; p4 < roboLUTDimensions[4]; p4++) {
//                				for (int p5 = 0; p5 < roboLUTDimensions[5]; p5++) {
//                					for (int p6 = 0; p6 < roboLUTDimensions[6]; p6++) {
//                						w.println("\t" + p0+p1+p2+p3+p4+p5+p6);
//                					}
//                				}
//            				}
//                		}
//                	}
//                }
//            }
//	    } catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	    finally {
//	        if (w != null) {
//	            w.close();
//	        }
//	    } 
//	    return SUCCESS_exportData;
//	}
    

    /**binaryActivation function
     * @param x
     * @return squashedValue. 
     */
 	public double binaryActivation(double x) {
// 		System.out.println("BINARY ");
 		double newVal = 1/(1 + Math.exp(-x)); 
// 		System.out.println("binary " + newVal );
 		return newVal;
 	}
 	
 	/**Function name: bipolarActivation 
 	 * @param: current hidden value "z"
 	 * @return: new value evaluated at the f(x) = (2/(1 + e(-x))) - 1 
 	**/ 	
 	public double bipolarActivation(double x) {
 		double newVal = (2/(1 + Math.exp(-x)))-1; 
 		return newVal; 
 	}
 	/** Function name: binaryDerivative
 	 * @param: input to take the derivative of based on f'(x) = f(x)*(1-f(x)). 
 	 * @return: derivative of value. 
 	 * 
 	 **/
 	public double binaryDerivative(double x) {
 		double binFunc = binaryActivation(x);
 		double binDeriv = binFunc*(1 - binFunc); 
 		return binDeriv;
 	}
 	/** Function name: bipolarDerivative
 	 * @param: input to take the derivative of. 
 	 * @return: derivative of value: f'(x) =  0.5*(1 + f(x))*(1 - f(x));
 	 * 
 	 **/
 	public double bipolarDerivative(double x) {
 		double bipFunc = bipolarActivation(x);
 		double bipDeriv = 0.5*(1 + bipFunc)*(1 - bipFunc);  
 		return bipDeriv;
 	}
}