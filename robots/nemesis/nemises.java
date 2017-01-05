package nemesis;

import robocode.*;
//to set the color
import java.awt.Color;

public class nemises extends AdvancedRobot {
	
	public Driver driver;
	public Gunner gunner;
	public static Scanner scanner;

	public nemises(){
		//Allocate mem for the static variables
		Scanner.initstatic(this);
		// The three people in the tank
		driver = new Driver(this);
		scanner= new Scanner(this);
		gunner=  new Gunner(this);		
	}
	
	public void run() {
		driver.init();				
		
		// sets the colours of the robot
		setColors(Color.black, Color.black, Color.black);
		// Keep the gun still when we turn the tank
		setAdjustGunForRobotTurn(true);
		// Keep radar still when we turn the gun
		setAdjustRadarForGunTurn(true);		
		while (true) {			
			scanner.procscan();
			gunner.shoot();			
			driver.drive();			
			//out.println("One round done in :" +getTime()+"\n\n\n");
			execute();
		}
	}
	
	public void onHitByBullet(HitByBulletEvent e) {
		driver.hitbybullet(e);		
	}
	public void onScannedRobot(ScannedRobotEvent e) {
		scanner.botscanned(e);		
	}
	
	public void onHitWall(HitWallEvent e)
	{	
		driver.hitwall(e);
	}
	
	public void onRobotDeath(RobotDeathEvent e){
		scanner.robotdeath(e);
	}
	
	public void onDeath( DeathEvent e){		
		scanner.closefiles();
	}
	
	public void onBulletHit(BulletHitEvent e){
		gunner.scored(e);
	}
	
}
