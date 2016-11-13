package mainInterface;

import java.util.Arrays;

public class RoboLUT {
	// state-action pairs are stored in the look up table. 
	// LUT is sized NxM, where N is the number of states and M is the number of actions
	
	public void LUT(int argNumInputs, int [] argVariableFloor, int [] argVariableCeiling) {
	}
	
	
	public static void main(String args[]){
		int numStates = 3; 
		int numActions = 5; 
		double [][] LUT = new double [numStates][numActions]; 
		
		// first initialize LUT to zero. 
		for (int i = 0; i < numStates; i++){
			for (int j = 0; j < numActions; j++){
				LUT[i][j] = 0; 
			}
		}
//		System.out.println("LUT" + Arrays.deepToString(LUT));
		//for each action taken, call LUT again. 
		
		
		
		
		
		
	}
	

	
	
}
