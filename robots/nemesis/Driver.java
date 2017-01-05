package nemesis;
import robocode.*;

public class Driver{
	
	nemises tank;
	//to keep track of how often we were hit
	double hitrecently;
	//battle field dimensions
	double W,H;
	
	//to control our drunken walk..
	static int turn_direction = 0;
	static int turn_times = 0;
	//flag to indicate wall collision avoidance in progress
	boolean avoidingwalls=false;
	
	//the constructor called only once
	Driver(nemises the_tank){
		//from now on, we'll use our variable 'tank'
		this.tank=the_tank;				
	}
	
	//initialise the variables, could not be done in constructor since the 'run'
	//method has to be called..which can happen only after construction of object
	public void init(){
		W=tank.getBattleFieldWidth();
		H=tank.getBattleFieldHeight();
	}
	
	//Main function to move the bot. Checks for proximity of walls first and then
	//goes into a drunken walk across the battle field.
	public void drive(){		
			
		if(nearwalls()){
			//tank.out.println("Wall avoidance kicked in...");
		}else{
			if(tank.getDistanceRemaining()<50){						
				if((Math.random()*15)<1){
					//run straight occasionally for a longer distance				
					tank.setAhead(300);				
				}else{
					tank.setAhead(220);
				}
			}
			//the drunkeness...
			if(tank.getTurnRemaining()==0){				
				//turn around once a while to throw off any NN using
				//our heading to target us or predict our motion the
				//time spent in turning in place will also act as a 'stop'
				if((Math.random()*20)<1){
					tank.setTurnLeft(180);
					tank.out.println("Spinning..");					
				}else{
					if ((turn_direction % 2)==0) {
						tank.setTurnRight(50);						
					} else {
						tank.setTurnLeft(50);						
					}				
					++turn_direction;
				}
			}
		}
	}//end of drive

	//On hitting the wall ..'reflect' offit..
	public void hitwall(HitWallEvent w){
		//this event should ideally not happen...
		//tank.out.println("OMG ..I hit the wall...");
		//if it does..reflect off it..
		tank.setTurnLeft(2*w.getBearing());
		tank.setAhead(100);
		avoidingwalls=true;
	}
	
	//if we get hit by a bullet twice in a short span ..make a headlong
	//rush out of the fire area
	public void hitbybullet(HitByBulletEvent e){		
		//if we are hit twice within a short time
		double absangle=e.getBearing();
		if(absangle<0)absangle+=360;
		if((tank.getTime()-hitrecently)<500){			
			//run like hell...hope to throw off the targetting
			//randomly turn to left or right
			if(Math.random()>.5){
				tank.setAhead(150);
			}else{
				tank.setBack(150);
			}
		}		
		hitrecently=tank.getTime();
		return;		
	}
	
	//check for proximity with the walls ...turn the other way if we are close
	public boolean nearwalls(){		
		double x=tank.getX();
		double y=tank.getY();		
		
		if(avoidingwalls==true){
			if(tank.getDistanceRemaining()==0){
				avoidingwalls=false;
				return false;
			}else{
				return true;
			}
		}
		
		if(x<100){
			turnto(60);			tank.setAhead(50);			avoidingwalls=true;
			return true;
		}
		if(x>W-100){
			turnto(-60);		tank.setAhead(50);			avoidingwalls=true;
			return true;
		}
		if(y<100){
			turnto(20);			tank.setAhead(50);			avoidingwalls=true;
			return true;
		}
		if(y>H-100){
			turnto(150);		tank.setAhead(50);			avoidingwalls=true;
			return true;
		}
		avoidingwalls=false;
		return false;
	}
	
	
	//turn to given heading thru left or right for min turn 
	public void turnto(double heading){
		double angle=(tank.getHeading()-heading)%360;		
		if(0<=angle && angle<180)tank.setTurnLeft(angle);
		if(180<=angle && angle<=360)tank.setTurnRight(angle);
	}
	
}
