# Relational Event Network Models

Relational Event Network Models (RENMs) attempt to model the rate of events that occur on dyads in a network.
A typical example might be texts or chats sent from one person to another on a mobile or social network.

This code implements a maximum likelihood estimator of a RENM. Unfortunately, the code is poorly documented at the moment.
There is also an early stage [port of this project to R](https://github.com/balachia/renmr).


## Setup

The project is configured for Apache Ant. Build the project by entering the root folder in terminal and typing

```sh
ant all
```

The *test* folder contains a simple test case that should take no more than a few minutes to run.  Run the test case by entering 

```sh
java -cp classes:"extlib/*" eventnet.debug.startFromTable \
    --verbose \
    --attributes "test/attrs.txt" \
    --statistics "test/stats.txt" \
    --types "test/types.txt" \
    --events "test/sample_data.txt" 
```

After 12 or so iterations, the solver should converge at the following output:

```sh
ITERATION 12
Param   SE  Stat
-6.782192499979157  0.03480543913913448 ** Constant
0.01110773826798984 9.015498050049717E-4    ** RelCov (OUT chat)
0.0013576213737991824   8.885454473675229E-4    RelCov (IN chat)
9.96632548221402E-4 2.5946425919005754E-4   ** RelCov (IN chatfast)
0.02713187611063073 0.004577435777085113    ** RelProd (OUT chat, OUT friend)
-0.04808858308289873    0.004432269549438358    ** RelProd (IN chat, OUT friend)
0.0017362896790377904   8.378395655138633E-4    * RelProd (IN chatfast, OUT friend)
4.5411033647038506E-4   4.807046386921453E-5    ** Degree (SRC/SYM chat)
6.693584532912896E-5    6.939226116448735E-5    Degree (TAR/SYM chat)
0.004271084978883075    5.411610527223405E-4    ** Triangle (SYM chat, SYM chat)
0.020902723578907816    0.0045312733373824065   ** CondTri (SYM chat, SYM chat [OUT friend])
LL: -7876.057111339139
AIC:    15774.114222678278
AICc:   15774.342398996341
BIC:    15829.81716625447

|dX| (P2): 2.558009421365071E-13
|dLL|: 2.5283952709287405E-10
```

The output lists the parameter value at the maximum likelihood estimate (MLE), the standard error associated with the parameter, and the parameter name. Summary statistics follow below that.
|dX| and |dLL| are the changes in parameter values and log-likelihood from the previous iteration and are used for convergence detection.


## Data Format

*test/sample_data.txt* contains a sample of the data the solver expects. Data is tab-delimited with 6 columns:

1. Event Sender
2. Event Receiver
3. Event Time (integer)
4. Event Type
5. Event Weight
6. Necessary column containing the value 'TRUE'

The sample data contains 'friend' events in rows 1-915, taking weights 1-7, and 'chat' events from there, taking values 1 or greater (representing length of the chat). 


## Types File

The types file is a tab-delimited format mapping event types to network types. It also specifies the form of the transformation.

In the sample data, *test/types.txt* maps the 'chat' event type to network type 'chat' (the slow-decaying chat network) and 'chatfast' (the fast-decaying chat network). Both map through the 'Identity' transformation -- weights in the network equal weights of the event.

In addition, the sample data maps the 'friend' event type to the 'friend' network type and the 'unfriend' network type. The 'Threshold' transformation produces a 1 if the value of the event type exceeds the specified cut-off and 0 otherwise. The 'Invert' transformation subtracts the event weight from the specified value to produce the network weight.

Available transformations are in *src/eventnet/functions*.


## Attributes File

The attributes file is a tab-delimited file that specifies the half-lives of network types. A half-life value of 0 implies no decay.

In the sample data, *test/attrs.txt* gives the 'chat' network a half-life of 10 and the 'chatfast' network a half-life of 1. The friend network types do not decay.


## Statistics File

As of now, the statistics file serves no purpose and the estimated statistics must be set in the source code and the solver recompiled.

Statistics are set in the `parseStatistics` function of *src/eventnet/debug/startFromTable.java*. The available statistics are available in *src/eventnet/statistics*.


## Further Settings

Two additional parameters must be specified to estimate a particular model:

1. Starting time of the network.
The model starts estimation after the starting time. All events in the data that occur at or before the starting time are treated as the initial state of the network.

2. Minimum time difference.
The model lumps all events that occur within the minimum time difference of each other as occuring simultaneously.
For example, if time stamps are recorded in seconds, a minimum time difference of 60 will lump all events that occur within a minute of each other together.

Both parameters are set in the source code, in *src/eventnet/debug/startFromTable.java*, on the line

```Java
Tranformation trans = new Tranformation(..., ..., ..., ..., MIN_DIFFERENCE, STARTING_TIME)
```


## Contact

If you have questions, get in touch: avashevko at gmail

