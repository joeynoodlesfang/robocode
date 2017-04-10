MyRobots folder contains all the robots we created, as well as several supporting files. 
Data files are stored in a folder named name_of_the_robot.data.

Planning.txt : file containing future milestones of MyRobots.
Worklog.txt : file containing worklog. ya.

Robots of interest include:

NN2_LUTMimic.java *MOST UP-TO-DATE*
-----------------
NN2_LUTMimic is our first bot that applies neural network (NN or net) techniques to reinforcement 
learning(RL). Like its name suggests, NN2_LUTMimic mimicks a LUT-based robot (mainly LUTTrackfire) 
by replicating its many parameters, such as state and action parameters, instead of designing 
parameters that employ neural net advantages. The purpose of coding NN2 is to code a structure for 
future bots to develop NN-specific behaviours and other parameters, and we can make sure that the 
code works by comparing behaviour between LUTTrackfire and NN2.

NN2 currently uses single net with single output, with one layer of hidden net with five hidden nodes.


NN1_DeadBunnyCrying.java
------------------------
NN1_DBC will be the first bot that fully employs the benefits of neural net in combat. It is currently 
developed with the goal of improving performance against several host robots, in particular Trackfire (avoidance), 
Walls (predictive targeting and avoidance), and Spinbot (improved predictive targeting and likely self-generated 
movement patterns). To achieve this, we will at least employ several new parameters that best captures the 
advantages of NN that could not be employed by LUT RL, such as continuous input ranges, and a lot more possible 
states. Other possibilities include changing number of hidden nodes, and multiple nets.

NN1 is planned to use single net with single output, with one layer of hidden net with five hidden nodes.


LUTTrackfire.java
-----------------
LUTTrackfire is a robocode bot that implements reinforcement learning (RL) techniques to improve its combat 
abilities. RL requires the bot to remember the results of certain moves (aka actions) in certain situations (aka 
states). The results are converted by the coder into a reward, which can be positive or negative. The reward allows 
the bot to make better future decisions for the exact same situation. LUTTrackfire stores these rewards in the form 
of a look-up table. Each reward entry in the look-up table has a unique state and action parameter. Most of the combat
abilities of LUTTrackfire were taken from the example robot Trackfire.

Since memory is cleared when the Robocode program is terminated, LUT reward values are stored externally. Robocode
limits the size of all external files for a particular robot to be 200kB.

Dodger.java
-----------
Very first bot we built (god bless). Tests the feasibility of coding look-up table-based robot. The goal of the robot
is to avoid the walls. Its state parameters are the discretized x and y coordinates of the Robocode 800x600 arena.