package nemesis;

import robocode.*;
import java.awt.geom.*;

public class Gunner{
	nemises tank;	
	
	int spray_fire=0;
	//the index to the current target
	int target;
	
	Gunner(nemises the_tank){
		this.tank=the_tank;		
		return;		
	}
	
	public void shoot(){		
	
		if(spray_fire!=0){
			//the spray gun hasnt quite done with its job..
			fire_spray_gun(target,.5);
			return;
		}
		
		target=get_Target();
		
		//Will actually run the algo only if 5 bullets have been already fired at this target
		Scanner.enemies[target].applyRL();		
		
		//Decide on which action to take in this state
		switch(Scanner.enemies[target].getAction(Scanner.enemies[target].currentQ)){
		//switch(9){//toskip RL and use a single gun
		//take the action
		case 0:
			fire_nn_gun(target,.5);
			//tank.out.println("NN gun at .5");
			break;
		case 1:
			fire_nn_gun(target,1);
			//tank.out.println("NN gun at 1");
			break;
		case 2:
			fire_nn_gun(target,2);
			//tank.out.println("NN gun at 2");
			break;
		case 3:
			fire_nn_gun(target,3);		
			//tank.out.println("NN gun at 3");
			break;
		case 4:
			spray_fire=3;
			fire_spray_gun(target,1);
			//tank.out.println("S gun at 1x3");
			break;
		case 5:
			spray_fire=3;
			fire_spray_gun(target,.5);
			//tank.out.println("S gun at .5x3");
		}		
		//update the count of how many times we have fired at this bot
		Scanner.enemies[target].firedat++;
		
		//for comparision
		//fire_linear_gun(target,3);
		//fire_point_gun(target,3);
	}
	
	
	//Use the weights in the EnemyBot class object for the target to 
	//predict and general heading of the enemy, compute the probably 
	//position at impact, turn gun ..and fire
	public void fire_nn_gun(int target,double firepower){
		double[] pred_dxy=Scanner.enemies[target].fwdprop();
		double dx=pred_dxy[0];
		double dy=pred_dxy[1];
		//rotate coordinates back to normal
		dx=(dx*Math.cos(Math.toRadians(-Scanner.enemies[target].heading)) + dy*Math.sin(Math.toRadians(-Scanner.enemies[target].heading)));
		dy=(-dx*Math.sin(Math.toRadians(-Scanner.enemies[target].heading))+ dy*Math.cos(Math.toRadians(-Scanner.enemies[target].heading)));
		
		//account for the velocity and the time diff b/w scan and fire
		dx*=Scanner.enemies[target].velocity*(tank.getTime()-Scanner.enemies[target].update_t+1);
		//take the component along x axis to get x displacement
		dx*=Math.sin(Math.toRadians(Scanner.enemies[target].heading));
		dy*=Scanner.enemies[target].velocity*(tank.getTime()-Scanner.enemies[target].update_t+1);
		//component along y axis
		dy*=Math.cos(Math.toRadians(Scanner.enemies[target].heading));
		
		turnGunto(absoluteBearing(tank.getX(),tank.getY(),Scanner.enemies[target].x+dx,Scanner.enemies[target].y+dy));
				
		if((tank.getGunTurnRemaining()<1) && (tank.getGunHeat()==0)){			
			if(Scanner.enemies[target].alive && tank.getEnergy()>5)tank.fire(firepower);			
		}		
	}
	
	
	//Spray the previous location and around it with low power bullets
	public void fire_spray_gun(int target, double firepower){
		if(tank.getGunHeat()==0&&spray_fire==3){
			turnGunto(absoluteBearing(tank.getX(),tank.getY(),Scanner.enemies[target].x,Scanner.enemies[target].y));
			if(Scanner.enemies[target].alive && tank.getEnergy()>5){
				tank.fire(firepower);
				spray_fire--;				
			}
		}
		if(tank.getGunHeat()==0&&spray_fire==2){
			turnGunto(absoluteBearing(tank.getX(),tank.getY(),Scanner.enemies[target].x+60,Scanner.enemies[target].y));
			if(Scanner.enemies[target].alive && tank.getEnergy()>5){
				tank.fire(firepower);
				spray_fire--;				
			}
		}		
		if(tank.getGunHeat()==0&&spray_fire==1){
			turnGunto(absoluteBearing(tank.getX(),tank.getY(),Scanner.enemies[target].x,Scanner.enemies[target].y+60));
			if(Scanner.enemies[target].alive && tank.getEnergy()>5){
				tank.fire(firepower);
				spray_fire--;			
			}
		}		
	}
	
	
	//Use linear prediction based on previous location, heading and velocity
	public void fire_linear_gun(int target,double firepower){		
		double bulletspeed = 20 - firepower * 3;
		double timetohit = Scanner.enemies[target].distance/ bulletspeed;
		double d_moved=Scanner.enemies[target].velocity*timetohit;
		double new_x=Scanner.enemies[target].x+d_moved*Math.sin(Math.toRadians(Scanner.enemies[target].heading));
		double new_y=Scanner.enemies[target].y+d_moved*Math.cos(Math.toRadians(Scanner.enemies[target].heading));
		
		turnGunto(absoluteBearing(tank.getX(),tank.getY(),new_x,new_y));		
		if(tank.getGunHeat()==0){			
			if(Scanner.enemies[target].alive && tank.getEnergy()>5)tank.fire(firepower);			
		}		
	}
	
	
	//point an fire to prev location
	public void fire_point_gun(int target, double firepower){
		turnGunto(absoluteBearing(tank.getX(),tank.getY(),Scanner.enemies[target].x,Scanner.enemies[target].y));		
		if(tank.getGunHeat()==0){			
			if(Scanner.enemies[target].alive && tank.getEnergy()>5)tank.fire(firepower);			
		}		
		
	}
	
	
	//Compute the absolute bearing b/w two coordinate points
	double absoluteBearing(double x1, double y1, double x2, double y2) {
		double xo = x2-x1;	double yo = y2-y1;
		double hyp =    Point2D.distance(x1, y1, x2, y2);
		double arcSin = Math.toDegrees(Math.asin(xo/hyp));
		double bearing = 0;
		if (xo > 0 && yo > 0) { // both pos: lower-Left
			bearing = arcSin;
		} else if (xo < 0 && yo > 0) { // x neg, y pos: lower-right
			bearing = 360 + arcSin; // arcsin is negative here, actually 360 - ang
		} else if (xo > 0 && yo < 0) { // x pos, y neg: upper-left
			bearing = 180 - arcSin;
		} else if (xo < 0 && yo < 0) { // both neg: upper-right
			bearing = 180 - arcSin; // arcsin is negative here, actually 180 + ang
		}
		return bearing;
	}
	
	
	//Turn the gun to the desired bearning
	public void turnGunto(double absbearing){
		double angle=tank.getGunHeading()-absbearing;
		while (angle >  180) angle -= 360;
		while (angle < -180) angle += 360;
		tank.turnGunLeft(angle);
	}
	
	
	//Find the best target
	public int get_Target(){		
		//get the best robot from the scanners vector of enemies
		int indx;
		double min_d=1000000;
		int mind_target_indx=0;
		double energy=100;
		int mine_target_indx=0;
		for(indx=0;indx<Scanner.E_NUM;indx++){
			if((min_d>Scanner.enemies[indx].distance)&&Scanner.enemies[indx].alive){
				mind_target_indx=indx;
				min_d=Scanner.enemies[indx].distance;
			}
			if((energy>Scanner.enemies[indx].energy)&&Scanner.enemies[indx].alive){
				energy=Scanner.enemies[indx].energy;
				mine_target_indx=indx;
			}
		}
		
		if(energy<20){
			return mine_target_indx;
		}else{
			return mind_target_indx;
		}
	}	

	
	//Each time we hit, update the information for that bot with it
	public void scored(BulletHitEvent e){		
		int indx;				
		for(indx=0;indx<Scanner.E_NUM;indx++){						
			if(Scanner.enemies[indx].name.equals(e.getName()))break;			
		}			
		Scanner.enemies[indx].gothit++;
	}
}
