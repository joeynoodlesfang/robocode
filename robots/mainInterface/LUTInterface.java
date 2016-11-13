package mainInterface;

public interface LUTInterface extends CommonInterface{
	
/**
 * 
 * Constructor. (You will need to define one in your implementation)
 * @param argNumInputs The number of inputs in your input vector
 * @return 
 * @paramargVariableFloor An array specifying the lowest value of each variable in the input vector.
 * @paramargVariableCeiling An array specifying the highest value of each of the variables in the input vector.
 * The order must match the order as referred to in argVariableFloor. 
 * 
 * public LUT (
 * int argNumInputs,
 * int [] argVariableFloor,
 * int [] argVariableCeiling );
*/

	
/**
** Initialize the look up table to all zeros.  
**/
public void initialiseLUT(); 


/*
** Helper method to translate vector used to index look up table into an ordinal to access associated look up table element. 
** @param X - the state action vector used to index the LUT 
** @return  - the index where this vector maps to.  
*/
public int indexFor(double [] X); 

}