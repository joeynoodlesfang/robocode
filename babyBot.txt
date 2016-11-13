package babyBot;
import java.awt.Color;
import java.util.Arrays;

import robocode.AdvancedRobot;
import robocode.Bullet;
import robocode.DeathEvent;
import robocode.HitWallEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;
import robocode.RateControlRobot;

public class FireKrackerKat extends AdvancedRobot{
	/* 
	 * Declarations are done here first
	 * Declare empty array for LUT 
	 * Declare empty array for the currState, nextState, prevState
	 */		
	static int x   = 3; 				//3 levels
	static int y   = 3;					//3 levels
	static int eM  = 3; 				//3 levels
	static int eE  = 3; 				//3 levels
	static int velocityM = 3; 			//3 power levels
	static int velocityE = 3; 			//3 power levels
	static int gunHeading = 4; 				//4 levels for scanning range. 
	static int bodyHeading = 4; 				//4 levels
	static int a = 6;		 			//5 actions
	static int other = 1; 				//for other states or actions later on. 
	static int numStateActions = x + y + eM + eE + gunHeading + bodyHeading + a + other;
	private double[][][][][][][][][][] roboLUT = new double[x][y][eM][eE][velocityM][velocityE][gunHeading][bodyHeading][a][other];		//this is the look-up table.
	static int featureVectorLength = 9;		
	static double [] roboRewards = new double[featureVectorLength]; 
	LUTNew myLUT = new LUTNew(numStateActions);
	/* run() is the main method for the robot to take actions
	 * @param: none
	 * @return: none
	 */
	public void run() {
		setColors(Color.pink,Color.blue,Color.green, Color.cyan, Color.yellow); // body,gun,radar
		while(true) {
			//here is where we continuously do some action. 
			ahead(100);
			turnRight(10); 
			ahead(100); 
			turnLeft(10);
			
		}
	}
	
	//Event Handlers:
	
	public void onScannedRobot(ScannedRobotEvent event) {
		//What to do when you scan enemy
       if (event.getDistance() < 100 && getEnergy() > 70) {
    	   System.out.println("distance " + event.getDistance());
           fire(2);
           //here, I took an action, here is where I update my LUT. 
           stop(); 
//           myLUT.updateLUT();
           resume(); 
       } 
       else {
    	   ahead(10); 
	       }
	   }
	
	public void onHitWall(HitWallEvent event){
		System.out.println("HIT WALL");
		double myCurrentBearing  = event.getBearing(); 
		double currVelocity = getVelocity();
		System.out.println("currVelocity " + currVelocity);
		System.out.println("myCurrentLocation " + myCurrentBearing );
        stop(); 
        myLUT.updateLUT(currVelocity);
        resume(); 
		//need to fix this code.. do some math. 
		if (myCurrentBearing > -90 && myCurrentBearing <= 90){
			back(100);
		}
		else{
			ahead(100);
		}
	}
//	public void onWin(WinEvent event){
//		ahead(10);	
//		stop(); 
//		resume();
//		turnRight(45); 
//		turnLeft(45); 
//		ahead(-10); 
//		updateLUT();
//	}
//	public void onDeath(DeathEvent event){
//		updateLUT();
//	}
//	public void updateLUT(){
//       /*
//        * e.g. prevState = currState; 
//        * currState = onScannedRobot(ScannedRobotEvent event) + currState; 
//        * takeAction --. what is this? is this like "onBulletHit(e)?"
//        * myLUT[prevStateAction] = myLUT[prevStateAction] + alpha*(newReward + gamma*myLUT[currStateAction] - myLUT[prevStateAction])
//        * */		
//	}
	}	

//if (getGunHeat() == 0) {
//       Bullet bullet = fireBullet(Rules.MAX_BULLET_POWER);
//       // Get the velocity of the bullet
//       if (bullet != null) {
//           double bulletVelocity = bullet.getVelocity();
//       }
//   }