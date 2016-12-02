package MyRobots;

import java.util.Arrays;

public class testingJava {

	public static void main(String[] args) {
	    final int[] action = new int [10]; 								
		final int enemyBearing = 360; 							// 360 degrees 
	    final int distanceX = 800; 								//800x600
	    final int distanceY = 600; 								//800x600
	    final int myEnergyState = 120;								//energy can be more than 100
	    final int enemyEnergyState = 120; 							//energy can be more than 100
	    
	    double [][][][][][][][][][][][][][][] inputs 
	        = new double
	        [action[0]]
	        [action[1]]
	        [action[2]]
	        [action[3]]
	        [action[4]]
	        [action[5]]
	        [action[6]]
	        [action[7]]
	        [action[8]]
	        [action[9]]
	        [enemyBearing]
	        [distanceX ]		
	        [distanceY]
	        [myEnergyState]
	        [enemyEnergyState];
	    
	    double [] index0 = {-1, 1}; 
	    double [] index1 = {-1, 1}; 
	    double [] index2 = {-1, 1};
	    double [] index3 = {-1, 1};
	    double [] index4 = {-1, 1}; 
	    double [] index5 = {-1, 1}; 
	    double [] index6 = {-1, 1};
	    double [] index7 = {-1, 1};
	    double [] index8 = {-1, 1}; 
	    double [] index9 = {-1, 1};
	    double [] index10 = new double[800]; 
	    for (int p11 = 0; p11 < 800; p11++){
	    	index10[p11] = p11; 
		}
	    int count = 0; 
	    for(int p1 = 0; p1 < 2; p1++){ 
		  	for(int p2 = 0; p2 < 2; p2++){
				for (int p3 = 0; p3 < 2; p3++) {
					for (int p4 = 0; p4 < 2; p4++) {
						for (int p5 = 0; p5 < 2; p5++) {
							for (int p6 = 0; p6 < 2; p6++) {
			  					for (int p7 = 0; p7 < 2; p7++) {
			      					for (int p8 = 0; p8 < 2; p8++) {
			          					for (int p9 = 0; p9 < 2; p9++) {
			              					for (int p10 = 0; p10 < 360; p10++) {
			                  					for (int p11 = 0; p11 < 800; p11++) {
			                      					for (int p12 = 0; p12 < 600; p12++) {
			                          					for (int p13 = 0; p13 < 120; p13++) {
			                              					for (int p14 = 0; p14 < 120; p14++) {
			                              						inputs[count][0] = index0[p0];
			                              						inputs[count][1] = 0;
			                              						inputs[count][2] = ops[z];
			                              		                count++;
			                              					}
			                          					}
			                      					}	
			                  					}
			              					}	
			          					}		
			      					}	
			  					}
							}
						}
					}	
				}
		  	}
	    }
//	    inputs[0][0][0][0][0][0][0][0][0][0][0][0][0][0][0] =  
//		String[][] allOps = new String[64][3];
//		String[] ops = new String[] {"+","-","*","/"};
//	    int count = 0;
//	    for (int x = 0; x < 4; x++) {
//	        for (int y = 0; y < 4; y++) {
//	            for (int z = 0; z < 4; z++) {
//	                allOps[count][0] = ops[x];
//	                allOps[count][1] = ops[y];
//	                allOps[count][2] = ops[z];
//	                count++;
//	            }
//	        }
//	    }	    

	     
//	      		}
//	      	}
//	      }
//  }

//	    System.out.println(Arrays.deepToString(allOps));

	}

}
