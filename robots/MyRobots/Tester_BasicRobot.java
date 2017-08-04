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

import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;     // for Point2D's
import java.lang.*;         // for Double and Integer objects
import java.util.ArrayList; // for collection of waves

/**
 * SpinBot - a sample robot by Mathew Nelson.
 * <p/>
 * Moves in a circle, firing hard when an enemy is detected.
 *
 * @author Mathew A. Nelson (original)
 * @author Flemming N. Larsen (contributor)
 */
public class Tester_BasicRobot extends AdvancedRobot {
    public Point2D.Double _myLocation;     // our bot's location
    public Point2D.Double _enemyLocation;  // enemy bot's location
	private int settings = 0;
	String strSettings = null;
	/**
	 * SpinBot's run method - Circle
	 * @return 
	 */
	private double [][][] Q_NNFP_all = new double 
    		[4]
    		[2]
    		[3];
	public void run() {
		// Set colors
		
		setBodyColor(Color.blue);
		setGunColor(Color.blue);
		setRadarColor(Color.red);
		setScanColor(Color.yellow);
		
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
        for(;;){
        	setTurnRadarRight(20);
    		execute();					//from "AdvancedRobot" to allow parallel commands. 
        }
	}



	/**
	 * onScannedRobot: Fire hard!
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		_myLocation = new Point2D.Double(getX(), getY());
		double absBearing = e.getBearingRadians() + getHeadingRadians();
		_enemyLocation = project(_myLocation, absBearing, e.getDistance());
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
			double lateralVelocity = getVelocity()*Math.sin(e.getBearingRadians());
//			out.println(getVelocity() + " gbR:" + getHeadingRadians() + " gb:" + e.getHeadingRadians() + " s(gbR):" + Math.sin(e.getBearingRadians()));
			double enemyBearingFromRadar = (double)getHeading() + e.getBearing() - getRadarHeading();
			setTurnRadarRight(normalRelativeAngleDegrees(enemyBearingFromRadar));
		    double absBearing = e.getBearingRadians() + getHeadingRadians();
		    double absssBearing = absoluteBearing(_myLocation, _enemyLocation);
		    double factor = Utils.normalRelativeAngle(absBearing);
		    out.println(e.getBearingRadians() + " " + getHeadingRadians() + " " + absBearing + " " + e.getHeadingRadians() + " " + (e.getHeadingRadians() - absBearing) + " " +Math.sin(e.getHeadingRadians() - absBearing));
		    setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians()) * 2);
		 
		    scan();
		    execute();

		}
	}

	public void learningLoop3(){
		
	}
    public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }
	public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) {
        return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
            sourceLocation.y + Math.cos(angle) * length);
    }
}
