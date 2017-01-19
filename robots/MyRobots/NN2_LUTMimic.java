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
	//joey: gotta track Q_val somehow, maybe tally up differences and write it in at the end
	/*
	 * SAV Change Rules:
	 * 1. update STATEACTION VARIABLES
	 * 2. update roboLUT initialization
	 * 3. update roboLUTDimensions
	 * 4. If adding or deleting states: Change for loops in import and export functions 
	 *    (twice in import including zeroLUT loop, once in export)
	 * 5. Update the corresponding dimension in generateCurrentStateVector()
	 * 6. if adding or deleting states: In getMax(), update the roboLUT access in the maxQ for loop.
	 * 7. if adding or deleting states: In qFunction(), update the roboLUT access.
	 * 8. if adding or deleting states: In updateLUT(), update roboLUT access as well as DEBUG
	 * 9. if adding or deleting actions: In doAction(), edit accordingly.
	 */
	
	/*
	 * SAV Change Rules: //Joey: finish this
	 */
	
	/**
	 * ===================================FINALS (defines)====================================================================================
	 */
	 //variables for the q-function. Robot will NOT change analysis pattern mid-fight.
	//alpha describes to which extent the newly acquired information will override the old information.
    private static final double alpha = 0.1;
    //gamma describes the importance of future rewards
    private static final double gamma = 0.8;                
    //epsilon describes the degree of exploration
    private static final double epsilon = 0.01; 				 
    
    //policy:either greedy or exploratory or SARSA 
    private static final int greedy = 0;
    private static final int exploratory = 1;
    private static final int SARSA = 2;
    
    
    /* 
     * CONFIGMASK - used as settings written in data files to be read by functions. 
     * 			  - contains 2 bytes (4 hex digits)
     * 			  - functions will use AND conditions to evaluate if the settings in the file match the mask. 
     */
    // _ _ _ _  _ _ _ _  _ _ _ _  _ _ _ _ 
    //   MSnib		filename		LSnib
    // MSnib is the first nibble baby (4 bits)
    // the 2nd and 3rd nibbles are used for recognizing specific files
    // LSnib is used for file-specific settings.
    //
    // Current available config settings:
    //     				stringTest: 16400 (0x4010)
    // 					strLUT:		16416 (0x4020), zeroLUT = 16417 (0x4021)
    //     				WL:			16448 (0x4040), zero WL = 16449 (0x4041)
    //					NN weights: 16512 (0x4080), zeroing = 16513 (0x4081)
    private static final short CONFIGMASK_ZEROINGFILE  =				0x0001;
    private static final short CONFIGMASK_VERIFYSETTINGSAVAIL = 		0x4000;
    private static final short CONFIGMASK_FILETYPE_stringTest =			0x0010;
    private static final short CONFIGMASK_FILETYPE_LUTTrackfire =		0x0020;
    private static final short CONFIGMASK_FILETYPE_winLose = 			0x0040;
    private static final short CONFIGMASK_FILETYPE_weights =			0x0080;
    
    //IMPORT/EXPORT status returns.
    
    private static final int SUCCESS_importData = 						0x00;
    private static final int SUCCESS_exportData = 						0x00;
    private static final int SUCCESS_importDataWeights =				0x00;
    private static final int SUCCESS_exportDataWeights =				0x00;
    
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
    private static final int ERROR_12_importWeights_IOException = 		12;
    private static final int ERROR_13_importWeights_typeConversionOrBlank = 13;
    private static final int ERROR_14_exportWeights_cannotWrite_NNWeights_inputToHidden = 14;
    private static final int ERROR_15_exportWeights_cannotWrite_NNWeights_hiddenToOutput = 15;
    private static final int ERROR_16_exportWeights_IOException = 		16;
    private static final int ERROR_17 = 								17;
    private static final int ERROR_18 = 								18;
    private static final int ERROR_19_import_wrongFileName_weights =	19;
    private static final int ERROR_20_import_weights_wrongNetSize =		20;
    private static final int ERROR_21 = 								21;
    
    /*
	 * NN STATEACTION VARIABLES for stateAction ceilings (for array designs and other modular function interactions).
	 * 
	 */
    //Action related finals.
    //- One concern with the complexity of the action design for the robot is the amount of calculation time spent in forward propagation. 
    //- As of now, the No. times NN needs to forward propagate per round = 4 * 2 * 3 = 24
    private static final int input_action0_moveReferringToEnemy_possibilities = 4; //0ahead50, 0ahead-50, -90ahead50, -90ahead-50
    private static final int input_action1_fire_possibilities = 2;    //1, 3
    private static final int input_action2_fireDirection_possibilities = 3;    //-10deg, 0, 10deg
    private static final int numActionContainers = 3;
    
    private static final int numActions = input_action0_moveReferringToEnemy_possibilities 
    									  * input_action1_fire_possibilities
    									  * input_action2_fireDirection_possibilities;
    
    //State related finals.
    private static final int input_state0_myPos_possibilities = 5;    //center, left, right, top, bottom (cannot be undiscretized) 
    private static final int input_state1_myHeading_originalPossilibities = 4;    //0-89deg, 90-179, 180-269, 270-359
    private static final int input_state2_enemyEnergy_originalPossibilities = 2;    //>30, <30
    private static final int input_state3_enemyDistance_originalPossibilities = 3;    //<150, <350, >=350
    private static final int input_state4_enemyDirection_originalPossibilities = 3;    //head-on (still (abs <30 || >150), left (<0 relative dir w/ positive velo || >0 with negative velo), right (<0 dir w/ negative velo || >0 with positive velo)
    private static final int numStateContainers = 5;
    
    //NN neuron parameters.
    private static final int numInputBias = 0;
    private static final int numHiddenBias = 1;
    private static final int numOutputBias = 0; //Actual code disregards possibility of output bias by starting loops relating to output at 0 for first output.
    private static final int numHiddenNeuron = 4;
    private static final int numInputsTotal = ( numInputBias + numActionContainers + numStateContainers ); 
    private static final int numHiddensTotal = ( numHiddenBias+ numHiddenNeuron );
    private static final int numOutputsTotal = 1;
    
    
    
    //NN activation function choices
    private static final boolean binaryMethod = true;
    private static final boolean bipolarMethod = false;
 
    
    
    /**
     * STRINGS used for importing or extracting files =========================================== 
     */
    String strStringTest = "stringTest.dat";    
    String strLUT = "LUTTrackfire.dat";
    String strWL = "winlose.dat";
    String strSA = "stateAction.dat"; 
    String strError = "saveErrorForActions.dat" ;
    String strWeights = "weights.dat";
    String strLog = "templog.txt";
    
    /**
     * FLAGS AND COUNTS ===========================================================================
     */
    //DEBUG flags. Each allows printouts written for specific functions. DEBUG will print out all.
    private final static boolean DEBUG = false;
//	private final static boolean DEBUG_run = false;
	private final static boolean DEBUG_onScannedRobot = true;
	private final static boolean DEBUG_analysis = false;
//	private final static boolean DEBUG_learnThisRound = false;
	private final static boolean DEBUG_obtainReward = false;
//	private final static boolean DEBUG_copyCurrentQValueIntoPrev = false;
	private final static boolean DEBUG_generateCurrentStateVector = true;
//	private final static boolean DEBUG_RL_and_NN = false;
	private final static boolean DEBUG_getAllQsFromNet = false;
	private final static boolean DEBUG_forwardProp = false; //can be used to debug for the multiple fxns encompassed by FP.
	private final static boolean DEBUG_getMax = true;
	private final static boolean DEBUG_qFunction = false;
	private final static boolean DEBUG_backProp = false;
//	private final static boolean DEBUG_resetReward = false;
    private final static boolean DEBUG_doAction_Q = false;
//	private final static boolean DEBUG_doAction_notLearning = false;
//	private final static boolean DEBUG_doAction_mandatoryPerTurn = false;
//	private final static boolean DEBUG_importDataWeights = false;
//	private final static boolean DEBUG_exportDataWeights = false;
    private final static boolean DEBUG_import = false;
    private final static boolean DEBUG_export = false;
    
    // Flags used in data imp/exp fxns.
    //		Purposes:
    // 		1. prevents overwrite, and protects against wrong file entries
    //		2. data for a particular file must be exported before importing for the same file occurs again.
    // 		false == only imports can access; true == only exports can access.
    //		Always initialize these as false.
    private boolean flag_stringTestImported = false;
    private boolean flag_LUTImported = false;
    private boolean flag_WLImported = false;
    private boolean flag_weightsImported = false;
    private boolean flag_alreadyExported = false;    
    private static boolean flag_useOfflineTraining = true; //Joey: check usage 

    // printout error flag - used to check function return statuses.
    // initialized to 0, which is no error.
    private int flag_error = 0;


    /**
     *  OTHER VARIABLES USABLE BY THIS ROBOT'S CLASS FUNCTIONS ==============================================================================
     */
    
    
    // weights connecting between input and hidden layers. calculated using definitions defined above.
    private static double[][] arr_wIH 
        = new double
        [numInputsTotal]
        [numHiddensTotal] 
        ;
    
    // weights connecting between hidden layer to output.
    private static double[][] arr_wHO
    	= new double
    	[numHiddensTotal]
    	[numOutputsTotal]
    	;
    

    // temp vars for importing/exporting files: config settings for the external files, stored in the first line of .dat
    private short fileSettings_temp = 0;
    private short fileSettings_stringTest = 0;
    private short fileSettings_LUT = 0; 
    private short fileSettings_WL = 0;
    private short fileSettings_weights = 0;
    private short fileSettings_log = 0;
    

    // reward and reward calculation vars.
    private double reward = 0.0;
    private double reward_normalized = 0.0;
    private double energyDiffCurr = 0.0;
    private double energyDiffPrev = 0.0;
    

    //vars that store current and previous stateAction vectors
    private double currentStateActionVector[] = new double [numInputsTotal];
    private double prevStateActionVector[]    = new double [numInputsTotal]; //might not be used 

    //Q-var storages.
    //- "Y" and "Q" pretty much refers to the same thing, but to make it easier to understand when coding, we use "Y" for the BP calculations.
    private double Q_curr = 0.0; // stores the maximum currSAV QMax
    private double Q_prev = 0.0;
    private double Q_target = 0.0;
    private double[] Y_calculated = new double [numOutputsTotal]; 
    private double[] Y_target = new double [numOutputsTotal]; 
    
    //array to store the q values obtained from net forward propagation, using the current state values as well as all possible actions as inputs. 
    private double [][][] Q_NNFP_all = new double 
    		[input_action0_moveReferringToEnemy_possibilities]
    		[input_action1_fire_possibilities]
    		[input_action2_fireDirection_possibilities];

    //variables used for getMax.
    private int [] action_QMax_all = new int [numActions]; //array for storing all actions with maxqval
    private int action_QMax_chosen = 0; 				//stores the chosen currSAV with maxqval before policy							
    private int randomVal_actions = numActions;

    //chosen policy. greedy or exploratory or SARSA 
    private static int policy = exploratory; //SARSA 
    private static int learningRate = 4; //learningAlgo is run every 4 ticks. //Joey: consider allowing robot to self-optimize for this.

    //enemy bot information
    private double enemyDistance = 0.0;
    private double enemyHeadingRelative = 0.0;
    private double enemyHeadingRelativeAbs = 0.0;
    private double enemyVelocity = 0.0;
    private double enemyBearingFromRadar = 0.0;
    private double enemyBearingFromGun = 0.0;
    private double enemyBearingFromHeading = 0.0;
    private double enemyEnergy = 0.0;
    
    //my bot information
    private double myHeading = 0.0; 
    private double myEnergy = 0.0;
    private double myPosX = 0.0;
    private double myPosY = 0.0;
    
    //misc battle information
    private int turn = 0;
    
    //used to update WL export
    private int totalFights = 0;
    private int[] battleResults = new int [520000];
    private int currentBattleResult = 0;
	
    //used for debugging purposes (deprecated)
    private static double[] QErrors = new double [520000];
    private static int currentRoundOfError = 0;

    /**  
     * Neural net stuff
     * 
     */

    private double [] Z_in = new double[numHiddensTotal]; 		// Array to store Z[j] before being activate
	private double [] Z    = new double[numHiddensTotal];		// Array to store values of Z 
	private double [] Y_in = new double[numOutputsTotal];		// Array to store Y[k] before being activated
	private double [] Y	   = new double[numOutputsTotal];		// Array to store values of Y
	
    // analysis rate //Joey: ask Andrea about use of this.
	private double lRate = 0.05; 			
	//value of momentum //Joey: consider adding some momentum lel
	private double momentum = 0.1;  		
	
	// arrays used for momentum
	private double [][] vPast  = new double[numInputsTotal] [numHiddensTotal];	// Input to Hidden weights for Past.
	private double [][] vNext  = new double[numInputsTotal] [numHiddensTotal];	// Input to Hidden weights.
	private double [][] wPast  = new double[numHiddensTotal][numOutputsTotal];  // Hidden to Output weights for Past.
	private double [][] wNext  = new double[numHiddensTotal][numOutputsTotal];  // Hidden to Output weights.
	//arrays in BP
	private double [][] vDelta = new double[numInputsTotal] [numHiddensTotal];	// Change in Input to Hidden weights
	private double [][] wDelta = new double[numHiddensTotal][numOutputsTotal]; 	// Change in Hidden to Output weights
	  
	private double [] delta_out    = new double[numOutputsTotal];
	private double [] delta_hidden = new double[numHiddensTotal];
	
	private boolean activationMethod = bipolarMethod; 
	
	//bias for hidden initialized as value 1
    private int valHiddenBias = 1;
    
    /*
     * other misc vars
     */
    private static String[] LOG = new String [520000];
    private static int lineCount = 0;
    
    //@@@@@@@@@@@@@@@ RUN & EVENT CLASS FUNCTIONS @@@@@@@@@@@@@@@@@    
    
    /**
     * @name: 		run
     * @purpose:	1. Initializes robot colour
     * 				2. Clears log from previous session in case it used up all alloted space.
     * 				3. Imports weights, win-lose records, and any other files desired by the user.
     * 				4. Enters infinite scan mode until enemy robot is scanned, which triggers event onScannedRobot. This is where
     * 				   most of the robot's logic begins.
     * @param:		n
     * @return:		n
     */
   
    public void run() {
        
        // Sets Robot Colors.
        setColors();
        
        if (DEBUG) {
        	out.println("@I have been a dodger duck (robot entered run)"); 
        }
        
        // Import data. ->Change imported filename here<-
        
        //clears log from previous session in case it used up all allowed harddrive. (robocode allows for 200kB of external data per robot)
        fileSettings_log += CONFIGMASK_ZEROINGFILE;
        flag_error = exportData(strLog);
        if(flag_error != SUCCESS_importData) {
        	out.println("ERROR @run blankingWeights: " + flag_error);
        }
        fileSettings_log -= CONFIGMASK_ZEROINGFILE;
        
        flag_error = importDataWeights();
        if(flag_error != SUCCESS_importDataWeights) {
        	out.println("ERROR @run weights: " + flag_error);
        }
        
        flag_error = importData(strWL);
        if( flag_error != SUCCESS_importData) {
        	out.println("ERROR @run WL: " + flag_error);
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

    	
    	flag_error = exportDataWeights();	
        if(flag_error != SUCCESS_exportDataWeights) {
        	out.println("ERROR @onBattleEnded weights: " + flag_error); //only one to export due to no learningloop(), but fileSettings_
        	//LUT is 0'd, causing error 9 (export_dump)
        }
        
        
        flag_error = exportData(strError);					//"strError" = saveError.dat
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onBattleEnded Error: " + flag_error);
        }
        
    	flag_error = exportData(strLog); //export log first					
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onBattleEnded Log: " + flag_error);
        }
        
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
        if( flag_error != SUCCESS_exportDataWeights) {
        	out.println("ERROR @onDeath weights: " + flag_error);
        }
        
        flag_error = exportData(strWL);					//"strWL" = winLose.dat
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onDeath WL: " + flag_error);
        }
        
        flag_error = exportData(strError);					//"strError" = saveError.dat
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onDeath WL: " + flag_error);
        }        

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
        if( flag_error != SUCCESS_exportDataWeights) {
        	out.println("ERROR @onWin weights: " + flag_error);
        }
        
        flag_error = exportData(strWL);
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onWin WL: " + flag_error);
        }
        
        flag_error = exportData(strError);
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onWin WL: " + flag_error);
        }

	}

	/**
     * @name:		onScannedRobot
     * @brief:		This function is called by the game when the enemy is located by the scanner. It is the only function that will obtain info on the current game 
     * 				 environment, and decide what the robot will perform during this turn.
     * @purpose:	1. determines:
     * 					- my heading
     * 					- my position: x and y
     * 					- my energy
     * 					- enemy heading
     * 					- enemy velocity
     * 					- enemy bearing
     * 					- enemy distance
     * 					- enemy energy
     * 					- current turn
     * 				2. call analysis fxn to determine the next move for this turn.
     * @param:		ScannedRobotEvent event
     * @return:		none, but updates:
     * 				1. getGunBearing
     * 				2. enemyDistance
     */

	public void onScannedRobot(ScannedRobotEvent event){
		
		myHeading = (int)getHeading();
		myPosX = (int)getX();
		myPosY = (int)getY();
		myEnergy = (int)getEnergy();
		enemyHeadingRelative = (int)normalRelativeAngleDegrees(event.getHeading() - getGunHeading());
		enemyHeadingRelativeAbs = Math.abs(enemyHeadingRelative);
		enemyVelocity = (int)event.getVelocity();
		enemyBearingFromRadar = (double)myHeading + event.getBearing() - getRadarHeading();
		enemyBearingFromGun = (double)myHeading + event.getBearing() - getGunHeading();
		enemyBearingFromHeading = event.getBearing();
		enemyDistance = (int)event.getDistance(); 
		enemyEnergy = (int)event.getEnergy();
		turn = (int)getTime();
		
    	
    	analysis();
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
     * @name:		analysis
     * @purpose:	1. Analyze all environmental and self conditions.
     * 				2. Perform action.
     * @NNet:		Our neural net online training involves the following:
     * 				0. (not in NN) Determine if learning should happen this turn. Fxn learnThisRound returns a boolean.
     * 				1. Calculate how well we have done since the last NN update. (The time between this NN access and previous NN update is called a round)
     * 				2. Store the previous state and action (currentSAV -> prevSAV)
     * 				3. Use information from the environment to determine the current state.
     * 				4. Perform QFunction (detailed further in function).
     * 				5. Reset rewards. IE: all events affect reward only once unless further emphasized by events other than onScannedRobot. (NONE YET)
     * 				6. Perform chosen action. (learning-specific as well as those mandatory per turn).
     * @param:		n
     * @return:		n
     */
    /* Training online detailed by A (important, several points repeated from @NNet): 
     *  step (1) - need a vector of just states "copyCurrentSV into prev SV". 
        step (2) - get weights array - neural net do forwardpropagation for each action in the "CurrentSV"  , remembering all outputs "Y" in an array
        step (3) - choose maximum "Y" from array 
        step (4) - call qFunction() below using prevSAV as qOld and qNew is the SAV with max Y (chosen in step (3)) 
        		   - with return being the qOld_(new) 
        step (5) - with prevSAV (inputs) and qOld_new (correct target)  and qOld (calculated output), run backpropagation & save weights 
        step (6) - save error for graph
        step (7) - repeat steps 1-6 using saved weights from backpropagation to feed into NN for step (2)  
     */
    public void analysis() {
    	if (learnThisRound()){
    		
    		//this debug fxn is related to onScannedRobot fxn, but placed here so that we can log it only when RL is firing.
        	if(DEBUG_onScannedRobot || DEBUG) {
        		LOG[lineCount++] = "@@@ TURN " + turn + ":";
        		LOG[lineCount++] = "myHeading:" + myHeading + "\tmyPosX:" + myPosX + "\tmyPosY:" + myPosY + "\tmyEnergy:" + myEnergy;
        		LOG[lineCount++] = "enemyHeadingRelative:" + enemyHeadingRelative + "\tenemyVelocity:" + enemyVelocity;
        		LOG[lineCount++] = String.format("enemyBearingFromRadar:%.1f enemyBearingFromGun:%.1f enemyBearingFromHeading:%.1f", enemyBearingFromRadar, enemyBearingFromGun, enemyBearingFromHeading);
        		LOG[lineCount++] = "enemyDistance:" + enemyDistance + "\tenemyEnergy" + enemyEnergy;
        	}
    		
    		if(DEBUG_analysis || DEBUG) {
        		LOG[lineCount++] = "- analysis (weight vals)";
        		LOG[lineCount++] = "arr_wIH:" + Arrays.deepToString(arr_wIH);
        		LOG[lineCount++] = "arr_wHO:" + Arrays.deepToString(arr_wHO);
        	}
    		
    		obtainReward();
            copyCurrentQValueIntoPrev();
            generateCurrentStateVector();
            RL_and_NN();
            resetReward();
            doAction_Q();
    	}
        else {
        	doAction_notLearning();
        }
    	doAction_mandatoryPerTurn();
    	
    }

    /**
     * @name:		boolean learnThisRound
     * @purpose:	To determine if analysis algo should be run this round
     * @param:		none, but uses int turn
     * @return:		boolean
     */

    public boolean learnThisRound() {
    	if (turn%learningRate == 0)
    		return true;
    	else
    		return false; 
    }
    
	/**
     * @name:		obtainReward
     * @purpose:	calculates reward based on change in energy difference of robots. A later function will normalize the reward value.
     * @param:		none
     * @return:		none
     */
    public void obtainReward(){
    	energyDiffPrev = energyDiffCurr;
    	energyDiffCurr = myEnergy - enemyEnergy;
    	reward += energyDiffCurr - energyDiffPrev;
    	reward_normalized = bipolarActivation(reward); 
    	
    	if(DEBUG_obtainReward || DEBUG) {
    		LOG[lineCount++] = "- rewards";
    		LOG[lineCount++] = String.format("reward:%f reward(normalized):%.3f", reward, reward_normalized);
    	}
    }
    
    /**
     * @name:		copyCurrentQValueIntoPrev
     * @purpose:	Copies max Q obtained from FP in last round into Q_prev.
     * @param:		n, but uses:
     * 				1. Q_curr 
     * @return:		n
     */
    public void copyCurrentQValueIntoPrev(){
    	Q_prev = Q_curr; 
    }
    
    /**
     * @name: 		generateCurrentStateVector
     * @brief:		Obtains robot values from
     * @purpose: 	1. gets state values from environment. 
     * 				2. Update array of current stateAction vector.  
     * @param: 		n
     * @return: 	none
     * currentStateVector positions [0][1][2] are all the actions. 
     */
    public void generateCurrentStateVector(){
    	//First few INPUTS are ACTIONS and hence will be IGNORED for generating CSAV
    	//INPUTS 0, 1 and 2 are ACTION
        
    	//Dimension 3 - private static final int input_state0_myPos_possibilities = 5;
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

    	//Dimension 4 - private static final int input_state1_myHeading_originalPossilibities = 4;
    	currentStateActionVector[4] = myHeading*4/360;			//to normalize. 
    	
    	//Dimension 5 - enemyEnergy
    	if (enemyEnergy < 30){
    		currentStateActionVector[5] = enemyEnergy/60;
    	}
    	
    	else if (enemyEnergy >= 30){
    		currentStateActionVector[5] =((enemyEnergy-30)/70)+0.5;
    	}
    	//Dimension 6:  //<150, <350, >=350(to1000)
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
    	
		//Dimension 7: is enemy moving right, left, or within the angle of my gun?
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
    	
    	if (DEBUG_generateCurrentStateVector || DEBUG){
    		LOG[lineCount++] = "currentSAV:" + Arrays.toString(currentStateActionVector);
    	}
    	
    }
     
 


    /**
     * @name:		RL_and_NN
     * @purpose: 	1. Obtain the action in current state with the highest q-value FROM the outputarray "Yout" of the neural net. 
     * 				2. The q value is the maximum "Y" from array
     * 				3. Q_curr is assigned prevQVal 
     * 				4. Y_calculated is previousNetQ
     * 			    5. run backProp with the input X as currentStateActionVector, qExpected as currentNetQ, Y_calculated is prevNetQVal 
     * @param: 		none, but uses:
     * 				1.	double reward 
     * 				2.	int currentStateVector[] already discretized (size numStateContainers)
     * 				3.	double[].. LUT table, 
     * 				4.	int [] currentStateActionVector. 
     * @return: 	n
     */
    public void RL_and_NN(){
    	getAllQsFromNet();
        getMax(); 
        qFunction();
        backProp(); 
    }

    /** 
     * @name:		getAllQsFromNet
     * @input: 		currentStateVector 
     * @purpose: 	1. For current state, cycle through all possible actions and obtain the action in current state with the highest q-value 
     * 					from the outputarray "Yout" of the neural net. 
     * @param:		n //Joey: i think
     * @return: 	n
     */

	public void getAllQsFromNet() {
		if (DEBUG_getAllQsFromNet || DEBUG_forwardProp || DEBUG){
			LOG[lineCount++] = "- FP";
    	}

		for (int i_A0 = 0; i_A0 < input_action0_moveReferringToEnemy_possibilities; i_A0++){
			for (int i_A1 = 0; i_A1 < input_action1_fire_possibilities; i_A1++){
				for(int i_A2 = 0; i_A2 < input_action2_fireDirection_possibilities; i_A2++){
					currentStateActionVector[0] = i_A0;
					currentStateActionVector[1] = i_A1;
					currentStateActionVector[2] = i_A2;
					Q_NNFP_all[i_A0][i_A1][i_A2] = forwardProp(); 
				}
			}
		}

    	if (DEBUG_getAllQsFromNet || DEBUG_forwardProp || DEBUG){
    		LOG[lineCount++] = "Q_NNFP_all going into getMax:" + Arrays.deepToString(Q_NNFP_all);
    	}
    	
    	return;
	}
	
//Joey: check all fxn readme's to make sure they all make sense
	/** 
	 * @brief: forward propagation done in accordance to pg294 in Fundamentals of Neural Network, by Laurene Fausett.
	 * 			Feedforward (step 3 to 5):
	 * 				step 3: Each input unit (X[i], i = 1, ..., n) receives input signal xi and broadcasts this signal to all units in the layer above (the hidden units).
	 * 				step 4: Each hidden unit (Z[j], j = 1, ..., p) sums its weighted input signals,
	 * 								Z_in[j] = v[0][j] + (sum of from i = 1 to n)x[i]v[i][j],                <- v = weights between input and hidden.
	 * 						applies its activation fxn to compute its output signal,
	 * 								Z[j] = f(Z_in[j]),
	 * 						and sends this signal to all units in the layer above (output units).
	 * 				step 5: Each output unit (Y[k], k = 1, ..., m) sums its weighted input signals, (treating k = 0 to start instead of 1 for now b/c no output)
	 * 								Y_in[k] = w[0][k] + (sum of from j = 1 to p)Z[j]w[j][k]                 <- w = weights between hidden and output.
	 * 						and applies its activation fxn to compute its output signal,
	 * 								Y[k] = f(Y_in[k])
	 * @purpose: does forwardPropagation on the inputs from the robot. 
	 * @return: an array of Y values for all the state pairs. 
	 **/
    public double forwardProp() {
    	if (DEBUG_forwardProp || DEBUG){
    		LOG[lineCount++] = "selectedSAV:" + Arrays.toString(currentStateActionVector);
    	}
    	
    	//step 3 and 4:    	
		for (int j = 1; j < numHiddensTotal; j++){ 		//p = numHiddensTotal
			double sumIn = 0.0;
			for (int i = 0; i < numInputsTotal; i++){	   //n = numInputsTotal
				sumIn += currentStateActionVector[i]*arr_wIH[i][j]; //NO INPUT BIAS, that's why j = 1
			}
			Z_in[j] = sumIn; 									//save z_in[0] for the bias hidden unit. 
			Z_in[0] = valHiddenBias; 									//set z_in[0] = bias. HIDDEN BIAS = 1
			Z[0] = Z_in[0]; //can choose to optimize here if needs be: run during run.
			
			if (activationMethod == binaryMethod)
				Z[j] = binaryActivation(Z_in[j]); 				
			else
				Z[j] = bipolarActivation(Z_in[j]);
			
			if (DEBUG_forwardProp || DEBUG){
				LOG[lineCount++] = String.format("Z[%d]:%.3f Z_in[%d]:%.3f sumIn%.3f", j, Z[j], j, Z_in[j], sumIn);
			}
			
		}
		//step 5:
		for (int k = 0; k < numOutputsTotal; k++){
			double sumOut = 0.0; 
			for (int j= 0; j < numHiddensTotal; j++){
				sumOut += Z[j]*arr_wHO[j][k]; 
			}
			Y_in[k] = sumOut; 	
			
			if (activationMethod == binaryMethod)
				Y[k] = binaryActivation(Y_in[k]); 
			else
				Y[k] = bipolarActivation(Y_in[k]);
			
			if (DEBUG_forwardProp || DEBUG){
				LOG[lineCount++] = String.format("Y[%d]:%.3f Y_in[%d]:%.3f sumOut%.3f", k, Y[k], k, Y_in[k], sumOut);
			}
			
		}
		return Y[0]; 
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
	 *							(3) Store the (now 3 dimension) action index into action_QMax_all[numMaxActions-1]
	 *						ii. if indexQVal == currMax:
	 *							(1) numMaxActions++
	 *							(2) Store the (now 3 dimension) action index into action_QMax_all[numMaxActions-1]
	 *						iii. if indexQVal < currMax:
	 *							ignore.
	 *					c. record chosen action. If multiple actions with max q-values, randomize chosen action.
	 *						i. if numMaxActions > 1, 
	 *						   randomly select between 0 and numMaxActions - 1. The randomed 
	 *						   number will correspond to the array location of the chosen
	 *						   action in action_QMax_all. 
	 *						ii. action_QMax_chosen = action_QMax_all[randomed number]
	 *					d. record associated q-value.
     * @param: 		none, but uses:
     * 				1.	current SAV[].
     * 				2.	roboLUT 
     * 				3.	currQArrayMax
     * @return: 	n
     */
    public void getMax() {
    	//currmax stores the maximum Q found.
    	double currMax = -100.0;  
    	//total number of actions with the same value as the max Q.
        int numMaxActions = 0;
        //used to generate a random number starting from 0 to numMaxActions.
        int randMaxAction = 0;
        //actions are listed in one container instead of in containers.
        int actionLinearized = 0;
        //reseting the global var that stores the chosen action with maximum Q.
        action_QMax_chosen = 0;
        //resets the array that stores all of Qmax, although randMaxAction should random between 0 and numMaxActions.
        Arrays.fill(action_QMax_all, 0);
        
    	if (DEBUG_forwardProp || DEBUG_getMax || DEBUG) {
        	LOG[lineCount++] = "Q_NNFP_all:" + Arrays.deepToString(Q_NNFP_all);
        }
    	
    	for (int i_A0 = 0; i_A0 < Q_NNFP_all.length; i_A0++){
		    for (int i_A1 = 0; i_A1 < Q_NNFP_all[0].length; i_A1++){
		    	for (int i_A2 = 0; i_A2 < Q_NNFP_all[0][0].length; i_A2++, actionLinearized++){
		    		if (Q_NNFP_all[i_A0][i_A1][i_A2] > currMax){
		    			currMax = Q_NNFP_all[i_A0][i_A1][i_A2];
		            	numMaxActions = 1;
		            	action_QMax_all[numMaxActions-1] = actionLinearized;		
		            }
		            else if (Q_NNFP_all[i_A0][i_A1][i_A2] == currMax){
		            	action_QMax_all[numMaxActions++] = actionLinearized;
		            }	            
		    	}
    		}
    	}
    	
    	if (DEBUG_forwardProp || DEBUG_getMax || DEBUG) {
        	LOG[lineCount++] = "action_QMax_all:" + Arrays.toString(action_QMax_all);
        }
        Q_curr = currMax;
        
        if (numMaxActions > 1) {
        	randMaxAction = (int)(Math.random()*(numMaxActions)); //math.random randoms btwn 0.0 and 0.999. Allows selection array position from 0 to num-1 through int truncation. 
        	
        	if (DEBUG_forwardProp || DEBUG_getMax || DEBUG) {
            	LOG[lineCount++] = ">1 max vals, randomly chosen action " + randMaxAction;
            }
        }
        
        //Choosing next action based on policy. Greedy is default
        //exploratory uses this line to perform if-false actions.
        action_QMax_chosen = action_QMax_all[randMaxAction]; //if numMaxActions <= 1, randMaxAction = 0;
        
        if (policy == SARSA || policy == exploratory) {
	    	randomVal_actions = (int)(Math.random()*(numActions));
	        if (policy == SARSA) {
	        	action_QMax_chosen = randomVal_actions;
	        }
	        else if(policy == exploratory) {
	        	action_QMax_chosen = (Math.random() > epsilon ? action_QMax_chosen : randomVal_actions);
	        }
        }
	        
        if (DEBUG_forwardProp || DEBUG_getMax || DEBUG) {
        	LOG[lineCount++] = "enacting policy:" + policy + "(0=gre 1=exp 2=SAR)";
        	LOG[lineCount++] = String.format("Action Chosen (linear) %d, with QVal:%.3f", action_QMax_chosen, Q_curr);
        }
        
        for (int i_A0 = 0; i_A0 < Q_NNFP_all.length; i_A0++){
		    for (int i_A1 = 0; i_A1 < Q_NNFP_all[0].length; i_A1++){
		    	for (int i_A2 = 0; i_A2 < Q_NNFP_all[0][0].length; i_A2++){
		    		if (action_QMax_chosen-- == 0) {
		    			currentStateActionVector[0] = i_A0; 
		    			currentStateActionVector[1] = i_A1;
		    			currentStateActionVector[2] = i_A2;
		    			
		    			if (DEBUG_forwardProp || DEBUG_getMax || DEBUG) {
		    	        	LOG[lineCount++] = "chosen actions(in containers):" + (int)currentStateActionVector[0] + " " + (int)currentStateActionVector[1] + " " + (int)currentStateActionVector[2];
		    	        }
		    			
		    			return;
		    		}
		    	}
		    }
        }
    }
    
    /**
     * @name		qFunction
     * @purpose		1. Calculate the new prev q-value based on Qvalue function.
     * @param		n, but uses:
     * 				1. Q_curr
     * @return		prevQVal
     	Q(s’,a’)-Q(s,a)
     */
    public void qFunction(){
    	
    	//Joey: ask andrea about papers for good gamma terms. (close to 1?)
    	Q_target = Q_prev + alpha*(reward_normalized + (gamma*Q_curr) - Q_prev); //Joey: mby bipolar activate the reward
    	
    	if (DEBUG_qFunction || DEBUG) {
    		LOG[lineCount++] = "- Q function";
    		LOG[lineCount++] = String.format("Q_target%.3f  Q_prev:%.3f  Q_curr:%.3f", Q_target, Q_prev, Q_curr);
    		LOG[lineCount++] = String.format("alpha:%.2f reward_N:%.3f gamma:%.2f", alpha, reward_normalized, gamma);
    	}
    }
    
    /**
     * @name:		runbackProp
     * @brief:		pg 295 in Fundamentals of Neural Networks by Lauren Fausett, Backpropagation of error: steps 6 to 8.
     * 				step 6:
     * 				Each output unit (Y[k], k = 1, ..., m) receives a target pattern corresponding to the input training pattern, computes its error information term,
     * 					delta[k] = (t[k] - y[k])f'(y_in[k]),
     * 				calculates its weight correction term (used to update w[j][k] later),
     * 					delta_weight_w[j][k] = alpha * delta[k] * Z[j],
     * 				calculates its bias correction term (used to update w[0][k] later),
     * 					delta_weight_w[0][k] = alpha * delta[k],
     * 				and continue to use delta[k] for lower levels.
     *				
     *				step 7: 
     *				Each hidden unit (Z[j], j = 1 ..., p) sums its delta inputs (from units in the layer above),
     *					delta_in[j] = (sum of from k = 1 to m)(delta[k] * w[j][k]),
     *				multiplies by the derivative of its activation fxn to calculate its error information term,
     *					delta[j] = delta_in[j] * f'(z_in[j]),
     *				calculates its weight correction term (used to update v[i][j] later),
     *					delta_weight_v[i][j] = alpha * delta[j] * x[i],
     *				and calculates its bias correction term (used to update v[0][j] later),
     *					delta_weight_v[0][j] = alpha * delta[j].
     *				
     *				step 8: Update weights and biases
     *				Each output unit (Y[k], k = 1, ..., m) updates its bias and weights (j = 0, ..., p):
     *					w[j][k](new) = w[j][k](old) + delta_weights_w[j][k].
     *				Each hidden unit (Z[j], j = 1, ..., p) updates its bias and weights (i = 0, ..., n):
     *					v[i][j](new) = v[i][j](old) + delta_weights_v[i][j].
     *
     *				To assist with rate of convergence, we have also included the ability for the net to use momentum. Momentum requires data one or more previous
     *				training patterns. In the simplest form, the weights at t+1 are based on the weights at t and t-1:
     *					w[j][k](t+1) = w[j][k](t) + alpha*delta_out[k]*Z[j] + mu[w[j][k](t) - w[j][k](t-1)],
     *				and
     *					v[i][j](t+1) = v[i][j](t) + alpha*delta_in[j]*X[i] + mu[v[i][j](t) - v[j][k](t-1)].
     * @param:		n, but uses many global NN parameters.
     * @return:		n
     */
    public void backProp() {      
    	double[] temp = new double [numOutputsTotal];
    	
    	if (DEBUG_backProp || DEBUG) {
			LOG[lineCount++] = "- BP";
			LOG[lineCount++] = "momentum:" + momentum;
		}
    	
    	Y_calculated[0] = Q_prev; 
        Y_target[0] = Q_target; 
        
    	//step 6-8 for hidden-to-output weights
        
        if (DEBUG_backProp || DEBUG) {
			LOG[lineCount++] = "@output cycle:";
			LOG[lineCount++] = "arr_wHO(pre):" + Arrays.deepToString(arr_wHO);
		}
        
		for (int k = 0; k <numOutputsTotal; k++){ // m = numOutputsTotal. pretending output bias doesn't exist so our output vector starts at 0 (horrificallylazyXD)
			if (activationMethod == binaryMethod){
				temp[k] = binaryDerivative(Y_in[k]);
				delta_out[k] = (Y_target[k] - Y_calculated[k])*temp[k]; 
			}
			else{
				temp[k] = bipolarDerivative(Y_in[k]);
				delta_out[k] = (Y_target[k] - Y_calculated[k])*temp[k];	
			}

			if (DEBUG_backProp || DEBUG) {
				LOG[lineCount++] = String.format("delta_out[%d]:%.3f (%s)", k, delta_out[k], (activationMethod==binaryMethod)?"bin":"bip");
				LOG[lineCount++] = String.format("Y_target[%d]:%.3f Y_calculated[%d]:%.3f Y_in[%d]:%.3f Y_in_der[%d]:%.3f", k, Y_target[k], k, Y_calculated[k], k, Y_in[k], k, temp[k]);
				
			}
			for (int j = 0; j < numHiddensTotal; j++){
				wDelta[j][k] = alpha*delta_out[k]*Z[j];
				
				if (DEBUG_backProp || DEBUG) {
					LOG[lineCount++] = String.format("wDelta[%d][%d]:%.3f wNext[%d][%d]:%.3f wPast[%d][%d]:%.3f", j, k, wDelta[j][k], j, k, wNext[j][k], j, k, wPast[j][k]);
				}
				
				//momentum equations
				wNext[j][k] = arr_wHO[j][k] + wDelta[j][k] + momentum*(arr_wHO[j][k] - wPast[j][k]); 
				wPast[j][k] = arr_wHO[j][k]; 
				arr_wHO[j][k] = wNext[j][k]; 
			}
		}
		
		if (DEBUG_backProp || DEBUG) {
			LOG[lineCount++] = "arr_wHO(post):" + Arrays.deepToString(arr_wHO);
		}
		
		//for input-to-hidden layer
		
        if (DEBUG_backProp || DEBUG) { 
        	LOG[lineCount++] = "@i-to-h cycle:";
			LOG[lineCount++] = "arr_wIH(pre):" + Arrays.deepToString(arr_wIH);
		}
		
		for (int j = 0; j < numHiddensTotal; j++){
			double sumDeltaInputs = 0.0;
			for (int k = 0;  k < numOutputsTotal; k++){ //pretending output bias doesn't exist so our output vector starts at 0
				sumDeltaInputs += delta_out[k]*arr_wHO[j][k];
				if (activationMethod == binaryMethod){
					delta_hidden[j] = sumDeltaInputs*binaryDerivative(Z_in[j]); 
				}
				else{
					delta_hidden[j] = sumDeltaInputs*bipolarDerivative(Z_in[j]);	
				}
			}
			for (int i = 0; i< numInputsTotal; i++){ //because no input bias, i = 0 will be a wasted cycle (ah wellz)
				vDelta[i][j] = alpha*delta_hidden[j]*currentStateActionVector[i];
				
				if (DEBUG_backProp || DEBUG) {
					LOG[lineCount++] = String.format("vDelta[%d][%d]:%.3f vNext[%d][%d]:%.3f vPast[%d][%d]:%.3f", i, j, vDelta[i][j], i, j, vNext[i][j], i, j, vPast[i][j]);
				}
				
				vNext[i][j] = arr_wIH[i][j] + vDelta[i][j] + momentum*(arr_wIH[i][j] - vPast[i][j]); 
				vPast[i][j] = arr_wIH[i][j]; 
				arr_wIH[i][j] = vNext[i][j]; 
			}
		}
		
        if (DEBUG_backProp || DEBUG) {
			LOG[lineCount++] = "arr_wIH(post):" + Arrays.deepToString(arr_wIH);
		}
        
//		
//		//Step 9 - Calculate local error. 
//		double error = 0.0;
//		for (int k = 0; k < numOutputsTotal; k++){ 
//			error = 0.5*(java.lang.Math.pow((Y_target[k] - Y_calculated[k]), 2)); 
//		}
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
     * @name:		doAction_Q
     * @purpose: 	Converts state Action vector into action by reading currentSAV[0], and other analysis specific actions.
     * @param: 		n, but uses:
     * 				1. Array currentSAV.
     * @return:		n
     */
    	
    public void doAction_Q(){
    	//maneuver behaviour (chase-offensive/defensive)
    	if      ( currentStateActionVector[0] == 0 ) {setTurnRight(enemyBearingFromHeading); 										setAhead(50); }
    	else if ( currentStateActionVector[0] == 1 ) {setTurnRight(enemyBearingFromHeading); 										setAhead(-50);}
    	else if ( currentStateActionVector[0] == 2 ) {setTurnRight(normalRelativeAngleDegrees(enemyBearingFromHeading - 90)); 	setAhead(50); }
    	else if ( currentStateActionVector[0] == 3 ) {setTurnRight(normalRelativeAngleDegrees(enemyBearingFromHeading + 90)); 	setAhead(50); }
    	
    	if      ( currentStateActionVector[1] == 0 ) {setFire(1);}
    	else if ( currentStateActionVector[1] == 1 ) {setFire(3);}
    	
    	//firing behaviour (to counter defensive behaviour)
    	if      ( currentStateActionVector[2] == 0 ) {setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun));}
    	else if ( currentStateActionVector[2] == 1 ) {setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun + 10));}
    	else if ( currentStateActionVector[2] == 2 ) {setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun - 10));}   	

//    	LOG[lineCount++] = "currentStateActionVector" + Arrays.toString(currentStateActionVector));     
    	if (DEBUG_doAction_Q || DEBUG) {
    		LOG[lineCount++] = "- doAction(Q)";
    		LOG[lineCount++] = "currSAV (chosen actions):" + Arrays.toString(currentStateActionVector);
    	}
    }

    /**
     * @name:		doAction_notLearning
     * @purpose: 	performs actions for rounds that do not perform learning, mainly to maintain gun angle proximity to enemy.
     * @param: 		n
     * @return:		n
     */
    public void doAction_notLearning() {
    	setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun));
    }

    /**
     * @name:		doAction_mandatoryPerTurn
     * @purpose: 	performs actions mandatory for the round, mostly to maintain radar lock on the enemy.
     * @param: 		n
     * @return:		n
     */
    public void doAction_mandatoryPerTurn() {
	    setTurnRadarRight(normalRelativeAngleDegrees(enemyBearingFromRadar));
	    scan();
	    execute();
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
	        	
	            try {
	            	if (flag_useOfflineTraining) {reader = new BufferedReader(new FileReader(getDataFile("inToHiddenWeights_OfflineTraining.txt")));}
	            	else                         {reader = new BufferedReader(new FileReader(getDataFile("finalHiddenWeights.txt"))); }
	            	
	            	for (int i = 0; i < numInputsTotal; i++) {
	            		for (int j = 0; j < numHiddensTotal; j++) {
	            			arr_wIH[i][j] = Double.parseDouble(reader.readLine());
		                }
	            	}
	            } 
	            finally {
	                if (reader != null) {
	                    reader.close();
	                }
	            }
	            
	            BufferedReader reader2 = null;
	            try {
	            	if (flag_useOfflineTraining) {reader2 = new BufferedReader(new FileReader(getDataFile("hiddenToOutWeights_OfflineTraining.txt")));}
	            	else                         {reader2 = new BufferedReader(new FileReader(getDataFile("finalOuterWeights.txt")));}
	            	for (int i = 0; i < numHiddensTotal; i++) {
	            		for (int j = 0; j < numOutputsTotal; j++) {
	            			arr_wHO[i][j] = Double.parseDouble(reader2.readLine());
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
	    	try {
	    		w1 = new PrintStream(new RobocodeFileOutputStream(getDataFile("finalHiddenWeights.txt")));
	    		if (w1.checkError()) {
	            	if (DEBUG_export || DEBUG) {
	            		LOG[lineCount++] = "Something done messed up (Error 14 cannot write)";
	            	}
	            	return ERROR_14_exportWeights_cannotWrite_NNWeights_inputToHidden;
	    		}
	    		 
	    		for (int i = 0; i < numInputsTotal; i++) {
	         		for (int j = 0; j < numHiddensTotal; j++) {
	         			w1.println(arr_wIH[i][j]);
	                }
	         	} 
	    	}
	    	catch (IOException e) {
	    		if (DEBUG_export || DEBUG) {
	    			LOG[lineCount++] = "IOException trying to write: ";
	    		}
	            e.printStackTrace(out); //Joey: lol no idea what this means
	            return ERROR_16_exportWeights_IOException;
	        } 
	        finally {
	            if (w1 != null) {
	                w1.close();
	            }
	        }      
	    	PrintStream w2 = null;
	    	try {
	    		
	    		w2 = new PrintStream(new RobocodeFileOutputStream(getDataFile("finalOuterWeights.txt")));
	    		if (w2.checkError()) {
	                //Error 0x03: cannot write
	            	if (DEBUG_export || DEBUG) {
	            		LOG[lineCount++] = "Something done messed up (Error 15 cannot write)";
	            	}
	            	return ERROR_15_exportWeights_cannotWrite_NNWeights_hiddenToOutput;
	    		 }
	    		 
	    		for (int i = 0; i < numHiddensTotal; i++) {
	         		for (int j = 0; j < numOutputsTotal; j++) {
	         			w2.println(arr_wHO[i][j]);
	                }
	         	}
	    	}
	    	catch (IOException e) {
	    		if (DEBUG_export || DEBUG) {
	    			LOG[lineCount++] = "IOException trying to write: ";
	    		}
	            e.printStackTrace(out); //Joey: lol no idea what this means
	            return ERROR_16_exportWeights_IOException;
	        } 
	        finally {
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
    
    
    //TODO
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
     * 				
     * @param: 		1. stringname of file desired to be written. The fxn currently accepts 3(three) 
     * 				files: LUTTrackfire.dat, winlose.dat, and stringTest.dat. Any other string 
     * 				name used for file name will be flagged as erroneous.
     * 				also uses:
     * 				1. bool flag_LUTImported, static flag for preventing multiple imports by multiple instances of robot (hopefully?).
     * @return:		1. int importLUTDATA success/error;
     */
    public int importData(String strName){
    	if (DEBUG_import || DEBUG) {
    		LOG[lineCount++] = "- importData: at beginning of fxn";
    		LOG[lineCount++] = "printing fileSettings: ";
    		LOG[lineCount++] = "fileSettings_temp: " + fileSettings_temp;
    		LOG[lineCount++] = "fileSettings_stringTest: " + fileSettings_stringTest;
//    		LOG[lineCount++] = "fileSettings_LUT: " + fileSettings_LUT;
    		LOG[lineCount++] = "fileSettings_WL: "+ fileSettings_WL;
    		LOG[lineCount++] = "fileSettings_weights: " + fileSettings_weights;
    	}
    	
        try {
        	BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(getDataFile(strName)));
                //reads first line of code to obtain what is in "fileSettings_temp"
                fileSettings_temp = (short)Integer.parseInt(reader.readLine());			
                
                if (DEBUG_import || DEBUG) {
                	LOG[lineCount++] = "extracted fileSettings into default: ";
                	LOG[lineCount++] = "fileSettings_temp: " + fileSettings_temp;
            	}
                // CONFIGMASK_VERIFYSETTINGSAVAIL = 0x4000
                // & is bit-wise "and". It compares each bit of the chosen CONFIGMASK with fileSettings_temp.
                // CONFIGMASK_VERIFYSETTINGSAVAIL is used to make sure the value in the first line is a fileSettings number (0x4000 is too large to be used as weights or LUT HOPEFULLY T_T)
                if ((fileSettings_temp & CONFIGMASK_VERIFYSETTINGSAVAIL) != CONFIGMASK_VERIFYSETTINGSAVAIL) {
                	return ERROR_3_import_verification;
                }
                
                //else: we verified the file has a fileSettings line. Let's read what file it is!
                //this prevents accidentally importing from wrong file. It matches the filename given to the function with the fileSettings read from the file.
                else { 
                	
                	if ( ((fileSettings_temp & CONFIGMASK_FILETYPE_stringTest) == CONFIGMASK_FILETYPE_stringTest) && (flag_stringTestImported == false) ) {
                		if (strName != "stringTest.dat") {
                			return ERROR_4_import_wrongFileName_stringTest;
                		}
                		//else: the fileSettings read from the file matches with the one given to the function, continue!
                		//Clarification: the reason behind having both flag and file-specific fileSettings variable set, is due to the fact that the program
                		//	may change the fileSettings (eg. zeroing file on purpose) by writing a different settings back, so there must be a way to store the settings.
                		fileSettings_stringTest = fileSettings_temp; 
                		flag_stringTestImported = true;
                	}

                	else if ( ((fileSettings_temp & CONFIGMASK_FILETYPE_weights) == CONFIGMASK_FILETYPE_weights) && (flag_weightsImported == false) ) {
                		if (strName != "weights.dat") {
                			return ERROR_19_import_wrongFileName_weights;
                		}
            			if ( (fileSettings_temp & CONFIGMASK_ZEROINGFILE) == CONFIGMASK_ZEROINGFILE ) {
            				if (DEBUG_import || DEBUG) {
            					LOG[lineCount++] = "- writing blank weights into local weights array: ";
                    		}
            				for (int i = 0; i < numInputsTotal; i++) {
        	            		for (int j = 0; j < numHiddensTotal; j++) { 
        	            			arr_wIH[i][j] = 0;
        		                }
        	            	}
            				for (int i = 0; i < numHiddensTotal; i++) {
        	            		for (int j = 0; j < numOutputsTotal; j++) {
        	            			arr_wHO[i][j] = 0;
        		                }
        	            	}
            				//Subtracts zeroingfile setting from fileSettings, so that the weights are zeroed only once.
            				fileSettings_temp -= CONFIGMASK_ZEROINGFILE;
            				
            				if (DEBUG_import || DEBUG) {
            					LOG[lineCount++] = "Imported blank weights.";
                    		}
            				
            			}
            			else {
            				if (DEBUG_import || DEBUG) {
            					LOG[lineCount++] = "- writing recorded weights into local weights array: ";
                    		}
            				
            				for (int i = 0; i < numInputsTotal; i++) {
        	            		for (int j = 0; j < numHiddensTotal; j++) { 
        	            			arr_wIH[i][j] = Double.parseDouble(reader.readLine());
        		                }
        	            	}
            				for (int i = 0; i < numHiddensTotal; i++) {
        	            		for (int j = 0; j < numOutputsTotal; j++) {
        	            			arr_wHO[i][j] = Double.parseDouble(reader.readLine());
        		                }
        	            	}
            				//value 999 is at the end of the weights file to make sure net is the desired size.
            				//TODO learn interaction with EOF
            	            if (Double.parseDouble(reader.readLine()) != 999) {
            	            	return ERROR_20_import_weights_wrongNetSize;
            	            }
            	            
            	            if (DEBUG_import || DEBUG) {
            	            	LOG[lineCount++] = "Imported recorded weights.";
                    		}
            			}
            			fileSettings_weights = fileSettings_temp;
            			flag_weightsImported = true;
            		}
                	
                	
                	//continue onwards in the same manner to another file.
                	else if( ((fileSettings_temp & CONFIGMASK_FILETYPE_winLose) == CONFIGMASK_FILETYPE_winLose) && (flag_WLImported == false) ) {
                		if (strName != "winlose.dat") {
                			return ERROR_5_import_wrongFileName_WL; //error 5 - coder mislabel during coding
                		}
                		if ( (fileSettings_temp & CONFIGMASK_ZEROINGFILE) == CONFIGMASK_ZEROINGFILE ) {
            				if (DEBUG_import || DEBUG) {
            					LOG[lineCount++] = "- blanking fight records (winLose):";
                    		}
            				totalFights = 0; //these honestly should not be necessary; initialized as 0 and object(robot) is made new every fight.
            				for (int i = 0; i < battleResults.length; i++){
	                    			battleResults[i] = 0;
	                    	}
            				fileSettings_temp -= CONFIGMASK_ZEROINGFILE;
            				
            				if (DEBUG_import || DEBUG) {
            					LOG[lineCount++] = "Imported blank records.";
                    		}
                		}
                		else {
            				if (DEBUG_import || DEBUG) {
            					LOG[lineCount++] = "- importing saved fight records (winLose):";
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
	                    	if (DEBUG_import || DEBUG) {
	                    		LOG[lineCount++] = "Imported saved fight records.";
                    		}
                		}
                    	fileSettings_WL = fileSettings_temp;
                    	flag_WLImported = true;
                	} // end of WinLose
                	
                	//write code for new file uses here. 
                	//also change the string being called 
                	//ctr+f: Import data. ->Change imported filename here<- 
                	
                	//file is undefined - so returns error 8
                	else {
                		if (DEBUG_import || DEBUG) {
                    		LOG[lineCount++] = "error 8:";
                    		LOG[lineCount++] = "fileSettings_temp: " + fileSettings_temp;
                    		LOG[lineCount++] = "fileSettings_stringTest: " + fileSettings_stringTest;
//                    		LOG[lineCount++] = "fileSettings_LUT: " + fileSettings_LUT;
                    		LOG[lineCount++] = "fileSettings_WL: "+ fileSettings_WL;
                    		LOG[lineCount++] = "fileSettings_weights: " + fileSettings_weights;
//                    		LOG[lineCount++] = "CONFIGMASK_FILETYPE_LUTTrackfire|verification: " + (CONFIGMASK_FILETYPE_LUTTrackfire | CONFIGMASK_VERIFYSETTINGSAVAIL);
                    		LOG[lineCount++] = "CONFIGMASK_FILETYPE_winLose|verification: " + (CONFIGMASK_FILETYPE_winLose | CONFIGMASK_VERIFYSETTINGSAVAIL);
                    		LOG[lineCount++] = "CONFIGMASK_FILETYPE_weights|verification: " + (CONFIGMASK_FILETYPE_weights | CONFIGMASK_VERIFYSETTINGSAVAIL);
//                    		LOG[lineCount++] = "flag_LUTImported: " + flag_LUTImported;
                    		LOG[lineCount++] = "flag_weightsImported: " + flag_weightsImported;
                    		LOG[lineCount++] = "fileSettings_temp & CONFIGMASK_ZEROINGFILE: " + (fileSettings_temp & CONFIGMASK_ZEROINGFILE);
                    		LOG[lineCount++] = "CONFIGMASK_FILETYPE_weights: " + CONFIGMASK_FILETYPE_weights;
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
        	//error in file reading
            return ERROR_1_import_IOException;
        } 
        // type of exception where there is a wrong number format (type is wrong or blank)  
        catch (NumberFormatException e) {
        	//Error0x02 error in type conversion - check class throw for more details
            return ERROR_2_import_typeConversionOrBlank;
        }
       
    	if (DEBUG_import || DEBUG) {
    		LOG[lineCount++] = "end of fxn fileSettings check (succeeded):";
    		LOG[lineCount++] = "fileSettings_temp: " + fileSettings_temp;
    		LOG[lineCount++] = "fileSettings_stringTest: " + fileSettings_stringTest;
//    		LOG[lineCount++] = "fileSettings_LUT: " + fileSettings_LUT;
    		LOG[lineCount++] = "fileSettings_WL: "+ fileSettings_WL;
    		LOG[lineCount++] = "fileSettings_weights: " + fileSettings_weights;
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
    	if (DEBUG_export || DEBUG) {
    		LOG[lineCount++] = "@exportData: beginning";
    		LOG[lineCount++] = "printing fileSettings: ";
    		LOG[lineCount++] = "fileSettings_temp: " + fileSettings_temp;
    		LOG[lineCount++] = "fileSettings_stringTest: " + fileSettings_stringTest;
    		LOG[lineCount++] = "fileSettings_WL: "+ fileSettings_WL;
    		LOG[lineCount++] = "fileSettings_weights: " + fileSettings_weights;
    		
    	}
    	
    	//this condition prevents wrong file from being accidentally deleted. File is cleared whenever printstream accesses it (how?), so writing the correct information 
    	//	into the desired file is paramount to data retention.
    	if(  ( (strName == strStringTest) && (fileSettings_stringTest > 0) && (flag_stringTestImported == true) ) 
    	  || ( (strName == strWL)         && (fileSettings_WL > 0)         && (flag_WLImported == true) )
    	  || ( (strName == strWeights)    && (fileSettings_weights > 0)    && (flag_weightsImported == true) )
    	  || ( (strName == strLog) ) 
    					){ 
	    	
    		PrintStream w = null;
	        
	        try {
	            w = new PrintStream(new RobocodeFileOutputStream(getDataFile(strName)));
	            // different commands between files
	            if (w.checkError()) {
	                //Error 0x03: cannot write
	            	if (DEBUG_export || DEBUG) {
	            		LOG[lineCount++] = "Something done messed up (Error 6 cannot write)";
	            	}
	            	return ERROR_6_export_cannotWrite;
	            }
	            
	            //if scope for exporting files to stringTest
	            if ( (strName == strStringTest) && (fileSettings_stringTest > 0) && (flag_stringTestImported == true) ) {
	            	
	            	if (DEBUG_export || DEBUG) {
	            		LOG[lineCount++] = "- writing into strStringTest:";
	            	}
	            	
	            	w.println(fileSettings_stringTest);
	            	
	            	if (DEBUG_export || DEBUG) {
	            		LOG[lineCount++] = "Successfully written into strStringTest.";
	            	}
	            	
	            	flag_stringTestImported = false;
	            } //end of testString

	            // weights
	            else if ( (strName == strWeights) && (fileSettings_weights > 0) && (flag_weightsImported == true) ) {
	            	if (DEBUG_export || DEBUG) {
	            		LOG[lineCount++] = "- writing into weights.dat:";
	            	}
	            	for (int i = 0; i < numInputsTotal; i++) {
		         		for (int j = 0; j < numHiddensTotal; j++) {
		         			w.println(arr_wIH[i][j]);
		                }
		         	} 
	            	for (int i = 0; i < numHiddensTotal; i++) {
		         		for (int j = 0; j < numOutputsTotal; j++) {
		         			w.println(arr_wHO[i][j]);
		                }
		         		w.println("999");
		         	}
	            	
	            	if (DEBUG_export || DEBUG) {
	            		LOG[lineCount++] = "Successfully written into weights.";
	            	}
	            	
	            	flag_weightsImported = false;
	            } //end weights export
	            
//	            winlose
	            else if ( (strName == strWL) && (fileSettings_WL > 0) && (flag_WLImported == true) ){
	            	if (DEBUG_export || DEBUG) {
	            		LOG[lineCount++] = "- writing into winLose:";
	            	}
	            	w.println(fileSettings_WL);
	            	w.println(totalFights+1);
	            	for (int i = 0; i < totalFights; i++){
	        			w.println(battleResults[i]);
	            	}
	        		w.println(currentBattleResult);
	            	
	            	if (DEBUG_export || DEBUG) {
	            		LOG[lineCount++] = "Successfully written into winLose.";
	            	}
	            	
	            	flag_WLImported = false;
	            }// end winLose
	            
//	            //strError
//	            else if((strName == strError)){
//	            	w.println("contains Q_curr-Q_prev for each turn");
//	            	for (int i = 0; i < currentRoundOfError; i++) {
//	            		w.println(QErrors[i]);
////	            		w.println(Arrays.toString(QErrorSAV[i]));
//	            	}
//	            }
//	           
	            else if (strName == strLog) {
	            	//zeroes the log file in case it was filled from previous log session.
	            	if ((fileSettings_log & CONFIGMASK_ZEROINGFILE) == CONFIGMASK_ZEROINGFILE){
	            		w.println(0);
	            	}
	            	else {
		            	for (int i = 0; i < lineCount; i++){
		        			w.println(LOG[i]);
		            	}
	            	}
	            }
	            /* 
	             * add new files here - remember to add config settings and add to the beginning ifs
	             */
	                        
	            
	            else {
	            	if (DEBUG_export || DEBUG) {
	            		LOG[lineCount++] = "error 9";
	            		
	            	}
	            	return ERROR_9_export_dump;
	            }
	        }
	        
	        //OC: PrintStreams don't throw IOExceptions during prints, they simply set a flag.... so check it here.
	        catch (IOException e) {
	    		if (DEBUG_export || DEBUG) {
	    			LOG[lineCount++] = "IOException trying to write: ";
	    		}
	            e.printStackTrace(out); //Joey: lol no idea what this means
	            return ERROR_7_export_IOException;
	        } 
	        finally {
	            if (w != null) {
	                w.close();
	            }
	        }
	        
	        flag_alreadyExported = true;
	        
	        if (DEBUG_export || DEBUG) {
	        	LOG[lineCount++] = "(succeeded export)";
	        }
	        return SUCCESS_exportData;
    	} //end of big if.
    	
    	//this should prevent wipes by writing when data isn't ready or available. If import was successful, then fileSettings would already be set.
    	//goal is to prevent accidentally wiping irrelevant file.
    	else {
    		return ERROR_10_export_mismatchedStringName;
    	}
    }
 
    
    /**binaryActivation function
     * @param x
     * @return newVal. 
     */
 	public double binaryActivation(double x) {
 		double newVal = 1/(1 + Math.exp(-x)); 
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
////    	LOG[lineCount++] = "Missed Bullet" + reward;
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
////    	LOG[lineCount++] = "Hit Bullet" + reward;
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
////    	LOG[lineCount++] = "Hit Wall" + reward;
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
	
 	
