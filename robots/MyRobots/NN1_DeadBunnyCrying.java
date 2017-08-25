/*

<-> INTRO TO THE BOT <->
NN1_DBC will be the first bot that fully employs the benefits of neural net in combat. It is currently being 
developed with the goal of improving performance against several host robots, in particular Trackfire (avoidance), 
Walls (predictive targeting and avoidance), and Spinbot (improved predictive targeting and likely self-generated 
movement patterns). To achieve this, we will at least employ several new parameters that best captures the 
advantages of NN that could not be employed by LUT RL, such as continuous input ranges, and a lot more possible 
states. Other possibilities include changing number of hidden nodes, and multiple nets.

<-> INTRO TO ROBOCODE, NEURAL NET, RL.
ROBOCODE is a program designed for learning the basics of java. The goal is to code a bot that 
competes with others in an arena. The coder has no direct influence during the fight - instead, 
all the strategies and fight mechanics employed by the robot are coded before the fight takes 
place. The coder is free to employ any tactics he/she wants to win - as long as the bot adheres to 
the rules of the game. (Robocode ReadMe: http://robocode.sourceforge.net/docs/ReadMe.html	
Robocode wiki: http://robowiki.net)

The premise for this project is that reinforcement learning can be used to improve robot combat
adaptibility against all types of enemy robot behaviours, by learning counters to their behaviour 
in real time. 

REINFORCEMENT LEARNING, RL, refers to the method by which a machine selects an action. 
The decision is made by choosing the action that maximizes a conceptualized reward: the bot 
performs an action within a measurable environment, and rewards itself based upon the results of 
said action. The reward alters the likelihood of re-performing the same action again in the same 
environment: winning moves can be recreated, and poor moves can be avoided. This ability to learn 
during the actual battle gives the robot combat adaptibility.

Neural network (NN) is a computation network that solves problems by employing complexity, 
approximation, and trial and error. It imitates biological neurons in design through making both 
inputs and outputs to the system to be neuronal nodes, which, like a biological neuron network, 
connects to other neurons and forms a network with the connections. The net has the ability to 
solve problems by acting sort of like an equation - a very complex equation with changeable 
coefficients, and through trial and error of repeatedly trying to achieve correct outputs with 
various given inputs, the network can create a relatively accurate model of the system. Complexity 
is often a benefit for correct modeling (much like a linear line is a less efficient model of a 
higher order equation, whereas higher order equations can model lower ones relatively easily). The 
network often includes other nodes called 'hidden nodes' to add complexity. 
It can also be structured in a complex manner (such as multidimensional nodal connections). The 
simplest structure of a NN - which is used for our bots - is a 2-dimensional net consisting of 3 
1-D layers: a 1D layer of input nodes connected to a layer of hidden nodes, which connects to both 
the input layer as well as a layer of output nodes. These connections have changeable values 
associated to them (aka 'weights'), and is the engine behind the net's ability to adjust its 
approximation.

Neural net is used here for RL, just like LUT, with one main distinction being that NN can only 
estimate the final value of actions. In exchange, NN allows more inputs, and promotes greater input
variety. In a LUT, each selection of inputs has a corresponding value: for a system with 2 inputs, 
with respectively 3 and 4 possible values, the total number of values to be stored is 
3x4 = 12. This is a problem for robocode: For the robot to remember behaviours after combat, the LUT 
method requires a list of QVals to be stored in a file in between executions. Robocode limits all files 
to a size of 200kBs, and it doesn't take long before a robot with multiple inputs to reach 200kBs. NN on 
the other hand estimates the QVals through calculations, and stores only the values associated with the 
neural net connections, or 'weights'. This is a much smaller set of values. Weights describe the strength 
of the connections between nodes in a neural net, and it can be any real value. By requiring only 
weights to be stored, NN allows a much greater selection of inputs to be used. Even continuous 
input ranges can be used, which is impossible for LUT.

-> SUMMARY OF HOW NN1 WORKS <-
Each turn of battle, an event is triggered. The event does the following:
	1. Obtain information about the environment.
	2. Once every X turns, RL will take place (allows robot to complete movement actions that takes a few turns.)
	   
	   in RL: 	1) Convert environmental information into inputs that are usable by net, including states and rewards.
	   			2) Convert several key NN variables from 'current' tense into 'previous' tense.
 		 		3) Choose best action for current cycle - forward propagation using current states.
   				4) Perform Q value function to create adjustment quantities for net.
   				5) Correct weights - back propagate using previous layer values
   				6) Perform action based on current cycle's maxQ. 
       non-RL turns: 1) maintain scanlock on enemy.
	3. Perform actions mandatory per turn, such as maintaining scanner lock on enemy.
  
NN1 consists of several data analyzing fxns:
	1. Logging of important sections in templog.txt
	2. Logging of Qvals per cycle of RL in qVals.txt
	3. Logging of errors calculated during Q function in saveErrorForActions.dat. These errors 
	   are used to determine if Qvalue is converging.
	4. Errors during import/export are printouts in o.stream in addition to being logged.

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
	 * RULES FOR CHANGING STATE ACTION VECTOR(SAV):
	 * 1. State/Action related finals must be changed accordingly.
	 * 2. NN input dimension related finals must be changed accordingly.
	 * 3. If states changed, change the state accordingly in generateCurrentStateVector()
	 * 4. If actions changed, change the actions in doAction_Q() AND all action for loops T_T
	 * 5. In NN2, no states or actions are expected to be changed. But for future related bots, changing the dimensions of the weights will likely cause error during input.
	 *      Note that error and use that as prompt to input a randomized weight selection.
	 */
	
	/*
	 * STEPS FOR IMPORTING/EXPORTING A NEW FILE
	 * 1. Create the txt/dat file in the robotname.data directory (or try to export and the game will create directory and file for u)
	 * 2. Add a configmask code for the file. ie. CONFIGMASK_FILETYPE_FILESHORTNAMEHERE = 0x0..0, and make the addition in the comment paragraph above.
	 * 3. Create a string that refers to the filename in the "STRINGS used for importing or extracting files" section.
	 * 4. Add flag_imported for the specific file.
	 * 5. Add permission flag for file if applicable.
	 * 6. Add fileSettings_fileshortname for the specific file.
	 * 7. Add data storage array and total lines of data if applicable.
	 * 8. If file requires import, write an import fxn call by following the correct format in the function run().
	 * 9. If the file requires export, write an export in the proper locations, in fxns onBattleEnded() or/and onWin() + onDeath().
	 * 10. Add actual code.
	 * 11. In importData(): 
	 * 		update available config section in fxn's @brief comments
	 * 		add fileSettings log line in (1) beginning of function,
	 *									 (2) dump, 
	 *									 (3) and end of function.
	 * 		add section in else if ( ((fileSettings_temp & CONFIGMASK_FILETYPE_blah) == CONFIGMASK_FILETYPE_blah) && (flag_blahImported == false) ) {, which includes:
	 *				NOTE: for these sections, we may refer to other codes.
	 * 				1. a section on if wrong file name
	 *				2. section for if zeroconfig
	 *				3. section for if not zeroconfig
	 *				4. fileSettings_fileShortName = fileSettings_temp
	 *				5. file_--imported = true
	 * 12. In exportData():
	 * 		update available config section in fxn's @brief comments (zeroing section not necessary)
	 *		add fileSettings log line in beginning of function
	 *		add if condition for file.
	 *		add section in else if ( (strName == strWL) && (fileSettings_WL > 0) && (flag_WLImported == true) ), which includes:
	 *				1. LOG indications that the code has arrived in/leaving this section.
	 *				2. streamname.println(fileSettings_fileShortName)
	 *				3. codes for export
	 *				4. flag_--imported = false
	 */
	
	/**
	 * ===================================FINALS (defines)====================================================================================
	 */
	//variables for the q-function. Robot will currently NOT change critical q-function coefficients (alpha, gamma, epsilon) mid-fight.
	//alpha describes the extent to which the newly acquired information will override the old information.
     static final double alpha = 0.1;
    //gamma describes the importance of current rewards
     static final double gamma = 0.8;                
    //epsilon describes the degree of exploration, 1 = max randomness.
     static final double epsilon = 0.05; 				 
    
    //policy:either greedy or exploratory (both are Q-learning, perhaps possibility of implementing SARSA in the future?)
     static final int greedy = 0;
     static final int exploratory = 1;
    //Joey: SARSA is unusable - currently incomplete.
    //SARSA uses the reward gained by performing action 2 to update reward in action 1.
     static final int SARSA = 2;
    
    
    /* 
     * CONFIGMASK: a list of numbers that describe what's in the file, and provide some limited instructions to the reader (which is the import fxn).  
     * 			  - contains 2 bytes (4 hex digits)
     * 			  - functions will use AND conditions to evaluate if the settings in the file match the mask. 
     */
    // _ _ _ _  _ _ _ _  _ _ _ _  _ _ _ _ 
    //   MSnib	filename(2 nibs)   LSnib
    // MSnib is the first nibble (4 bits) - currently used to verify the config setting is present in the file.
    // the 2nd and 3rd nibbles are used for recognizing specific files
    // LSnib is used for file-specific settings.
    //
    // Current available config settings:
    //     				stringTest: 16400 (0x4010)
    // 					strLUT:		16416 (0x4020), zeroLUT = 16417 (0x4021)
    //     				WL:			16448 (0x4040), zero WL = 16449 (0x4041)
    //					NN weights: 16512 (0x4080), zeroing = 16513 (0x4081)
    //					QVals:		16640 (0x4100), zeroing = 16641 (0x4101)
    //					BPErrors:	16896 (0x4200), zeroing = 16897 (0x4201)
     static final short CONFIGMASK_ZEROINGFILE  =				0x0001;
    static final short CONFIGMASK_VERIFYSETTINGSAVAIL = 		0x4000;
    static final short CONFIGMASK_FILETYPE_stringTest =			0x0010;
    static final short CONFIGMASK_FILETYPE_LUTTrackfire =		0x0020;
    static final short CONFIGMASK_FILETYPE_winLose = 			0x0040;
    static final short CONFIGMASK_FILETYPE_weights =			0x0080;
    static final short CONFIGMASK_FILETYPE_QVals =				0x0100;
    static final short CONFIGMASK_FILETYPE_BPErrors = 			0x0200;
    
    //ERROR PROMPTS: IMPORT/EXPORT status returns.
    
    static final int SUCCESS_importData = 						0x00;
    static final int SUCCESS_exportData = 						0x00;
    static final int SUCCESS_importDataWeights =				0x00;
    static final int SUCCESS_exportDataWeights =				0x00;
    
    static final int ERROR_1_import_IOException = 				1;
    static final int ERROR_2_import_typeConversionOrBlank = 	2;
    static final int ERROR_3_import_verification = 				3;
    static final int ERROR_4_import_wrongFileName_stringTest =	4;
    static final int ERROR_5_import_wrongFileName_WL =			5;
    static final int ERROR_6_export_cannotWrite =				6;
    static final int ERROR_7_export_IOException =				7;
    static final int ERROR_8_import_dump =						8;
    static final int ERROR_9_export_dump =						9;
    static final int ERROR_10_export_dump =		10;
    static final int ERROR_11_import_wrongFileName_LUT = 		11;
    static final int ERROR_12_importWeights_IOException = 		12;
    static final int ERROR_13_importWeights_typeConversionOrBlank = 13;
    static final int ERROR_14_exportWeights_cannotWrite_NNWeights_inputToHidden = 14;
    static final int ERROR_15_exportWeights_cannotWrite_NNWeights_hiddenToOutput = 15;
    static final int ERROR_16_exportWeights_IOException = 		16;
    static final int ERROR_17_importWeights_flagImportedFalse = 17;
    static final int ERROR_18_exportWeights_flagImportedTrue = 	18;
    static final int ERROR_19_import_wrongFileName_weights =	19;
    static final int ERROR_20_import_weights_wrongNetSize =		20;
    static final int ERROR_21_import_wrongFileName_QVals =		21;
    static final int ERROR_22_import_wrongFileName_BPErrors =	22;
    
    /*
	 * NN STATEACTION VARIABLES for stateAction ceilings (for array designs and other modular function interactions).
	 * 
	 */
    //Action-related finals.
    //- One concern with the complexity of the actions a robot can perform is the amount of calculation time spent in forward propagation.
    //- Each possible action requires one forward propagation.
    //- As of now, the No. times NN needs to forward propagate per round = 4 * 2 * 3 = 24
    static final int input_action0_moveReferringToEnemy_possibilities = 4; //0ahead50, 0ahead-50, -90ahead50, -90ahead-50
    static final int input_action1_fire_possibilities = 2;    //1, 3
    static final int input_action2_fireDirection_possibilities = 3;    //-10deg, 0, 10deg
    static final int numActionContainers = 3;   
    static final int numActions = input_action0_moveReferringToEnemy_possibilities 
    									  * input_action1_fire_possibilities
    									  * input_action2_fireDirection_possibilities;
    
    //State related finals.
    static final int input_state0_myPos_possibilities = 5;    //center, left, right, top, bottom (cannot be undiscretized) 
    static final int input_state1_myHeading_originalPossilibities = 4;    //0-89deg, 90-179, 180-269, 270-359
    static final int input_state2_enemyEnergy_originalPossibilities = 2;    //>30, <30
    static final int input_state3_enemyDistance_originalPossibilities = 3;    //<150, <350, >=350
    static final int input_state4_enemyDirection_originalPossibilities = 3;    //head-on (still (abs <30 || >150), left (<0 relative dir w/ positive velo || >0 with negative velo), right (<0 dir w/ negative velo || >0 with positive velo)
    static final int numStateContainers = 5;
    
    //NN neuron parameters.
    static final int numInputBias = 0;
    static final int numHiddenBias = 1;
    static final int numOutputBias = 0; //Actual code disregards possibility of output bias by starting loops relating to output at 0 referring to first output instead of bias.
    static final int numHiddenNeuron = 4;
    static final int numInputsTotal = ( numInputBias + numActionContainers + numStateContainers ); 
    static final int numHiddensTotal = ( numHiddenBias + numHiddenNeuron );
    static final int numOutputsTotal = 1;
    
    //NN activation function choices
    static final boolean binaryMethod = true;
    static final boolean bipolarMethod = false;
 
    
    /**
     * STRINGS used for importing or exporting files =========================================== 
     */
    String strStringTest = "stringTest.dat";    
    String strLUT = "LUTTrackfire.dat";
    String strWL = "winlose.dat";
    String strSA = "stateAction.dat"; 
    String strBPErrors = "BPErrors.txt" ;
    String strWeights = "weights.dat";
    String strLog = "templog.txt";
    String strQVals = "qVals.txt";
    
    /**
     * FLAGS AND COUNTS ===========================================================================
     */
    
    
    //<<<<<< CRITICAL FLAGS >>>>>>
    //
    //Flags that allow certain parts of code or data to be used!
    //
    //flag that prompts user to use offline training data from LUT. (ONLY applicable for NN2)
    //static boolean flag_useOfflineTraining = true;

    //flag to permit log file to be imported/exported.
    static boolean flag_recordLog = false;
    //flag used to permit program to record QVals
    static boolean flag_recordQVals = false;
    //flag used to permit program to record BP round errors
    static boolean flag_recordBPErrors = false;
    
    
    
    //DEBUG_ALL flags. Each allows printouts written for specific functions. DEBUG_ALL will print out all.
    final static boolean DEBUG_ALL = false; 
	final static boolean DEBUG_run = false;
	final static boolean DEBUG_onScannedRobot = false;
	final static boolean DEBUG_analysis = false;
	final static boolean DEBUG_onBattleEnded = false;
	final static boolean DEBUG_onDeath = false;
	final static boolean DEBUG_onWin = false;
//	final static boolean DEBUG_learnThisRound = false;
	final static boolean DEBUG_obtainReward = false;
	final static boolean DEBUG_generatePrevs = false;
	final static boolean DEBUG_generateCurrentStateVector = false;
//	final static boolean DEBUG_RL_and_NN = false;
	final static boolean DEBUG_MULTI_forwardProp = false; //can be used to debug the multiple fxns encompassed by FP.
	final static boolean DEBUG_getAllQsFromNet = false;
	final static boolean DEBUG_forwardProp = false;
	final static boolean DEBUG_getMax = false;
	final static boolean DEBUG_qFunction = false;
	final static boolean DEBUG_MULTI_backProp = false;
	final static boolean DEBUG_prepareBackProp = false;
	final static boolean DEBUG_backProp = false;
//	final static boolean DEBUG_resetReward = false;
    final static boolean DEBUG_doAction_Q = false;
//	final static boolean DEBUG_doAction_notLearning = false;
//	final static boolean DEBUG_doAction_mandatoryPerTurn = false;
//	final static boolean DEBUG_importDataWeights = false;
//	final static boolean DEBUG_exportDataWeights = false;
    final static boolean DEBUG_MULTI_file = true; //logs from all functions that contribute directly to moving/editing files, which include more than import/export fxns.
    final static boolean DEBUG_import = false;
    final static boolean DEBUG_export = false;
    

    

    
    
    // Flags used in data imp/exp fxns.
    //		Purposes:
    // 		 - prevents overwrite, and protects against wrong file entries
    //		 - data for a particular file must be exported before importing for the same file occurs again.
    // 		false == only imports can access; true == only exports can access.
    //		ALWAYS initialize these as false.
    static boolean flag_stringTestImported = false;
    static boolean flag_LUTImported = false;
    static boolean flag_WLImported = false;
    static boolean flag_weightsImported = false;
    static boolean flag_QValsImported = false;
    static boolean flag_BPErrorsImported = false;

    // printout error flag - used to record the return value of functions.
    // initialized to 0, which is no error.
    int flag_fileAccessStatus = 0;

    /**
     *  OTHER VARIABLES USABLE BY THIS ROBOT'S CLASS FUNCTIONS ==============================================================================
     */
    
    // weights connecting between input and hidden layers. calculated using definitions defined above.
    static double[][] arr_wIH 
        = new double
        [numInputsTotal]
        [numHiddensTotal] 
        ;
    
    // weights connecting between hidden layer to output.
    static double[][] arr_wHO
    	= new double
    	[numHiddensTotal]
    	[numOutputsTotal]
    	;
    
    // temp vars for importing/exporting files: config settings for the external files, stored in the first line of .dat
    // PART OF THE CONFIGMASK SERIES.
    short fileSettings_temp = 0;
    short fileSettings_stringTest = 0;
    short fileSettings_LUT = 0; 
    short fileSettings_WL = 0;
    short fileSettings_weights = 0;
    short fileSettings_log = 0;
    short fileSettings_QVals = 0;
    short fileSettings_BPErrors = 0;
    
    // reward and reward calculation vars.
    double reward_normalized = 0.0;
    double energyDiffCurr = 0.0;
    double energyDiffPrev = 0.0;

    //activation method used for binary and bipolar methods.
    boolean activationMethod = bipolarMethod; 

    //chosen policy. greedy or exploratory or SARSA
    //possibility of allowing robot to change these patterns
    static int policy = greedy; //SAR
    static int learningRate = 4; //learningAlgo is run every 4 ticks. 

    //enemy bot information
    double enemyDistance = 0.0;
    double enemyHeadingRelative = 0.0;
    double enemyHeadingRelativeAbs = 0.0;
    double enemyVelocity = 0.0;
    double enemyBearingFromRadar = 0.0;
    double enemyBearingFromGun = 0.0;
    double enemyBearingFromHeading = 0.0;
    double enemyEnergy = 0.0;
    
    //my bot information
    double myHeading = 0.0; 
    double myEnergy = 0.0;
    double myPosX = 0.0;
    double myPosY = 0.0;
    
    //misc battle information
    int turn = 0;
    
    //used to update WL export
    int totalFights = 0;
    int[] battleResults = new int [520000];
    int currentBattleResult = 0;
	
    //vars and arrays used for debugging purposes - storage of data, total lines of data
    static String[] LOG = new String [520000];
    static int lineCount = 0;
    static double[] arr_QVals = new double [520000];
    static int totalQValRecords = 0;
    static double[] arr_BPErrors = new double [520000];
    static int totalBPErrorsRecords = 0;

    //class vars used to store function call time
    // long aveDuration = 0;
    // static long totalDuration = 0;
    // static int durationCount = 0;
    
    //vars that store current and previous stateAction vectors
    double currentStateActionVector[] = new double [numInputsTotal];
    double prevStateActionVector[]    = new double [numInputsTotal]; //might not be used 

    //Q-var storages.
    //- "Y" and "Q" pretty much refers to the same thing, but to make it easier to understand when coding, we use "Y" for the BP calculations, and Q for Q fxn.

    double[] Q_prev_new = new double[numOutputsTotal];
    
    
    /**  
     * Neural net stuff  
     */
	double [] y	   = new double[numOutputsTotal];		// Array to store values of Y
    // analysis rate //Joey: ask Andrea about use of this.
	// double lRate = 0.05; 			
	//value of momentum //Joey: research the used of momentum for optimal values. 
	static final double momentum = 0.1;  		
	
	// arrays used for momentum
	double [][] wIH_past  = new double[numInputsTotal] [numHiddensTotal];	// Input to Hidden weights for Past.
	double [][] wIH_next  = new double[numInputsTotal] [numHiddensTotal];	// Input to Hidden weights.
	double [][] wHO_past  = new double[numHiddensTotal][numOutputsTotal];  // Hidden to Output weights for Past.
	double [][] wHO_next  = new double[numHiddensTotal][numOutputsTotal];  // Hidden to Output weights.

	//bias for hidden initialized as value 1
	// static final int valInputBias = 0;
    static final int valHiddenBias = 1;
    // static final int valOutputBias = 0;
    
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
        
        out.println("@I have been a dodger duck (robot entered run)"); 
        if(DEBUG_MULTI_file || DEBUG_run || DEBUG_ALL) {
        	LOG[lineCount++] = "@I have been a dodger duck (robot entered run)";
        }
        
        // Import data. Change imported filename below
        
        //always clears log from previous session in case it used up all allowed harddrive. (robocode allows for 200kB of external data per robot)
        
        fileSettings_log += CONFIGMASK_ZEROINGFILE; //changes file setting to zeroing on next open. strLog is the next file to be opened, and it is typically so large that it is only once per run.	        
        flag_fileAccessStatus = exportData(strLog);
        if (flag_fileAccessStatus != SUCCESS_importData) {
        	out.println("ERROR @run blankingWeights: " + flag_fileAccessStatus);
        }
        fileSettings_log -= CONFIGMASK_ZEROINGFILE; //resets file setting to post-zeroing.
    
        if (flag_recordQVals) { //only recordQVals when flagged.
	        flag_fileAccessStatus = importData(strQVals);
	        if(flag_fileAccessStatus != SUCCESS_importData) {
	        	out.println("ERROR @run QVals: " + flag_fileAccessStatus);
	        	if(DEBUG_MULTI_file || DEBUG_run || DEBUG_ALL) {
	        		LOG[lineCount++] = "Error @run Qvals: " + flag_fileAccessStatus;
	        	}
	        }
        }
        
        if (flag_recordBPErrors) { //only recordBPErrors when flagged.
	        flag_fileAccessStatus = importData(strBPErrors);
	        if(flag_fileAccessStatus != SUCCESS_importData) {
	        	out.println("ERROR @run BPErrors: " + flag_fileAccessStatus);
	        	if(DEBUG_MULTI_file || DEBUG_run || DEBUG_ALL) {
	        		LOG[lineCount++] = "Error @run BPErrors: " + flag_fileAccessStatus;
	        	}
	        }
        }
        
        
        flag_fileAccessStatus = importData(strWeights);
        if (flag_fileAccessStatus != SUCCESS_importDataWeights) {
        	out.println("ERROR @run weights: " + flag_fileAccessStatus);
        	if(DEBUG_MULTI_file || DEBUG_run || DEBUG_ALL) {
            	LOG[lineCount++] = "ERROR @run weights: " + flag_fileAccessStatus;
            }
        }
    
        
        flag_fileAccessStatus = importData(strWL); //Joey: consider not storing WL on default.
        if (flag_fileAccessStatus != SUCCESS_importData) {
        	out.println("ERROR @run WL: " + flag_fileAccessStatus);
        	if(DEBUG_MULTI_file || DEBUG_run || DEBUG_ALL) {
            	LOG[lineCount++] = "ERROR @run WL: " + flag_fileAccessStatus;
            }
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
     * @purpose: 	1. 	Export
     * 				2.	Exports weights from memory to .txt file, which stores weights 
     * 				   	linearly. Exporting will occur only once per fight, either during 
     * 				   	death or fight end.
     * @param:		1.	BattleEndedEvent class from Robot
     * @return:		n
     */
    public void onBattleEnded(BattleEndedEvent event){
    	//pausing export weights here to prevent double weight export errors (should have no impact on data)
//    	//exporting weights
//    	flag_fileAccessStatus = exportData(strWeights);	
//        if (flag_fileAccessStatus != SUCCESS_exportData) {
//        	out.println("ERROR @onBattleEnded weights: " + flag_fileAccessStatus);
//        	if(DEBUG_MULTI_file|| DEBUG_onBattleEnded || DEBUG_ALL) {
//        		LOG[lineCount++] = "ERROR @onBattleEnded weights: " + flag_fileAccessStatus;
//        	}
//        }
//        
        if (flag_recordLog) { // log is exported only in onBattleEnded because it is typically too large to .... //Joey: test if onBattleEnded and onDeath/onWin runs twice.
	    	flag_fileAccessStatus = exportData(strLog); //export log LAST to prevent oversize for other critical files.					
	        if (flag_fileAccessStatus != SUCCESS_exportData) {
	        	out.println("ERROR @onBattleEnded Log: " + flag_fileAccessStatus);
	        	if(DEBUG_MULTI_file || DEBUG_onBattleEnded || DEBUG_ALL) {
	        		LOG[lineCount++] = "ERROR @onBattleEnded Log: " + flag_fileAccessStatus;
	        	}
	        }
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
    	
    	currentBattleResult = 0;    					//global variable that stores a 0 for loss. 
    	
    	if (flag_recordQVals) {
	    	flag_fileAccessStatus = exportData(strQVals);
	        if(flag_fileAccessStatus != SUCCESS_exportData) {
	        	out.println("ERROR @onDeath QVals: " + flag_fileAccessStatus);
	        	if(DEBUG_MULTI_file || DEBUG_onDeath || DEBUG_ALL) {
	        		LOG[lineCount++] = "ERROR @onDeath QVals: " + flag_fileAccessStatus;
	        	}
	        }
    	}
        
    	if (flag_recordBPErrors) {
	    	flag_fileAccessStatus = exportData(strBPErrors);
	        if(flag_fileAccessStatus != SUCCESS_exportData) {
	        	out.println("ERROR @onDeath BPErrors: " + flag_fileAccessStatus);
	        	if(DEBUG_MULTI_file || DEBUG_onDeath || DEBUG_ALL) {
	        		LOG[lineCount++] = "ERROR @onDeath BPErrors: " + flag_fileAccessStatus;
	        	}
	        }
    	}
    	
    	flag_fileAccessStatus = exportData(strWL);					//"strWL" = winLose.dat
        if (flag_fileAccessStatus != SUCCESS_exportData) {
        	out.println("ERROR @onDeath WL: " + flag_fileAccessStatus);
        	if(DEBUG_MULTI_file || DEBUG_onDeath || DEBUG_ALL) {
        		LOG[lineCount++] = "ERROR @onDeath WL: " + flag_fileAccessStatus;
        	}
        }
        
        flag_fileAccessStatus = exportData(strWeights);
        if (flag_fileAccessStatus != SUCCESS_exportData) {
        	out.println("ERROR @onDeath weights: " + flag_fileAccessStatus);
        	if(DEBUG_MULTI_file || DEBUG_onDeath || DEBUG_ALL) {
        		LOG[lineCount++] = "ERROR @onDeath weights: " + flag_fileAccessStatus;
        	}
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
    	
    	if (flag_recordQVals) {
	    	flag_fileAccessStatus = exportData(strQVals);
	        if( flag_fileAccessStatus != SUCCESS_exportData) {
	        	out.println("ERROR @onWin QVals: " + flag_fileAccessStatus);
	        	if(DEBUG_MULTI_file || DEBUG_onDeath || DEBUG_ALL) {
	        		LOG[lineCount++] = "ERROR @onWin QVals: " + flag_fileAccessStatus;
	        	}
	        }
    	}
    	
    	if (flag_recordBPErrors) {
	    	flag_fileAccessStatus = exportData(strBPErrors);
	        if(flag_fileAccessStatus != SUCCESS_exportData) {
	        	out.println("ERROR @onWin BPErrors: " + flag_fileAccessStatus);
	        	if(DEBUG_MULTI_file || DEBUG_onDeath || DEBUG_ALL) {
	        		LOG[lineCount++] = "ERROR @onWin BPErrors: " + flag_fileAccessStatus;
	        	}
	        }
    	}
        
        flag_fileAccessStatus = exportData(strWL);
        if (flag_fileAccessStatus != SUCCESS_exportData) {
        	out.println("ERROR @onWin WL: " + flag_fileAccessStatus);
        	if(DEBUG_MULTI_file || DEBUG_onWin || DEBUG_ALL) {
        		LOG[lineCount++] = "ERROR @onWin WL: " + flag_fileAccessStatus;
        	}
        }
        
    	flag_fileAccessStatus = exportData(strWeights);
        if (flag_fileAccessStatus != SUCCESS_exportData) {
        	out.println("ERROR @onWin weights: " + flag_fileAccessStatus);
        	if(DEBUG_MULTI_file || DEBUG_onWin || DEBUG_ALL) {
        		LOG[lineCount++] = "ERROR @onWin weights: " + flag_fileAccessStatus;
        	}
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
     * @purpose:	1. Analyze all environmental and self values.
     * 				2. Decide if reinforcement learning via NNet should be used this round
     * 					2a. Perform NN action, or
     * 					2b. Perform standard action.
     * 				3. Perform mandatory action.
     * @NNet:		Our neural net online training involves the following:
     * 				0. (not in NN) Determine if learning should happen this turn. Fxn learnThisRound returns a boolean.
     * 				1. Calculate how well we have done since the last NN update. (The time between this NN access and previous NN update is called a round)
     * 				2. Store the previous state and action (currentSAV -> prevSAV), and previous Q value.
     * 				3. Use information from the environment to determine the current state.
     * 				4. Decide best action based on max Q, which is calculated using all possible actions from current state.
     * 				5. Perform QFunction (detailed further in function).
     * 				6. Use result of QFunction to correct net.
     * 				7. Reset rewards. IE: all events affect reward only once unless further emphasized by events other than onScannedRobot. (NONE YET)
     * 				8. Perform chosen action. (learning-specific as well as those mandatory per turn).
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
    		
    		//this DEBUG_ALL fxn is related to onScannedRobot fxn, but placed here so that we can log it only when RL is firing.
    		if(DEBUG_onScannedRobot || DEBUG_MULTI_forwardProp || DEBUG_backProp || DEBUG_ALL || DEBUG_MULTI_file) {
    			LOG[lineCount++] = " ";
        		LOG[lineCount++] = "@@@ TURN " + turn + ":";
    		}
    		
        	if(DEBUG_onScannedRobot || DEBUG_ALL) {
        		LOG[lineCount++] = "myHeading:" + myHeading + "\tmyPosX:" + myPosX + "\tmyPosY:" + myPosY + "\tmyEnergy:" + myEnergy;
        		LOG[lineCount++] = "enemyHeadingRelative:" + enemyHeadingRelative + "\tenemyVelocity:" + enemyVelocity;
        		LOG[lineCount++] = String.format("enemyBearingFromRadar:%.1f enemyBearingFromGun:%.1f enemyBearingFromHeading:%.1f", enemyBearingFromRadar, enemyBearingFromGun, enemyBearingFromHeading);
        		LOG[lineCount++] = "enemyDistance:" + enemyDistance + "\tenemyEnergy" + enemyEnergy;
        	}
    		
    		if(DEBUG_analysis || DEBUG_ALL) {
        		LOG[lineCount++] = "- analysis (weight vals)";
        		LOG[lineCount++] = "arr_wIH:" + Arrays.deepToString(arr_wIH);
        		LOG[lineCount++] = "arr_wHO:" + Arrays.deepToString(arr_wHO);
        	}
    		
    		obtainReward(reward_normalized);
            generatePrevs(y, Q_prev_new, prevStateActionVector, currentStateActionVector);
            generateCurrentStateVector(currentStateActionVector);
            RL_NN(currentStateActionVector, prevStateActionVector, y, Q_prev_new,
            		arr_wIH, arr_wHO,
            		wIH_past, wIH_next, wHO_past, wHO_next,
            		reward_normalized, activationMethod);
            resetReward(reward_normalized);
            doAction_Q(currentStateActionVector, enemyBearingFromHeading, enemyBearingFromGun);
    	}
        else {
        	doAction_notLearning(enemyBearingFromGun);
        }
    	doAction_mandatoryPerTurn(enemyBearingFromRadar);
    	
    }

    /**
     * @name:		boolean learnThisRound
     * @purpose:	Make a decision is RLNN should be run.
     * @param:		none, but uses int turn
     * @return:		boolean
     */
    public boolean learnThisRound() {
    	if (turn%learningRate == 0) {return true;}
    	else						{return false;} 
    }
    
	/**
     * @name:		obtainReward
     * @purpose:	calculates what the reward is for this cycle.
     * @param:		none
     * @return:		none
     */
    public void obtainReward(double reward_normalized){
    	double reward = 0.0;
    	energyDiffPrev = energyDiffCurr;
    	energyDiffCurr = myEnergy - enemyEnergy;
    	reward += energyDiffCurr - energyDiffPrev; //using +=, not =, to allow for other events to affect reward if desired.
    	reward_normalized = reward/600; //currently linear normalization. This fits the biggest possible enemy to around 0.05.
    	
    	if(DEBUG_obtainReward || DEBUG_ALL) {
    		LOG[lineCount++] = "- rewards";
    		LOG[lineCount++] = String.format("reward:%f reward(normalized):%.3f", reward, reward_normalized);
    	}
    }
    
    /**
     * @name:		generatePrevs
     * @purpose:	1. Copies Q_prev_new obtained from Q fxn in last round into Q_prev.
     * 				2. Copies last round's currSAV into prevSAV, to be used for NNet.
     * @param:		n, but uses:
     * 				1. Q_curr 
     * @return:		n
     */
    public void generatePrevs(double[] Q_prev, double[] Q_prev_new, double[] prevSAV, double[] currSAV){
    	if(DEBUG_generatePrevs || DEBUG_ALL) {
    		LOG[lineCount++] = "- generatePrevs:";
    		LOG[lineCount++] = "start values";
    		LOG[lineCount++] = "prevSAV: " + Arrays.toString(prevSAV);
    		LOG[lineCount++] = "currSAV: " + Arrays.toString(currSAV);
    		LOG[lineCount++] = "Q_prev: " + Arrays.toString(Q_prev);
    		LOG[lineCount++] = "Q_prev_new:" + Arrays.toString(Q_prev_new); 
    	}
    	
    	System.arraycopy(Q_prev_new, 0, Q_prev, 0, Q_prev.length);
    	System.arraycopy(currSAV, 0, prevSAV, 0, currSAV.length);
    	
    	if(DEBUG_generatePrevs || DEBUG_ALL) {
    		LOG[lineCount++] = "end values";
    		LOG[lineCount++] = "prevSAV: " + Arrays.toString(prevSAV);
    		LOG[lineCount++] = "currSAV: " + Arrays.toString(currSAV);
    		LOG[lineCount++] = "Q_prev: " + Arrays.toString(Q_prev);
    		LOG[lineCount++] = "Q_prev_new:" + Arrays.toString(Q_prev_new);
    		LOG[lineCount++] = "#eo generatePrevs";
    	}
    }
    
    /**
     * @name: 		generateCurrentStateVector
     * @brief:		Generates the current state vectors (NOT actions).
     * @purpose: 	1. gets state values from environment. 
     * 				2. Update array of current stateAction vector.  
     * @param: 		n
     * @return: 	none
     * currentStateVector positions [0][1][2] are all the actions. 
     */
    public void generateCurrentStateVector(double[] currentStateActionVector){
    	//First few INPUTS are ACTIONS and hence will be zeroed for generating currSAV
    	//INPUTS 0, 1 and 2 are ACTION
    	for (int i = 0; i < numActionContainers; i++) {
    		currentStateActionVector[i] = 0;
    	}
    	
    	//Dimension 3 - private static final int input_state0_myPos_possibilities = 5;
    	//left
    	if (  (myPosX<=50)  &&  ( (myPosX <= myPosY) || (myPosX <= (600-myPosY)) )  ){					
    		currentStateActionVector[3] = 1;						
    	}
    	
    	//right
    	else if (  (myPosX>=750)  &&  ( ((800-myPosX) <= myPosY) || ((800-myPosX) <= (600-myPosY)) )  ){
    		currentStateActionVector[3] = 2;						
    	}
    	
    	//top
    	else if (myPosY<=50) { 
    		currentStateActionVector[3] = 3;
    	}
    	
    	//bottom
    	else if (myPosY>=550) {				
    		currentStateActionVector[3] = 4;
    	}
    	
    	//center
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
    	
    	//Dimension 6: enemyDistance  
    	//<150, <350, >=350(to1000)
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
    	
    	if(DEBUG_generateCurrentStateVector || DEBUG_ALL){
    		LOG[lineCount++] = "- generateCurrentStateVector:";
    		LOG[lineCount++] = "currentSAV:" + Arrays.toString(currentStateActionVector);
    		LOG[lineCount++] = "#eo generateCurrentStateVector";
    	}
    	
    }


    /**
     * @name:		resetReward
     * @purpose: 	Resets reward to 0.
     * @param: 		1. reward
     * @return:		n
     */
    public void resetReward(double reward){
        
        reward = 0;
        
    }
    
    /**
     * @name:		doAction_Q
     * @purpose: 	Converts state Action vector into action by reading currentSAV[0], and other analysis specific actions.
     * @param: 		1. Array currentSAV.
     * 				2. enemyBearingFromHeading:		enemy's bearing in terms of degrees from our bot's heading
     * 				3. enemyBearingFromGun:			enemy's bearing in terms of degrees from the direction which our gun is pointing to
     * @return:		n
     */
    public void doAction_Q(double[] currSAV, double enemyBearingFromHeading, double enemyBearingFromGun){
    	//maneuver behaviour (chase-offensive/defensive)
    	if      ( currSAV[0] == 0 ) {setTurnRight(enemyBearingFromHeading); 									setAhead(50); }
    	else if ( currSAV[0] == 1 ) {setTurnRight(enemyBearingFromHeading); 									setAhead(-50);}
    	else if ( currSAV[0] == 2 ) {setTurnRight(normalRelativeAngleDegrees(enemyBearingFromHeading - 90)); 	setAhead(50); }
    	else if ( currSAV[0] == 3 ) {setTurnRight(normalRelativeAngleDegrees(enemyBearingFromHeading + 90)); 	setAhead(50); }
    	
    	if      ( currSAV[1] == 0 ) {setFire(1);}
    	else if ( currSAV[1] == 1 ) {setFire(3);}
    	
    	//firing behaviour (to counter defensive behaviour)
    	if      ( currSAV[2] == 0 ) {setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun));}
    	else if ( currSAV[2] == 1 ) {setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun + 10));}
    	else if ( currSAV[2] == 2 ) {setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun - 10));}   	
    	
    	if(DEBUG_doAction_Q || DEBUG_ALL) {
    		LOG[lineCount++] = "- doAction_Q:";
    		LOG[lineCount++] = "currSAV (chosen actions):" + Arrays.toString(currSAV);
    		LOG[lineCount++] = "#eo doAction_Q.";
    	}
    }

    /**
     * @name:		doAction_notLearning
     * @purpose: 	performs actions for rounds that do not perform learning, mainly to maintain gun angle proximity to enemy.
     * @param: 		1. enemyBearingFromGun:			enemy's bearing in terms of degrees from the direction which our gun is pointing to
     * @return:		n
     */
    public void doAction_notLearning(double enemyBearingFromGun) {
    	setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun));
    }

    /**
     * @name:		doAction_mandatoryPerTurn
     * @purpose: 	performs actions mandatory for the round, mostly to maintain radar lock on the enemy.
     * @param: 		1. enemyBearingFromRadar		enemy's bearing in terms of degrees from the direction which the bot radar is pointing to
     * @return:		n
     */
    public void doAction_mandatoryPerTurn(double enemyBearingFromRadar) {
	    setTurnRadarRight(normalRelativeAngleDegrees(enemyBearingFromRadar));
	    scan();
	    execute();
    }

    
    //TODO marker for import/export
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
     * 				Most available config settings:
     * 				    stringTest: 16400 (0x4010)
     * 					strLUT:		16416 (0x4020), zeroLUT = 16417 (0x4021)
     *    				WL:			16448 (0x4040), zero WL = 16449 (0x4041)
     *					NN weights: 16512 (0x4080), zeroing = 16513 (0x4081)
     *					QVals:		16640 (0x4100), zeroing = 16641 (0x4101)
     *					BPErrors:	16896 (0x4200), zeroing = 16897 (0x4201)
     * 				
     * @param: 		1. stringname of file desired to be written. The fxn currently accepts 3(three) 
     * 				files: LUTTrackfire.dat, winlose.dat, and stringTest.dat. Any other string 
     * 				name used for file name will be flagged as erroneous.
     * 				also uses:
     * 				1. bool flag_LUTImported, static flag for preventing multiple imports by multiple instances of robot (hopefully?).
     * @return:		1. int importLUTDATA success/error;
     */
    public int importData(String strName){
    	if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
    		LOG[lineCount++] = "- importData: at beginning of fxn";
    		LOG[lineCount++] = "printing fileSettings: ";
    		LOG[lineCount++] = "fileSettings_temp: " + fileSettings_temp;
    		LOG[lineCount++] = "fileSettings_stringTest: " + fileSettings_stringTest;
//    		LOG[lineCount++] = "fileSettings_LUT: " + fileSettings_LUT;
    		LOG[lineCount++] = "fileSettings_WL: "+ fileSettings_WL;
    		LOG[lineCount++] = "fileSettings_weights: " + fileSettings_weights;
    		LOG[lineCount++] = "fileSettings_QVals: " + fileSettings_QVals;
    		LOG[lineCount++] = "fileSettings_BPErrors: " + fileSettings_BPErrors;
    	}
    	
        try {
        	BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(getDataFile(strName)));
                
                //reads first line of code to obtain what is the file. Information about file must be available in the first line of the file for it to be used by prog.
                fileSettings_temp = (short)Integer.parseInt(reader.readLine());			
                
                if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
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
                	// else: import weights
                	else if ( ((fileSettings_temp & CONFIGMASK_FILETYPE_weights) == CONFIGMASK_FILETYPE_weights) && (flag_weightsImported == false) ) {
                		if (strName != "weights.dat") {
                			return ERROR_19_import_wrongFileName_weights; //how the foik does this even happen lel
                		}
                		// the alternate way of zeroing the weights file
            			if ( (fileSettings_temp & CONFIGMASK_ZEROINGFILE) == CONFIGMASK_ZEROINGFILE ) {
            		    	double randomCount = ((numInputsTotal*numHiddensTotal) + (numHiddensTotal*numOutputsTotal))/20;
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "- writing blank weights into local weights array: ";
            				}
            				for (int i = 0; i < numInputsTotal; i++) {
            		    		for (int j = 0; j < numHiddensTotal; j++) { 
            		    			arr_wIH[i][j] = randomCount;
            		    			randomCount -= 0.1;
            		            }
            		    	}
            				for (int i = 0; i < numHiddensTotal; i++) {
            		    		for (int j = 0; j < numOutputsTotal; j++) {
            		    			arr_wHO[i][j] = randomCount;
            		    			randomCount -= 0.1;
            		            }
            		    	}
            				
            				//Subtracts zeroingfile setting from fileSettings, so that the weights are zeroed only once.
            				fileSettings_temp -= CONFIGMASK_ZEROINGFILE;
            				
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "Imported blank weights.";
                    		}
            				
            			}
            			
            			else {
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "- writing recorded weights into local weights array: ";
                    		}
            				
            				for (int i = 0; i < numInputsTotal; i++) {
        	            		for (int j = 0; j < numHiddensTotal; j++) { 
        	            			arr_wIH[i][j] = Double.parseDouble(reader.readLine());
        		                }
        	            	}
            				
            				if (Double.parseDouble(reader.readLine()) != 998) {
            	            	return ERROR_20_import_weights_wrongNetSize;
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
            	            
            	            if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            	            	LOG[lineCount++] = "Imported recorded weights.";
                    		}
            			}
            			
            			fileSettings_weights = fileSettings_temp;
            			flag_weightsImported = true;
            		}
                	
                	
                	//WL Import
                	else if( ((fileSettings_temp & CONFIGMASK_FILETYPE_winLose) == CONFIGMASK_FILETYPE_winLose) && (flag_WLImported == false) ) {
                		if (strName != "winlose.dat") {
                			return ERROR_5_import_wrongFileName_WL; //error 5 - coder mislabel during coding
                		}
                		if ( (fileSettings_temp & CONFIGMASK_ZEROINGFILE) == CONFIGMASK_ZEROINGFILE ) {
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "- blanking fight records (winLose):";
                    		}
            				totalFights = 0; //these honestly should not be necessary; initialized as 0 and object(robot) is made new every fight.
            				for (int i = 0; i < battleResults.length; i++){
	                    			battleResults[i] = 0;
	                    	}
            				fileSettings_temp -= CONFIGMASK_ZEROINGFILE;
            				
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "Imported blank records.";
                    		}
                		}
                		else {
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
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
	                    	if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
	                    		LOG[lineCount++] = "Imported saved fight records.";
                    		}
                		}
                    	fileSettings_WL = fileSettings_temp;
                    	flag_WLImported = true;
                	} // end of WinLose
                	
                	//QVals Import
                	else if( ((fileSettings_temp & CONFIGMASK_FILETYPE_QVals) == CONFIGMASK_FILETYPE_QVals) && (flag_QValsImported == false) ) {
                		if (strName != "qVals.txt") {
                			return ERROR_21_import_wrongFileName_QVals; //error 21 - coder mislabel during coding
                		}
                		if ( (fileSettings_temp & CONFIGMASK_ZEROINGFILE) == CONFIGMASK_ZEROINGFILE ) {                			
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "- blanking QVal records:";
                    		}
            				
            				totalQValRecords = 0; //these honestly should not be necessary; initialized as 0 and object(robot) is made new every fight.
            				
            				for (int i = 0; i < arr_QVals.length; i++){
	                    			arr_QVals[i] = 0;
	                    	}
            				
            				fileSettings_temp -= CONFIGMASK_ZEROINGFILE;
            				
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "Imported blank QVal records.";
                    		}
                		}
                		else {
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "- importing saved QVals:";
                    		}
	                		
            				totalQValRecords = Integer.parseInt(reader.readLine());
            				for (int i = 0; i < arr_QVals.length; i++){
	                    		if (i < totalQValRecords) {
	                    			arr_QVals[i] = Double.parseDouble(reader.readLine());
	                    		}
	                    		else {
	                    			arr_QVals[i] = 0;
	                    		}
	                    	}
	                    	if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
	                    		LOG[lineCount++] = "Imported saved QVals.";
                    		}
                		}
                    	fileSettings_QVals = fileSettings_temp;
                    	flag_QValsImported = true;
                	}//eo QVals
                	
                	//BPErrors Import
                	else if( ((fileSettings_temp & CONFIGMASK_FILETYPE_BPErrors) == CONFIGMASK_FILETYPE_BPErrors) && (flag_BPErrorsImported == false) ) {
                		if (strName != "BPErrors.txt") {
                			return ERROR_22_import_wrongFileName_BPErrors; //error 22: file mislabel (wuht how did i screw this up)
                		}
                		if ( (fileSettings_temp & CONFIGMASK_ZEROINGFILE) == CONFIGMASK_ZEROINGFILE ) {                			
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "- blanking BPErrors records:";
                    		}
            				
            				totalBPErrorsRecords = 0;
            				
            				for (int i = 0; i < arr_BPErrors.length; i++){
	                    			arr_BPErrors[i] = 0;
	                    	}
            				
            				fileSettings_temp -= CONFIGMASK_ZEROINGFILE;
            				
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "Imported blank BPErrors records.";
                    		}
                		}
                		else {
            				if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
            					LOG[lineCount++] = "- importing saved BPErrors:";
                    		}
	                		
            				totalBPErrorsRecords = Integer.parseInt(reader.readLine());
            				for (int i = 0; i < arr_BPErrors.length; i++){
	                    		if (i < totalBPErrorsRecords) {
	                    			arr_BPErrors[i] = Double.parseDouble(reader.readLine());
	                    		}
	                    		else {
	                    			arr_BPErrors[i] = 0;
	                    		}
	                    	}
	                    	if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
	                    		LOG[lineCount++] = "Imported saved BPErrors.";
                    		}
                		}
                    	fileSettings_BPErrors = fileSettings_temp;
                    	flag_BPErrorsImported = true;
                	}//eo BPErrors
                	
                	//write code for new file uses here. 
                	//also change the string being called 
                	//ctr+f: Import data. ->Change imported filename here<- 
                	
                	//file is undefined - so returns error 8
                	else {
                		if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
                    		LOG[lineCount++] = "error 8:";
                    		LOG[lineCount++] = "fileSettings_temp: " + fileSettings_temp;
                    		LOG[lineCount++] = "fileSettings_stringTest: " + fileSettings_stringTest;
//                    		LOG[lineCount++] = "fileSettings_LUT: " + fileSettings_LUT;
                    		LOG[lineCount++] = "fileSettings_WL: "+ fileSettings_WL;
                    		LOG[lineCount++] = "fileSettings_weights: " + fileSettings_weights;
                    		LOG[lineCount++] = "fileSettings_QVals: " + fileSettings_QVals;
                    		LOG[lineCount++] = "fileSettings_BPErrors: " + fileSettings_BPErrors;
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
       
    	if(DEBUG_MULTI_file || DEBUG_import || DEBUG_ALL) {
    		LOG[lineCount++] = "end of fxn fileSettings check (succeeded):";
    		LOG[lineCount++] = "fileSettings_temp: " + fileSettings_temp;
    		LOG[lineCount++] = "fileSettings_stringTest: " + fileSettings_stringTest;
//    		LOG[lineCount++] = "fileSettings_LUT: " + fileSettings_LUT;
    		LOG[lineCount++] = "fileSettings_WL: "+ fileSettings_WL;
    		LOG[lineCount++] = "fileSettings_weights: " + fileSettings_weights;
    		LOG[lineCount++] = "fileSettings_QVals: " + fileSettings_QVals;
    		LOG[lineCount++] = "fileSettings_BPErrors: " + fileSettings_BPErrors;
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
     * 				Most available config settings:
     * 				    stringTest: 16400 (0x4010)
     * 					strLUT:		16416 (0x4020)
     *    				WL:			16448 (0x4040)
     *					NN weights: 16512 (0x4080)
     *					QVals:		16640 (0x4100)
     *					BPErrors:	16896 (0x4200)
     * 
     * @param: 		1. string of file name
     * 				and uses:
     * 				1. bool flag_LUTImported, static flag for preventing multiple imports
     * 				
     */

    public int exportData(String strName) {
    	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
    		LOG[lineCount++] = "@exportData: beginning";
    		LOG[lineCount++] = "printing fileSettings: ";
    		LOG[lineCount++] = "fileSettings_temp: " + fileSettings_temp;
    		LOG[lineCount++] = "fileSettings_stringTest: " + fileSettings_stringTest;
    		LOG[lineCount++] = "fileSettings_WL: "+ fileSettings_WL;
    		LOG[lineCount++] = "fileSettings_weights: " + fileSettings_weights;
    		LOG[lineCount++] = "fileSettings_QVals: " + fileSettings_QVals;
    		LOG[lineCount++] = "fileSettings_BPErrors: " + fileSettings_BPErrors;
    		
    	}
    	
    	//this condition prevents wrong file from being accidentally deleted. File is cleared whenever printstream accesses it (how?), so writing the correct information 
    	//	into the desired file is paramount to data retention.
    	if(  ( (strName == strStringTest) && (fileSettings_stringTest > 0) && (flag_stringTestImported == true) ) 
    	  || ( (strName == strWL)         && (fileSettings_WL > 0)         && (flag_WLImported == true) )
    	  || ( (strName == strWeights)    && (fileSettings_weights > 0)    && (flag_weightsImported == true) )
    	  || ( (strName == strQVals)      && (fileSettings_QVals > 0)	   && (flag_QValsImported == true) )	
    	  || ( (strName == strBPErrors)	  && (fileSettings_BPErrors > 0)   && (flag_BPErrorsImported == true) )	
    	  || ( (strName == strLog) ) 
    					){ 
	    	
    		PrintStream w = null;
	        
	        try {
	            w = new PrintStream(new RobocodeFileOutputStream(getDataFile(strName)));
	            // different commands between files
	            if (w.checkError()) {
	                //Error 0x03: cannot write
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "Something done messed up (Error 6 cannot write)";
	            	}
	            	return ERROR_6_export_cannotWrite;
	            }
	            
	            //if scope for exporting files to stringTest
	            if ( (strName == strStringTest) && (fileSettings_stringTest > 0) && (flag_stringTestImported == true) ) {
	            	
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "- writing into strStringTest:";
	            	}
	            	
	            	w.println(fileSettings_stringTest);
	            	
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "Successfully written into strStringTest.";
	            	}
	            	
	            	flag_stringTestImported = false;
	            } //end of testString
	            //TODO export weights
	            // weights
	            else if ( (strName == strWeights) && (fileSettings_weights > 0) && (flag_weightsImported == true) ) {
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "- writing into weights.dat:";
	            	}
	            	w.println(fileSettings_weights);
	            	for (int i = 0; i < numInputsTotal; i++) {
		         		for (int j = 0; j < numHiddensTotal; j++) {
		         			w.println(arr_wIH[i][j]);
		                }
		         	}
	            	
	            	w.println("998");
	            	
	            	for (int i = 0; i < numHiddensTotal; i++) {
		         		for (int j = 0; j < numOutputsTotal; j++) {
		         			w.println(arr_wHO[i][j]);
		                }	
		         	}
	            	w.println("999");
	            	
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "Successfully written into weights.";
	            	}
	            	
	            	flag_weightsImported = false;
	            } //end weights export
	            
//	            winlose
	            else if ( (strName == strWL) && (fileSettings_WL > 0) && (flag_WLImported == true) ){
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "- writing into winLose:";
	            	}
	            	
	            	w.println(fileSettings_WL);
	            	w.println(totalFights+1);
	            	for (int i = 0; i < totalFights; i++){
	        			w.println(battleResults[i]);
	            	}
	        		w.println(currentBattleResult);
	            	
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "Successfully written into winLose.";
	            	}
	            	
	            	flag_WLImported = false;
	            }// end winLose
	            
	            //ARR_QVals
	            else if ( (strName == strQVals) && (fileSettings_QVals > 0) && (flag_QValsImported == true) ){
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "- writing into QVals:";
	            	}
	            	
	            	w.println(fileSettings_QVals);
	            	w.println(totalQValRecords);
	            	for (int i = 0; i < totalQValRecords; i++){
	        			w.println(arr_QVals[i]);
	            	}
	            	
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "Successfully written into QVals.";
	            	}
	            	
	            	flag_QValsImported = false;
	            }// end QVals
	            
	            //ARR_BPErrors
	            else if ( (strName == strBPErrors) && (fileSettings_BPErrors > 0) && (flag_BPErrorsImported == true) ){
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "- writing into BPErrors:";
	            	}
	            	
	            	w.println(fileSettings_BPErrors);
	            	w.println(totalBPErrorsRecords);
	            	for (int i = 0; i < totalBPErrorsRecords; i++){
	            		w.println(arr_BPErrors[i]);
	            	}
	            	
	            	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	            		LOG[lineCount++] = "Successfully written into BPErrors.";
	            	}
	            }// end BPErrors
	            
	            //STRLOG
	            else if (strName == strLog) {
	            	//zeroes the log file in case it was filled from previous log session.
	            	if ((fileSettings_log & CONFIGMASK_ZEROINGFILE) == CONFIGMASK_ZEROINGFILE){
	            		w.print(0);
	            	}
	            	else{
		            	for (int i = 0; i < lineCount; i++){
		        			w.println(LOG[i]);
		            	}
	            	}
	            }
	            //end strLog
	            
	            
	            /* 
	             * add new files here - remember to add config settings and add to the beginning ifs
	             */
	                        
	        }
	        
	        //OC: PrintStreams don't throw IOExceptions during prints, they simply set a flag.... so check it here.
	        catch (IOException e) {
	    		if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	    			LOG[lineCount++] = "IOException trying to write: ";
	    		}
	            e.printStackTrace(out);
	            return ERROR_7_export_IOException;
	        } 
	        finally {
	            if (w != null) {
	                w.close();
	            }
	        }
	        
	        if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
	        	LOG[lineCount++] = "(succeeded export)";
	        }
	        return SUCCESS_exportData;
    	} //end of big if.
    	
    	//this should prevent wipes by writing when data isn't ready or available. If import was successful, then fileSettings would already be set.
    	//goal is to prevent accidentally wiping irrelevant file.
    	else {
    		return ERROR_10_export_dump;
    	}
    }
 

    
    /**binaryActivation function
     * @param X
     * @return newVal. 
     */
 	public double binaryActivation(double X) {
 		double newVal = 1/(1 + Math.exp(-X)); 
 		return newVal;
 	}
 	
 	/**Function name: bipolarActivation 
 	 * @param: current hidden value "z"
 	 * @return: new value evaluated at the f(X) = (2/(1 + e(-X))) - 1 
 	**/ 	
 	public double bipolarActivation(double X) {
 		double newVal = (2/(1 + Math.exp(-X)))-1; 
 		return newVal; 
 	}
 	
 	/** Function name: binaryDerivative
 	 * @param: input to take the derivative of based on f'(X) = f(X)*(1-f(X)). 
 	 * @return: derivative of value. 
 	 * 
 	 **/
 	public double binaryDerivative(double X) {
 		double binFunc = binaryActivation(X);
 		double binDeriv = binFunc*(1 - binFunc); 
 		return binDeriv;
 	}
 	
 	/** Function name: bipolarDerivative
 	 * @param: input to take the derivative of. 
 	 * @return: derivative of value: f'(X) =  0.5*(1 + f(X))*(1 - f(X));
 	 * 
 	 **/
 	public double bipolarDerivative(double X) {
 		double bipFunc = bipolarActivation(X);
 		double bipDeriv = 0.5*(1 + bipFunc)*(1 - bipFunc);  
 		return bipDeriv;
 	}
}


//TODO END OF ACTIVE CODE
/*
 *  Abandon all hope, ye who read below. Herein lies the graves of code obselete.
 */

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
	
 	
//
///**
//* @name:		importDataWeights
//* @author:		Andrea; partly written in sittingduckbot
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
//          	if (flag_useOfflineTraining) {reader = new BufferedReader(new FileReader(getDataFile("inToHiddenWeights_OfflineTraining.txt")));}
//          	else                         {reader = new BufferedReader(new FileReader(getDataFile("finalHiddenWeights.txt"))); }
//          	
//          	for (int i = 0; i < numInputsTotal; i++) {
//          		for (int j = 0; j < numHiddensTotal; j++) {
//          			arr_wIH[i][j] = Double.parseDouble(reader.readLine());
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
//          	if (flag_useOfflineTraining) {reader2 = new BufferedReader(new FileReader(getDataFile("hiddenToOutWeights_OfflineTraining.txt")));}
//          	else                         {reader2 = new BufferedReader(new FileReader(getDataFile("finalOuterWeights.txt")));}
//          	
//          	for (int i = 0; i < numHiddensTotal; i++) {
//          		for (int j = 0; j < numOutputsTotal; j++) {
//          			arr_wHO[i][j] = Double.parseDouble(reader2.readLine());
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
//		return ERROR_17_importWeights_flagImportedFalse;
//	}
//}
//
///**
//* @name: 		exportDataWeight
//* @author: 	Andrea; mostly sittingduckbot
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
//          	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
//          		LOG[lineCount++] = "Something done messed up (Error 14 cannot write)";
//          	}
//          	return ERROR_14_exportWeights_cannotWrite_NNWeights_inputToHidden;
//  		}
//  		 
//  		for (int i = 0; i < numInputsTotal; i++) {
//       		for (int j = 0; j < numHiddensTotal; j++) {
//       			w1.println(arr_wIH[i][j]);
//              }
//       	} 
//  	}
//  	catch (IOException e) {
//  		if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
//  			LOG[lineCount++] = "IOException trying to write: ";
//  		}
//          e.printStackTrace(out); 
//          return ERROR_16_exportWeights_IOException;
//      } 
//      finally {
//          if (w1 != null) {
//              w1.close();
//          }
//      }     
//  	
//  	PrintStream w2 = null;
//  	try {
//  		
//  		w2 = new PrintStream(new RobocodeFileOutputStream(getDataFile("finalOuterWeights.txt")));
//  		if (w2.checkError()) {
//              //Error 0x03: cannot write
//          	if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
//          		LOG[lineCount++] = "Something done messed up (Error 15 cannot write)";
//          	}
//          	return ERROR_15_exportWeights_cannotWrite_NNWeights_hiddenToOutput;
//  		 }
//  		 
//  		for (int i = 0; i < numHiddensTotal; i++) {
//       		for (int j = 0; j < numOutputsTotal; j++) {
//       			w2.println(arr_wHO[i][j]);
//              }
//       	}
//  	}
//  	catch (IOException e) {
//  		if(DEBUG_MULTI_file || DEBUG_export || DEBUG_ALL) {
//  			LOG[lineCount++] = "IOException trying to write: ";
//  		}
//          e.printStackTrace(out);
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
//		return ERROR_18_exportWeights_flagImportedTrue;
//	}
//}