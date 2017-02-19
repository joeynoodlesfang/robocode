MyRobots folder contains all the robots we created, as well as several supporting files. 
Data files are stored in a folder named name_of_the_robot.data.

Planning.txt : file containing future milestones of MyRobots.
Worklog.txt : file containing worklog. ya.

Robots of interest include:

NN2_LUTMimic.java
-----------------

NN2_LUTMimic is a neural net-based robot that mimicks the logic and the various parameters of a LUT-based robot (LUTTrackfire). 
This is done to limit the effect of the NN technique on robot performance - ie. we wanted the code foundation to build future 
bots that will have parameters which exploit NN.  

NN2 currently uses single net with single output, with one layer of hidden net with five hidden nodes.


NN1_DeadBunnyCrying.java
------------------------

Currently being developed with the goal of improving performance against several host robots, in particular Trackfire (avoidance), 
Walls (predictive targeting and avoidance), Spinbot (improved predictive targeting and likely self-generated movement patterns). 
To achieve this, we will at least employ several new parameters that best captures the advantages of NN that could not be 
employed by LUT RL. Other possibilities include changing number of hidden nodes, and multiple nets.

NN1 is planned to use single net with single output, with one layer of hidden net with five hidden nodes.


LUTTrackfire.java
-----------------

Reinforcement learning robot that utilizes a look-up table with Q-values representing various state actions.  

Dodger.java
-----------

Very first bot built. Tests the feasibility of coding LUT-based robot.