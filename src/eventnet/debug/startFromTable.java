package eventnet.debug;

import com.martiansoftware.jsap.*;

import java.io.*;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.ArrayList;
//import java.util.Arrays.*;

import eventnet.*;
import eventnet.EventNetwork.Direction;
import eventnet.events.EdgeEvent;
import eventnet.functions.*;
import eventnet.statistics.*;
import eventnet.statistics.DegreeStat.Base;

public class startFromTable {
	//JAVA YOU NEED A FUCKING TUPLE RETURN TYPE
	//parse attribute stuff
	private static String[] m_attributes;
	private static double[] m_t_halfs;
	
	private static String[] m_eventTypes;
	
	private static HashMap<String, Integer> m_attr2index;
	private static HashMap<Integer, Integer> m_wgtFcn2index;
	
	//parse event type stuff
	private static ArrayList<String> et_eventTypes;
	private static ArrayList<String> et_attributes;
	private static ArrayList<WeightFunction> et_WgtFcns;
	private static ArrayList<Integer> et_WgtFcnHashes;
	
	//parse event stuff
	private static LinkedList<EdgeEvent> m_edgeEvents;
	private static HashSet<String> m_uniqueActors;
	private static int m_stochasticEventCount;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		JSAPResult config;
		try {
			SimpleJSAP jsap = new SimpleJSAP(
					"eventNet starter",
					"Starts eventNet solver at a specified stage",
					new Parameter[]
							{
								new FlaggedOption( "attrFile", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'a', "attributes",
					                    "The attributes description file." ),
			                    new FlaggedOption( "statFile", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 's', "statistics",
					                    "The statistics description file." ),
			                    new FlaggedOption( "typesFile", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 't', "types",
					                    "The event types description file." ),
			                    new FlaggedOption( "eventFile", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'e', "events",
					                    "The event types description file." ),
			                    new Switch("calculateWeights", 'w', "calculateWeights"),
			                    new Switch("verbose", 'v', "verbose")
							}
					);
			
			config = jsap.parse(args);
		} catch (JSAPException e) {
			System.out.println("lololwut?");
			System.out.println("JSAP watchu talkin bout?");
			throw e;
		}
		
		//read in options
		boolean calculateWeights = config.getBoolean("calculateWeights");
		boolean verbose = config.getBoolean("verbose");
		String attrFile = config.getString("attrFile");
		String statFile = config.getString("statFile");
		String eventTypesFile = config.getString("typesFile");
		String eventFile = config.getString("eventFile");
		
		//boolean calculateWeights = false;
		
		//String attrFile = "F:\\Users\\avashevko\\Documents\\SONIC\\Eventnet\\SLChat\\SLattrs.txt";
		//String statFile = "F:\\Users\\avashevko\\Documents\\SONIC\\Eventnet\\SLChat\\stats.txt";
		//String eventTypesFile = "F:\\Users\\avashevko\\Documents\\SONIC\\Eventnet\\SLChat\\SLtypes.txt";
		
		//String eventFile = "F:\\Users\\avashevko\\Documents\\SONIC\\Eventnet\\SLChat\\daf5aeba_all_events.txt";
		//eventFile = "F:\\Users\\avashevko\\Documents\\SONIC\\Eventnet\\SLChat\\61ebfaf7_all_events.txt";
		//eventFile = "F:\\Users\\avashevko\\Documents\\SONIC\\Eventnet\\SLChat\\89a77899_all_events.txt";
		//eventFile = "F:\\Users\\avashevko\\Documents\\SONIC\\Eventnet\\SLChat\\b85bccb3_all_events.txt";
		
		
		
		//Butts testing
		//attrFile = "F:\\Users\\avashevko\\Dropbox\\Dropbox\\MTS Relational Event Network Modeling\\technical meeting\\relevent testing\\roptim\\Rattrs.txt";
		//eventTypesFile = "F:\\Users\\avashevko\\Dropbox\\Dropbox\\MTS Relational Event Network Modeling\\technical meeting\\relevent testing\\roptim\\Rtypes.txt";
		//eventFile = "F:\\Users\\avashevko\\Dropbox\\Dropbox\\MTS Relational Event Network Modeling\\technical meeting\\relevent testing\\roptim\\testcase6java.txt";
		//eventFile = "out.txt";

		
		
		parseAttributes(attrFile);
		parseEventTypes(eventTypesFile);
		
		//make event processor
		String[][] wgtIDtbl = buildWeightFunctionIDTable(et_eventTypes, et_attributes, et_WgtFcns);
		WeightFunction[] wgtTbl = buildWeightFunctions(et_WgtFcns);
		EventProcessor eProc = new EventProcessor(m_attributes, m_eventTypes, wgtIDtbl, wgtTbl);
		
		//make transformation
		Statistic[] stats = parseStatistics(statFile);
		Transformation trans = new Transformation(stats, eProc, m_attributes, m_t_halfs, 1.0, 14335.0 + 28.0 + 0.9);
//		Transformation trans = new Transformation(stats, eProc, m_attributes, m_t_halfs, 0.0, 0.0);
		
		//build events
		parseEvents(eventFile);
		trans.addEdgeEvents(m_edgeEvents, m_uniqueActors, m_stochasticEventCount);
		
		trans.transformConditionalType(calculateWeights, verbose);
	}
	
	private static WeightFunction[] buildWeightFunctions(List<WeightFunction> wgtFcns) {
		WeightFunction[] ret = new WeightFunction[wgtFcns.size()];
		m_wgtFcn2index = new HashMap<Integer, Integer>();
		
		for(int i = 0; i < wgtFcns.size(); i++) {
			ret[i] = wgtFcns.get(i);
			m_wgtFcn2index.put(wgtFcns.get(i).hashCode(), i);
		}
		
		return ret;
	}
	
	private static String[][] buildWeightFunctionIDTable(List<String> eventTypes, List<String> attributes, List<WeightFunction> wgtFcns) {
		ArrayList<String[]> table = new ArrayList<String[]>();
		ArrayList<String> et_array = new ArrayList<String>();
		
		//String[][] ret = new String[][m_attributes.length];
		
		HashMap<String, Integer> type2index = new HashMap<String, Integer>();
		
		//build a square table associating each eventType to the attributes it affects using the appropriate weight function
		int index = 0;
		for(int i = 0; i < eventTypes.size(); i++) {
			String type = eventTypes.get(i);
			String[] temp;
			
			if(type2index.containsKey(type)) {
				temp = table.get(type2index.get(type));
			} else {
				temp = new String[m_attributes.length];
				type2index.put(type, index);
				java.util.Arrays.fill(temp, "None");
				table.add(temp);
				index += 1;
				et_array.add(type);
			}
			
			int attrIndex = m_attr2index.get(attributes.get(i));
			temp[attrIndex] = wgtFcns.get(i).getName();
		}
		
		m_eventTypes = et_array.toArray(new String[1]);
		return table.toArray(new String[1][]);
	}
	
	private static void parseAttributes(String attrFile) throws FileNotFoundException {
		Scanner scanner = new Scanner(new FileReader(new File(attrFile)));
		
		ArrayList<String> attributes = new ArrayList<String>();
		ArrayList<Double> t_halfs = new ArrayList<Double>();
		
		m_attr2index = new HashMap<String, Integer>();
		
		int index = 0;
		while( scanner.hasNextLine() ) {
			String line = scanner.nextLine();
			
			String[] terms = line.split("\t");
			
			attributes.add(terms[0]);
			t_halfs.add(Double.parseDouble(terms[1]));
			
			m_attr2index.put(terms[0], index);
			index += 1;
		}
		
		m_attributes = attributes.toArray(new String[1]);
		
		Double[] temp_t_halfs = t_halfs.toArray(new Double[1]);
		m_t_halfs = new double[temp_t_halfs.length];
		
		//rewrite Integers to ints
		//fuck you Java
		for( int i = 0; i < m_t_halfs.length; i++) {
			m_t_halfs[i] = temp_t_halfs[i];
		}
		
		//return m_attr2index, m_attributes, m_t_halfs
	}
	
	private static void parseEventTypes(String eventTypesFile) throws FileNotFoundException {
		Scanner scanner = new Scanner(new FileReader(new File(eventTypesFile)));
		
		et_eventTypes = new ArrayList<String>();
		et_attributes = new ArrayList<String>();
		et_WgtFcns = new ArrayList<WeightFunction>();
		et_WgtFcnHashes = new ArrayList<Integer>();
		
		while( scanner.hasNextLine() ) {
			String line = scanner.nextLine();
			
			String[] terms = line.split("\t");
			
			et_eventTypes.add(terms[0]);
			et_attributes.add(terms[1]);
			
			
			WeightFunction temp = new Identity();
			if(terms[2].equals("Identity")) {
				temp = new Identity();
			} else if(terms[2].equals("Invert")) {
				temp = new Invert(Double.parseDouble(terms[3]));
			} else if(terms[2].equals("Threshold")) {
				temp = new Threshold(Double.parseDouble(terms[3]));
			}
			et_WgtFcns.add(temp);
			et_WgtFcnHashes.add(temp.hashCode());
		}
		
		//return et_eventTypes, et_attributes, et_WgtFcns, et_WgtFcnHashes
	}
	
	private static Statistic[] parseStatistics(String statsFile) throws FileNotFoundException {
		//Statistic[] stats = new Statistic[9];
		//stats = new Statistic[2];
		//stats = new Statistic[1];
		
		LinkedList<Statistic> statsList = new LinkedList<Statistic>();
		
		// CMM paper
		statsList.add(new ConstantStat());                                                        //constant
		statsList.add(new DirectRelationStat("chat", Direction.OUT));                             //inertia
		statsList.add(new DirectRelationStat("chat", Direction.IN));                              //reciprocity
		statsList.add(new DirectRelationStat("chatfast", Direction.IN));                              //reciprocity
		statsList.add(new RelationProductStat(Direction.OUT, "chat", Direction.OUT, "friend"));   //friend inertia
		statsList.add(new RelationProductStat(Direction.IN, "chat", Direction.OUT, "friend"));    //friend reciprocity
		statsList.add(new RelationProductStat(Direction.IN, "chatfast", Direction.OUT, "friend"));    //friend reciprocity
		statsList.add(new DegreeStat("chat",Direction.SYM,Base.SRC));                             //ego hub
		statsList.add(new DegreeStat("chat",Direction.SYM,Base.TAR));                             //alter hub
		statsList.add(new TriangleStat(Direction.SYM,"chat",Direction.SYM,"chat"));               //triadic closure
//		statsList.add(new TriangleStat(Direction.SYM,"chatfast",Direction.SYM,"chatfast"));               //triadic closure
		//friend closure
		statsList.add(new ConditionedTriangleStat(Direction.SYM,"chat",Direction.SYM,"chat",Direction.OUT,"friend"));
//		statsList.add(new ConditionedTriangleStat(Direction.SYM,"chatfast",Direction.SYM,"chatfast",Direction.OUT,"friend"));
//		statsList.add(new ConditionedTriangleStat(Direction.SYM,"friend",Direction.SYM,"friend",Direction.SYM,"friend"));
		
		//statsList.add(new ConstantStat());
//		statsList.add(new NormDirectRelationStat("chat", Direction.OUT));
//		statsList.add(new NormDirectRelationStat("chat", Direction.IN));
		//statsList.add(new ButtsTwoPathsStat(Direction.OUT, "chat", Direction.OUT, "chat"));
		//statsList.add(new ButtsTwoPathsStat(Direction.OUT, "chat", Direction.IN, "chat"));
		//statsList.add(new ButtsTwoPathsStat(Direction.IN, "chat", Direction.OUT, "chat"));
		//statsList.add(new ButtsTwoPathsStat(Direction.IN, "chat", Direction.IN, "chat"));
//		statsList.add(new TwoPathsStat(Direction.OUT, "chat", Direction.OUT, "chat"));
//		statsList.add(new TwoPathsStat(Direction.OUT, "chat", Direction.IN, "chat"));
//		statsList.add(new TwoPathsStat(Direction.IN, "chat", Direction.OUT, "chat"));
//		statsList.add(new TwoPathsStat(Direction.IN, "chat", Direction.IN, "chat"));
		
		Statistic[] stats = new Statistic[statsList.size()];
		statsList.toArray(stats);
		
		/*
		stats[0] = new DirectRelationStat("chat", Direction.IN);
		stats[1] = new DirectRelationStat("chat", Direction.OUT);
		stats[2] = new TriangleStat(Direction.SYM,"chat",Direction.SYM,"chat");
		stats[3] = new DegreeStat("chat",Direction.OUT,Base.SRC);
		stats[4] = new DegreeStat("chat",Direction.OUT,Base.TAR);
		stats[5] = new DegreeStat("chat",Direction.IN,Base.SRC);
		stats[6] = new DegreeStat("chat",Direction.IN,Base.TAR);
		stats[7] = new RelationProductStat(Direction.IN, "chat", Direction.OUT, "friend");
		stats[8] = new TriangleStat(Direction.SYM,"friend",Direction.SYM,"friend");
		*/
		
		/*
		stats[0] = new DirectRelationStat("chat", Direction.OUT);
		stats[1] = new DirectRelationStat("chat", Direction.IN);
		stats[2] = new TriangleStat(Direction.SYM,"chat",Direction.SYM,"chat");
		stats[3] = new DegreeStat("chat",Direction.SYM,Base.SRC);
		stats[4] = new DegreeStat("chat",Direction.SYM,Base.TAR);
		stats[5] = new RelationProductStat(Direction.OUT, "chat", Direction.OUT, "friend");
		stats[6] = new RelationProductStat(Direction.IN, "chat", Direction.OUT, "friend");
		//stats[7] = new TriangleStat(Direction.SYM,"friend",Direction.SYM,"friend");
		stats[7] = new ConditionedTriangleStat(Direction.SYM,"friend",Direction.SYM,"friend",Direction.SYM,"friend");
		stats[8] = new ConstantStat();
		*/
		
		//testcase1
		//stats[0] = new NormDirectRelationStat("chat", Direction.OUT);
		//stats[1] = new TwoPathsStat(Direction.OUT, "chat", Direction.OUT, "chat");	//otp?
		//stats[1] = new TwoPathsStat(Direction.OUT, "chat", Direction.IN, "chat");		//osp?
		//stats[1] = new TwoPathsStat(Direction.IN, "chat", Direction.OUT, "chat");		//isp?
		//stats[1] = new TwoPathsStat(Direction.IN, "chat", Direction.IN, "chat");		//itp?
		
		//stats[0] = new ConstantStat();
		
		//testcase2
		//stats[0] = new NormDirectRelationStat("chat", Direction.OUT);					//frpsndsnd
		//stats[1] = new NormDirectRelationStat("chat", Direction.IN);					//frprec
		//stats[1] = new TwoPathsStat(Direction.OUT, "chat", Direction.OUT, "chat");	//otp?
		//stats[1] = new TwoPathsStat(Direction.OUT, "chat", Direction.IN, "chat");		//osp?
		//stats[1] = new TwoPathsStat(Direction.IN, "chat", Direction.OUT, "chat");		//isp?
		//stats[1] = new TwoPathsStat(Direction.IN, "chat", Direction.IN, "chat");		//itp?
		
		//testcase3
		//stats[0] = new NormDirectRelationStat("chat", Direction.IN);
		//stats[1] = new TwoPathsStat(Direction.OUT, "chat", Direction.OUT, "chat");	//otp?
		//stats[1] = new TwoPathsStat(Direction.OUT, "chat", Direction.IN, "chat");		//osp?
		//stats[1] = new TwoPathsStat(Direction.IN, "chat", Direction.OUT, "chat");		//isp?
		//stats[1] = new TwoPathsStat(Direction.IN, "chat", Direction.IN, "chat");		//itp?
		
		/*
		stats[0] = new DirectRelationStat("pos", Direction.IN);
		stats[1] = new DirectRelationStat("neg", Direction.IN);
		stats[2] = new DirectRelationStat("pos", Direction.OUT);
		stats[3] = new DirectRelationStat("neg", Direction.OUT);
		stats[4] = new TriangleStat(Direction.SYM,"pos",Direction.SYM,"pos");
		stats[5] = new TriangleStat(Direction.SYM,"neg",Direction.SYM,"pos");
		stats[6] = new TriangleStat(Direction.SYM,"pos",Direction.SYM,"neg");
		stats[7] = new TriangleStat(Direction.SYM,"neg",Direction.SYM,"neg");
		stats[8] = new DegreeStat("pos",Direction.OUT,Base.SRC);
		stats[9] = new DegreeStat("neg",Direction.OUT,Base.SRC);
		stats[10] = new DegreeStat("pos",Direction.OUT,Base.TAR);
		stats[11] = new DegreeStat("neg",Direction.OUT,Base.TAR);
		stats[12] = new DegreeStat("pos",Direction.IN,Base.SRC);
		stats[13] = new DegreeStat("neg",Direction.IN,Base.SRC);
		stats[14] = new DegreeStat("pos",Direction.IN,Base.TAR);
		stats[15] = new DegreeStat("neg",Direction.IN,Base.TAR);
		*/
		
		return stats;
	}
	
	private static void parseEvents(String eventsFile) throws FileNotFoundException {
		Scanner scanner = new Scanner(new FileReader(new File(eventsFile)));
		
		ArrayList<String> sources = new ArrayList<String>();
		ArrayList<String> targets = new ArrayList<String>();
		ArrayList<Double> times = new ArrayList<Double>();
		ArrayList<String> types = new ArrayList<String>();
		ArrayList<Double> weights = new ArrayList<Double>();
		ArrayList<Boolean> stochastics = new ArrayList<Boolean>();
		
		while( scanner.hasNextLine() ) {
			String line = scanner.nextLine();
			
			String[] terms = line.split("\t");
			
			sources.add(terms[0]);
			targets.add(terms[1]);
			times.add(Double.parseDouble(terms[2]));
			types.add(terms[3]);
			weights.add(Double.parseDouble(terms[4]));
			stochastics.add(Boolean.valueOf(terms[5]));
		}
		
		//rewrite all this shit to edge events
		m_edgeEvents = new LinkedList<EdgeEvent>();
		m_uniqueActors = new HashSet<String>();
		
		m_stochasticEventCount = 0;
		for (int i = 0; i < sources.size(); i++) {
			m_edgeEvents.add(new EdgeEvent(sources.get(i), targets.get(i), times.get(i), types.get(i), weights.get(i), stochastics.get(i)));
			m_uniqueActors.add(sources.get(i));
			m_uniqueActors.add(targets.get(i));
			
			if (stochastics.get(i)) {
				m_stochasticEventCount++;
			}
		}
		
		//return m_edgeEvents, m_uniqueActors, m_stochasticEventCount 
	}

}
