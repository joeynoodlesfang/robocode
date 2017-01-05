/*
	
	december 12, 2016
		- implement NN basic online training structure. 

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
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.DeathEvent;
import robocode.HitWallEvent;
import robocode.RobocodeFileOutputStream;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;

public class NN1_DeadBunnyCrying extends AdvancedRobot{
	/*
	 * SAV Change Rules: //Joey: finish this
	 */
	
	/**
	 * ===================================FINALS (defines)====================================================================================
	 */
	 //variables for the q-function. Robot will NOT change analysis pattern mid-fight.
    private static final double alpha = 0.1;                //to what extent the newly acquired information will override the old information.
    private static final double gamma = 0.1;                //importance of future rewards
    private static final double epsilon = 0.05; 				//degree of exploration 
    
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
    //   MSB		filename		LSB
    // verificatn                 file-specific settings
    //
    // Config settings:
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
    
    /*
     * IMPORT/EXPORT status returns.
     */
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
    //One concern with the complexity of the robot action design is the amount of calculation time spent in forward propagation. 
    //No. times NN needs to forward propagate per round = 4 * 2 * 3 = 24
    private static final int input_action0_moveReferringToEnemy_possibilities = 4; //0ahead50, 0ahead-50, -90ahead50, -90ahead-50
    private static final int input_action1_fire_possibilities = 2;    //1, 3
    private static final int input_action2_fireDirection_possibilities = 3;    //-10deg, 0, 10deg
    private static final int numActions = 3;
    
    private static final int input_state0_myPos_possibilities = 5;    //center, left, right, top, bottom (cannot be undiscretized) 
    private static final int input_state1_myHeading_originalPossilibities = 4;    //0-89deg, 90-179, 180-269, 270-359
    private static final int input_state2_enemyEnergy_originalPossibilities = 2;    //>30, <30
    private static final int input_state3_enemyDistance_originalPossibilities = 3;    //<150, <350, >=350
    private static final int input_state4_enemyDirection_originalPossibilities = 3;    //head-on (still (abs <30 || >150), left (<0 relative dir w/ positive velo || >0 with negative velo), right (<0 dir w/ negative velo || >0 with positive velo)
    private static final int numStates = 5;
    
    private static final int numInputBias = 0;
    private static final int numHiddenBias = 1;
    private static final int numHiddenNeuron = 4;
    private static final int numInputsTotal = ( numInputBias + numActions + numStates ); 
    private static final int numHiddensTotal = ( numHiddenBias+ numHiddenNeuron );
    private static final int numOutputsTotal = 1;
    
    
    /*
     * Activation function choices for the back propagation net
     */
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
    
    
    
    /**
     * FLAGS AND COUNTS ===========================================================================
     */
    //debug flags. Each allows printouts written for specific functions. debug prints out all.
    private boolean debug = false;  
    private boolean debug_doAction_updateLearningAlgo = false;
    private boolean debug_import = false;
    private boolean debug_export = false;
    private boolean debug_onScannedRobot = false;
    private boolean debug_forwardProp = false;
    private boolean debug_getMax = false;
    
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
    private static boolean flag_useOfflineTraining = false; //Joey: check usage 

    // printout error flag - used to check function return statuses.
    // initialized to 0, which is no error.
    private int flag_error = 0;


    /**
     *  OTHER VARIABLES USABLE BY ALL CLASS FUNCTIONS ==============================================================================
     */
    
    
    // weights connecting between input and hidden layers. calculated using definitions defined above.
    private static double[][] arr_NNWeights_inputToHidden 
        = new double
        [numInputsTotal]
        [numHiddensTotal] 
        ;
    
    // weights connecting between hidden layer to output.
    private static double[][] arr_NNWeights_hiddenToOutput
    	= new double
    	[numHiddensTotal]
    	[numOutputsTotal]
    	;
    

    // temp vars: config settings for the external files, stored in the first line of .dat
    private short fileSettings_temp = 0;
    private short fileSettings_stringTest = 0;
    private short fileSettings_LUT = 0; 
    private short fileSettings_WL = 0;
    private short fileSettings_weights = 0;
    

    // vars used for storing reward, and reward calculation.
    private double reward = 0.0; 
    private int energyDiffCurr = 0;
    private int energyDiffPrev = 0;
    

    //vars that store current and previous stateAction vectors
    private double currentStateActionVector[] = new double [numInputsTotal];
    private double prevStateActionVector[]    = new double [numInputsTotal]; 

    private double currentNetQVal = 0.0;
    private double previousNetQVal= 0.0; 
    private double[] Y_calculated = new double [1]; 			//because backProp takes in a vector for Y_calculated (which is qprevious). 
    private double expectedYVal = 0.0; 
    
    //array to store the q values obtained from net forward propagation, using the current state values as well as all possible actions as inputs. 
    private double [][][] qValsFromNet = new double 
    		[input_action0_moveReferringToEnemy_possibilities]
    		[input_action1_fire_possibilities]
    		[input_action2_fireDirection_possibilities];

    //variables used for getMax.
    int num_actions = 24; 
    private int [] arrAllMaxActions = new int [num_actions]; //array for storing all actions with maxqval
    private int actionChosenForQValMax = 0; 				//stores the chosen currSAV with maxqval before policy
    private double qValMax = 0.0; 							// stores the maximum currSAV QMax

    //chosen policy. greedy or exploratory or SARSA 
    private static int policy = greedy; 
    private static int learningRate = 4; //learningAlgo is run every 4 ticks. //Joey: consider allowing robot to self-optimize for this.

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
	

    private static double[] QErrors = new double [520000];
    private static int currentRoundOfError = 0;

    /** Neural net stuff 
     * 
     * */

    
    /*Initiate variables */
		
	private double lRate = 0.05; 			//analysis rate
	private double momentum = 0.0;  		//value of momentum //Joey: consider adding some momentum lel
	
	// initialize arrays 
	private double [][] vPast 	= new double[numInputsTotal][numHiddensTotal];			// Input to Hidden weights for Past.
	private double [][] wPast 	= new double[numHiddensTotal][numOutputsTotal];    		// Hidden to Output weights for Past.
	private double [][] vNext	= new double[numInputsTotal][numHiddensTotal];	
	private double [][] wNext 	= new double[numHiddensTotal][numOutputsTotal];    		// Hidden to Output weights.
	private double [][] deltaV = new double [numInputsTotal][numHiddensTotal];		// Change in Input to Hidden weights
	private double [][] deltaW = new double [numHiddensTotal][numOutputsTotal]; 	// Change in Hidden to Output weights
	private double [] Z_in = new double[numHiddensTotal]; 		// Array to store Z[j] before being activate
	private double [] Z    = new double[numHiddensTotal];		// Array to store values of Z 
	private double [] Y_in = new double[numOutputsTotal];		// Array to store Y[k] before being activated
	private double [] Y	   = new double[numOutputsTotal];		// Array to store values of Y  
	private double [] delta_out = new double[numOutputsTotal];
	private double [] delta_hidden = new double[numHiddensTotal];
	private boolean activationMethod = bipolarMethod; 
	private final int bias = 1; 

    
    //@@@@@@@@@@@@@@@ RUN & EVENT CLASS FUNCTIONS @@@@@@@@@@@@@@@@@    
    
    /**
     * @name: 		run
     * @purpose:	1. Initializes robot colour
     * 				2. Imports LUT data from file into local memory array.
     * 				4. Runs LUT-based analysis code. See fxn learningLoop for details.
     * @brief:		To import desired file name, simply write Stringvar of filename or "filenamehere.dat" as param of importData. 
     * @brief:		To ZeroLUT, add 1 to top line in .dat file (or change 16416 to 16417)
     * @param:		n
     * @return:		n
     */
    public void run() {
        
        // Sets Robot Colors.
        setColors();
        
        if (debug) {
        	out.println("@I have been a dodger duck (robot entered run)"); 
        }
        
        // Import data. ->Change imported filename here<-

        flag_error = importData(strWeights);
        if(flag_error != SUCCESS_importData) {
        	out.println("ERROR @run LUT: " + flag_error);
        }
        
        flag_error = importData(strWL);
        if( flag_error != SUCCESS_importData) {
        	out.println("ERROR @run WL: " + flag_error);
        }
            
        //set independent movement for gun, radar and body (robocode properties). 
        setAdjustGunForRobotTurn(true);
    	setAdjustRadarForGunTurn(true);	
    	setAdjustRadarForRobotTurn(true);

    	// anything in infinite loop is initial behaviour of robot
    	// current initial behaviour is to turn radar until enemy located.
        for(;;){
        	setTurnRadarRight(20);
    		execute();					//from "AdvancedRobot" to allow parallel commands. 
        }
         
    }
    

    /**
     * @name: 		onBattleEnded
     * @brief:		Overwrites default onBattleEnded class fxn in order to perform exports data.
     * @purpose: 	1. 	Exports weights and win/lose on end of battle to datafile - IN CASES WHERE ONDEATH OR ONWIN DOES NOT TRIGGER 
     * @param:		1.	BattleEndedEvent class from Robot
     * @return:		n
     */
    public void onBattleEnded(BattleEndedEvent event){
    	
    	if (!flag_alreadyExported) {
	        flag_error = exportData(strWeights);				//strWeights = weights.dat
	        if(flag_error != SUCCESS_exportData) {
	        	out.println("ERROR @onBattleEnded: " + flag_error); //only one to export due to no learningloop(), but fileSettings_
	        }
	        
	        flag_error = exportData(strWL);					//"strWL" = winLose.dat
	        if( flag_error != SUCCESS_exportData) {
	        	out.println("ERROR @onBattleEnded: " + flag_error);
	        }
    	}
    }
    
    /**
     * @name: 		onDeath
     * @purpose: 	1. 	Exports weights and win/lose on death(we lost lel) to datafile.
     * @param:		1.	DeathEvent class from Robot
     * @return:		n
     */
    public void onDeath(DeathEvent event){
    	
    	currentBattleResult = 0;    					//global variable. 
    	
        flag_error = exportData(strWeights);
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onDeath: " + flag_error);
        }
        
        flag_error = exportData(strWL);					//"strWL" = winLose.dat
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR: " + flag_error);
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
    	
        flag_error = exportData(strWeights);
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR: " + flag_error);
        }
        
        flag_error = exportData(strWL);
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR: " + flag_error);
        }
	}
	

	/**
     * @name:		onScannedRobot
     * @brief:		This function is called by the game when the enemy is located by the scanner. It is the only function that will obtain info on the current game 
     * 				 environment, and decide what the robot will perform during this tick.
     * @purpose:	1. determines:
     * 					- my heading
     * 					- my position: x and y
     * 					- my energy
     * 					- enemy heading
     * 					- enemy velocity
     * 					- enemy bearing
     * 					- enemy distance
     * 					- enemy energy
     * 					- current tick
     * 				2. call analysis fxn to determine the next move for this tick.
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
		tick = (int)getTime();
		
		if (debug_onScannedRobot || debug) {
			out.println("Time is" + event.getTime());
		}
		
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
     * 				0. (not in NN) Determine if learning should happen this tick. Fxn learnThisRound returns a boolean.
     * 				1. Calculate how well we have done since the last NN update. (The time between this NN access and previous NN update is called a round)
     * 				2. Store the previous state and action (currentSAV -> prevSAV)
     * 				3. Use information from the environment to determine the current state.
     * 				4. Perform QFunction (detailed further in function).
     * 				5. Reset rewards. IE: all events affect reward only once unless further emphasized by events other than onScannedRobot. (NONE YET)
     * 				6. Perform chosen action. (learning-specific as well as those mandatory per tick).
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
    		obtainRawReward();
            copyCurrentSAVIntoPrevSAV();
            generateCurrentStateVector();
            qFunction();
            resetReward();
            doAction_updateLearningAlgo();
    	}
        else {
        	doAction_notLearning();
        }
    	doAction_mandatoryPerTurn();

    }

    /**
     * @name:		boolean learnThisRound
     * @purpose:	To determine if analysis algo should be run this round
     * @param:		none, but uses int tick
     * @return:		boolean
     */

    public boolean learnThisRound() {
    	if (tick%learningRate == 0)
    		return true;
    	else
    		return false; 
    }
    
	/**
     * @name:		obtainRawReward
     * @purpose:	calculates reward based on change in energy difference of robots. A later function will normalize the reward value.
     * @param:		none
     * @return:		none
     */
    public void obtainRawReward(){
    	energyDiffPrev = energyDiffCurr;
    	energyDiffCurr = myEnergy - enemyEnergy;
    	reward += energyDiffCurr - energyDiffPrev; 
    }
    
    /**
     * @name:		copyCurrentSAVIntoPrevSAV
     * @purpose:	Copies array currentStateActionVector into array prevStateActionVector
     * @param:		n, but uses:
     * 				1. currentStateActionVector
     * 				2. previous and current Net Q Val 
     * @return:		n
     */
    public void copyCurrentSAVIntoPrevSAV(){
    	for (int i = 0; i < currentStateActionVector.length; i++) {
    		prevStateActionVector[i] = currentStateActionVector[i];
    	}
    	previousNetQVal = currentNetQVal; 
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
    	if (debug){
    		out.println("currentStateVector " + Arrays.toString(currentStateActionVector));
    	}
    	out.println("currentStateVector " + Arrays.toString(currentStateActionVector));
    }
     
 


    /**
     * @name:		qFunction
     * @purpose: 	1. Obtain the action in current state with the highest q-value FROM the outputarray "Yout" of the neural net. 
     * 				2. The q value is the maximum "Y" from array
     * 				3. currentNetQVal is assigned prevQVal 
     * 				4. Y_calculated is previousNetQ
     * 			    5. run runBackProp with the input X as currentStateActionVector, qExpected as currentNetQ, Y_calculated is prevNetQVal 
     * @param: 		none, but uses:
     * 				1.	double reward 
     * 				2.	int currentStateVector[] already discretized (size numStates)
     * 				3.	double[].. LUT table, 
     * 				4.	int [] currentStateActionVector. 
     * @return: 	n
     */
    public void qFunction(){
    	getAllQsFromNet();
        getMax(); 
        currentNetQVal =  calcNewPrevQVal();
        //currentStateActionVector = X inputs, prevQVal (double) is the target, qNew, 
        Y_calculated[0] = previousNetQVal;
        expectedYVal = currentNetQVal; 
        runBackProp(); 
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
		if (debug_forwardProp || debug){
    		out.println("- entering getAllQsFromNet:");
    	}

		for (int action0 = 0; action0 < input_action0_moveReferringToEnemy_possibilities; action0++){
			for (int action1 = 0; action1 < input_action1_fire_possibilities;             	 action1++){
				for(int action2 = 0; action2 < input_action2_fireDirection_possibilities;       action2++){
					qValsFromNet[action0][action1][action2] = forwardProp(); 
				}
			}
		}

    	if (debug_forwardProp || debug){
    		out.println("YCalc " + Arrays.deepToString(qValsFromNet));
    	}
    	
    	return;
	}
	

	/** function for forwardpropagation
	 * @brief: forward propagation done in accordance to pg294 in Fundamentals of Neural Network, by Laurene Fausett.
	 * 			Feedforward (step 3 to 5):
	 * 				step 3: Each input unit (Xi, i = 1, ..., n) receives input signal xi and broadcasts this signal to all units in the layer above (the hidden units).
	 * 				step 4: Each hidden unit (Zj, j = 1, ..., p) sums its weighted input signals,
	 * 								z_inj = v0j + (sum of from i = 1 to n)xivij,                <- v = weights between input and hidden.
	 * 						applies its activation fxn to compute its output signal,
	 * 								zj = f(z_inj),
	 * 						and sends this signal to all units in the layer above (output units).
	 * 				step 5: Each output unit (Yk, k = 1, ..., m) sums its weighted input signals,
	 * 								y_ink = w0k + (sum of from j = 1 to p)zjwjk                 <- w = weights between hidden and output.
	 * 						and applies its activation fxn to compute its output signal,
	 * 								yk = f(y_ink)
	 * @purpose: does forwardPropagation on the inputs from the robot. 
	 * @return: an array of Y values for all the state pairs. 
	 **/
    public double forwardProp() {
    	if (debug_forwardProp || debug){
    		out.println("- in forward prop :");
    		out.println(Arrays.toString(currentStateActionVector));
    	}
    	//step 3 and 4:
		for (int j = 1; j < numHiddensTotal; j++){ 		//p = numHiddensTotal
			double sumIn = 0.0;
			for (int i = 0; i < numInputsTotal; i++){	   //n = numInputsTotal
				sumIn += currentStateActionVector[i]*arr_NNWeights_inputToHidden[i][j]; //NO INPUT BIAS
			}
			Z_in[j] = sumIn; 									//save z_in[0] for the bias hidden unit. 
			Z_in[0] = bias; 									//set z_in[0] = bias. HIDDEN BIAS = 1
			Z[0] = Z_in[0]; //can choose to optimize here if needs be: run during run.
			
			if (activationMethod == binaryMethod)
				Z[j] = binaryActivation(Z_in[j]); 				
			else
				Z[j] = bipolarActivation(Z_in[j]); 
		}
		//step 5:
		for (int k = 0; k < numOutputsTotal; k++){
			double sumOut = 0.0; 
			for (int j= 0; j < numHiddensTotal; j++){	
				sumOut += Z[j]*arr_NNWeights_hiddenToOutput[j][k]; 
			}
			Y_in[k] = sumOut; 	
			
			if (activationMethod == binaryMethod)
				Y[k] = binaryActivation(Y_in[k]); 
			else
				Y[k] = bipolarActivation(Y_in[k]);				
		}		
//		out.println("Yactual " + Y[0]); 
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
    	double currMax = -100.0;
//    	double currMax = qValsFromNet[0][0][0];  
        int numMaxActions = 0;
        int randMaxAction = 0;
        int actionLinearized = 0;
//        out.println("qValsFromNet.length " + qValsFromNet.length); 
    	for (int i = 0; i < qValsFromNet.length; i++){
		    for (int j = 0; j < qValsFromNet[0].length; j++){
		    	for (int k = 0; k < qValsFromNet[0][0].length; k++, actionLinearized++){
		    		
//		    		out.println("qValsFromNet" + qValsFromNet[i][j][k]);
		    		if (qValsFromNet[i][j][k] > currMax){
		    			currMax = qValsFromNet[i][j][k];
		            	numMaxActions = 1;
		            	arrAllMaxActions[numMaxActions-1] = actionLinearized;		
		            }
		            else if (qValsFromNet[i][j][k] == currMax){
		            	arrAllMaxActions[numMaxActions++] = actionLinearized;
		            }	            
		    		
		    		
		            if (debug_getMax || debug) {
		            	out.print(i + ": " + qValsFromNet[i][j][k] + "  ");
		            }
		    	}
    		}
    	}
        qValMax = currMax;
        
        if (numMaxActions > 1) {
        	randMaxAction = (int)(Math.random()*(numMaxActions)); //math.random randoms btwn 0.0 and 0.999. Add 1 to avoid truncation after typecasting to int.
        	
        	if (debug_getMax || debug) {
            	System.out.println("randMaxAction " + randMaxAction + " numMaxActions " + numMaxActions);
            }
        }
        
        actionChosenForQValMax = arrAllMaxActions[randMaxAction]; //if numMaxActions <= 1, randMaxAction = 0;

        
        if (debug_getMax || debug) {
        	System.out.println("Action Chosen: " + actionChosenForQValMax  + " qVal: " + qValMax);
        }
        
    	int valueRandom = 0;
    	//Choosing next action based on policy.
        valueRandom = (int)(Math.random()*(num_actions));
        if (policy == SARSA) {
        	actionChosenForQValMax = valueRandom;
        }
        else if(policy == exploratory) {
        	actionChosenForQValMax = (Math.random() > epsilon ? actionChosenForQValMax : valueRandom);
        }
        else if(policy == greedy){ 
//        	actionChosenForQValMax = actionChosenForQValMax;
        }
        
        for (int action0 = 0; action0 < qValsFromNet.length; action0++){
		    for (int action1 = 0; action1 < qValsFromNet[0].length; action1++){
		    	for (int action2 = 0; action2 < qValsFromNet[0][0].length; action2++){
		    		if (actionChosenForQValMax-- == 0) {
		    			currentStateActionVector[0] = action0; //Joey: test this shit out LEL
		    			currentStateActionVector[1] = action1;
		    			currentStateActionVector[2] = action2;
		    			return;
		    		}
		    	}
		    }
        }
    }
    
    /**
     * @name		calcNewPrevQVal
     * @purpose		1. Calculate the new prev q-value based on Qvalue function.
     * @param		n, but uses:
     * 				1. qValMax
     * @return		prevQVal
     	Q(s’,a’)-Q(s,a)
     */
    public double calcNewPrevQVal(){
    	double normalizedReward = bipolarActivation(reward); //Joey: put this in reward fxn>
    	//Joey ask andrea about papers for good gamma terms. (close to 1?)
    	
    	currentNetQVal +=  alpha*(normalizedReward + gamma*qValMax - previousNetQVal); //Joey: mby bipolar activate the reward
    	return currentNetQVal;
    }
    
    /**
     * @name:		runbackProp
     * @brief:		pg 295 in Fundamentals of Neural Networks by Lauren Fausett, Backpropagation of error: steps 6 to 8.
     * @param:		1. //TODO
     * @return:		1. //TODO
     */
    public void runBackProp() {      
		for (int k = 0; k <numOutputsTotal; k++){
			if (activationMethod == binaryMethod){
				delta_out[k] = (expectedYVal - Y_calculated[k])*binaryDerivative(Y_in[k]); 
			}
			else{
				delta_out[k] = (expectedYVal - Y_calculated[k])*bipolarDerivative(Y_in[k]);	
			}
//			System.out.println("\n");
//			System.out.println("delta " + delta_out[k]);
			for (int j = 0; j < numHiddensTotal; j++){
//				System.out.println("wPast[j][k] " + wPast[j][k]);
//				System.out.println("arr_NNWeights_hiddenToOutput[j][k] " + arr_NNWeights_hiddenToOutput[j][k]);
				deltaW[j][k] = alpha*delta_out[k]*Z[j];
				wNext[j][k] = arr_NNWeights_hiddenToOutput[j][k] + deltaW[j][k] + momentum*(arr_NNWeights_hiddenToOutput[j][k] - wPast[j][k]); 
				wPast[j][k] = arr_NNWeights_hiddenToOutput[j][k]; 
				arr_NNWeights_hiddenToOutput[j][k] = wNext[j][k]; 
//				System.out.println("wPast[j][k] " + wPast[j][k]);
//				System.out.println("arr_NNWeights_hiddenToOutput[j][k] " + arr_NNWeights_hiddenToOutput[j][k]);
//				System.out.println("wNext[j][k] " + wNext[j][k]);
			}
		}
		
		//for hidden layer
		for (int j = 0; j < numHiddensTotal; j++){
			double sumDeltaInputs = 0.0;
			for (int k = 0;  k < numOutputsTotal; k++){
				sumDeltaInputs += delta_out[k]*arr_NNWeights_hiddenToOutput[j][k];
				if (activationMethod == binaryMethod){
					 delta_hidden[j] = sumDeltaInputs*binaryDerivative(Z_in[j]); 
				}
				else{
					delta_hidden[j] = sumDeltaInputs*bipolarDerivative(Z_in[j]);	
				}
			}
			for (int i = 0; i< numInputsTotal; i++){
//				System.out.println("vPast[i][j] " + vPast[i][j]);
//				System.out.println("arr_NNWeights_inputToHidden[i][j] " + arr_NNWeights_inputToHidden[i][j]);
				deltaV[i][j] = alpha*delta_hidden[j]*currentStateActionVector[i]; //Joey: what about the action vectors?
				vNext[i][j]  = arr_NNWeights_inputToHidden[i][j] + deltaV[i][j] + momentum*(arr_NNWeights_inputToHidden[i][j] - vPast[i][j]); 
				vPast[i][j] = arr_NNWeights_inputToHidden[i][j]; 
				arr_NNWeights_inputToHidden[i][j] = vNext[i][j]; 
//				System.out.println("vPast[i][j] " + vPast[i][j]);
//				System.out.println("arr_NNWeights_inputToHidden[i][j] " + arr_NNWeights_inputToHidden[i][j]);
//				System.out.println("vNext[i][j] " + vNext[i][j]);
			}
		}
		//Step 9 - Calculate local error. 
		double error = 0.0;
		for (int k = 0; k < numOutputsTotal; k++){ 
			error = 0.5*(java.lang.Math.pow((expectedYVal - Y_calculated[k]), 2)); 
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
     * @name:		doAction_updateLearningAlgo
     * @purpose: 	Converts state Action vector into action by reading currentSAV[0], and other analysis specific actions.
     * @param: 		n, but uses:
     * 				1. Array currentSAV.
     * @return:		n
     */
    	
    public void doAction_updateLearningAlgo(){
    	//maneuver behaviour (chase-offensive/defensive)
    	if (currentStateActionVector[0] == 0) {
    		setTurnRight(enemyBearingFromHeading);
    		setAhead(50);
    	}
    	else if(currentStateActionVector[0] == 1){
    		setTurnRight(enemyBearingFromHeading);
    		setAhead(-50);
    	}
    	else if(currentStateActionVector[0] == 2){
    		setTurnRight(normalRelativeAngleDegrees(enemyBearingFromHeading - 90));
    		setAhead(50);
    	}
    	else if(currentStateActionVector[0] == 3){
    		setTurnRight(normalRelativeAngleDegrees(enemyBearingFromHeading - 90));
    		setAhead(-50);
    	}
    	
    	if (currentStateActionVector[1] == 0){
    		setFire(1);
    	}
    	else if (currentStateActionVector[1] == 1){
    		setFire(3);
    	}
    	
    	//firing behaviour (to counter defensive behaviour)
    	if (currentStateActionVector[2] == 0){
    		setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun));
    	}
    	else if (currentStateActionVector[2] == 1){
    		setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun + 10));
    	}
    	else if (currentStateActionVector[2] == 2){
    		setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun - 10));
    	}   	

//    	out.println("currentStateActionVector" + Arrays.toString(currentStateActionVector));     
      if (debug_doAction_updateLearningAlgo || debug) {
    	  out.println("currentStateActionVector" + Arrays.toString(currentStateActionVector));
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
    	if (debug_import || debug) {
    		out.println("@importData: at beginning of fxn");
    		out.println("printing fileSettings: ");
    		out.println("fileSettings_temp: " + fileSettings_temp);
    		out.println("fileSettings_stringTest: " + fileSettings_stringTest);
//    		out.println("fileSettings_LUT: " + fileSettings_LUT);
    		out.println("fileSettings_WL: "+ fileSettings_WL);
    		out.println("fileSettings_weights: " + fileSettings_weights);
    	}
    	
        try {
        	BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(getDataFile(strName)));
                //reads first line of code to obtain what is in "fileSettings_temp"
                fileSettings_temp = (short)Integer.parseInt(reader.readLine());			
                
                if (debug_import || debug) {
            		out.println("extracted fileSettings into default: ");
            		out.println("fileSettings_temp: " + fileSettings_temp);
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
            				if (debug_import || debug) {
                    			out.println("- writing blank weights into local weights array: ");
                    		}
            				for (int i = 0; i < numInputsTotal; i++) {
        	            		for (int j = 0; j < numHiddensTotal; j++) { 
        	            			arr_NNWeights_inputToHidden[i][j] = 0;
        		                }
        	            	}
            				for (int i = 0; i < numHiddensTotal; i++) {
        	            		for (int j = 0; j < numOutputsTotal; j++) {
        	            			arr_NNWeights_hiddenToOutput[i][j] = 0;
        		                }
        	            	}
            				//Subtracts zeroingfile setting from fileSettings, so that the weights are zeroed only once.
            				fileSettings_temp -= CONFIGMASK_ZEROINGFILE;
            				
            				if (debug_import || debug) {
                    			out.println("Imported blank weights.");
                    		}
            				
            			}
            			else {
            				if (debug_import || debug) {
                    			out.println("- writing recorded weights into local weights array: ");
                    		}
            				
            				for (int i = 0; i < numInputsTotal; i++) {
        	            		for (int j = 0; j < numHiddensTotal; j++) { 
        	            			arr_NNWeights_inputToHidden[i][j] = Double.parseDouble(reader.readLine());
        		                }
        	            	}
            				for (int i = 0; i < numHiddensTotal; i++) {
        	            		for (int j = 0; j < numOutputsTotal; j++) {
        	            			arr_NNWeights_hiddenToOutput[i][j] = Double.parseDouble(reader.readLine());
        		                }
        	            	}
            				//value 999 is at the end of the weights file to make sure net is the desired size.
            				//TODO learn interaction with EOF
            	            if (Double.parseDouble(reader.readLine()) != 999) {
            	            	return ERROR_20_import_weights_wrongNetSize;
            	            }
            	            
            	            if (debug_import || debug) {
                    			out.println("Imported recorded weights.");
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
            				if (debug_import || debug) {
                    			out.println("- blanking fight records (winLose):");
                    		}
            				totalFights = 0; //these honestly should not be necessary; initialized as 0 and object(robot) is made new every fight.
            				for (int i = 0; i < battleResults.length; i++){
	                    			battleResults[i] = 0;
	                    	}
            				fileSettings_temp -= CONFIGMASK_ZEROINGFILE;
            				
            				if (debug_import || debug) {
                    			out.println("Imported blank records.");
                    		}
                		}
                		else {
            				if (debug_import || debug) {
                    			out.println("- importing saved fight records (winLose):");
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
	                    	if (debug_import || debug) {
                    			out.println("Imported saved fight records.");
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
                		if (debug_import || debug) {
                    		out.println("error 8:");
                    		out.println("fileSettings_temp: " + fileSettings_temp);
                    		out.println("fileSettings_stringTest: " + fileSettings_stringTest);
//                    		out.println("fileSettings_LUT: " + fileSettings_LUT);
                    		out.println("fileSettings_WL: "+ fileSettings_WL);
                    		out.println("fileSettings_weights: " + fileSettings_weights);
//                    		out.println("CONFIGMASK_FILETYPE_LUTTrackfire|verification: " + (CONFIGMASK_FILETYPE_LUTTrackfire | CONFIGMASK_VERIFYSETTINGSAVAIL));
                    		out.println("CONFIGMASK_FILETYPE_winLose|verification: " + (CONFIGMASK_FILETYPE_winLose | CONFIGMASK_VERIFYSETTINGSAVAIL));
                    		out.println("CONFIGMASK_FILETYPE_weights|verification: " + (CONFIGMASK_FILETYPE_weights | CONFIGMASK_VERIFYSETTINGSAVAIL));
//                    		out.println("flag_LUTImported: " + flag_LUTImported);
                    		out.println("flag_weightsImported: " + flag_weightsImported);
                    		out.println("fileSettings_temp & CONFIGMASK_ZEROINGFILE: " + (fileSettings_temp & CONFIGMASK_ZEROINGFILE));
                    		out.println("CONFIGMASK_FILETYPE_weights: " + CONFIGMASK_FILETYPE_weights);
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
       
    	if (debug_import || debug) {
    		out.println("end of fxn fileSettings check (succeeded):");
    		out.println("fileSettings_temp: " + fileSettings_temp);
    		out.println("fileSettings_stringTest: " + fileSettings_stringTest);
//    		out.println("fileSettings_LUT: " + fileSettings_LUT);
    		out.println("fileSettings_WL: "+ fileSettings_WL);
    		out.println("fileSettings_weights: " + fileSettings_weights);
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
    		out.println("fileSettings_temp: " + fileSettings_temp);
    		out.println("fileSettings_stringTest: " + fileSettings_stringTest);
    		out.println("fileSettings_WL: "+ fileSettings_WL);
    		out.println("fileSettings_weights: " + fileSettings_weights);
    		
    	}
    	
    	//this condition prevents wrong file from being accidentally deleted. File is cleared whenever printstream accesses it (how?), so writing the correct information 
    	//	into the desired file is paramount to data retention.
    	if(  ( (strName == strStringTest) && (fileSettings_stringTest > 0) && (flag_stringTestImported == true) ) 
    	  || ( (strName == strWL)         && (fileSettings_WL > 0)         && (flag_WLImported == true) )
    	  || ( (strName == strWeights)    && (fileSettings_weights > 0)    && (flag_weightsImported == true) )
    	  /* ||  ( (strName == strError) ) */ 
    					){ 
	    	
    		PrintStream w = null;
	        
	        try {
	            w = new PrintStream(new RobocodeFileOutputStream(getDataFile(strName)));
	            // different commands between files
	            if (w.checkError()) {
	                //Error 0x03: cannot write
	            	if (debug_export || debug) {
	            		out.println("Something done messed up (Error 6 cannot write)");
	            	}
	            	return ERROR_6_export_cannotWrite;
	            }
	            
	            //if scope for exporting files to stringTest
	            if ( (strName == strStringTest) && (fileSettings_stringTest > 0) && (flag_stringTestImported == true) ) {
	            	
	            	if (debug_export || debug) {
	            		out.println("- writing into strStringTest:");
	            	}
	            	
	            	w.println(fileSettings_stringTest);
	            	
	            	if (debug_export || debug) {
	            		out.println("Successfully written into strStringTest.");
	            	}
	            	
	            	flag_stringTestImported = false;
	            } //end of testString

	            // weights
	            else if ( (strName == strWeights) && (fileSettings_weights > 0) && (flag_weightsImported == true) ) {
	            	if (debug_export || debug) {
	            		out.println("- writing into weights.dat:");
	            	}
	            	for (int i = 0; i < numInputsTotal; i++) {
		         		for (int j = 0; j < numHiddensTotal; j++) {
		         			w.println(arr_NNWeights_inputToHidden[i][j]);
		                }
		         	} 
	            	for (int i = 0; i < numHiddensTotal; i++) {
		         		for (int j = 0; j < numOutputsTotal; j++) {
		         			w.println(arr_NNWeights_hiddenToOutput[i][j]);
		                }
		         		w.println("999");
		         	}
	            	
	            	if (debug_export || debug) {
	            		out.println("Successfully written into weights.");
	            	}
	            	
	            	flag_weightsImported = false;
	            } //end weights export
	            
//	            winlose //Joey: why was winlose disabled
	            else if ( (strName == strWL) && (fileSettings_WL > 0) && (flag_WLImported == true) ){
	            	if (debug_export || debug) {
	            		out.println("- writing into winLose:");
	            	}
	            	w.println(fileSettings_WL);
	            	w.println(totalFights+1);
	            	for (int i = 0; i < totalFights; i++){
	        			w.println(battleResults[i]);
	            	}
	        		w.println(currentBattleResult);
	            	
	            	if (debug_export || debug) {
	            		out.println("Successfully written into winLose.");
	            	}
	            	
	            	flag_WLImported = false;
	            }// end winLose
	            
//	            //strError
//	            else if((strName == strError)){
//	            	w.println("contains currentNetQVal-previousNetQVal for each tick");
//	            	for (int i = 0; i < currentRoundOfError; i++) {
//	            		w.println(QErrors[i]);
////	            		w.println(Arrays.toString(QErrorSAV[i]));
//	            	}
//	            }
//	            
	            /* 
	             * add new files here - remember to add config settings and add to the beginning ifs
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
	        
	        flag_alreadyExported = true;
	        
	        if (debug_export || debug) {
	        	out.println("(succeeded export)");
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


///**
//* @name:		importDataWeights
//* @author:		partly written in sittingduckbot
//* @purpose:	to extract neural net weights stored in finalHiddenWeights.txt 
//* 				and finalOuterWeights.txt into arrays NNWeights_inputToHidden[][]
//* 				and NNWeights_hiddenToOutput[][], respectively.
//* @param:		n, but uses these globals:
//* 				NNWeights_inputToHidden[][]
//* 				NNWeights_hiddenToOutput[][]
//* @return:		n
//*/
//public int importDataWeights() {
//	if (flag_weightsImported == false) {
//  	try {
//      	BufferedReader reader = null;
//      	
//          try {
//          	if (flag_useOfflineTraining) {
//          		reader = new BufferedReader(new FileReader(getDataFile("inToHiddenWeights_OfflineTraining.txt")));
//          	}
//          	else {
//          		reader = new BufferedReader(new FileReader(getDataFile("finalHiddenWeights.txt")));
//          	}
//          	
//          	for (int i = 0; i < numInputsTotal; i++) {
//          		for (int j = 0; j < numHiddensTotal; j++) {
////          			out.println("Double.parseDouble(reader.readLine())" + Double.parseDouble(reader.readLine()));
////          			out.println("i " + i); 
////          			out.println("j " + j); 
//          			arr_NNWeights_inputToHidden[i][j] = Double.parseDouble(reader.readLine());
//	                }
//          	}
//          } 
//          finally {
//              if (reader != null) {
//                  reader.close();
//              }
//          }
//          
//          BufferedReader reader2 = null;
//          try {
//          	if (flag_useOfflineTraining) {
//          		reader2 = new BufferedReader(new FileReader(getDataFile("hiddenToOutWeights_OfflineTraining.txt")));
//          	}
//          	else {
//          		reader2 = new BufferedReader(new FileReader(getDataFile("finalOuterWeights.txt")));
//          	}
//          	for (int i = 0; i < numHiddensTotal; i++) {
//          		for (int j = 0; j < numOutputsTotal; j++) {
////          			out.println("Double.parseDouble(reader.readLine())" + Double.parseDouble(reader2.readLine()));
//          			arr_NNWeights_hiddenToOutput[i][j] = Double.parseDouble(reader2.readLine());
//	                }
//          	}
//          } 
//          finally {
//              if (reader2 != null) {
//                  reader2.close();
//              }
//          }
//      } 
//      //exception to catch when file is unreadable
//      catch (IOException e) {
//          return ERROR_12_importWeights_IOException;
//      } 
//      // type of exception where there is a wrong number format (type is wrong or blank)  
//      catch (NumberFormatException e) {
//          return ERROR_13_importWeights_typeConversionOrBlank;
//      }
//  	
//  	flag_weightsImported = true;
//  	if (flag_useOfflineTraining) {
//  		flag_useOfflineTraining = false;
//  	}
//  	return SUCCESS_importDataWeights;
//	}
//	
//	else {
//		return ERROR_17;
//	}
//}
//
///**
//* @name: 		exportDataWeight
//* @author: 	mostly sittingduckbot
//* @purpose: 	1. stores the weights back into finalHiddenWeights.txt, 
//* 				finalOuterWeights.txt from data NNWeights_inputToHidden[][]
//* 				and NNWeights_hiddenToOutput[][], respectively.
//* 
//*/
//public int exportDataWeights() {
//	if(flag_weightsImported == true) {
//		PrintStream w1 = null;
//  	try {
//  		w1 = new PrintStream(new RobocodeFileOutputStream(getDataFile("finalHiddenWeights.txt")));
//  		if (w1.checkError()) {
//              //Error 0x03: cannot write
//          	if (debug_export || debug) {
//          		out.println("Something done messed up (Error 14 cannot write)");
//          	}
//          	return ERROR_14_exportWeights_cannotWrite_NNWeights_inputToHidden;
//  		}
//  		 
//  		for (int i = 0; i < numInputsTotal; i++) {
//       		for (int j = 0; j < numHiddensTotal; j++) {
//       			w1.println(arr_NNWeights_inputToHidden[i][j]);
//              }
//       	} 
//  	}
//  	catch (IOException e) {
//  		if (debug_export || debug) {
//  			out.println("IOException trying to write: ");
//  		}
//          e.printStackTrace(out); //Joey: lol no idea what this means
//          return ERROR_16_exportWeights_IOException;
//      } 
//      finally {
//          if (w1 != null) {
//              w1.close();
//          }
//      }      
//  	PrintStream w2 = null;
//  	try {
//  		
//  		w2 = new PrintStream(new RobocodeFileOutputStream(getDataFile("finalOuterWeights.txt")));
//  		if (w2.checkError()) {
//              //Error 0x03: cannot write
//          	if (debug_export || debug) {
//          		out.println("Something done messed up (Error 15 cannot write)");
//          	}
//          	return ERROR_15_exportWeights_cannotWrite_NNWeights_hiddenToOutput;
//  		 }
//  		 
//  		for (int i = 0; i < numHiddensTotal; i++) {
//       		for (int j = 0; j < numOutputsTotal; j++) {
//       			w2.println(arr_NNWeights_hiddenToOutput[i][j]);
//              }
//       	}
//  	}
//  	catch (IOException e) {
//  		if (debug_export || debug) {
//  			out.println("IOException trying to write: ");
//  		}
//          e.printStackTrace(out); //Joey: lol no idea what this means
//          return ERROR_16_exportWeights_IOException;
//      } 
//      finally {
//          if (w2 != null) {
//              w2.close();
//          }
//      }    
//  	
//  	
//  	flag_weightsImported = false;
//  	return SUCCESS_exportDataWeights;
//	}
//	else {
//		return ERROR_18;
//	}
//}