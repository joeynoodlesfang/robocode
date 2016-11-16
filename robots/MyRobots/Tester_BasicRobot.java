/**
 * Copyright (c) 2001-2016 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://robocode.sourceforge.net/license/epl-v10.html
 */
package MyRobots;


import robocode.AdvancedRobot;
import robocode.HitRobotEvent;
import robocode.ScannedRobotEvent;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.*;


/**
 * SpinBot - a sample robot by Mathew Nelson.
 * <p/>
 * Moves in a circle, firing hard when an enemy is detected.
 *
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (contributor)
 */
public class Tester_BasicRobot extends AdvancedRobot {

	/**
	 * SpinBot's run method - Circle
	 * @return 
	 */
	
	public void run() {
		// Set colors
		
		setBodyColor(Color.blue);
		setGunColor(Color.blue);
		setRadarColor(Color.red);
		setScanColor(Color.yellow);
		
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		
		learningLoop();

	}

	/**
	 * onScannedRobot: Fire hard!
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		
		learningLoop2(e);
	}

	public void learningLoop(){
		while (true) {
		
		setTurnRadarRight(45);
		execute();
			
//			fire(1);
//			turnLeft(30);
//			ahead(50);

			out.println(getGunHeat());
		}
	}

	public void learningLoop2(ScannedRobotEvent e){
		while (true) {
			double bearingFromRadar = getHeading() + e.getBearing() - getRadarHeading();
			double bearingFromGun = getHeading() + e.getBearing() - getGunHeading();
			setTurnRadarRight(normalRelativeAngleDegrees(bearingFromRadar));
			setTurnGunRight(normalRelativeAngleDegrees(bearingFromGun));
			setAhead(50);
			setTurnRight(-30);
			
			scan();
//			

			
			execute();
		}
	}

	public void learningLoop3(){
		
	}
	
}
