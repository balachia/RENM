package eventnet.io;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.swing.JFileChooser;


public class PoliticParserMain {

	private static HashMap<Integer, Double> code2weight = new HashMap<Integer, Double>();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("process goldstein");
		processGoldstein();
		
		System.out.println("process event file");
		processEventData();
		processEventData();
		processEventData();
		processEventData();
		
	}


	
	/**
	 * parses a event file e.g. gulf and rearranges the columns. The event name column is omitted.
	 * The better R readable file is stored with the postfix "_processed"
	 */
	private static void processEventData() {
		
		final JFileChooser fc = new JFileChooser();
		File eventFile;
		File eventFileOut;
		
		fc.setCurrentDirectory(new File("D:\\_Z_Daten\\Workspaces\\R\\Data"));
		int ret = fc.showOpenDialog(null);
		
		//choose Event File
        if (ret == JFileChooser.APPROVE_OPTION) {
        	eventFile = fc.getSelectedFile();
        	eventFileOut = new File(eventFile.getAbsolutePath() + "_processed");
    		try {
    			BufferedReader in = new BufferedReader(new FileReader(eventFile));
    			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(eventFileOut)));
    			
    			//write header
    			out.println("actor1" + '\t' + "actor2" + '\t' + "time" +  '\t' + "eventType");
    			
    			String line = null;
    			
    			int shortLine = 0;
    			int errorLine = 0;
    			int selfLoop = 0;
    			int unknownCode = 0;
    			
    			//process the lines
    			while ((line = in.readLine()) != null) {
    				
    				String[] tokens = line.split("( )|(\t)");
    				
    				if (tokens.length >= 4) {
    					if (tokens[0].matches("[0-9]+") && tokens[1].matches("[A-Z]+") && tokens[2].matches("[A-Z]+") && tokens[3].matches("[0-9]+")) {
    						
    						String[] time = new String[3];
    						time[0] = tokens[0].substring(0,2);
    						time[1] = tokens[0].substring(2,4);
    						time[2] = tokens[0].substring(4,6);
    						
    						if (Integer.valueOf(time[0]) > 30) {
    							time[0] = "19"+time[0];
    						} else {
    							time[0] = "20"+time[0];
    						}
    						
    						GregorianCalendar greg = new GregorianCalendar(Integer.valueOf(time[0]), Integer.valueOf(time[1]), Integer.valueOf(time[2]));
    						Long timestamp = greg.getTimeInMillis();
    						timestamp /= 1000;
    						timestamp /= 86400;
    						
    						
    						String act1 = tokens[1];
    						String act2 = tokens[2];
    						int eventCode = Integer.valueOf(tokens[3]);
    						
    						if (code2weight.containsKey(eventCode)) {
    							if (!act1.equals(act2)) {
    								//everything ok
    								out.println(act1 + '\t' + act2 + '\t' + timestamp + '\t' + eventCode);
    							} else {
    								selfLoop++;
    							}
    						} else {
    							unknownCode++;
    						}
    						
    					} else {
    						errorLine++;
    					}
    				} else {
    					shortLine++;
    				}
    				
    			}
    			
    			
    			System.out.println("done with " + eventFile.getName());
    			System.out.println(shortLine + " short lines have been recorded");
    			System.out.println(errorLine + " errors have been recorded");
    			System.out.println(unknownCode + " entries can't be resolved in the goldstein file");
    			System.out.println(selfLoop + " entries were self loops");
    			
    			in.close();
    			out.flush();
    			out.close();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
	    }
	}
	
	
	
	/**
	 * parses the goldstein weight file and quotes the event names
	 * the better R readable file is stored with the postfix "_processed"
	 */
	private static void processGoldstein() {
		
		final JFileChooser fc = new JFileChooser();
		File goldSteinFile;
		File goldSteinFileOut;
		
		fc.setCurrentDirectory(new File("D:\\_Z_Daten\\Workspaces\\R\\Data"));
		int ret = fc.showOpenDialog(null);
		
		//choose Goldstein File
        if (ret == JFileChooser.APPROVE_OPTION) {
        	goldSteinFile = fc.getSelectedFile();
        	goldSteinFileOut = new File(goldSteinFile.getAbsolutePath() + "_processed");
    		try {
    			BufferedReader in = new BufferedReader(new FileReader(goldSteinFile));
    			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(goldSteinFileOut)));
    			
    			int shortLine = 0;
    			int errors = 0;
    			
    			//write header
    			out.println("eventType" + '\t' + "eventName" + '\t' + "weight");
    			
    			String line = null;
    			
    			//process the lines
    			while ((line = in.readLine()) != null) {
    				
    				String[] tokens = line.split("( )|(\t)");
    				
    				if (tokens.length >= 3) {
    					if (tokens[0].matches("[0-9]+") && tokens[tokens.length-1].matches("-?[0-9]+\\.[0-9]+")) {
    						//get the code
    						int eventCode = Integer.valueOf(tokens[0]);
    						
    						//rest should be the name
    						String eventName = "";
    						for (int i=1; i < tokens.length-1; i++) {
    							eventName += tokens[i] + " ";
    						}
    						eventName = eventName.substring(0, eventName.length()-1);
    						
    						//get the weight
    						double weight = Double.valueOf(tokens[tokens.length-1]);
    						
        					code2weight.put(eventCode, weight);
    						
        					//now print it
        					out.println(""+ eventCode + '\t' + '"' + eventName + '"'  + '\t' + weight);
    					} else {
    						errors++;
    					}
    				} else {
    					shortLine++;
    				}
    			}
    			
    			System.out.println("done with goldstein weights");
    			System.out.println(shortLine + " short lines have been recorded");
    			System.out.println(errors + " errors have been recorded");
    			
    			in.close();
    			out.flush();
    			out.close();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
	    }
	}

}
