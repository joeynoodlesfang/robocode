package MyRobots;
/* @author: Andrea Marti 23208093
 * @date : October 26, 2016 
 * @class: EECE 592 
 * @purpose: Class "BackPropTest" is used to test the backPropagation XOR problem. 
*/

import java.io.BufferedReader;
/* Import headers */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

/* Main Testing Class */
public class backPropFinalTest{
	public static void main(String args[])
	{
		for (int a = 0; a < 1; a ++){	 
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
			double [] outputs = new double [8641]; 
		    /**
			 * input data
			 */
			final int num_actions = 24; 
		    final int myPositionDiscretized_states = 5;
		    final int myHeadingDiscretized_states = 4;
		    final int enemyDirection_states = 3; 							
		    final int enemyDistance_states = 3;					
		    final int enemyEnergy_states = 2;
		    // LUT table stored in memory.
		    int [][][][][][][] input
		        = new int
		        [num_actions]
		        [myPositionDiscretized_states]
		        [myHeadingDiscretized_states]		
		        [enemyEnergy_states]
		        [enemyDistance_states]
		        [enemyDirection_states]
		        [1];
		    
		    // Dimensions of LUT table, used for iterations.
		    int[] roboLUTDimensions = {
		        num_actions, 
		        myPositionDiscretized_states,
		        myHeadingDiscretized_states,
		        enemyEnergy_states,
		        enemyDistance_states,
		        enemyDirection_states,
		        1}; 
		    // Dimensions of LUT table, used for iterations.
   
		    BufferedReader reader = null;
		    try {
		    	String dir1 = "C:/Users/Andrea/github/robocode/robots/MyRobots/LUTTrackfire.data";
				reader = new BufferedReader(new FileReader(dir1 + "/" + "LUTTrackfire.dat"));
				try {
					for (int i=0; i < 8641; i++){
						outputs[i] = Integer.parseInt(reader.readLine()); 
					}
					//store each line into output block. 
					
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    System.out.println("output" + Arrays.toString(outputs)); 
		    
		    double [][] testArray = new double [3][4]; 
		    
		    for (int i = 0; i < 3; i++){
		    	
			    for (int j = 0; j < 4; j++){
			    	testArray[i][j] = j; 
			    }
		    }
		    System.out.println("testArray" + Arrays.deepToString(testArray)); 
		    
		    for (int p0 = 0; p0 < roboLUTDimensions[0]; p0++) {
                for (int p1 = 0; p1 < roboLUTDimensions[1]; p1++) {
                	for (int p2 = 0; p2 < roboLUTDimensions[2]; p2++) {
                		for (int p3 = 0; p3 < roboLUTDimensions[3]; p3++) {
                			for (int p4 = 0; p4 < roboLUTDimensions[4]; p4++) {
                				for (int p5 = 0; p5 < roboLUTDimensions[5]; p5++) {
                					for (int p6 = 0; p6 < roboLUTDimensions[6]; p6++) {
//                						input[[0]][][][][][][] = {p0, p1, p2, p3, p4, p5,p6};
                					}
                				}
                			}
                		}
                	}
                }
		    }
                
			/* For binary training set */						//if flag == true, then binary, if false, then bipolar
//			double inputs[][] = {{1, 0, 0}, {1, 0, 1}, {1, 1, 0}, {1, 1, 1}}; 	// binary inputs
//			double outputs[]   = {0, 1, 1, 0}; 		  				// binary outputs
////	
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
//					double[] Ycalc = myNeuralNet.outputForward(inputs[i], flag, i);
					//Call function for backward propagation
//					double error = myNeuralNet.train(inputs[i], outputs[i], Ycalc, flag, i);	
//					totalError += error; 
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
