package MyRobots;
/* @author: Andrea Marti 23208093
 * @date : October 26, 2016 
 * @class: EECE 592 
 * @purpose: Class "BackPropTest" is used to test the backPropagation XOR problem. 
*/

/* Import headers */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/* Main Testing Class */
public class backPropFinalTest{
	public static void main(String args[])
	{
		
		for (int a = 0; a < 10; a ++){	 
			/*Initiate variables */
			int numOutput = 1; //number of outputs per training set. 
			int numInputs = 3; //number of inputs for training set
			int numHidden = 5; //number of hidden inputs
			int numTrials = 4; //number of trials in the training set
			double lRate = 0.2; //learning rate
			double errorThreshold = 0.05;//error threshold
			double momentum = 0.0;  	//value of momentum 
			boolean stopError = false; 	//if flag == false, then stop loop, else continue 
			int maxEpoch = 10000; 	//if reach maximum number of Epochs, stop loop. 
			double upperThres = 0.5; 	//upper threshold for random weights
			double lowerThres = -0.5;	//lower threshold for random weights
			
			/* Neural net state action pair*/
			boolean flag = true; 
			
		    // Dimensions of LUT table, used for iterations.
		    /**
			 * STATEACTION VARIABLES
			 */
		    final int[] action = new int [10]; 								
			final int enemyBearing = 360; 							// 360 degrees 
		    final int distanceX = 800; 								//800x600
		    final int distanceY = 600; 								//800x600
		    final int myEnergyState = 120;								//energy can be more than 100
		    final int enemyEnergyState = 120; 							//energy can be more than 100
		    
		    double [][][][][][][][][][][][][][][] inputs 
		        = new double
		        [action[0]]
		        [action[1]]
		        [action[2]]
		        [action[3]]
		        [action[4]]
		        [action[5]]
		        [action[6]]
		        [action[7]]
		        [action[8]]
		        [action[9]]
		        [enemyBearing]
		        [distanceX ]		
		        [distanceY]
		        [myEnergyState]
		        [enemyEnergyState];
	

	    
//			for (int p0 = 0; p0 < inputs[0]; p0++) {
//                for (int p1 = 0; p1 < inputs[1]; p1++) {
//                	for(int p2 = 0; p2 <inputs[2]; p2++){
//                		for (int p3 = 0; p3 < inputs[3]; p3++) {
//                			for (int p4 = 0; p4 < inputs[4]; p4++) {
//                				for (int p5 = 0; p5 < inputs[5]; p5++) {
//                					for (int p6 = 0; p6 < inputs[6]; p6++) {
//                    					for (int p7 = 0; p7 < inputs[7]; p7++) {
//                        					for (int p8 = 0; p8 < inputs[8]; p8++) {
//                            					for (int p9 = 0; p9 < inputs[9]; p9++) {
//                                					for (int p10 = 0; p9 < inputs[10]; p10++) {
//                                    					for (int p11 = 0; p11 < inputs[11]; p11++) {
//                                        					for (int p12 = 0; p12 < inputs[12]; p12++) {
//                                            					for (int p13 = 0; p13 < inputs[13]; p13++) {
//                                                					for (int p14 = 0; p14 < inputs[14]; p14++) {
//                                                    					for (int p15 = 0; p15 < inputs[15]; p15++) {
//                                                    						inputs[0][1][2][3][4][5][6][7][8][9][10][11][12][13][14] = 0;
//
//                                                    					}
//                                                					}
//                                            					}
//                                        					}	
//                                    					}
//                                					}	
//                            					}		
//                        					}	
//                    					}
//                					}
//                				}
//            				}
//                		}
//                	}
//                }
//            }
			//The number of inputs to the NN would be states + actions (5 states, 10 actions = 15) . 
			
			backPropFinal myNeuralNet = new backPropFinal(numInputs, numHidden, lRate, momentum);		/*Create new object of class "myBackProp */
			myNeuralNet.initializeWeights(upperThres, lowerThres);  							//Initialize weights to random weights between -0.5 and 0.5
			
			/* save data into files */
			File robotFile = new File ("robotFile.txt");

			PrintStream saveFile = null;
			PrintStream saveFileBipolar = null;
			PrintStream saveFileBipolarM = null;
			try {
				saveFile = new PrintStream( new FileOutputStream(robotFile));
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			/* Start epochs */  
			int numEpoch = 0; 
			while (stopError == false){ 
				double totalError = 0.0;
				for (int i = 0; i < numTrials; i++){
					//Call function for forward propagation
					double[] Ycalc = myNeuralNet.outputForward(inputs[i], flag, i);
					//Call function for backward propagation
					double error = myNeuralNet.train(inputs[i], outputs[i], Ycalc, flag, i);	
					totalError += error; 
				}
				if (totalError <= errorThreshold || numEpoch > maxEpoch){
					System.out.println("Trial " + a + "\tEpoch " + numEpoch + "\tError " + totalError);
					stopError = true;
				}				
				myNeuralNet.save (totalError, numEpoch, saveFile, stopError);
				numEpoch +=1;
			}			
		}
	}
}
