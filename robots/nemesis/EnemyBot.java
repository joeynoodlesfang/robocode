package nemesis;

import robocode.*;
import java.io.*;

public class EnemyBot{
	
	public nemises tank;
	public boolean alive;
	public double x,y;
	public double energy;
	public double velocity;
	public double heading,p_heading,absBearing;
	public double update_t;
	public double distance;	
	//name of the enemy
	public String name;
	//one file per enemy to save data, opened once in a battle
	public PrintStream movesfile;
	
	//For the NN (Back propagation)
	//Learning constant 
	public static final double rho=.3;
	//number of inputs (MUST BE 2n)
	public static final int nIP=12;
	//number of neurons in hidden layer
	public static final int nHL=11;
	//number of neurons in output *FIXED*
	public static final int nOP=2;
	//the container for the inputs, fresh inputs are shifted in from the right
	public double[] cIP;
	//the weights for the inputs
	public double[][] wIP;
	//the output of the hidden layer
	public double[] ui;
	//the delta of the hidden layer
	public double[] delta;
	//the weights for the outputs
	public double[][] wOP;
	public double WMSE;
	
	
	//Q table for the RL algo
	//number of states
	public static final int nS=3;
	//number of actions
	public static final int nA=6;
	//greedy vs exploration
	public static double epsilon=.8;	
	//learning constant
	public static double gamma=.5;
	public double[][] Q={//-> type of gun/firepower (each column)
							{.7, .5, .5, .5, .5, .5 },
							{.5, .5, .5, 1.5, .5, .5},
							{.5, .5, .5, 1.5, .5, .5}
					//^Each row is a state: [0, (1-2), >=3] hit per 5 bullets fired		
						};	
	//Number of times we hit the enemy
	public int gothit=0;
	//Number of times we fired at it
	public int firedat=0;
	//Current state of the RL algo
	public int currentQ=0;
	public int previousQ=0;
	//the action we took in the last state
	public int action;
	
	//gets called during the static variable initialization of Scanner class
	public void reset(nemises the_tank){	
		name="NA";
		x=y=0;
		energy=100;
		velocity=8;
		absBearing=0;
		distance=1000000000;		
		alive=true;
		tank=the_tank;	
		cIP=  new double [nIP];  	ui=   new double [nHL];
		delta=new double [nHL];
		wIP=  new double [nIP][nHL];wOP=  new double [nHL][nOP];
		
		//Initialize the weights and the data containers
		for(int i_nL=0;i_nL<nHL;++i_nL){
			ui[i_nL]=0;      delta[i_nL]=0;
			for(int indx=0;indx<nIP;++indx){
				cIP[indx]=0; wIP[indx][i_nL]=.5;
			}
			for(int i_nOP=0;i_nOP<nOP;++i_nOP){
				wOP[i_nL][i_nOP]=Math.random();				
			}
		}
	}

	
	//First time we find a robot, we assign it a name and open a file to store the data
	public void init(String name){
		this.name=name;		
		try{
			movesfile = new PrintStream(new RobocodeFileOutputStream(tank.getDataFile(name + ".moves")));		
		}catch(IOException e){
			tank.out.println("OOPS: file opening error for" + name);
		}		
	}


	//function which updates the info for this enemy. Uses BP every time this
	//bot is scanned to update the weights to predict the next position
	public void update(ScannedRobotEvent e,nemises the_tank,boolean writetofile){
		
		double dx,dy;
		int tx,ty;		
		update_t=tank.getTime();
		//we have scanned this ..so it has to be on the battlefield
		alive=true;

		absBearing = (the_tank.getHeading() + e.getBearing());		
		//absolute bearing is angle to enemy wrt north from where we are currently
		if (absBearing < 0) absBearing += 360;
		absBearing%=360;		

		//hold on to the previous coordinates				
		dy=y;dx=x;
		
		//compute the present coordinate
		distance=e.getDistance();		
		y= Math.cos(Math.toRadians(absBearing))*distance + the_tank.getY();
		x= Math.sin(Math.toRadians(absBearing))*distance + the_tank.getX();		
		
		//change of origin (translation) to location of enemy bot in last update
		dy=y-dy;dx=x-dx;
		
		//change of origin (rotation) to the heading of the enemy bot in last update
		dx=(dx*Math.cos(Math.toRadians(90-heading)) + dy*Math.sin(Math.toRadians(90-heading)));
		dy=(-dx*Math.sin(Math.toRadians(90-heading))+ dy*Math.cos(Math.toRadians(90-heading)));		

		//squash to 3 states, -1,0,+1. The 8 combinations give the direction of motion wrt
		//to its previous location and heading
		if(dx>.2){tx=+1;}else if(dx<-.2){tx=-1;}else{tx=0;};
		if(dy>.2){ty=+1;}else if(dy<-.2){ty=-1;}else{ty=0;};
		
		//Update the NN with this output observation
		bplearn(tx,ty);
		
		//shift in the 2 inputs
		for(int i_IP=2;i_IP<nIP;i_IP+=2){
			cIP[i_IP-2]=cIP[i_IP];
			cIP[i_IP-1]=cIP[i_IP+1];
		}
		cIP[nIP-2]=tx;
		cIP[nIP-1]=ty;
		
				
		//compute the MSE save some data
		double[] pred=new double[nOP];
		pred=fwdprop();
		WMSE=0.8*WMSE+(tx-pred[0]*(tx-pred[0])+(ty-pred[1])*(ty-pred[1]));
		if(writetofile){
			//movesfile.println((int)tx+ " " +(int)ty+" " + pred[0]+" " +pred[1]+" " +tank.getTime());
			//movesfile.println((int)velocity +" "+(int)heading);
			//movesfile.println((int)tx+ " " +(int)ty);
			movesfile.println(WMSE);
		}
		

		//save current heading for next time
		p_heading=heading;
		heading=e.getHeading();
		//update the rest;
		velocity=e.getVelocity();
		energy=e.getEnergy();
	}
	
	
	//The BP learning algo
	public void bplearn(int x,int y){		
		double[] output   =new double[nOP];
		double[] delta_op =new double[nOP];
		
		//Get the NN's estimate for current data also note that this 
		//means ui's would already be computed and in place
		output=fwdprop();	
 		
		//Compute the delta at the output 
		delta_op[0]=(x-output[0])*output[0]*(1-output[0]);
		delta_op[1]=(y-output[1])*output[1]*(1-output[1]);

		//update the weights b/w hidden layer and outputs: wOP
		for(int i_HL=0;i_HL<nHL;i_HL++){
			//at the same time, compute the deltas for the hidden layer
			double sum_w_delta=0;			
			for(int i_OP=0;i_OP<nOP;i_OP++){
				//for use in delta computation 
				sum_w_delta+=wOP[i_HL][i_OP]*delta_op[i_OP];
				//weight update
				wOP[i_HL][i_OP]=wOP[i_HL][i_OP]+ rho*delta_op[i_OP]*ui[i_HL];				
			}
			//the delta at this hidden node
			delta[i_HL]=ui[i_HL]*(1-ui[i_HL])*sum_w_delta;
		}	
		
		//update the weigts b/w inputs and the hidden layer: wIP
		for(int i_IP=0;i_IP<nIP;i_IP++){
			for(int i_HL=0;i_HL<nHL;i_HL++){
				wIP[i_IP][i_HL]=wIP[i_IP][i_HL]+rho*delta[i_HL]*cIP[i_IP];
			}
		}		
	}//end of bplearn
	
	
	//The forward prop step in the NN, gets called during weight update 
	//and also during firing by the Gunner class
	public double[] fwdprop(){		
		double[] output=new double[nOP];		
		//find the activation at the output of hidden layer
		for(int i_HL=0;i_HL<nHL;i_HL++){
			ui[i_HL]=0;
			for(int i_IP=0;i_IP<nIP;i_IP++){
				ui[i_HL]+=wIP[i_IP][i_HL]*cIP[i_IP];				
			}
			ui[i_HL]=2/(1+Math.exp(-ui[i_HL]))-1;
		}		
		//find the output 
		for(int i_OP=0;i_OP<nOP;i_OP++){
			output[i_OP]=0;
			for(int i_HL=0;i_HL<nHL;i_HL++){
				output[i_OP]+=wOP[i_HL][i_OP]*ui[i_HL];				
			}
			output[i_OP]=2/(1+Math.exp(-output[i_OP]))-1;			
		}		
		return output;
	}
	
	//Pick an action based on the state asked for, called by the Gunner class
	//to decide on which gun to use for this enemy
	public int getAction(int state){
		
		if(Math.random()<=epsilon){
			//choose the best action (greedy)
			double max_value_action=0;
			for(int indx=0;indx<nA;++indx){
				if(max_value_action<Q[state][indx]){
					max_value_action=Q[state][indx];
					action=indx;
				}
			}		
		}else{
			//choose an exploratory action 
			return (int)Math.floor(Math.random()*nS);
		}		
		return action;
	}
	
	//Apply the observation from the evnironment into the RL system..
	public void applyRL(){
		if(firedat>=5){
			//process this only if we have fired 5 times at the enemy
			//reward is based on the number of hits we scored with these 5 shots
			double reward=(gothit-1)*2;
			
			double max_val_action=0;
			//decide on current state based on our scoring rate
			if(gothit<1)currentQ=0;
			if(1<=gothit && gothit<=2)currentQ=1;
			if(3<gothit)currentQ=2;
			
			//Find the best value at this state
			for(int indx=0;indx<nA;++indx){
				if(max_val_action<Q[currentQ][indx]){
					max_val_action=Q[currentQ][indx];					
				}
			}
			
			//RL learnring
			Q[previousQ][action]+=gamma*(reward+max_val_action-Q[previousQ][action]);
			
			previousQ=currentQ;
			firedat=0;gothit=0;
			
			tank.out.println(reward);
			for(int inds=0;inds<nS;++inds)
				tank.out.println(Q[inds][0]+" "+Q[inds][1]+" "+Q[inds][2]+" "+Q[inds][3]+" "+Q[inds][4]+" "+Q[inds][5]);
			tank.out.println("--------");
		}
		
	}
	
	
}
