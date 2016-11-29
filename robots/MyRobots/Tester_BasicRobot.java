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
import robocode.RobocodeFileOutputStream;
import robocode.ScannedRobotEvent;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;


/**
 * SpinBot - a sample robot by Mathew Nelson.
 * <p/>
 * Moves in a circle, firing hard when an enemy is detected.
 *
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (contributor)
 */
public class Tester_BasicRobot extends AdvancedRobot {

	private int settings = 0;
	String strSettings = null;
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
		
		try {
			BufferedReader reader = null;
			try {
				// Read file "count.dat" which contains 2 lines, a round count, and a battle count
				reader = new BufferedReader(new FileReader(getDataFile("test.dat")));

				// Try to get the counts
				settings = Integer.parseInt(reader.readLine());

			} 
			finally {
				if (reader != null) {
					reader.close();
				}
			}
		} catch (IOException e) {
			// Something went wrong reading the file, reset to 0.
		} catch (NumberFormatException e) {
			// Something went wrong converting to ints, reset to 0
		}


		// Increment the # of rounds
		settings = 0x4000;

		// If we haven't incremented # of battles already,
		// Note: Because robots are only instantiated once per battle, member variables remain valid throughout it.

		PrintStream w = null;
		try {
			w = new PrintStream(new RobocodeFileOutputStream(getDataFile("test.dat")));

			w.println(settings);

			// PrintStreams don't throw IOExceptions during prints, they simply set a flag.... so check it here.
			if (w.checkError()) {
				out.println("I could not write the count!");
			}
		} catch (IOException e) {
			out.println("IOException trying to write: ");
			e.printStackTrace(out);
		} finally {
			if (w != null) {
				w.close();
			}
		}
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
			setTurnGunRight(normalRelativeAngleDegrees(bearingFromGun+20));
			setTurnRight(normalRelativeAngleDegrees(e.getBearing() - 90));
    		setAhead(50);
			
			scan();
//			

			
			execute();
		}
	}

	public void learningLoop3(){
		
	}
	
}
