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
public class backPropNoQuantization{
	public static void main(String args[])
	{
		for (int a = 0; a < 1; a ++){	 

			/*Initiate variables */
			int numInputs = 9; 		//6 states + 3 actions. 
			int numHidden = 5;			//number of hidden inputs
			int numTrials = 8641; 		//number of trials in the training set	
			double lRate = 0.05; 			//learning rate
			double momentum = 0.2;  	//value of momentum 
			boolean stopError = false; 	//if flag == false, then stop loop, else continue 
			int maxEpoch = 100; 	//if reach maximum number of Epochs, stop loop. 
			double upperThres = -0.5; 	//upper threshold for random weights
			double lowerThres = 0.5;	//lower threshold for random weights
			
			/* Neural net state action pair*/
			boolean flag = false;  
			double [] outputs = new double [8641]; 
			String [] inputsRaw = new String [8641];
			String [] inputs1 = new String [8640]; 
			String [] inputsACT = new String [24]; 
			ArrayList<String> inputToNN = new ArrayList<String>();
			
			/* define files to save data */
			File robotFile = new File ("robotFile.txt");
			File NNFile = new File ("savingInputs.txt"); 
			File saveWeights = new File ("finalWeights.txt"); 
			
//			PrintStream saveWeightFile = null;
			PrintStream saveFile1 = null;
			PrintStream saveFile2 = null; 
			try {
				saveFile1 = new PrintStream( new FileOutputStream(robotFile));
				saveFile2 = new PrintStream( new FileOutputStream(NNFile));
//				saveWeightFile = new PrintStream( new FileOutputStream(saveWeights));
				
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
					 while ( ( (line1 = readerLUT.readLine()) != null) && (countI < 8640) ) { 
						 //loop will run from 2nd line
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
					for (int i=0; i < 24; i++){
						inputsACT[i] = readerACT.readLine();
						inputsACT[i] = inputsACT[i].replace("\t", "");
					}				
//					System.out.println("inputsACT " + Arrays.toString(inputsACT));
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

			/* Obtaining just the first two digits to insert the correct action (instead of going from 0 - 23). 
			 * 
			 */
			String [] inputNNRe  = new String [inputToNN.size()];
			Integer [] inputAsInteger = new Integer[inputToNN.size()];
			Integer [] firstDigits = new Integer[inputToNN.size()];
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
			
			for (int i = 0; i <(updatedInputs.length); i++){
				if (firstDigits[i] == 0){
					updatedInputs[i] = inputsACT[0]+inputNNRe[i].substring(1);
				}
				else if (firstDigits[i] == 1){
					updatedInputs[i] = inputsACT[1]+inputNNRe[i].substring(1);
				
				}
				else if (firstDigits[i] == 2){
					updatedInputs[i] = inputsACT[2]+inputNNRe[i].substring(1);
				}
				else if (firstDigits[i] == 3){
					updatedInputs[i] = inputsACT[3]+inputNNRe[i].substring(1);
				}
				else if (firstDigits[i] == 4){
					updatedInputs[i] = inputsACT[4]+inputNNRe[i].substring(1);
				
				}
				else if (firstDigits[i] == 5){
					updatedInputs[i] = inputsACT[5]+inputNNRe[i].substring(1);
				}
				else if (firstDigits[i] == 6){
					updatedInputs[i] = inputsACT[6]+inputNNRe[i].substring(1);
				}
				else if (firstDigits[i] == 7){
					updatedInputs[i] = inputsACT[7]+inputNNRe[i].substring(1);
				}
				else if (firstDigits[i] == 8){
					updatedInputs[i] = inputsACT[8]+inputNNRe[i].substring(1);
				
				}
				else if (firstDigits[i] == 9){
					updatedInputs[i] = inputsACT[9]+inputNNRe[i].substring(1);
				}
				else if (firstDigits[i] == 10){
					updatedInputs[i] = inputsACT[10]+inputNNRe[i].substring(2);
				
				}
				else if (firstDigits[i] == 11){
					updatedInputs[i] = inputsACT[11]+inputNNRe[i].substring(2);
				}
				else if (firstDigits[i] == 12){
					updatedInputs[i] = inputsACT[12]+inputNNRe[i].substring(2);
				}
				else if (firstDigits[i] == 13){
					updatedInputs[i] = inputsACT[13]+inputNNRe[i].substring(2);
				
				}
				else if (firstDigits[i] == 14){
					updatedInputs[i] = inputsACT[14]+inputNNRe[i].substring(2);
				}
				else if (firstDigits[i] == 15){
					updatedInputs[i] = inputsACT[15]+inputNNRe[i].substring(2);
				}
				else if (firstDigits[i] == 16){
					updatedInputs[i] = inputsACT[16]+inputNNRe[i].substring(2);
				}
				else if (firstDigits[i] == 17){
					updatedInputs[i] = inputsACT[17]+inputNNRe[i].substring(2);
				}
				else if (firstDigits[i] == 18){
					updatedInputs[i] = inputsACT[18]+inputNNRe[i].substring(2);
				}
				else if (firstDigits[i] == 19){
					updatedInputs[i] = inputsACT[19]+inputNNRe[i].substring(2);
				
				}
				else if (firstDigits[i] == 20){
					updatedInputs[i] = inputsACT[20]+inputNNRe[i].substring(2);
				}
				else if (firstDigits[i] == 21){
					updatedInputs[i] = inputsACT[21]+inputNNRe[i].substring(2);
				}
				else if (firstDigits[i] == 22){
					updatedInputs[i] = inputsACT[22]+inputNNRe[i].substring(2);
				}
				else if (firstDigits[i] == 23){
					updatedInputs[i] = inputsACT[23]+inputNNRe[i].substring(2);
				}
			} 	
			
			/* normalize the outputs */
//			System.out.println("output " + Arrays.toString(outputs));
			double qMax = outputs[0];
			for (int i = 1; i < outputs.length; i++) {
			    if (outputs[i] > qMax) {
			      qMax = outputs[i];
			    }
			}
			double qMin = outputs[0];
			for (int i = 1; i < outputs.length; i++) {
			    if (outputs[i] < qMin) {
			    	qMin = outputs[i];
			    }
			}
			
			Double [] normalizedOutputs = new Double[outputs.length]; 
			for (int i = 0; i < outputs.length; i++){
				normalizedOutputs[i] = outputs[i]/qMax; 
			}			
//			System.out.println("normalizedOutputs " + Arrays.toString(normalizedOutputs));
			
			/*Get rid of dimentionality (does not mean add more states - just change the values of the current states
			 * 
			 * 
			 */
			ArrayList<String> inputWithNewActions = new ArrayList<String>();
			
			for(int i = 0; i < updatedInputs.length; i++) {
				char[] inputArray1 = new char [updatedInputs[i].length()];
			    for (int j = 0; j < updatedInputs[i].length(); j++){
			    	inputArray1[j] = updatedInputs[i].charAt(j); 
			    }
			    inputWithNewActions.add((String)Arrays.toString(inputArray1));  
			}
			
			String [] nnone = new String [inputWithNewActions.size()];
			String [] lastDigits = new String[inputWithNewActions.size()];
			
			//first must convert all "characters" to integers! 
			for (int i = 0; i < updatedInputs.length; i++){
				for (int j = 0; j < updatedInputs[i].length(); j++){
					nnone[i] = ((String) inputWithNewActions.get(i)).replaceAll("[^\\d-]", "");
				}
				if (nnone[i].length() == 13){
					lastDigits[i] = (nnone[i].substring(7, 13));
				}
				else if (nnone[i].length() == 12){
					lastDigits[i] = (nnone[i].substring(7, 12));
				}
			}
			System.out.println("input with new actions " + Arrays.toString(nnone));
			System.out.println("input with new actions " + Arrays.toString(lastDigits));
//			Integer [] asInteger = new Integer[lastDigits.length]; 
//			for (int i = 0; i < lastDigits.length; i++){
//				asInteger[i] = Integer.parseInt(lastDigits[i]); 
//			}
			for (int i = 0; i < lastDigits.length; i++){
				for (int j = 0; j < lastDigits[i].length(); j++){
					if (lastDigits[i].charAt(j) == '0'){
						System.out.println("char " + lastDigits[i].charAt(j));
					}
				}
			}
			
//				    	//state 1 now goes from 0 - 100
//				    	if (j == 7){
//				    		System.out.println("inputArray[j] " + updatedInputs[i].charAt(j));
////				    		
////				    			updatedInputs[i].charAt(j) = i; 
////				    		}
////				    		else if (updatedInputs[i].charAt(j) == 1){
////				    			
////				    		}	
////				    		else if (updatedInputs[i].charAt(j) == 2){
////				    			
////				    		}
////				    		else if (updatedInputs[i].charAt(j) == 3){
////				    			
////				    		}
//				    	}
//				    }
//				}
//				else if (updatedInputs[i].length() == 12){
//				    for (int j = 6; j < updatedInputs[i].length(); j++){
//				    	System.out.println("inputArray[j] " + updatedInputs[i].charAt(j));		    	
//				    }				
//				}
//			    System.out.println("\n");
//			}
			/* initialize myNeuralNet. 
			 * */
//		    backPropFinal myNeuralNet = new backPropFinal(numInputs, numHidden, lRate, momentum, qMax, qMin);		/*Create new object of class "myBackProp */
//			myNeuralNet.initializeWeights(upperThres, lowerThres);  							//Initialize weights to random weights between -0.5 and 0.5
//			
//			/* Start epochs */ 
//			
//			int numEpoch = 0; 
//			double error = 0.0; 
//			double rmsError = 0.0;
//			double allRMSError = 0.0; 
//			while (stopError == false){ 
//				double totalError = 0.0;
//				for (int i = 0; i < (numTrials-1); i++){
////					//Call function for forward propagation
//					double[] Ycalc = myNeuralNet.outputForward(updatedInputs[i], flag, i);
//					error = myNeuralNet.train(updatedInputs[i], normalizedOutputs[i], Ycalc, flag, i);	
//					totalError += error;
//					saveFile2.println("StateActionVector \t " + updatedInputs[i] + "\t error \t " + error);
//				}
//				if (numEpoch > maxEpoch){
////					System.out.println("Trial " + a + "\tEpoch " + numEpoch);
//					stopError = true;
//					saveFile2.close();	
//				}		
//				
////				rmsError =  sqrt(Etotal/2), where Etotal is 1/2*sum(xi-yi)^2 for i from 0 - maxTrials
//				rmsError = Math.sqrt(totalError/2);
////				System.out.println("rmsError " + rmsError);
//				myNeuralNet.save (rmsError, numEpoch, saveFile1, stopError);
//				numEpoch +=1;
//			}
//			System.out.println("numTrial " + numEpoch);
		}
	}
}