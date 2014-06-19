package eventnet.debug;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 
 * @author Michael Zinsmaier, University of Konstanz
 * 
 * simple Writer that outputs some data. Used to get Data for debugging etc while
 * connected to R 
 *
 */
public class HelperWriter {

	/** defines the path of the output file */
	private final static String filePath = "D:/Test.txt";
	
	/**
	 * @param lines array that defines what should be written to the file
	 */
	public static void write(String[] lines) {
        try {
			PrintWriter pw = new PrintWriter(new BufferedWriter( new FileWriter(filePath)));
			
			for (String line : lines) {
				pw.write(line + '\n');
			}
			
			pw.flush();
			pw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
}
