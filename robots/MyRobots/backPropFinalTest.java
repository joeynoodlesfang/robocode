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

			/*Initiate variables */
			int numInputs = 8; 			//5 states + 3 actions. 
			int numHidden = 5;			//number of hidden inputs
			int numTrials = 8641; 		//number of trials in the training set	
			double lRate = 0.01; 			//learning rate
			double momentum = 0.2;  	//value of momentum 
			boolean stopError = false; 	//if flag == false, then stop loop, else continue 
			int maxEpoch = 3000; 	//if reach maximum number of Epochs, stop loop. 

			
			/* Neural net state action pair*/
			boolean flag = false;  
			double [] outputs = new double [8641]; 
			String [] inputsRaw = new String [8641];
			String [] inputs1 = new String [8640]; 
			String [] inputsACT = new String [24]; 
			ArrayList<String> inputToNN = new ArrayList<String>();
			
			/* define files to save data */
			File robotFile = new File ("robotFile.txt");

			PrintStream saveFile1 = null;
			try {
				saveFile1 = new PrintStream( new FileOutputStream(robotFile));

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}			
		    /* read the q-values from "LUTTrackfire.dat" and read the input state-action vector from "stateAction.dat"
		     */
		    BufferedReader readerLUT = null;
		    BufferedReader readerSAV = null;
		    BufferedReader readerACT = null;
		    try {
		    	String dir1 = "C:/Users/Andy/github/robocode/robots/MyRobots/LUTTrackfire.data";
//		    	String dir1 = "C:/Users/Andrea/github/robocode/robots/MyRobots/LUTTrackfire.data";
				readerLUT = new BufferedReader(new FileReader(dir1 + "/" + "LUTTrackfire.dat"));
				readerSAV = new BufferedReader(new FileReader(dir1 + "/" + "stateAction.dat"));
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
//		    System.out.println("outputs " + Arrays.toString(outputs));
			//reformat the input as a string without '[], or space'
			String [] inputNNRe  = new String [inputToNN.size()];
			Integer [] inputAsInteger = new Integer[inputToNN.size()];
			Integer [] firstDigits = new Integer[inputToNN.size()];
			String [] updatedInputs = new String[inputToNN.size()];
			
			for (int i = 0; i < inputToNN.size(); i++){
				inputNNRe[i] = ((String) inputToNN.get(i)).replaceAll("\\D+","");
				inputAsInteger[i] = Integer.parseInt(inputNNRe[i]);
				if (inputNNRe[i].length() == 6){
					firstDigits[i] = Integer.parseInt(inputNNRe[i].substring(0, 1));
				}
				else if (inputNNRe[i].length() == 7){
					firstDigits[i] = Integer.parseInt(inputNNRe[i].substring(0, 2));
				}
			}
//			System.out.println("firstDigits " + Arrays.toString(inputNNRe)); 
			for (int i = 0; i <(inputNNRe.length); i++){
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
			
			/* preprocess outputs 
			 * 
			 */
//			System.out.println("updatedInputs " + Arrays.toString(updatedInputs)); 
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
			/* normalize the outputs */
			Double [] normalizedOutputs = new Double[outputs.length]; 
			for (int i = 0; i < outputs.length; i++){
				normalizedOutputs[i] = outputs[i]/qMax; 
			}
			//obtain the max and min from normalized outputs and use them to initialize the random weights upper and lower thresholds
			double upperThres = 0.5; 	//upper threshold for random weights
			double lowerThres = -0.5;	//lower threshold for random weights
//			double upperThres = outputs[0];
//			for (int i = 1; i < outputs.length; i++) {
//			    if (outputs[i] > upperThres) {
//			    	upperThres = outputs[i];
//			    }
//			}
//			double lowerThres = outputs[0];
//			for (int i = 1; i < outputs.length; i++) {
//			    if (outputs[i] <lowerThres) {
//			    	lowerThres = outputs[i];
//			    }
//			}		
//			System.out.println("lowerThres " + lowerThres + "uppderThres " + upperThres); 
//			System.out.println("normalizedOutputs " + Arrays.toString(normalizedOutputs));
			/* initialize myNeuralNet. 
			 * */
		    backPropFinal myNeuralNet = new backPropFinal(numInputs, numHidden, lRate, momentum, qMax, qMin);		/*Create new object of class "myBackProp */
			myNeuralNet.initializeWeights(upperThres, lowerThres);  							//Initialize weights to random weights between -0.5 and 0.5
			
			/* Start epochs */ 
			int numEpoch = 0; 
			double error = 0.0; 
			double rmsError = 0.0;
			while (stopError == false){ 
				boolean finalTrial = false; 
				double totalError = 0.0;
				for (int i = 0; i < (numTrials-1); i++){
					//Call function for forward propagation
					double[] Ycalc = myNeuralNet.outputForward(updatedInputs[i], flag, i);
					if (i == (numTrials-2)){
						finalTrial = true; 
					}
					error = myNeuralNet.train(updatedInputs[i], normalizedOutputs[i], Ycalc, flag, i, finalTrial, numEpoch);	
//					System.out.println("error " + error);
					totalError += error;
				}
				if (numEpoch > maxEpoch){
//					System.out.println("Trial " + a + "\tEpoch " + numEpoch);
					stopError = true;
				}	
//				System.out.println("total error " + totalError);
				myNeuralNet.save (totalError, numEpoch, saveFile1, stopError);
				numEpoch +=1;
			}
			 
			//rmsError is the 1/k*(sum from 1 - k of the sqrt((y-t)^2))
//			rmsError =  sqrt(Etotal/2), where Etotal is 1/2*sum(xi-yi)^2 for i from 0 - maxTrials
//			rmsError = Math.sqrt(285.72906385145296/2);
//			System.out.println("rmsError " + rmsError);
//			myNeuralNet.save (rmsError, numEpoch, saveFile1, stopError);
//			System.out.println("numTrial " + numEpoch);
		}

		
	}
}
