This is a robocode project that is using reinforcement learning (Q LUT) and neural net.

Copyright? Take what u want

To access the robots that we are working on, or older robots, follow the directories given below. The MyRobots directory also contains a README that explains the robots in a little more detail.

Reinforcement learning (LUT) robot:
-----------------------------------
Directory: robots -> MyRobots -> LUTTrackfire.java

Neural Net:
-----------
NN2_LUTMimic is an introductory robot designed to mimic the LUT robot behaviour, with similar states and actions as parameters.
Directory: robots -> MyRobots -> NN2_LUTMimic.java

Introduction:
-------------

Robocode:
 * Robocode is a program designed for learning the basics of java. The goal is to code a bot that 
 * compete with others in an arena. The coder will have no direct influence on how the fighting is 
 * done - instead, all the fight mechanics and strategies employed by the robot are coded before 
 * the fight ever took place. So the robot is free to employ any tactics it wants to win - as long 
 * as the bot adheres to the rules of the game. 

Reinforcement Learning:
 * Reinforcement learning, RL, refers to a method by which a machine decides on which action to 
 * take, in order to maximize a conceptualized reward. The bot learns by performing an action 
 * within a measurable environment, and rewarding itself based upon the results of said action. 
 * The reward alters the likelihood of performing the same action again in the same environment: 
 * winning moves can be recreated, and poor moves can be avoided. 

 Neural Network:
 * Neural network (NN) is a computation network that solves problems by using complexity, 
 * approximation, and trial and error. It imitates biological neurons by setting both inputs and 
 * outputs as neuronal nodes, and connecting them through a network, which oftentimes includes 
 * other nodes called hidden nodes. Nodes of similar functionalities are oftentimes - at least for 
 * our bots - placed in a layer, and layers talk to other layers. Simple nets may contain a layer 
 * of input nodes connected to a layer of hidden nodes, which are connected to a layer of output 
 * nodes. The effect of the connections are changeable - each has a coefficient to it, called a 
 * weight, that affects how the nodes it is connected to perceive each other. 