//nothing's changed
package sampleex;


import robocode.AdvancedRobot;
import robocode.BattleEndedEvent;
import robocode.DeathEvent;
import robocode.RobocodeFileOutputStream;
import robocode.robotinterfaces.IBasicEvents;
import robocode.robotinterfaces.IBasicRobot;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;


public class DodgerRelocated extends AdvancedRobot{
	
	private int[][][] testArray = new int[3][3][3];

	
	
	public void run() {
		// Set colors
		setBodyColor(Color.white);
		setGunColor(Color.black);
		setRadarColor(Color.black);
		setScanColor(Color.white);
		setBulletColor(Color.red);
		

		try {
			BufferedReader reader = null;
			try {
				// Read file "count.dat" which contains 2 lines, a round count, and a battle count
				reader = new BufferedReader(new FileReader(getDataFile("DodgerQFile.dat")));
				
				for (int i = 0; i < 3; i++) {
					for (int j = 0; j < 3; j++) {
						for (int k = 0; k < 3; k++) {
							testArray[i][j][k] = Integer.parseInt(reader.readLine());
						}
					}
				}
				
			} finally {
				if (reader != null) {
					reader.close();
				}
			}
		} 
		catch (IOException e) {
			// Error0x01: error in file reading
			out.println("Something done fucked up (Error0x01 error in file reading)");
		} 
		catch (NumberFormatException e) {
			// Error0x02: error in int conversion
			out.println("Something done fucked up (Error0x02 error in int conversion)");
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					for (int k = 0; k < 3; k++) {
						testArray[i][j][k] = 0;
					}
				}
			}
		}

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				for (int k = 0; k < 3; k++) {
					testArray[i][j][k]+=5;
				}
			}
		}
		
//		
//		PrintStream w = null;
//		try {
//			w = new PrintStream(new RobocodeFileOutputStream(getDataFile("DodgerQFile.dat")));
//
//			for (int i = 0; i < 3; i++) {
//				for (int j = 0; j < 3; j++) {
//					for (int k = 0; k < 3; k++) {
//						w.println(testArray[i][j][k]);
//					}
//				}
//			}
//			// PrintStreams don't throw IOExceptions during prints, they simply set a flag.... so check it here.
//			if (w.checkError()) {
//				//Error 0x03: cannot write
//				out.println("Something done fucked up (Error0x03 cannot write)");
//			}
//		} catch (IOException e) {
//			out.println("IOException trying to write: ");
//			e.printStackTrace(out); //Joey: lol no idea what this means
//		} finally {
//			if (w != null) {
//				w.close();
//			}
//		}

		out.println("I have been a dodger duck"); 

		// Loop forever
		while (true) {
			turnGunRight(1); // Scans automatically
		}
	}
	
	public void OnBattleEnded(BattleEndedEvent event){
		out.println("wewhat");
		PrintStream w = null;
		try {
			w = new PrintStream(new RobocodeFileOutputStream(getDataFile("DodgerQFile.dat")));

			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					for (int k = 0; k < 3; k++) {
						w.println(testArray[i][j][k]);
					}
				}
			}
			// PrintStreams don't throw IOExceptions during prints, they simply set a flag.... so check it here.
			if (w.checkError()) {
				//Error 0x03: cannot write
				out.println("Something done fucked up (Error0x03 cannot write)");
			}
		} catch (IOException e) {
			out.println("IOException trying to write: ");
			e.printStackTrace(out); //Joey: lol no idea what this means
		} finally {
			if (w != null) {
				w.close();
			}
		}
	}
	public void OnDeath(DeathEvent event){
	}

}