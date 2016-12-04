package MyRobots;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

/** 
 * @date: October 17, 2016
 * @author: Andrea Marti (23208093)
 * @purpose: Implement the backpropagation class using the NeuralNetInterface (trial 3) 
 */

public class backPropFinal implements NeuralNetInterface {
	
	/**
	 * Constructor.
	 * @param argNumInputs The number of inputs in your input vector
	 * @param argNumHidden The number of hidden neurons in your hidden layer. Only a single hidden layer is supported
	 * @param argLearningRate The learning rate coefficient
	 * @param argMomentumTerm The momentum coefficient
	 */
	double mom = 0.0; 
	double alpha = 0.0; 

	public backPropFinal (int numIn, int numHid, double lRate, double momentum)
	{
		// define variables 
		this.mom = momentum; 
		this.alpha = lRate;	
	}
	// define variables 
	int numInputs = 3; 								//two X and 1 bias
	int numHidden = 5; 								//four hidden and 1 bias
	int numOutput = 1; 								//1 Y
	int numTrials = 4; 								//four test sets
	
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
// 	 	  Weights from the textbook 		
		//vNow[0][j] = weights from bias input to each hidden neuron 
//		vNow[0][0] = 0.0; 
// 		vNow[0][1] = -0.3378; 
//		vNow[0][2] =  0.2771; 
//		vNow[0][3] = 0.2859;
//		vNow[0][4] = -0.3329; 
//				
//	// 	 			//vNow[1][j] = weight from first input to each hidden neuron 
//		vNow[1][0] = 0.0; 
//		vNow[1][1] = 0.1970; 
//		vNow[1][2] =  0.3191; 
//		vNow[1][3] = -0.1448;
//		vNow[1][4] = 0.3594;		
//	 	
//	// 	 			//vNow[2][j] = weight from second input to each hidden neuron
//		vNow[2][0] = 0.0; 
//		vNow[2][1] = 0.3099;  			
//		vNow[2][2] = 0.1904; 
//		vNow[2][3] = -0.0347; 
//		vNow[2][4] = -0.4861; 			
//		
//	// 	 			//weights to the output unit. wNow[j][k], where [j] is number of hidden neurons and [k] is number of outputs
//		wNow[0][0] = -0.1401; 
//		wNow[1][0] = 0.4919; 			
//		wNow[2][0] = -0.2913; 
//		wNow[3][0] = -0.3979; 
//		wNow[4][0] = 0.3581; 
 	
//	 		//initialize hidden weights
	 		for (int i=0; i < numInputs; i++){	
//	 			System.out.println("I " + i);
				for (int j=1; j < numHidden; j++){
//					System.out.println("J " + j);
					double result = Math.random() *(upperThres - lowerThres) + lowerThres;
					vNow[i][j] = result;
				}
			}
			// initialize output weights
			for (int i=0; i < numHidden; i++){	
//				System.out.println("I " + i);
				for (int j=0; j < numOutput; j++){
//					System.out.println("J " + j);
					double result = Math.random() *(upperThres - lowerThres) + lowerThres;
					wNow[i][j] = result;
				}
			}	
 	
 	}

	@Override
	public double[] outputForward(double[] X, boolean flag, int numTrial) {
// 		System.out.println("VNow " + Arrays.deepToString(vNow));
// 		System.out.println("wNow " + Arrays.deepToString(wNow));
		for (int j = 1; j < numHidden; j++){
			double sumIn = 0.0; 
			//inner loop is to go through each X input
			for (int i= 0; i < numInputs; i++){	
				sumIn += X[i]*vNow[i][j]; 
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

	@Override
	public double train(double[] X, double Yreal, double[] Ycalc, boolean flag, int numTrial) {
//		System.out.println("z_in " + Arrays.toString(Z_in));
//		System.out.println("Y_in " + Arrays.toString(Y_in));	
		for (int k = 0; k < numOutput; k++){
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
				deltaV[i][j] = alpha*delta_hidden[j]*X[i];
				vNext[i][j]  = vNow[i][j] + deltaV[i][j] + mom*(vNow[i][j] - vPast[i][j]); 
				vPast[i][j] = vNow[i][j]; 
				vNow[i][j] = vNext[i][j]; 
//				System.out.println("vPast[i][j] " + vPast[i][j]);
//				System.out.println("vNow[i][j] " + vNow[i][j]);
//				System.out.println("vNext[i][j] " + vNext[i][j]);
			}
		}
//		//Step 9 - Calculate local error. 
		double error = 0.0;
		for (int k = 0; k < numOutput; k++){ 
			error = 0.5*(java.lang.Math.pow((Yreal - Ycalc[k]), 2)); 
		}
//		System.out.println("error " + error);
		return error;
	}

	@Override
	public void save(double error, int numTrial, PrintStream saveFile, boolean flag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void load(String argFileName) throws IOException {
		// TODO Auto-generated method stub
		
	}
	/*Function name: binaryActivation 
 	 * @param: current hidden value "z"
 	 * @return: new value evaluated at the f(x) = 1/(1 + Math.exp(-x)); 
 	*/ 
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