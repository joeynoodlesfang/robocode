/*
-> INTRO TO THE BOT <-
LUTTrackfire is a robocode bot that implements reinforcement learning (RL) techniques to improve its combat 
abilities. RL requires the bot to remember the results of certain moves (aka actions) in certain situations (aka 
states). The results are converted by the coder into a reward, which can be positive or negative. The reward allows 
the bot to make better future decisions for the exact same situation. LUTTrackfire stores these rewards in the form 
of a look-up table. Each reward entry in the look-up table has a unique state and action parameter. Most of the combat
abilities of LUTTrackfire were taken from the example robot Trackfire.

Since memory is cleared when the Robocode program is terminated, LUT reward values are stored externally. Robocode
limits the size of all external files for a particular robot to be 200kB.

REINFORCEMENT LEARNING, RL, refers to the method by which a machine decides on the action to take. 
The decision is made by choosing the action that maximizes a conceptualized reward: the bot 
performs an action within a measurable environment, and rewards itself based upon the results of 
said action. The reward alters the likelihood of re-performing the same action again in the same 
environment: winning moves can be recreated, and poor moves can be avoided. This ability to learn 
during the actual battle gives the robot combat adaptibility.
*/

/** Plan of attack - have multiple battle plans. 
 *	THIS IS THE ROBOT TO TRAIN. 
 *  Two states and one action [offensive - trackfire, defensive -moveLeft, adjustAngleGun, action]
	Exploratory 	- (1) search for enemy, if find enemy give highest reward. That way, go back to this state
	Battle Plan 1.  - (1) Found enemy, attack . If hit enemy, award high reward. If miss enemy, award low reward. If get hit, negative reward
	Battle Plan 2. 	- (1) Found enemy, check distance. If close, fire hard, if far, move closer. 
	
	Questions:
	Joey: What sort of inputs?
		e.getBearing  - these two combine for bearingFromGun
		getGunHeading -
		getHeading
		getDistance
	Joey: why does static vars keep their vals after every battle?
	
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
		- [done] rewrite imports
		- [done] rewrite exports
		- [done] add config lines in current txt
		- `change run fxn
	
	2016-11-21 - j
		- [done] switch around error labels to have number first
		
	2016-11-23 - j
		- test imp/exp settings
		- [done; too much time-wise to string convert both ways] explore possibilities of converting string-reading into hex 
		- `learn about throws, catches, exceptions
		- fixed: multiplefileflag not flipping in between - due to learningloop() invoked prior to export leaving static flag true and carry over
		- [currently nonstatic] consider: changing flag to non-static to allow for proper import if export fails and battle ends.
					- testing as non-staic currently
					?Will this assist in preventing issues from multiple samebot invokes in importing and exporting data?
					we want it to be static so that it will prevent multiple accesses, so that we keep 1 import -> 1 export format, 
					and locked in the use of the import of the file. 
					?Can robot2 export robot1's import? 
					no matter
					the point of flag is also to act as indicator if immediately previous import was successful. therefore should not be static.
		- fixed: sometimes file gets wiped: check if accessing file during beginning of export clears file. - added multiple or's for 
				 stringname, and thus will require editing for every new file added.
		
	2016-11-26 - j
		- continue testing imp/exp
		- `consider: static flag_error
		- `test out zeroLUT
		- tested out import export for LUT, zerolut, WL, all work.
		
	
	November 28, 2016. 4:03 pm. 
		implementing new state-action pairs. 
		hope to train NN by tonight 
		okay.. don't know how to implement states for the velocity and for the firing action.. not sure what this is
	
	december 1, 2016
		hotfixed zeroLUT bug and an oob bug.
	
	december 2, 2016
		working on new states branch.
		- `figure out sine/cosine enemy angle
		- `consider implementing reward system based on "change in health difference"
		
	-to zero data, go to the first line of LUTTrackfire and add 1 to the number. 
	
	This is the one that is used for offline training. 
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

public class LUTTrackfire extends AdvancedRobot{
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
    private static final double alpha = 0.6;                //to what extent the newly acquired information will override the old information.
    private static final double gamma = 0.5;                //importance of future rewards
    private static final double epsilon = 0.80; 			//degree of exploration     
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
    
    //strings used for importing or extracting files 
    String strStringTest = "stringTest.dat";    
    String strLUT = "LUTTrackfire.dat";
    String strWL = "winlose.dat";
    String strSA = "stateAction.dat"; 
    
    /**
	 * STATEACTION VARIABLES for stateAction ceilings.
	 * Currently 518400 state/actions
	 */
    private static final int num_actions = 24; 
    private static final int myPositionDiscretized_states = 5;
    private static final int myHeadingDiscretized_states = 4; 
    private static final int enemyDirection_states = 3; 						
    private static final int enemyDistance_states = 3;							
    private static final int enemyEnergy_states = 2;		
    
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
    // printout error flag - initialized to 0, which is no error.
    static private int flag_error = 0;

//    //Flag used if user desires to zero LUT at the next battle. 
//    static private boolean zeroLUT = false; 

    /**
     *  OTHER GLOBALS
     */
    

    // LUT table configuration information, stored in the first line of .dat
    private short fileSettings_default = 0;
    private short fileSettings_stringTest = 0;
    private short fileSettings_LUT = 0; 
    private short fileSettings_WL = 0;
    
    // LUT table stored in memory.
    private static int [][][][][][] roboLUT 
        = new int
        [num_actions]
        [myPositionDiscretized_states]
        [myHeadingDiscretized_states]		
        [enemyEnergy_states]
        [enemyDistance_states]
        [enemyDirection_states];
    
    // Dimensions of LUT table, used for iterations.
    private static int[] roboLUTDimensions = {
        num_actions, 
        myPositionDiscretized_states,
        myHeadingDiscretized_states,
        enemyEnergy_states,
        enemyDistance_states,
        enemyDirection_states};
    
    // Stores current reward for action.
    private double reward = 0.0; //only one reward variable to brief both offensive and defensive maneuvers
    private int energyDiffCurr = 0;
    private int energyDiffPrev = 0;
    
    // Stores current and previous stateAction vectors.
    private int currentStateActionVector[] = new int [roboLUTDimensions.length];
    private int prevStateActionVector[]    = new int [roboLUTDimensions.length]; 
     
    //variables used for getMax.
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
        
//        flag_error = importData("strStringTest.dat");
//        if( flag_error != SUCCESS_importData) {
//        	out.println("ERROR: " + flag_error);
//        }
        
        flag_error = importData(strLUT);
        if(flag_error != SUCCESS_importData) {
        	out.println("ERROR @run LUT: " + flag_error);
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
        flag_error = exportData(strLUT);				//strLUT = "LUTTrackfire.dat"
        
        
        if(flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onBattleEnded: " + flag_error); //only one to export due to no learningloop(), but fileSettings_
        	//LUT is 0'd, causing error 9 (export_dump)
        }
//        flag_error =  exportData(strSA); 
        flag_error = saveData(strSA); 
        if(flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onBattleEnded: " + flag_error); //only one to export due to no learningloop(), but fileSettings_
        	//LUT is 0'd, causing error 9 (export_dump)
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
        flag_error = exportData(strLUT);
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR @onDeath: " + flag_error);
        }
        
        flag_error = exportData(strWL);					//"strWL" = winLose.dat
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR: " + flag_error);
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
        flag_error = exportData(strLUT);
        
        if( flag_error != SUCCESS_exportData) {
        	if (debug_export || debug) {
        		
        	}
        	out.println("ERROR: " + flag_error);
        }
        
        flag_error = exportData(strWL);
        if( flag_error != SUCCESS_exportData) {
        	out.println("ERROR: " + flag_error);
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
    		calculateReward();
        	copyCurrentSAVIntoPrevSAV();
        	generateCurrentStateVector();
        	qFunction(); 
        	resetReward();
        	doAction(); 
    	}
    }

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
    	//tick%3 is possible new state. 
//        if (tick%3 == 0) {

             calculateReward();
             copyCurrentSAVIntoPrevSAV();

             generateCurrentStateVector();


             qFunction();

             resetReward();

             doAction();

//        }
//
//        else {

            setTurnGunRight(normalRelativeAngleDegrees(enemyBearingFromGun));

//        }

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
    	//Dimension 1: input: myPositionDiscretized = 0-4: center, left, right, top, bot
    	if (  (myPosX<=50)  &&  ( (myPosX <= myPosY) || (myPosX <= (600-myPosY)) )  ){					//left
    		currentStateActionVector[1] = 1;						
    	}
    	else if (  (myPosX>=750)  &&  ( ((800-myPosX) <= myPosY) || ((800-myPosX) <= (600-myPosY)) )  ){		//right
    		currentStateActionVector[1] = 2;						
    	}
    	else if (myPosY<=50) {		//top 
    		currentStateActionVector[1] = 3;
    	}
    	else if (myPosY>=550) {		//bottom				
    		currentStateActionVector[1] = 4;
    	}
    	else {
    		currentStateActionVector[1] = 0; 
    	}

    	//Dimension 2: input: myHeading = 0-3: 0-89, 90-179, 180-269, 270-359
    	if (myHeading < 90) {
    		currentStateActionVector[2] = 0;
    	}
    	else if (myHeading < 180) {
    		currentStateActionVector[2] = 1;
    	}
    	else if (myHeading < 270) {
    		currentStateActionVector[2] = 2;
    	}
    	else {
    		currentStateActionVector[2] = 3;
    	}
		
		//Dimension 3: input: enemyEnergy: 0-1
		if (enemyEnergy > 30) {
			currentStateActionVector[3] = 0;
		}
		else {
			currentStateActionVector[3] = 1;
		}
		
		//Dimension 4: input: enemyDistance: 0-2
		if (enemyDistance < 150) {
			currentStateActionVector[4] = 0;
		}
		else if (enemyDistance < 350) {
			currentStateActionVector[4] = 1;
		}
		else {
			currentStateActionVector[4] = 2;
		}
		
		//Dimension 5: is enemy moving right, left, or within the angle of my gun?
		//requires mygunheading, enemyheading, enemyvelocity
		if ((enemyHeadingRelativeAbs < 30) || (enemyHeadingRelativeAbs > 150) || (enemyVelocity == 0)) {
			currentStateActionVector[5] = 0; //within angle of gun
		}
		else if ( ((enemyHeadingRelative < 0)&&(enemyVelocity > 0)) || ((enemyHeadingRelative > 0)&&(enemyVelocity < 0)) ) {
			currentStateActionVector[5] = 1; //enemy moving left
		}
		else if ( ((enemyHeadingRelative < 0)&&(enemyVelocity < 0)) || ((enemyHeadingRelative > 0)&&(enemyVelocity > 0)) ){
			currentStateActionVector[5] = 2; //enemy moving right
		}
		else {
			out.println("Error! CSAV[5]");
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
       int prevQVal = (int)calcNewPrevQVal();
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
            indexQVal = (double) roboLUT[i][currentStateActionVector[1]][currentStateActionVector[2]][currentStateActionVector[3]][currentStateActionVector[4]][currentStateActionVector[5]];
            
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
        System.out.println("Action Chosen: " + actionChosenForQValMax  + " qVal: " + qValMax);
    }
    
    /**
     * @name		calcNewPrevQVal
     * @purpose		1. Calculate the new prev q-value based on Qvalue function.
     * @param		n, but uses:
     * 				1. qValMax
     * @return		prevQVal
     */
    public int calcNewPrevQVal(){

        double prevQVal = roboLUT[prevStateActionVector[0]]
        						 [prevStateActionVector[1]]
        						 [prevStateActionVector[2]]		 
        						 [prevStateActionVector[3]]
        						 [prevStateActionVector[4]]
        						 [prevStateActionVector[5]];
        
        prevQVal += alpha*(reward + gamma*qValMax - prevQVal);
        return (int)prevQVal;
        
    }
    
    /**
     * @name:		updateLUT
     * @purpose:	1. Update prevSAV in LUT with the calculated prevQVal.
     * 				2. Update curr state with correct action based on policy (greedy or exploratory).
     * @param:		1. prevQVal
     * @return:		n
     */
    public void updateLUT(int prevQVal){
        int valueRandom = 0;
        
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
        if (policy == SARSA) {
        	currentStateActionVector[0] = valueRandom;
        }
        
        else if(policy == exploratory) {
        	currentStateActionVector[0] = (Math.random() > epsilon ? actionChosenForQValMax : valueRandom);
        }
        
        else{ 
        	currentStateActionVector[0] = actionChosenForQValMax;
        }
        //Choosing next action based on policy.
//        valueRandom = (int)(Math.random()*(num_actions));
//     
//        if (policy == exploratory) {
//        	currentStateActionVector[0] = valueRandom;
//        }
//        else if (policy == SARSA){ 
//        	currentStateActionVector[0] = actionChosenForQValMax;
//        }
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
                	else if ( ((fileSettings_default & CONFIGMASK_FILETYPE_LUTTrackfire) == CONFIGMASK_FILETYPE_LUTTrackfire)
                	&& (flag_LUTImported == false) )
                	{
                		if (strName != "LUTTrackfire.dat") {
            				if (debug_import || debug) {
            					out.println ("Import aborted (Imported wrong file - file declared LUTTrackfire.dat in settings)");
            				}
                			return ERROR_11_import_wrongFileName_LUT;
                		}
                		// confirmed filename matches filetype reported by settings in file for LUT. 
                		
                    	if ((fileSettings_default & CONFIGMASK_ZEROLUT) == CONFIGMASK_ZEROLUT) {
                    		if (debug_import || debug) {
                    			out.println("starting loop of zeroLUT:");
                    		}
    	                    for (int p0 = 0; p0 < roboLUTDimensions[0]; p0++) {
    	                        for (int p1 = 0; p1 < roboLUTDimensions[1]; p1++) {
    	                        	for(int p2 = 0; p2 < roboLUTDimensions[2]; p2++) {
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
    	                        					roboLUT[p0][p1][p2][p3][p4][p5] = Integer.parseInt(reader.readLine());
    	                        				}
    	                        			}
    	                        		}
    	                        	}
    	                        }
    	                    } // end of data extraction for LUTTrackfire
                    		if (debug_import || debug) {
                            	out.println("Imported LUT data (zeroLUT or normal)");
                            }
                    	}
                    	//fileSettings copied individually into LUT for exporting purposes.
                		fileSettings_LUT = fileSettings_default;
                		flag_LUTImported = true; //sets flag to prevent multiple instances of robot importing(and exporting) from same .dat (and causing weird interactions(?)).
                	} // end of LUTTrackfire
                	
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
        		out.println("Something done messed up (Error0x01 error in file reading)");
        	}
            return ERROR_1_import_IOException;
        } 
        // type of exception where there is a wrong number format (type is wrong or blank)  
        catch (NumberFormatException e) {
            if (debug_import || debug) {
            	out.println("Something done messed up (Error0x02 error in type conversion - check class throw for more details)");
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
	            		out.println("Something done messed up (Error 6 cannot write)");
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
	            
	        	//update LUT
	            else if ( (strName == strLUT) && (fileSettings_LUT > 0) && (flag_LUTImported == true) ) {
	            	
	            	//DEBUG
	            	if (debug_export || debug) {
	            		out.println("writing into strLUT");
	            	}
	            	
	            	//both zeroLUT and correct LUT will be written here
	        		//following if prevents repeat zeroLUT from occurring by editing the zero flag.
	        		if ((fileSettings_LUT & CONFIGMASK_ZEROLUT) == CONFIGMASK_ZEROLUT) {
	        			
	        			//DEBUG
	        			if (debug_export || debug) {
	        				out.println("attempting to write zeroes to LUT: ");
	        				out.println("fileSettings_LUT before zeroing:" + fileSettings_LUT);
	        			}
	        			
	        			//only this line in this if
	        			fileSettings_LUT -= CONFIGMASK_ZEROLUT;
	        			
	        			//DEBUG
	        			if (debug_export || debug) {
	        				out.println("fileSettings_LUT after zeroing:" + fileSettings_LUT);
	        			}
	        		}
	        		
	        		w.println(fileSettings_LUT);
	                for (int p0 = 0; p0 < roboLUTDimensions[0]; p0++) {
	                    for (int p1 = 0; p1 < roboLUTDimensions[1]; p1++) {
	                    	for (int p2 = 0; p2 < roboLUTDimensions[2]; p2++) {
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
	                flag_LUTImported = false;
	                
	        	} //endof trackfire

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
    
    public int saveData(String strName) {
    	PrintStream w = null;
        try {
            w = new PrintStream(new RobocodeFileOutputStream(getDataFile(strName)));
//        	out.println("writing into strSA");
        	//DEBUG
        	if (debug_export || debug) {
        		out.println("writing into strSA");
        	}
            for (int p0 = 0; p0 < roboLUTDimensions[0]; p0++) {
                for (int p1 = 0; p1 < roboLUTDimensions[1]; p1++) {
                	for (int p2 = 0; p2 < roboLUTDimensions[2]; p2++) {
                		for (int p3 = 0; p3 < roboLUTDimensions[3]; p3++) {
                			for (int p4 = 0; p4 < roboLUTDimensions[4]; p4++) {
                				for (int p5 = 0; p5 < roboLUTDimensions[5]; p5++) {
                					w.println("\t" + p0+p1+p2+p3+p4+p5);
                				}
            				}
                		}
                	}
                }
            }
    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    finally {
        if (w != null) {
            w.close();
        }
    } 
    return SUCCESS_exportData;
}

}