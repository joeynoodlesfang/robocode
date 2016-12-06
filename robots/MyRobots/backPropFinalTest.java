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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/* Main Testing Class */
public class backPropFinalTest{
	public static void main(String args[])
	{
		for (int a = 0; a < 1; a ++){	 
		    /*
			 * input data
			 */
			/*Initiate variables */
			int numOutput = 1; 			//number of outputs per training set. 
			int numInputs = 7; 			//number of inputs for training set
			int numHidden = 5;			//number of hidden inputs
			int numTrials = 3; 
//			int numTrials = 8641; 			//number of trials in the training set
			double lRate = 0.02; 			//learning rate
//			double errorThreshold = 0.05;	//error threshold
			double momentum = 0.0;  	//value of momentum 
			boolean stopError = false; 	//if flag == false, then stop loop, else continue 
			int maxEpoch = 10000; 	//if reach maximum number of Epochs, stop loop. 
			double upperThres = 0.5; 	//upper threshold for random weights
			double lowerThres = -0.5;	//lower threshold for random weights
			
			/* Neural net state action pair*/
			boolean flag = true; 
			double [] outputs = new double [8641]; 
			String [] inputsRaw = new String [8641];
			String [] inputs1 = new String [8640];  
			ArrayList<String> inputToNN = new ArrayList<String>();
			
		    /* read the q-values from "LUTTrackfire.dat" and read the input state-action vector from "stateAction.dat"
		     */
		    BufferedReader readerLUT = null;
		    BufferedReader readerSAV = null;
		    try {
		    	String dir1 = "C:/Users/Andrea/github/robocode/robots/MyRobots/LUTTrackfire.data";
		    	String dir2 = "C:/Users/Andrea/github/robocode/robots/MyRobots/LUTTrackfire.data";
				readerLUT = new BufferedReader(new FileReader(dir1 + "/" + "LUTTrackfire.dat"));
				readerSAV = new BufferedReader(new FileReader(dir2 + "/" + "stateAction.dat"));

				try {
					//store each line into output block, skipping the first line, which was the counter. 
					 readerLUT.readLine(); // this will read the first line
					 String line1=null;
					 int countI = 0; 
					 while ( ( (line1 = readerLUT.readLine()) != null) && (countI < 8640) ) { //loop will run from 2nd line
						 outputs[countI] = Integer.parseInt(line1);
						 countI +=1; 
					 }
//					System.out.println("output " + Arrays.toString(outputs));					 
					//store each input vector into input block.
					for (int i=0; i < 8640; i++){
						inputsRaw[i] = readerSAV.readLine();
						inputs1[i] = inputsRaw[i].replace("\t", "");				//remove the tab from the line. 
					}
//					System.out.println("output " + Arrays.toString(inputs1));
					//can't convert directly to Integer because lose the leading zeros. 
					//place each string into an array? 
					for(int i = 0; i < inputs1.length; i++) {
						char[] inputArray = new char [inputs1[i].length()];
					    for (int j = 0; j < inputs1[i].length(); j++){
					    	inputArray[j] = inputs1[i].charAt(j); 
					    }
					    inputToNN.add((String)Arrays.toString(inputArray));  
					}
				}
				
				catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
	            finally {
	                if (readerLUT != null) {
	                    try {
							readerLUT.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                }
	                if (readerSAV != null) {
	                    try {
							readerLUT.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                }
	            }
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//		    System.out.println( "Whole list=" + inputToNN);
		    backPropFinal myNeuralNet = new backPropFinal(numInputs, numHidden, lRate, momentum);		/*Create new object of class "myBackProp */
			myNeuralNet.initializeWeights(upperThres, lowerThres);  							//Initialize weights to random weights between -0.5 and 0.5
			
			/* save data into files */
			File robotFile = new File ("robotFile.txt");

			PrintStream saveFile = null;
			try {
				saveFile = new PrintStream( new FileOutputStream(robotFile));
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			//reformat the input as a string without '[], or space'
			String [] inputNNRe  = new String [inputToNN.size()];
			for (int i = 0; i < inputToNN.size(); i++){
				inputNNRe[i] = ((String) inputToNN.get(i)).replaceAll("\\D+","");
			}
			System.out.println("input " + Arrays.deepToString(inputNNRe));
			
			/* Start epochs */  
			int numEpoch = 0; 
			maxEpoch = 3; 
////			double inputs[][] = {{1, 0, 0}, {1, 0, 1}, {1, 1, 0}, {1, 1, 1}}; 	// binary inputs
////			double outputs[]   = {0, 1, 1, 0}; 		  				// binary outputs
			while (stopError == false){ 
				double totalError = 0.0;
				for (int i = 0; i < (numTrials-1); i++){
					//Call function for forward propagation
//					System.out.println("input " + (inputNNRe[i]));
					double[] Ycalc = myNeuralNet.outputForward(inputNNRe[i], flag, i);
//					System.out.println("YCalc " + Arrays.toString(Ycalc));
//					//Call function for backward propagation
//					System.out.println("outputs[i] " + outputs[i]);
//					System.out.println("YCalc " + inputNNRe[i]);
//					double error = myNeuralNet.train(inputNNRe[i], outputs[i], Ycalc, flag, i);	
//					
//					totalError += error; 
				}
////				System.out.println("totalError " + totalError);
				if (numEpoch > maxEpoch){
					System.out.println("Trial " + a + "\tEpoch " + numEpoch);
					stopError = true;
				}				
				myNeuralNet.save (totalError, numEpoch, saveFile, stopError);
				numEpoch +=1;
			}			
		}
	}
		
}
