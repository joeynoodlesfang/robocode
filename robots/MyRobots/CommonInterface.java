package MyRobots;

import java.io.File; 
import java.io.IOException;
import java.io.PrintStream;

/** 
 * @date 20 June 2012
 * @author sarbjit (from EECE 592 Website) 
 * 
 */

public interface CommonInterface{
	
	/** Function "outputFor" 
	 * @param X - Input Vector. Array of doubles
	 * @return The value returned by the look-up table (LUT) or neural net (NN) for the input vector
	 */
	
	public double[] outputForward(String X, boolean flag, int numTrial); 
	 
	/** Function "Train" 
	 * This is a PUBLIC METHOD that takes in an array of type double named "X". 
	 * The method will tell the NN or the LUT the correct output value for an input. 
	 * @param X - the input vector (array of doubles)
	 * @param argValue - The new value to learn. 
	 * @return The error in the output for that input vector. 
	 */
	public double train(String X, double argValue, double[] Ycalc, boolean flag, int numTrial); 
	
	/** Function: "save"
	 * This is a PUBLIC METHOD that takes in an array of type double named "X" and the new value to learn. 
	 * Writes either a LUT or the weights of a NN to the file. 
	 * @param argFile of type File
	 */
	public void save(double error, int numTrial, PrintStream saveFile, boolean flag); 
	
}
