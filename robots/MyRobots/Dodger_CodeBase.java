package MyRobots;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

import robocode.AdvancedRobot;
import robocode.RobocodeFileOutputStream;

public class Dodger_CodeBase extends AdvancedRobot {
		
	public void setColors() {
		setBodyColor(Color.white);
		setGunColor(Color.black);
		setRadarColor(Color.black);
		setScanColor(Color.white);
		setBulletColor(Color.red);
	}
	
	public boolean importLUTData(boolean repeatFlag, double[][][][][] roboLUT, int[] roboLUTDimensions){
		if (repeatFlag == false) {
			try {
				BufferedReader reader = null;
				try {
		// Read file "count.dat" which contains 2 lines, a round count, and a battle count
					reader = new BufferedReader(new FileReader(getDataFile("DodgerQFile.dat")));

					for (int p0 = 0; p0 < roboLUTDimensions[0]; p0++) {
						for (int p1 = 0; p1 < roboLUTDimensions[1]; p1++) {
							for (int p2 = 0; p2 < roboLUTDimensions[2]; p2++) {
								for (int p3 = 0; p3 < roboLUTDimensions[3]; p3++) {
									for (int p4 = 0; p4 < roboLUTDimensions[4]; p4++) {
										roboLUT[p0][p1][p2][p3][p4] = Double.parseDouble(reader.readLine());
									}
								}
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
			}
		}
		
		repeatFlag = true;
		
		return repeatFlag;
	}
	
	public boolean exportLUTData(boolean repeatFlag, double[][][][][] roboLUT, int[] roboLUTDimensions){
		
		if (repeatFlag == true) {

			out.println("wewhat");
			PrintStream w = null;
			try {
				w = new PrintStream(new RobocodeFileOutputStream(getDataFile("DodgerQFile.dat")));
	
				for (int p0 = 0; p0 < roboLUTDimensions[0]; p0++) {
					for (int p1 = 0; p1 < roboLUTDimensions[1]; p1++) {
						for (int p2 = 0; p2 < roboLUTDimensions[2]; p2++) {
							for (int p3 = 0; p3 < roboLUTDimensions[3]; p3++) {
								for (int p4 = 0; p4 < roboLUTDimensions[4]; p4++) {
									w.println(roboLUT[p0][p1][p2][p3][p4]);
								}
							}
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
		
		repeatFlag = false;
		
		return repeatFlag;
	}

	
	/*
	 * @purpose: get state values from program, discretize, and edit array of current state vector.  
	 * @param: Array of current state vector
	 * @return: none
	 */
	
	public void generateCurrentStateVector(int[] currentStateVector){
	
		/* Dimension 0:
		 *  xposdisc
		 */
		currentStateVector[0] = (int)(getX()/100);
		
		/* Dimension 1:
		 *  yposdisc
		 */
		
		currentStateVector[1] = (int)(getY()/100);
		
		
		/* Dimension 2:
		 *  getHeadingDisc
		 *  output: int bodyHeadingDiscretized
		 */
		
		currentStateVector[2] = (int)(getHeading() / 8);
		
		/* Dimension 3:
		 *  getVeloDisc
		 *  output: int
		 */
		
		currentStateVector[3] = (int)((getVelocity() + 8) / 5); // might not make sense
		
	}
	
    /* @purpose: LUT discretization, get current Q-value from state/action, get max q-value of all actions in that state, 
     * @purpose: calculate new q-value and update LUT. 
     * @param: double array reward, int currentStateVector[]  already discretized (size numStates), double [][][][][][[] LUT, int [] currentStateActionVector. 
     * @return: state-action vector
     */
    public void qFunction(double[] reward, int [] currentStateVector, double[][][][][] roboLUT, int [] currentStateActionVector){
        double qArrayMax[] = new double [2]; 
        int numActions = 4; 
        getMax(currentStateVector, roboLUT, qArrayMax);                             //call the GetMax function below to get maximum of all actions in that state
        updateLUT(roboLUT, qArrayMax, currentStateVector, currentStateActionVector, reward, numActions);
        
    }
    
    public void getMax(int[] newVec, double[][][][][] roboLUT, double [] qArrayMax) {
    	
        int numActions = 4; 
        int currIndex = 0;
        double currMax = 0.0; 
        for (int i = 0; i < numActions; i++){
            double newMax = 5;

            if (currMax <  newMax){
                currMax = newMax; 
                currIndex = i; 
            }
            else{
                currMax = currMax; 
            }
        }
        qArrayMax[0] = currMax; 
        qArrayMax[1] = currIndex;  
       
    }
    //update ALL actions in that state. 
    public void updateLUT(double[][][][][] roboLUT, double[] qArrayMax, int[] currentStateVector, int [] currentStateActionVector, double[] reward, int numActions){
        //variables for the q-function
        double alpha = 0.2;                 //to what extent the newly acquired information will override the old information.
        double gamma = 0.8;                 //importance of future rewards
        
        for (int index = 0; index < numActions; index++){
            double currQVal = 5;
            currQVal += currQVal + alpha*(reward[0] + gamma*qArrayMax[0] - currQVal);
            currentStateVector[3] = 5;
            roboLUT[0][0][0][0][0] = currQVal;
        }
        currentStateVector[0] = (int) qArrayMax[1];                     //new action
        for (int indexA = 1;  indexA < currentStateVector.length; indexA ++){
            currentStateActionVector[indexA] = currentStateVector[indexA - 1];  
        }
        reward[0] = 0;
    }
 
    /*
     * @purpose: convert state action vector into action
     * @param: Array of current state action vector
     * @return: none
     */
    public void doAction(int[] currentStateActionVector){
    	if (currentStateActionVector[0] == 0) {
    		ahead(100);
    	} else if (currentStateActionVector[0] == 1) {
    		back(100);
    	} else if (currentStateActionVector[0] == 2) {
    		turnLeft(45);
    	} else if (currentStateActionVector[0] == 3) {
    		turnRight(45);
    	}
    }

}