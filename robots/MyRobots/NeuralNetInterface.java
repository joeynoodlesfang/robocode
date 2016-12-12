package MyRobots; 

/** 
 * @date 20 June 2012
 * @author sarbjit (from EECE 592 Website) 
 * NeuralNetInterface is a subclass of CommonInterface, so it  inherits all the fields and methods of CommonInterface
 * and adds new fields and methods. 
 * The interface declaration consists of modifiers, the keyword interface, 
 * the interface name, a comma-separated list of parent interfaces (if any), and the interface body
 */
//
public interface NeuralNetInterface extends CommonInterface{
	
	// constant declarations
	final double bias = 1.0; 				//input for each neurons bias weight, "final" means that it always contains the same value
	
	// abstract method signatures
	public double binaryActivation(double x, double sigma); 
	/*Function name: binaryActivation 
	 * @param: current hidden value "z"
	 * @return: new value evaluated at the f(x) = 1/(1 + Math.exp(-x)); 
	*/ 
	public double bipolarActivation(double x, double sigma); 
	/*Function name: bipolarActivation 
	 * @param: current hidden value "z"
	 * @return: new value evaluated at the f(x) = (2/(1 + e(-x))) - 1 
	*/ 	
	public double binaryDerivative(double x, double sigma); 
	/* Function name: binaryDerivative
	 * @param: input to take the derivative of based on f'(x) = f(x)*(1-f(x)). 
	 * @return: derivative of value. 
	 * 
	 */
	public double bipolarDerivative(double x, double sigma); 
	/* Function name: binaryDerivative
	 * @param: input to take the derivative of. 
	 * @return: derivative of value: f'(x) =  0.5*(1 + f(x))*(1 - f(x));
	 * 
	 */
	public void initializeWeights(double upperThres, double lowerThres); 
}
//End of the public interface NeuralNetInterface