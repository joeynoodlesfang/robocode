package MyRobots;
import java.util.Arrays;

import robocode.*;

public class NeuralNet {
	NN1_DeadBunnyCrying tank;
	
	// you'd want to start all the nets during bot's run
	NeuralNet(NN1_DeadBunnyCrying _NN1) {
		this.tank = _NN1;
		/* 
		 * Params (array position):
		 * 0. net structure
		 * 1. no. inputs
		 * 2. no. hidden layers
		 * 3. no. outputs
		 * 
		 * 
		 * Extra Params...
		 * A. Hidden layer sizes:
		 * 0 to x. size of layer 1 (closest to input), layer 2, x+1.
		 * 
		 */
	}
	

	private void initNet() {
		
	}
	
	
    /**
     * @name:		netRun
     * @purpose: 	1. Cycle through all the possible actions via forward propagation with the current states, and calculate all Q values.
     * 				2. Find max Q value, determine action based on policy.
     * 				3. Perform Q function using the recorded previous Q value, and the just calculated the current Q value. Result is Q_prev_new
     * 				4. Readjust weights via backward propagation.
     * @param: 		many
     * @return: 	n
     */
    private void netRun (double[] currSAV, double[] prevSAV, double[] y, double[] Q_prev_new, //adjusted layer values
    					double[][] arr_wIH, double[][] arr_wHO, //weights
    					double[][] wIH_past, double[][] wIH_next, double[][] wHO_past, double[][] wHO_next, //backprop-momentum vars
    					double reward, boolean activationMethod){
    	
        double [][][] Q_NNFP_all = new double 				// list of generated Q values from currSAV-based FP
        		[input_action0_moveReferringToEnemy_possibilities]
        		[input_action1_fire_possibilities]
        		[input_action2_fireDirection_possibilities];
        double[] Q_curr = new double[numOutputsTotal];		// current cycle Q value generated from getMax
        double [] z_in = new double[numHiddensTotal]; 		// Array to store z[j] before being activate
    	double [] z    = new double[numHiddensTotal];		// Array to store values of z 
    	double [] y_in = new double[numOutputsTotal];		// Array to store Y[k] before being activated
    	//arrays in BP
    	double [][] vDelta = new double[numInputsTotal] [numHiddensTotal];	// Change in Input to Hidden weights
    	double [][] wDelta = new double[numHiddensTotal][numOutputsTotal]; 	// Change in Hidden to Output weights	  
    	double [] delta_out    = new double[numOutputsTotal];
    	double [] delta_hidden = new double[numHiddensTotal];
    	
    	
    	//TODO bookmark for main NN fxn.
    	getAllQsFromNet (Q_NNFP_all, currSAV, arr_wIH, arr_wHO, activationMethod);
        getMax			(Q_NNFP_all, currSAV, Q_curr, activationMethod); 
        qFunction		(Q_prev_new, y, reward, Q_curr);
        prepareBackProp	(prevSAV, z,
        					z_in, y_in,
        					arr_wIH, arr_wHO,
        					activationMethod);
        backProp		(prevSAV, z, y, Q_prev_new, 
        					z_in, y_in, 
        					delta_out, delta_hidden, vDelta, wDelta, 
        					arr_wIH, arr_wHO, 
        					activationMethod, 
        					wIH_past, wIH_next, wHO_past, wHO_next);
    }

    /** 
     * @name:		getAllQsFromNet
     * @input: 		currentStateVector 
     * @purpose: 	1. For current state, cycle through all possible actions and obtain all q-values (y), and stores in Q_NNFP_all.
     * 					With exception of the inputs and outputs, all NN structure parameters used are temporary parameters.
     * @param:		1. currSAV				aka currentStateActionVector or x, current state action vectors from environment.
     * 				2. Q_NNFP_all 			the Q values calculated from neural net forward propagation (aka y or output)
     * 				3. z					activated hidden layer
	 * 				4. Y					activated output layer, CURRENT (as opposed to past from Q fxn)
	 * 				5. z_in					pre-activation hidden layer
	 * 				6. y_in					pre-activation final layer
	 * 				7. arr_wIH				weights between input and hidden layer
	 * 				8. arr_wHO				weights between hidden and output layer
	 * 				9. activationMethod		binary (0 to 1) or bipolar (-1 to 1) activation function
     * @return: 	n
     */
	private void getAllQsFromNet(double [][][] Q_NNFP_all, double[] currSAV, double[][] arr_wIH, double[][] arr_wHO, boolean activationMethod) {
		
		double[] currSAV_temp = new double[numInputsTotal];
		double[] z_temp 	  = new double[numHiddensTotal];
		double[] y_temp 	  = new double[numOutputsTotal];
		double[] z_in_temp    = new double[numHiddensTotal];
		double[] y_in_temp    = new double[numOutputsTotal];

		if(bot.DEBUG_getAllQsFromNet || bot.DEBUG_MULTI_forwardProp || bot.DEBUG_ALL){
			bot.LOG[bot.lineCount++] = "- getAllQsFromNet:";
			bot.LOG[bot.lineCount++] = "currSAV:" + Arrays.toString(currSAV);
    	}
		
		System.arraycopy(currSAV, 3, currSAV_temp, 3, numStateContainers);
		
		if(bot.DEBUG_getAllQsFromNet || bot.DEBUG_MULTI_forwardProp || bot.DEBUG_ALL){
			bot.LOG[bot.lineCount++] = "currSAV_temp:" + Arrays.toString(currSAV_temp);
    	}
		
		for (int i_A0 = 0; i_A0 < input_action0_moveReferringToEnemy_possibilities; i_A0++){
			for (int i_A1 = 0; i_A1 < input_action1_fire_possibilities; i_A1++){
				for(int i_A2 = 0; i_A2 < input_action2_fireDirection_possibilities; i_A2++){
					currSAV_temp[0] = i_A0;
					currSAV_temp[1] = i_A1;
					currSAV_temp[2] = i_A2;
					forwardProp(currSAV_temp, z_temp, y_temp,
									z_in_temp, y_in_temp,
									arr_wIH, arr_wHO, 
									activationMethod);
					Q_NNFP_all[i_A0][i_A1][i_A2] = y_temp[0];
				}
			}
		}

    	if(bot.DEBUG_getAllQsFromNet || bot.DEBUG_MULTI_forwardProp || bot.DEBUG_ALL){
    		bot.LOG[bot.lineCount++] = "Q_NNFP_all going into getMax:" + Arrays.deepToString(Q_NNFP_all);
    		bot.LOG[bot.lineCount++] = "#eo getAllQsFromNet";
    	}
    	
    	return;
	}
	
	/** 
	 * @name:		forwardProp
	 * @brief: forward propagation done in accordance to pg294 in Fundamentals of Neural Network, by Laurene Fausett.
	 * 			Feedforward (step 3 to 5):
	 * 				step 3: Each input unit (x[i], i = 1, ..., n) receives input signal xi and broadcasts this signal to all units in the layer above (the hidden units).
	 * 				step 4: Each hidden unit (z[j], j = 1, ..., p) sums its weighted input signals,
	 * 								z_in[j] = v[0][j] + (sum of from i = 1 to n)x[i]v[i][j],                <- v = weights between input and hidden.
	 * 						applies its activation fxn to compute its output signal,
	 * 								z[j] = f(z_in[j]),
	 * 						and sends this signal to all units in the layer above (output units).
	 * 				step 5: Each output unit (Y[k], k = 1, ..., m) sums its weighted input signals, (treating k = 0 to start instead of 1 for now b/c no output)
	 * 								y_in[k] = w[0][k] + (sum of from j = 1 to p)z[j]w[j][k]                 <- w = weights between hidden and output.
	 * 						and applies its activation fxn to compute its output signal,
	 * 								Y[k] = f(y_in[k])
	 * @purpose: does forwardPropagation on the inputs from the robot. 
	 * @param: 		can find the same(except Q_NNFP_all) from getAllQsFromNet(), which invokes this fxn.
	 * @param:		1. x					input layer
     * 				2. z					activated hidden layer
	 * 				3. y					ALLactivated output layer, CURRENT (in contrast to PAST used for Qfxn)
	 * 				4. z_in					pre-activation hidden layer, ie: sum of inputs*weights
	 * 				5. y_in					pre-activation final layer, ie: sum of hidden*weights
	 * 				6. arr_wIH				weights between input and hidden layer
	 * 				7. arr_wHO				weights between hidden and output layer
	 * 				8. activationMethod		binary (0 to 1) or bipolar (-1 to 1) activation function
	 * @return: n. 
	 **/
    private void forwardProp(double[] x, double[] z, double[] y,
    							double[] z_in, double[] y_in,
    							double[][] arr_wIH, double[][] arr_wHO,
    							boolean activationMethod) {
    	if(bot.DEBUG_MULTI_forwardProp || bot.DEBUG_forwardProp || bot.DEBUG_ALL){
    		bot.LOG[bot.lineCount++] = "- FP:";
    		bot.LOG[bot.lineCount++] = "x:" + Arrays.toString(x);
    	}
    	
    	//step 3 and 4:    	
		for (int j = 1; j < numHiddensTotal; j++){ 		//p = numHiddensTotal
			double sumIn = 0.0;
			for (int i = 0; i < numInputsTotal; i++){	   //n = numInputsTotal
				sumIn += x[i]*arr_wIH[i][j]; //NO INPUT BIAS, that's why j = 1
			}
			z_in[j] = sumIn; 									//save z_in[0] for the bias hidden unit. 
			z_in[0] = valHiddenBias; 									//set z_in[0] = bias. HIDDEN BIAS = 1
			z[0] = z_in[0]; //can choose to optimize here by placing this outside of loop, since we know what valHiddenBias is.
			
			if (activationMethod == binaryMethod)
				z[j] = binaryActivation(z_in[j]); 				
			else
				z[j] = bipolarActivation(z_in[j]);
			
			if(bot.DEBUG_MULTI_forwardProp || bot.DEBUG_forwardProp || bot.DEBUG_ALL){
				bot.LOG[bot.lineCount++] = String.format("z[%d]:%.16f z_in[%d]:%.3f sumIn%.3f", j, z[j], j, z_in[j], sumIn);
			}
			
		}
		//step 5:
		for (int k = 0; k < numOutputsTotal; k++){
			double sumOut = 0.0; 
			for (int j= 0; j < numHiddensTotal; j++){
				sumOut += z[j]*arr_wHO[j][k]; 
			}
			y_in[k] = sumOut; 	
			
			if (activationMethod == binaryMethod)
				y[k] = binaryActivation(y_in[k]); 
			else
				y[k] = bipolarActivation(y_in[k]);
			
			if(bot.DEBUG_MULTI_forwardProp || bot.DEBUG_forwardProp || bot.DEBUG_ALL){
				bot.LOG[bot.lineCount++] = String.format("Y[%d]:%.16f y_in[%d]:%.3f sumOut%.3f", k, y[k], k, y_in[k], sumOut);
			}
			
		}
		return; 
	}
    
    /**
     * @name:		getMax()
     * @purpose: 	1. Obtain the action in current state with the highest q-value, 
     * 				   and its associated q-value. 
	 *					a. Start current max Q value at lower than obtainable value.
	 *					b. Cycle through all actions in current SAV, recording max q-values.
	 *						i. if indexQVal > QMax:
	 *							(1) Update QMax
	 *							(2) Set maxAction_totalNum = 1.
	 *							(3) Store the (now 3 dimension) action index into maxAction_all[maxAction_totalNum-1]
	 *						ii. if indexQVal == QMax:
	 *							(1) maxAction_totalNum++
	 *							(2) Store the (now 3 dimension) action index into maxAction_all[maxAction_totalNum-1]
	 *						iii. if indexQVal < QMax:
	 *							ignore.
	 *					c. record chosen action. If multiple actions with max q-values, randomize chosen action.
	 *						i. if maxAction_totalNum > 1, 
	 *						   randomly select between 0 and maxAction_totalNum - 1. The randomed 
	 *						   number will correspond to the array location of the chosen
	 *						   action in maxAction_all. 
	 *						ii. maxAction_policyBasedSelection = maxAction_all[randomed number]
	 *					d. record associated q-value.
     * @param: 		1.	Q_NNFP_all		array of q values for the forward propagations
     * 				2.	current SAV[]	current state action vectors
     * 				3.  Q_curr[0]		stores the maximum Q value //Joey: assuming there's only one possible max, for now (one output)
     * 				4.	activationMethod	binary/bipolar layer value normalization
     * @return: 	n
     */
    private void getMax(double[][][] Q_NNFP_all, double[] currSAV, double[] Q_curr, boolean activationMethod) {
    	//QMax stores the maximum Q found. starting at -100 allows the first one to be picked even if it's super negative. //Joey: apparently the reward system is so fucked that -1E99 can happen so, bug (May 31) don't we normalize Q val?
    	double QMax = -100.0;  
    	//total number of actions with the same value as the max Q.
        int maxAction_totalNum = 0;
        //used to generate a random number starting from 0 to maxAction_totalNum.
        int maxAction_arrIndex = 0;
        //this var stores the multi-dimensional actions into one container instead of multiple containers. Downstream functions require a linear action dimension.
        int forLoopsLinearized = 0;
        //stores the chosen action with maximum Q.
        int maxAction_policyBasedSelection = 0;
        //array for storing all actions with maxqval
        int [] maxAction_all = new int [numActions];
        //randomizes an action number. used for different policies.
        int randomVal = 0;
        
        
    	if(bot.DEBUG_MULTI_forwardProp || bot.DEBUG_getMax || bot.DEBUG_ALL) {
        	bot.LOG[bot.lineCount++] = "Q_NNFP_all:                  " + Arrays.deepToString(Q_NNFP_all);
        }
    	
    	// calculates all max values and stores multiple (really rare)
    	for (int i_A0 = 0; i_A0 < Q_NNFP_all.length; i_A0++){
		    for (int i_A1 = 0; i_A1 < Q_NNFP_all[0].length; i_A1++){
		    	for (int i_A2 = 0; i_A2 < Q_NNFP_all[0][0].length; i_A2++, forLoopsLinearized++){
		    		if (Q_NNFP_all[i_A0][i_A1][i_A2] > QMax){
		    			QMax = Q_NNFP_all[i_A0][i_A1][i_A2];
		            	maxAction_totalNum = 1;
		            	maxAction_all[maxAction_totalNum-1] = forLoopsLinearized;		
		            }
		            else if (Q_NNFP_all[i_A0][i_A1][i_A2] == QMax){
		            	maxAction_all[maxAction_totalNum++] = forLoopsLinearized;
		            }	            
		    	}
    		}
    	}
    	
    	//max Q value found
        Q_curr[0] = QMax;
        
    	if(bot.DEBUG_MULTI_forwardProp || bot.DEBUG_getMax || bot.DEBUG_ALL) {
        	bot.LOG[bot.lineCount++] = "maxAction_all:" + Arrays.toString(maxAction_all);
        	bot.LOG[bot.lineCount++] = "maxAction_totalNum: " + maxAction_totalNum;
        }
        
        if (maxAction_totalNum > 1) {
        	maxAction_arrIndex = (int)(Math.random()*(maxAction_totalNum)); //math.random randoms btwn 0.0 and 0.999. Allows selection array position from 0 to num-1 through int truncation. 
        	
        	if(bot.DEBUG_MULTI_forwardProp || bot.DEBUG_getMax || bot.DEBUG_ALL) {
            	bot.LOG[bot.lineCount++] = ">1 max vals, randomly chosen action " + maxAction_arrIndex;
            }
        }
        
        //Choosing next action based on policy. Greedy is default
        //exploratory uses this line to perform if-false actions.
        maxAction_policyBasedSelection = maxAction_all[maxAction_arrIndex]; //if maxAction_totalNum <= 1, maxAction_arrIndex = 0;
        
        
        //note: sarsa is currently not used. explained slightly further in comments in global final section near top of file.
        if (policy == SARSA || policy == exploratory) {
	    	randomVal = (int)(Math.random()*(numActions));
	        if (policy == SARSA) {
	        	maxAction_policyBasedSelection = randomVal;
	        }
	        else if(policy == exploratory) {
	        	maxAction_policyBasedSelection = (Math.random() > epsilon ? maxAction_policyBasedSelection : randomVal);
	        }
        }
	        
        if(bot.DEBUG_MULTI_forwardProp || bot.DEBUG_getMax || bot.DEBUG_ALL) {
        	bot.LOG[bot.lineCount++] = "enacting policy:" + policy + "(0=gre 1=exp 2=SAR)";
        	bot.LOG[bot.lineCount++] = String.format("Action Chosen (linear) %d", maxAction_policyBasedSelection);
        	bot.LOG[bot.lineCount++] = "lengths:" + Q_NNFP_all.length + Q_NNFP_all[0].length + Q_NNFP_all[0][0].length;
        }
        
        OUTERMOST: for (int i_A0 = 0; i_A0 < input_action0_moveReferringToEnemy_possibilities; i_A0++){
			for (int i_A1 = 0; i_A1 < input_action1_fire_possibilities; i_A1++){
				for(int i_A2 = 0; i_A2 < input_action2_fireDirection_possibilities; i_A2++){
		    		if (maxAction_policyBasedSelection < 1) {
		    			//currSAV glob var updated here
		    			currSAV[0] = i_A0; 
		    			currSAV[1] = i_A1;
		    			currSAV[2] = i_A2;
		    			
		    			break OUTERMOST;
		    		}
		    		maxAction_policyBasedSelection--;
		    	}
		    }
        }
        
		if(bot.DEBUG_MULTI_forwardProp || bot.DEBUG_getMax || bot.DEBUG_ALL) {
        	bot.LOG[bot.lineCount++] = "chosen actions(in containers):" + (int)currSAV[0] + " " + (int)currSAV[1] + " " + (int)currSAV[2];
        	bot.LOG[bot.lineCount++] = "with output: " + Q_curr[0];
        	bot.LOG[bot.lineCount++] = "#eo muxFP";
        }

        return;
    }
    
    /**
     * @name		qFunction
     * @purpose		1. Calculate the new prev q-value based on Qvalue function.
     * @param		1. Q_prev_new		aka Q_target, t. Records the corrected Qval.
     * 				2. Q_prev			aka y. The old corrected Qval.
     * 				3. reward			reward value - needs work. //joey: XD
     * 				4. Q_curr			Q value calculated during FP for current round. Used to correct Qval depending on its weight (gamma).
     * 				Utilizes following critical global vars directly:
     * 				1. gamma			describes weight of current Q value in calculation.
     * 				2. alpha			describes the extent to which the newly acquired information will override the old information.
     * @return		prevQVal
     */
    private void qFunction(double[] Q_prev_new, double[] Q_prev, double reward, double[] Q_curr){ //Joey: consider changing Q_prev into entire array.
    	
    	//Joey: ask andrea about papers for good gamma terms. (close to 1?)
    	
		Q_prev_new[0] = Q_prev[0] + alpha*(reward + (gamma*Q_curr[0]) - Q_prev[0]);
    	
    	//for bot.DEBUGging purposes: file recording Qval fluctuation
    	if (flag_recordQVals) {
    		arr_QVals[totalQValRecords++] = Q_curr[0];
    	}
    	if(bot.DEBUG_qFunction || bot.DEBUG_ALL) {
    		bot.LOG[bot.lineCount++] = "- qFunction:";
    		bot.LOG[bot.lineCount++] = String.format("Q_prev_new(t)%.3f  Q_prev(y):%.3f  Q_curr:%.3f", Q_prev_new[0], Q_prev[0], Q_curr[0]);
    		bot.LOG[bot.lineCount++] = String.format("alpha:%.2f reward:%.3f gamma:%.2f", alpha, reward, gamma);
    		bot.LOG[bot.lineCount++] = "#eo qFunction";
    	}
    }
 
    /** 
     * @name:		prepareBackProp
     * @purpose:	Populate NN parameters using previous SAV, in order to perform back propagation.
     * @param:		1. prevSAV		input to generate net - inputs
     * 				2. z			refreshes prev hidden layer
     * 				3. z_in			refreshes prev raw hidden layer
     * 				4. y_in			refreshes prev raw output layer
     * 				5. arr_wIH		input to generate net - IH weights
     * 				6. arr_wHO		input to generate net - HO weights
     * 				7. activationMethod 	binary/bipolar method of normalizing layers
     */
    private void prepareBackProp (double[] prevSAV, double[] z,
    								double[] z_in, double[] y_in,
    								double[][] arr_wIH, double[][] arr_wHO,
    								boolean activationMethod) {
    	
    	double[] y_temp = new double [numOutputsTotal];
    	
    	if(bot.DEBUG_MULTI_backProp || bot.DEBUG_prepareBackProp || bot.DEBUG_ALL) {
    		bot.LOG[bot.lineCount++] = "- prepareBackProp:";
    		bot.LOG[bot.lineCount++] = "start list";
    		bot.LOG[bot.lineCount++] = "prevSAV: " + Arrays.toString(prevSAV);
    		bot.LOG[bot.lineCount++] = "z: " + Arrays.toString(z);
    		bot.LOG[bot.lineCount++] = "z_in: " + Arrays.toString(z_in);
    		bot.LOG[bot.lineCount++] = "y_in:" + Arrays.toString(y_in);
    	}
    	
    	forwardProp(prevSAV, z, y_temp,
    				z_in, y_in,
    				arr_wIH, arr_wHO,
    				activationMethod);
    	
    	if(bot.DEBUG_MULTI_backProp || bot.DEBUG_prepareBackProp || bot.DEBUG_ALL) {
    		bot.LOG[bot.lineCount++] = "after list";
    		bot.LOG[bot.lineCount++] = "prevSAV: " + Arrays.toString(prevSAV);
    		bot.LOG[bot.lineCount++] = "z: " + Arrays.toString(z);
    		bot.LOG[bot.lineCount++] = "z_in: " + Arrays.toString(z_in);
    		bot.LOG[bot.lineCount++] = "y_in:" + Arrays.toString(y_in);
    		bot.LOG[bot.lineCount++] = "y_temp:" + Arrays.toString(y_temp); 
    		bot.LOG[bot.lineCount++] = "#eo prepareBackProp";
    	}
    }
    
    /**
     * @name:		backProp
     * @purpose:	Adjusts weights based on the difference between Q_prev_new and Q_prev.
     * @methodology:pg 295 in Fundamentals of Neural Networks by Lauren Fausett, Backpropagation of error: steps 6 to 8.
     * 				step 6:
     * 				Each output unit (Y[k], k = 1, ..., m) receives a target pattern corresponding to the input training pattern, computes its error information term,
     * 					delta_out[k] = (t[k] - y[k])f'(y_in[k]),
     * 				calculates its weight correction term (used to update w[j][k] later),
     * 					delta_weight_w[j][k] = alpha * delta[k] * z[j],
     * 				calculates its bias correction term (used to update w[0][k] later),
     * 					delta_weight_w[0][k] = alpha * delta[k],
     * 				and continue to use delta[k] for lower levels.
     *				
     *				step 7: 
     *				Each hidden unit (z[j], j = 1 ..., p) sums its delta inputs (from units in the layer above),
     *					delta_in[j] = (sum of from k = 1 to m)(delta[k] * w[j][k]),
     *				multiplies by the derivative of its activation fxn to calculate its error information term,
     *					delta[j] = delta_in[j] * f'(z_in[j]),
     *				calculates its weight correction term (used to update v[i][j] later),
     *					delta_weight_v[i][j] = alpha * delta[j] * x[i],
     *				and calculates its bias correction term (used to update v[0][j] later),
     *					delta_weight_v[0][j] = alpha * delta[j].
     *				
     *				step 8: Update weights and biases
     *				Each output unit (Y[k], k = 1, ..., m) updates its bias and weights (j = 0, ..., p):
     *					w[j][k](new) = w[j][k](old) + delta_weights_w[j][k].
     *				Each hidden unit (z[j], j = 1, ..., p) updates its bias and weights (i = 0, ..., n):
     *					v[i][j](new) = v[i][j](old) + delta_weights_v[i][j].
     *
     *				To assist with rate of convergence, we have also included the ability for the net to use momentum. Momentum requires data from one or more previous
     *				training patterns. In the simplest form, the weights at t+1 are based on the weights at t and t-1:
     *					w[j][k](t+1) = w[j][k](t) + alpha*delta_out[k]*z[j] + mu[w[j][k](t) - w[j][k](t-1)],
     *				and
     *					v[i][j](t+1) = v[i][j](t) + alpha*delta_in[j]*x[i] + mu[v[i][j](t) - v[j][k](t-1)].
     * @param:		BP variables:
     * 					1. x <- prevSAV
     * 					2. z <- previous cycle's hidden layer
     * 					3. y <- Q_prev: array of previous Q value from previous cycle.
     * 					4. t <- Q_prev_new: array of current calculated Q value from Q function.
     * 				Other general vars:
     * 					1. activationMethod (not global to reserve possibility of changing its value)
     * 				Momentum variables, which remembers past values:
     * 					1. vPast <- wIH_past 
     * 					2. vNext <- wIH_next
     * 					3. wPast <- wHO_past
     * 					4. wNext <- wHO_next
     * 					
     * @return:		n
     */
    private void backProp(double[] x, double[] z, double[] y, double[] t,
    						double[] z_in, double[] y_in, 
    						double[] delta_out, double[] delta_hidden, double[][] vDelta, double[][] wDelta, 
    						double[][] arr_wIH, double[][] arr_wHO, 
    						boolean activationMethod, 
    						double [][] vPast, double [][] vNext, double [][] wPast, double [][] wNext) {      
    	
    	//local var used to store activation derivative of y.
    	double[] temp_outputDerivative = new double [numOutputsTotal];
    	//local var stores raw output error - for bot.DEBUGging purposes.
    	double temp_outputErrorRaw = 0;
    	
    	if(bot.DEBUG_MULTI_backProp || bot.DEBUG_backProp || bot.DEBUG_ALL) {
			bot.LOG[bot.lineCount++] = "- BP";
			bot.LOG[bot.lineCount++] = "momentum:" + momentum;
		}
    	//Y_target is the variable calculated in QFunction to depict NN's converging(hopefully) approximation of the RL LUT.
 
        
    	//step 6-8 for hidden-to-output weights
        if(bot.DEBUG_MULTI_backProp || bot.DEBUG_backProp || bot.DEBUG_ALL) {
			bot.LOG[bot.lineCount++] = "@output cycle:";
			bot.LOG[bot.lineCount++] = "arr_wHO(pre):" + Arrays.deepToString(arr_wHO);
		}
        //step 6:
		for (int k = 0; k <numOutputsTotal; k++){ // m = numOutputsTotal. pretending output bias doesn't exist so our output vector starts at 0 (horrificallylazyXD)
			
			//delta_out[k] = (t[k] - y[k])f'(y_in[k])
			temp_outputErrorRaw = t[k] - y[k];
			
			if (activationMethod == binaryMethod){
				temp_outputDerivative[k] = binaryDerivative(y_in[k]);
				delta_out[k] = temp_outputErrorRaw*temp_outputDerivative[k]; 
			}
			else{
				temp_outputDerivative[k] = bipolarDerivative(y_in[k]);
				delta_out[k] = temp_outputErrorRaw*temp_outputDerivative[k];	
			}
			
			//misc data collections: calculating back propagation error for convergence calculation.
			if(flag_recordBPErrors) {
	        	arr_BPErrors[totalBPErrorsRecords++] = temp_outputErrorRaw; //thankfully, currently one output. Will need to correct code if more than error.
	        }
			
			if(bot.DEBUG_MULTI_backProp || bot.DEBUG_backProp || bot.DEBUG_ALL) {
				bot.LOG[bot.lineCount++] = String.format("delta_out[%d]:%.3f error_raw:%.8f (%s)", k, delta_out[k], temp_outputErrorRaw, (activationMethod==binaryMethod)?"bin":"bip");
				bot.LOG[bot.lineCount++] = String.format("t(target)[%d]:%.3f y(calc'd)[%d]:%.3f y_in[%d]:%.3f y_in_der[%d]:%.3f", k, t[k], k, y[k], k, y_in[k], k, temp_outputDerivative[k]);
			}
			
			//delta_weight_w[j][k] = alpha * delta[k] * z[j]
			for (int j = 0; j < numHiddensTotal; j++){
				wDelta[j][k] = alpha*delta_out[k]*z[j];
				
				if(bot.DEBUG_MULTI_backProp || bot.DEBUG_backProp || bot.DEBUG_ALL) {
					bot.LOG[bot.lineCount++] = String.format("wDelta[%d][%d]:%.3f wNext[%d][%d]:%.3f wPast[%d][%d]:%.3f", j, k, wDelta[j][k], j, k, wNext[j][k], j, k, wPast[j][k]);
				}
				
				//step 8: updating H-O weights using momentum
				wNext[j][k] = arr_wHO[j][k] + wDelta[j][k] + momentum*(arr_wHO[j][k] - wPast[j][k]); 
				wPast[j][k] = arr_wHO[j][k]; 
				arr_wHO[j][k] = wNext[j][k]; 
			}
		}
		
		if(bot.DEBUG_MULTI_backProp || bot.DEBUG_backProp || bot.DEBUG_ALL) {
			bot.LOG[bot.lineCount++] = "arr_wHO(post):" + Arrays.deepToString(arr_wHO);
		}
		
		//step 7:
		//for input-to-hidden layer
		
        if(bot.DEBUG_MULTI_backProp || bot.DEBUG_backProp || bot.DEBUG_ALL) { 
        	bot.LOG[bot.lineCount++] = "@i-to-h cycle:";
			bot.LOG[bot.lineCount++] = "arr_wIH(pre):" + Arrays.deepToString(arr_wIH);
		}
        
		for (int j = 0; j < numHiddensTotal; j++){
			double sumDeltaInputs = 0.0;
			for (int k = 0;  k < numOutputsTotal; k++){ //pretending output bias doesn't exist so our output vector starts at 0, when it should start at 1 if a slot is reserved for bias
				sumDeltaInputs += delta_out[k]*arr_wHO[j][k];
				if (activationMethod == binaryMethod){
					delta_hidden[j] = sumDeltaInputs*binaryDerivative(z_in[j]); 
				}
				else{
					delta_hidden[j] = sumDeltaInputs*bipolarDerivative(z_in[j]);	
				}
			}
			for (int i = 0; i< numInputsTotal; i++){ //because no input bias, i = 0 will be a wasted cycle (ah wellz)
				vDelta[i][j] = alpha*delta_hidden[j]*x[i];
				
				if(bot.DEBUG_MULTI_backProp || bot.DEBUG_backProp || bot.DEBUG_ALL) {
					bot.LOG[bot.lineCount++] = String.format("vDelta[%d][%d]:%.3f vNext[%d][%d]:%.3f vPast[%d][%d]:%.3f", i, j, vDelta[i][j], i, j, vNext[i][j], i, j, vPast[i][j]);
				}
				
				//step 8: updating I-H weights using momentum
				vNext[i][j] = arr_wIH[i][j] + vDelta[i][j] + momentum*(arr_wIH[i][j] - vPast[i][j]); //Joey: rest of this
				vPast[i][j] = arr_wIH[i][j]; 
				arr_wIH[i][j] = vNext[i][j]; 
			}
		}
		
        if(bot.DEBUG_MULTI_backProp || bot.DEBUG_backProp || bot.DEBUG_ALL) {
			bot.LOG[bot.lineCount++] = "arr_wIH(post):" + Arrays.deepToString(arr_wIH);
		}
        
//		
//		//Step 9 - Calculate local error. For debugging purposes; an additional way to measure if QVals is converging. //Joey: add flag to decide whether this is used.
//		double error = 0.0;
//		for (int k = 0; k < numOutputsTotal; k++){ 
//			error = 0.5*(java.lang.Math.pow((Y_target[k] - Y_calculated[k]), 2)); 
//		}
	}

}