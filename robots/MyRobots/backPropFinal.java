package MyRobots;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

/** 
 * @date: October 17, 2016
 * @author: Andrea Marti (23208093)
 * @purpose: Implement the backpropagation class using the NeuralNetInterface (trial 3) 
 */

public class backPropFinal implements NeuralNetInterface{
	
	/**
	 * Constructor.
	 * @param argNumInputs The number of inputs in your input vector
	 * @param argNumHidden The number of hidden neurons in your hidden layer. Only a single hidden layer is supported
	 * @param argLearningRate The learning rate coefficient
	 * @param argMomentumTerm The momentum coefficient
	 */
	// define variables 
	int numOutput = 1; 			//number of outputs per training set. 
	int numInputs = 8; 			//number of inputs for training set
	int numHidden = 5;			//number of hidden inputs
	double mom = 0.0; 
	double alpha = 0.0; 
	double maxQ = 0.0; 
	double minQ = 0.0; 
	
	public backPropFinal (int numIn, int numHid, double lRate, double momentum, double qMax, double qMin)
	{
		// define variables 
		this.mom = momentum; 
		this.alpha = lRate;
		this.maxQ = qMax; 
		this.minQ = qMin; 
	}


	// initialize arrays 
	double [][] vPast 	= new double[numInputs][numHidden];			// Input to Hidden weights for Past.
	double [][] wPast 	= new double[numHidden][numOutput];    		// Hidden to Output weights for Past.
	double [][] vNow 	= new double[numInputs][numHidden];			// Input to Hidden weights.
	double [][] wNow	= new double[numHidden][numOutput]; 
	double [][] vNext	= new double[numInputs][numHidden];	
	double [][] wNext 	= new double[numHidden][numOutput];    		// Hidden to Output weights.
	double [][] deltaV = new double [numInputs][numHidden];		// Change in Input to Hidden weights
	double [][] deltaW = new double [numHidden][numOutput]; 	// Change in Hidden to Output weights
	double [] Z_in = new double[numHidden]; 		// Array to store Z[j] before being activate
	double [] Z    = new double[numHidden];		// Array to store values of Z 
	double [] Y_in = new double[numOutput];		// Array to store Y[k] before being activated
	double [] Y	   = new double[numOutput];		// Array to store values of Y  
	double [] delta_out = new double[numOutput];
	double [] delta_hidden = new double[numHidden];
	
	
 	public void initializeWeights(double upperThres, double lowerThres) { 	
 		//initialize hidden weights
// 		System.out.println("VArray " + Arrays.toString(vNow)); 
 		for (int i=0; i < numInputs; i++){	
			for (int j=1; j < numHidden; j++){
//				System.out.println("Vnow " + vNow[i][j]);
				double result = Math.random() *(upperThres - lowerThres) + lowerThres;
				vNow[i][j] = result;
				
			}
		}
		// initialize output weights
		for (int i=0; i < numHidden; i++){	
			for (int j=0; j < numOutput; j++){
				double result = Math.random() *(upperThres - lowerThres) + lowerThres;
				wNow[i][j] = result;
			}
		}	
 	}

	public double[] outputForward(String X, boolean flag, int numTrial) {
		
// 		System.out.println("VNow " + Arrays.deepToString(vNow));
// 		System.out.println("wNow " + Arrays.deepToString(wNow));
//		System.out.println("X " + X);
//		System.out.println(X.getClass().getName());
		
//		System.out.println("************************* " + numTrial + " ************************* ");
		for (int j = 1; j < numHidden; j++){
			double sumIn = 0.0; 
			//inner loop is to go through each X input
			for (int i= 0; i < numInputs; i++){	
//				System.out.println("Xi " + Character.getNumericValue(X.charAt(i)));
				sumIn += Character.getNumericValue(X.charAt(i))*vNow[i][j]; 
			}
			Z_in[j] = sumIn; 									//save z_in[0] for the bias hidden unit. 
			Z_in[0] = bias; 									//set z_in[0] = bias
			//apply activation function for output signal. 
			if (flag == true){
				Z[j] = binaryActivation(Z_in[j]); 
				Z[0] = Z_in[0];
			}
			else{
				Z[j] = bipolarActivation(Z_in[j]); 
				Z[0] = Z_in[0];
			}
		}
//		System.out.println("Z " + Arrays.deepToString(Z));
//		System.out.println("Z_in " + Arrays.toString(Z[numTrial]));

//		//Second: For each output unit from 1 to k, sums its weighted hidden signals: Y[0]*W[0][k] + Sum(Y[k]*W[j][k]) for j number of hidden units (starting at 1) 
//		System.out.println("Output Weight " + Arrays.deepToString(w)); 
//		System.out.println("Z " + Arrays.toString(Z));
		for (int k = 0; k < numOutput; k++){
			double sumOut = 0.0; 
//			//inner loop is to go through each X input
			for (int j= 0; j < numHidden; j++){	
				sumOut += Z[j]*wNow[j][k]; 
			}
			Y_in[k] = sumOut; 	
//			//apply activation function for output signal. 
//			Y[k] =  customActivation(Y_in[k],maxQ, minQ);
			if (flag == true)
				Y[k] = binaryActivation(Y_in[k]); 
			else
				Y[k] = bipolarActivation(Y_in[k]);				
		}		
//	System.out.println("Hidden Neuron Values " + Arrays.toString(Z));
//	System.out.println("final Y " + Arrays.toString(Y));	
//	System.out.println("z_in " + Arrays.toString(Z_in));
//	System.out.println("Y_in " + Arrays.toString(Y_in));
	return Y; 
	}

	public double train(String X, double Yreal, double[] Ycalc, boolean flag, int numTrial, boolean lastTrial, int numEpoch) {

//		System.out.println("z_in " + Arrays.toString(Z_in));
//		System.out.println("Y_in " + Arrays.toString(Y_in));	
		for (int k = 0; k < numOutput; k++){
//			delta_out[k]  =  (Yreal - Ycalc[k])*customActivationDerivation(Y_in[k],maxQ, minQ);
			if (flag == true){
				delta_out[k] = (Yreal - Ycalc[k])*binaryDerivative(Y_in[k]); 
			}
			else{
				delta_out[k] = (Yreal - Ycalc[k])*bipolarDerivative(Y_in[k]);	
			}
//			System.out.println("\n");
//			System.out.println("delta " + delta_out[k]);
			for (int j = 0; j < numHidden; j++){
//				System.out.println("wPast[j][k] " + wPast[j][k]);
//				System.out.println("wNow[j][k] " + wNow[j][k]);
				deltaW[j][k] = alpha*delta_out[k]*Z[j];
				wNext[j][k] = wNow[j][k] + deltaW[j][k] + mom*(wNow[j][k] - wPast[j][k]); 
				wPast[j][k] = wNow[j][k]; 
				wNow[j][k] = wNext[j][k]; 
//				System.out.println("wPast[j][k] " + wPast[j][k]);
//				System.out.println("wNow[j][k] " + wNow[j][k]);
//				System.out.println("wNext[j][k] " + wNext[j][k]);
			}
		}
		
		//for hidden layer
		for (int j = 0; j < numHidden; j++){
			double sumDeltaInputs = 0.0;
			for (int k = 0;  k < numOutput; k++){
				sumDeltaInputs += delta_out[k]*wNow[j][k];
				if (flag == true){
					 delta_hidden[j] = sumDeltaInputs*binaryDerivative(Z_in[j]); 
				}
				else{
					delta_hidden[j] = sumDeltaInputs*bipolarDerivative(Z_in[j]);	
				}
			}
			for (int i = 0; i< numInputs; i++){
//				System.out.println("vPast[i][j] " + vPast[i][j]);
//				System.out.println("vNow[i][j] " + vNow[i][j]);
				deltaV[i][j] = alpha*delta_hidden[j]*Character.getNumericValue(X.charAt(i));
				vNext[i][j]  = vNow[i][j] + deltaV[i][j] + mom*(vNow[i][j] - vPast[i][j]); 
				vPast[i][j] = vNow[i][j]; 
				vNow[i][j] = vNext[i][j]; 
//				System.out.println("vPast[i][j] " + vPast[i][j]);
//				System.out.println("vNow[i][j] " + vNow[i][j]);
//				System.out.println("vNext[i][j] " + vNext[i][j]);
			}
		}
		//Step 9 - Calculate local error. 
		double error = 0.0;
		for (int k = 0; k < numOutput; k++){ 
			error = 0.5*(java.lang.Math.pow((Yreal - Ycalc[k]), 2)); 
		}
//		System.out.println("last trial " + lastTrial);
		if (lastTrial == true){
			saveFile(numEpoch, vNow, wNow);
//			System.out.println("epoch number " + numEpoch); 

		}
		return error;
	}
	
	public void saveFile(int epochNum, double [][] hiddenWeights, double [][] outerWeights){
		File saveWeights = new File ("C:\\Users\\Andy\\github\\robocode\\robots\\MyRobots\\NN2_LUTMimic.data\\inToHiddenWeights_OfflineTraining.txt"); 
		File saveOutWeights = new File ("C:\\Users\\Andy\\github\\robocode\\robots\\MyRobots\\NN2_LUTMimic.data\\hiddenToOutWeights_OfflineTraining.txt"); 
//		File saveWeights = new File ("C:\\Users\\Andrea\\github\\robocode\\robots\\MyRobots\\NN2_LUTMimic.data\\inToHiddenWeights_OfflineTraining.txt"); 
//		File saveOutWeights = new File ("C:\\Users\\Andrea\\github\\robocode\\robots\\MyRobots\\NN2_LUTMimic.data\\hiddenToOutWeights_OfflineTraining.txt"); 
		PrintStream saveHiddenWeights = null;
		PrintStream saveOuterWeights = null;
		try {
			saveHiddenWeights = new PrintStream( new FileOutputStream(saveWeights));
			saveOuterWeights = new PrintStream( new FileOutputStream(saveOutWeights));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
//		System.out.println("final weights " + Arrays.deepToString(hiddenWeights));
//		System.out.println("final weights " + Arrays.deepToString(outerWeights));
		for (int i = 0; i < hiddenWeights.length; i++){
			for (int j = 1; j < hiddenWeights[i].length; j++){
				saveHiddenWeights.println(hiddenWeights[i][j]);
			}        
		}
		for (int i = 0; i < outerWeights.length; i++){
			for (int j = 0; j < outerWeights[i].length; j++){
				saveOuterWeights.println(outerWeights[i][j]);
			}
		}
//		saveWeightFile.println("Epoch\t " + epochNum + "\nhiddenWeights\t " + Arrays.deepToString(hiddenWeights)+ "\nouterWeights\t " + Arrays.deepToString(outerWeights));
		saveHiddenWeights.close(); 
		saveOuterWeights.close(); 
	}
	public void save (double totalError, int epochNum, PrintStream saveFile, boolean lastOne) {
		saveFile.println("Epoch\t " + epochNum + "\terror\t " + totalError);
		if (lastOne == true)
			saveFile.close();		
	}
	
	/*Function name: binaryActivation 
 	 * @param: current hidden value "z"
 	 * @return: new value evaluated at the f(x) = 1/(1 + Math.exp(-x)); 
 	*/ 
	
	public double customActivation(double x, double maxQ, double minQ){
		double activationGamma = maxQ-minQ; 
		double dell = -minQ; 
		double fX = 1/(1 + Math.exp(-x));
		double gX = activationGamma*fX - dell; 
		return gX; 
	}
	
	public double customActivationDerivation(double x, double maxQ, double minQ){
		double activationGamma = maxQ-minQ; 
		double dell = -minQ; 
		double gX = customActivation(x, maxQ, minQ);
		double newVal = (1/activationGamma)*(dell + gX)*(activationGamma - dell - gX);
		return newVal;  
	}
	
 	public double binaryActivation(double x) {
// 		System.out.println("BINARY ");
 		double newVal = 1/(1 + Math.exp(-x)); 
// 		System.out.println("binary " + newVal );
 		return newVal;
 	}
 	
 	/*Function name: bipolarActivation 
 	 * @param: current hidden value "z"
 	 * @return: new value evaluated at the f(x) = (2/(1 + e(-x))) - 1 
 	*/ 	
 	public double bipolarActivation(double x) {
 		double newVal = (2/(1 + Math.exp(-x)))-1; 
 		return newVal; 
 	}
 	/* Function name: binaryDerivative
 	 * @param: input to take the derivative of based on f'(x) = f(x)*(1-f(x)). 
 	 * @return: derivative of value. 
 	 * 
 	 */
 	public double binaryDerivative(double x) {
 		double binFunc = binaryActivation(x);
 		double binDeriv = binFunc*(1 - binFunc); 
 		return binDeriv;
 	}
 	/* Function name: bipolarDerivative
 	 * @param: input to take the derivative of. 
 	 * @return: derivative of value: f'(x) =  0.5*(1 + f(x))*(1 - f(x));
 	 * 
 	 */
 	public double bipolarDerivative(double x) {
 		double bipFunc = bipolarActivation(x);
 		double bipDeriv = 0.5*(1 + bipFunc)*(1 - bipFunc);  
 		return bipDeriv;
 	}


}