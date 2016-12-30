MyRobots folder contains all the robots created by us, as well as several supporting files. Data files are stored in a folder
named name_of_the_robot.data.

Robots of interest include:


NN1_DeadBunnyCrying.java
------------------------

Currently being developed with the goal of improving performance against several host robots, in particular Trackfire (avoidance), 
Walls (predictive targeting and avoidance), Spinbot (improved predictive targeting and likely self-generated movement patterns). 
To achieve this, we will at least employ several new parameters that best captures the advantages of NN that could not be 
employed by RL (reinforcement learning). Other possibilities include changing number of hidden nodes, and multiple nets.

NN1 is planned to use single net with single output, with one layer of hidden net with five hidden nodes.


NN2_LUTMimic.java
-----------------

Instance of NN robot built based on parameters similar to those employed for our reinforcement learning robot. Mainly tested against
Spinbot and Trackfire.

NN2 currently uses single net with single output, with one layer of hidden net with five hidden nodes.


LUTTrackfire.java
-----------------

Reinforcement learning robot that utilizes a look-up table with Q-values representing various state actions.  