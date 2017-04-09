Introduction
============
This is a robocode project that is using reinforcement learning (Q LUT) and neural net.
The premise for this project is that reinforcement learning can be used to improve robot combatibility against all types of enemy robot behaviours, by learning counters to their behaviour in real time.

What is Robocode?
====================
This section is copied from the main robocode ReadMe http://robocode.sourceforge.net/docs/ReadMe.html
Learn from community at [RoboWiki] (http://robowiki.net/)

Robocode is a programming game where the goal is to code a robot battle tank to compete against other robots in a battle arena. So the name Robocode is a short for "Robot code". The player is the programmer of the robot, who will have no direct influence on the game. Instead, the player must write the AI of the robot telling it how to behave and react on events occurring in the battle arena. Battles are running in real-time and on-screen.

The motto of Robocode is: Build the best, destroy the rest!

Useful Directories
======================
To access the robots that we are currently working on, or older robots, or key files, follow the directories given below.
*** MOST UP-TO-DATE BOT IS THE FIRST ONE DIRECTLY BELOW: NN2_LUTMimic - click link for code!***
(The robots each contains a relevant readme in the beginning of the code.)
(Current branch worked on: origin/development)

<-> NN2_LUTMimic <->
Bot that uses reinforcement learning with artificial neural network:
is an introductory robot designed to mimic the LUT robot behaviour, with similar states and actions as parameters.
Directory: robots -> MyRobots -> NN2_LUTMimic.java (https://github.com/joeynoodlesfang/robocode/blob/development/robots/MyRobots/NN2_LUTMimic.java)

---

<-> LUTTrackfire <->
Reinforcement learning bot with LUT:
Directory: robots -> MyRobots -> LUTTrackfire.java (https://github.com/joeynoodlesfang/robocode/blob/development/robots/MyRobots/LUTTrackfire.java)

--- 

Worklog: robots -> MyRobots -> WORKLOG.txt (https://github.com/joeynoodlesfang/robocode/blob/development/robots/MyRobots/WORKLOG.txt)
Future work plans: robots -> MyRobots -> PLANNING.txt (https://github.com/joeynoodlesfang/robocode/blob/development/robots/MyRobots/PLANNING.txt)
More detailed summary of all our robots: robots -> MyRobots -> README.txt (https://github.com/joeynoodlesfang/robocode/blob/development/robots/MyRobots/README.txt)






Intro to Reinforcement Learning
==================================
REINFORCEMENT LEARNING, RL, refers to the method by which a machine decides on the action to take. 
The decision is made by choosing the action that maximizes a conceptualized reward: the bot performs 
an action within a measurable environment, and rewards itself based upon the results of said action. 
The reward alters the likelihood of re-performing the same action again in the same environment: 
winning moves can be recreated, and poor moves can be avoided. This ability to learn during the 
actual battle gives the robot combat adaptibility.


Intro to Neural Network
==========================

Neural network (NN) is a computation network that solves problems by employing complexity, 
approximation, and trial and error. It imitates biological neurons in design through making both 
inputs and outputs to the system to be neuronal nodes, which, like a biological neuron network, 
connects to other neurons and forms a network with the connections. The net has the ability to 
solve problems by acting sort of like an equation - a very complex equation with changeable 
coefficients, and through trial and error of repeatedly trying to achieve correct outputs with 
various given inputs, the network can create a relatively accurate model of the system. Complexity 
is often a benefit for correct modeling (much like a linear line is a less efficient model of a 
higher order equation, whereas higher order equations can model lower ones relatively easily). The 
network often includes other nodes designed by the coder called 'hidden nodes' to add complexity. 
It can also be structured in a complex manner (such as multidimensional nodal connections). The 
simplest structure of a NN - which is used for our bots - is a 2-dimensional net consisting of 3 
1-D layers: a 1D layer of input nodes connected to a layer of hidden nodes, which connects to both 
the input layer as well as a layer of output nodes. These connections have changeable values 
associated to them (aka 'weights'), and is the engine behind the net's ability to adjust its 
approximation.

Neural net is used here for RL, just like LUT, with one main distinction being that NN can only 
estimate the value of actions. In exchange for that, however, NN allows more inputs, and inputs of 
greater variety to be used. In a LUT, each selection of inputs has a corresponding value: for a 
system with two inputs, each with 3 possible values, the total number of values to be stored is 
3x3 = 9. For the robot to remember behaviours after wars, the LUT method requires a list of QVals 
to be stored in a file in between executions. Robocode limits all files to a size of 200kBs, and 
it doesn't take long before a robot with multiple inputs to reach 200kBs. NN on the other hand 
estimates the QVals through calculations, and stores only the values associated with the neural 
net connections, or 'weights'. This is a much smaller set of values. Weights describe the strength 
of the connections between nodes in a neural net, and it can be any real value. By requiring only 
weights to be stored, NN allows a much greater selection of inputs to be used. Even continuous 
input ranges can be used, which is impossible for LUT.