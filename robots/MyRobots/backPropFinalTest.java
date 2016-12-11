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

/* Main Testing Class */
public class backPropFinalTest{
	public static void main(String args[])
	{
		for (int a = 0; a < 1; a ++){	 
		    /*
			 * input data
			 */
			/*Initiate variables */
			int numInputs = 9; 		//6 states + 3 actions. 
			int numHidden = 5;			//number of hidden inputs
			int numTrials = 8641; 		//number of trials in the training set	
			double lRate = 0.002; 			//learning rate
			double errorThreshold = 0.005;	//error threshold
			double momentum = 0.4;  	//value of momentum 
			boolean stopError = false; 	//if flag == false, then stop loop, else continue 
			int maxEpoch = 1000; 	//if reach maximum number of Epochs, stop loop. 
			double upperThres = 0.5; 	//upper threshold for random weights
			double lowerThres = -0.5;	//lower threshold for random weights
			
			/* Neural net state action pair*/
			boolean flag = true;  
			double [] outputs = new double [8641]; 
			String [] inputsRaw = new String [8641];
			String [] inputs1 = new String [8640]; 
			String [] inputsACT = new String [8640]; 
			ArrayList<String> inputToNN = new ArrayList<String>();
			
			/* define files to save data */
			File robotFile = new File ("robotFile.txt");
			File NNFile = new File ("savingInputs.txt"); 
			File saveWeights = new File ("finalWeights.txt"); 
			PrintStream saveWeightFile = null;
			
			PrintStream saveFile1 = null;
			PrintStream saveFile2 = null; 
			try {
				saveFile1 = new PrintStream( new FileOutputStream(robotFile));
				saveFile2 = new PrintStream( new FileOutputStream(NNFile));
				saveWeightFile = new PrintStream( new FileOutputStream(saveWeights));
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}			
		    /* read the q-values from "LUTTrackfire.dat" and read the input state-action vector from "stateAction.dat"
		     */
		    BufferedReader readerLUT = null;
		    BufferedReader readerSAV = null;
		    BufferedReader readerACT = null;
		    try {
		    	String dir1 = "C:/Users/Andrea/github/robocode/robots/MyRobots/LUTTrackfire.data";
		    	String dir2 = "C:/Users/Andrea/github/robocode/robots/MyRobots/LUTTrackfire.data";
				readerLUT = new BufferedReader(new FileReader(dir1 + "/" + "LUTTrackfire.dat"));
				readerSAV = new BufferedReader(new FileReader(dir2 + "/" + "stateAction.dat"));
				readerACT = new BufferedReader(new FileReader("newActions.txt"));
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
					//store the updated actions into a "updatedA" block. 
					for (int i=0; i < 23; i++){
						inputsACT[i] = readerACT.readLine();
						inputs1[i] = inputsRaw[i].replace("\t", "");
					}				
					System.out.println("inputsACT " + Arrays.toString(inputsACT));
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
			
			//reformat the input as a string without '[], or space'
			String [] inputNNRe  = new String [inputToNN.size()];
			Integer [] inputAsInteger = new Integer[inputToNN.size()];
			Integer [] firstDigits = new Integer[inputToNN.size()];
			Integer [] asBinary  = new Integer[inputToNN.size()]; 
			String [] asString = new String[inputToNN.size()];
			String [] updatedInputs = new String[inputToNN.size()];

			for (int i = 0; i < inputToNN.size(); i++){
				inputNNRe[i] = ((String) inputToNN.get(i)).replaceAll("\\D+","");
				inputAsInteger[i] = Integer.parseInt(inputNNRe[i]);
				if (inputNNRe[i].length() == 7){
					firstDigits[i] = Integer.parseInt(inputNNRe[i].substring(0, 1));
				}
				else if (inputNNRe[i].length() == 8){
					firstDigits[i] = Integer.parseInt(inputNNRe[i].substring(0, 2));
				}
			}
			System.out.println("input " + Arrays.toString(firstDigits));
			
			Integer [] updatedArray = new Integer[3];
			//for the dimensionality reduction of the states instead of 0-24, goes 00000 --> 11000
			for (int i = 0; i < inputToNN.size(); i++){
				if (firstDigits[i] == 0){
					updatedArray[0] = 0; 
					updatedArray[1] = 0; 
					updatedArray[2] = 0; 
				} 
				else if (firstDigits[i] == 1){
					updatedArray[0] = 1; 
					updatedArray[1] = 0; 
					updatedArray[2] = 0; 
				} 
				else if (firstDigits[i] == 2){
					updatedArray[0] = 2; 
					updatedArray[1] = 0; 
					updatedArray[2] = 0; 
				} 
				else if (firstDigits[i] == 3){
					updatedArray[0] = 3; 
					updatedArray[1] = 0; 
					updatedArray[2] = 0; 
				} 
				else if (firstDigits[i] == 4){
					updatedArray[0] = 0; 
					updatedArray[1] = 1; 
					updatedArray[2] = 0; 
				} 
				else if (firstDigits[i] == 5){
					updatedArray[0] = 1; 
					updatedArray[1] = 1; 
					updatedArray[2] = 0; 
				} 
				else if (firstDigits[i] == 6){
					updatedArray[0] = 2; 
					updatedArray[1] = 1; 
					updatedArray[2] = 0; 
				} 
				else if (firstDigits[i] == 7){
					updatedArray[0] = 3; 
					updatedArray[1] = 1; 
					updatedArray[2] = 0; 
				} 
				else if (firstDigits[i] == 8){
					updatedArray[0] = 0; 
					updatedArray[1] = 0; 
					updatedArray[2] = 1; 
				}
				else if (firstDigits[i] == 9){
					updatedArray[0] = 1; 
					updatedArray[1] = 0; 
					updatedArray[2] = 1; 
				} 
				else if (firstDigits[i] == 10){
					updatedArray[0] = 2; 
					updatedArray[1] = 0; 
					updatedArray[2] = 1; 
				} 
				else if (firstDigits[i] == 11){
					updatedArray[0] = 3; 
					updatedArray[1] = 0; 
					updatedArray[2] = 1; 
				} 
				else if (firstDigits[i] == 12){
					updatedArray[0] = 0; 
					updatedArray[1] = 1; 
					updatedArray[2] = 1; 
				}
				else if (firstDigits[i] == 13){
					updatedArray[0] = 1; 
					updatedArray[1] = 1; 
					updatedArray[2] = 1; 
				} 
				else if (firstDigits[i] == 14){
					updatedArray[0] = 2; 
					updatedArray[1] = 1; 
					updatedArray[2] = 1; 
				} 
				else if (firstDigits[i] == 15){
					updatedArray[0] = 3; 
					updatedArray[1] = 1; 
					updatedArray[2] = 1; 
				} 
				else if (firstDigits[i] == 16){
					updatedArray[0] = 0; 
					updatedArray[1] = 0; 
					updatedArray[2] = 2; 
				}
				else if (firstDigits[i] == 17){
					updatedArray[0] = 1; 
					updatedArray[1] = 0; 
					updatedArray[2] = 2; 
				} 
				else if (firstDigits[i] == 18){
					updatedArray[0] = 2; 
					updatedArray[1] = 0; 
					updatedArray[2] = 2; 
				} 
				else if (firstDigits[i] == 19){
					updatedArray[0] = 3; 
					updatedArray[1] = 0; 
					updatedArray[2] = 2; 
				} 
				else if (firstDigits[i] == 20){
					updatedArray[0] = 0; 
					updatedArray[1] = 1; 
					updatedArray[2] = 2; 
				}
				else if (firstDigits[i] == 21){
					updatedArray[0] = 1; 
					updatedArray[1] = 1; 
					updatedArray[2] = 2; 
				} 
				else if (firstDigits[i] == 22){
					updatedArray[0] = 2; 
					updatedArray[1] = 1; 
					updatedArray[2] = 2; 
				} 
				else if (firstDigits[i] == 23){
					updatedArray[0] = 3; 
					updatedArray[1] = 1; 
					updatedArray[2] = 2; 
				} 
//				asBinary[i] = Integer.parseInt(Integer.toBinaryString(firstDigits[i])); 
//				asString[i] = asBinary[i].toString();
//				System.out.println(Arrays.toString(updatedArray));
//				System.out.println(updatedArray[0] + updatedArray[1]);
				if (inputNNRe[i].length() == 7){
//					
					updatedInputs[i] = (updatedArray)+inputNNRe[i].substring(1);
				}
//				else if (inputNNRe[i].length() == 8){
//					updatedInputs[i] = updatedArray[0] + updatedArray[1] + updatedArray[2]+inputNNRe[i].substring(2);
//				}
				
			}
			//not all the inputs are sized 11. 
//			System.out.println("input " + Arrays.toString(updatedInputs));
//			for (int i = 0; i < (updatedInputs.length); i++){
//				if (updatedInputs[i].length() < 11){
//					//add a zero to the beginning until string is length 11. 
//					int getLength = updatedInputs[i].length(); 
//					if (getLength == 7){
//						updatedInputs[i] = "0000"+updatedInputs[i].substring(0);
////						System.out.println("updatedInputs" + updatedInputs[i]); 
//					}
//					else if (getLength == 8){
//						updatedInputs[i] = "000"+updatedInputs[i].substring(0);
////						System.out.println("updatedInputs" + updatedInputs[i]); 
//					}
//					else if (getLength == 9){
//						updatedInputs[i] = "00"+updatedInputs[i].substring(0);
////						System.out.println("updatedInputs" + updatedInputs[i]); 
//					}
//					else if (getLength == 10){
//						updatedInputs[i] = "0"+updatedInputs[i].substring(0);
////						System.out.println("updatedInputs" + updatedInputs[i]); 
//					}
//				}
//				System.out.println("updatedInputs " + updatedInputs[i]); 	
//				saveFile2.println("StateActionVector \t " + updatedInputs[i]); 
//			}	
			saveFile2.close(); 
//			System.out.println("output " + Arrays.toString(outputs));
			/* Start epochs */  
//			int numEpoch = 0; 
//			double errorTemp = 0; 
//			while (stopError == false){ 
//				double totalError = 0.0;
////				numTrials = 4; 		//for testing
//				for (int i = 0; i < (numTrials-1); i++){
//					if (outputs[i] == 0.0){
//						continue;
//						
//					}
//					//Call function for forward propagation
////					System.out.println("input " + (updatedInputs[i]));
////					System.out.println("output " + (outputs[i]));
//					//should we eliminate the outputs that give only zero value? 
//					double[] Ycalc = myNeuralNet.outputForward(updatedInputs[i], flag, i);
////					System.out.println("YCalc " + Arrays.toString(Ycalc));
////					//Call function for backward propagation
////					System.out.println("outputs[i] " + outputs[i]);
////					System.out.println("YCalc " + inputNNRe[i]);
//					double error = myNeuralNet.train(updatedInputs[i], outputs[i], Ycalc, flag, i);	
////					System.out.println("error " + error); 
////					errorTemp = (java.lang.Math.pow(error, 2)); 
//					totalError += error;
//					saveFile2.println("StateActionVector \t " + updatedInputs[i] + "\t error \t " + error);
//					
//				}
//				saveFile2.close();	
//				//rmsError = sqrt(sumof(target - actual output)^2)/numPatterns)
////				System.out.println("error Temp " + errorTemp);
//				double rmsError = Math.sqrt(errorTemp)/numTrials; 
//////				System.out.println("totalError " + totalError);
//				System.out.println("numEpoch " + numEpoch);
//				if (numEpoch > maxEpoch){
//					System.out.println("Trial " + a + "\tEpoch " + numEpoch);
//					stopError = true;
//					
//				}		
////				System.out.println("rmsError " + rmsError);
//				myNeuralNet.save (rmsError, numEpoch, saveFile1, stopError);
//				numEpoch +=1;
//			}			
		}
	}
}
