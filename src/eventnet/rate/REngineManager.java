package eventnet.rate;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;

public class REngineManager {
	
	private static REngineManager _instance = null;
	private Rengine _rEngine;
	
	private REngineManager(){
		
		_rEngine=new Rengine(null, false, new TextConsole());
		//_rEngine=new Rengine(new String[] {"--max-mem-size=1G"}, false, new TextConsole());
		//_rEngine=new Rengine(new String[] {"--vanilla", "--max-mem-size=1G", "--enable-memory-profiling", "--tool=massif" }, false, new TextConsole());
		//_rEngine=new Rengine(null, false, null);
    	if (!_rEngine.waitForR()) {
            System.out.println("Cannot load R");
            return;
        }
	}
	
	public static REngineManager getInstance(){
		if(_instance==null){
			_instance = new REngineManager();
		}
		
		return _instance;
	}
	
	public Rengine getREngine(){
		return _rEngine;
	}
	
	
	
	class TextConsole implements RMainLoopCallbacks
	{
	    public void rWriteConsole(Rengine re, String text, int oType) {
	        System.out.print(text);
	    }
	    
	    public void rBusy(Rengine re, int which) {
	        System.out.println("rBusy("+which+")");
	    }
	    
	    public String rReadConsole(Rengine re, String prompt, int addToHistory) {
	        System.out.print(prompt);
	        try {
	            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
	            String s=br.readLine();
	            return (s==null||s.length()==0)?s:s+"\n";
	        } catch (Exception e) {
	            System.out.println("jriReadConsole exception: "+e.getMessage());
	        }
	        return null;
	    }
	    
	    public void rShowMessage(Rengine re, String message) {
	        System.out.println("rShowMessage \""+message+"\"");
	    }
		
	    public String rChooseFile(Rengine re, int newFile) {
		FileDialog fd = new FileDialog(new Frame(), (newFile==0)?"Select a file":"Select a new file", (newFile==0)?FileDialog.LOAD:FileDialog.SAVE);
		//fd.show();
		fd.setVisible(true);
		String res=null;
		if (fd.getDirectory()!=null) res=fd.getDirectory();
		if (fd.getFile()!=null) res=(res==null)?fd.getFile():(res+fd.getFile());
		return res;
	    }
	    
	    public void   rFlushConsole (Rengine re) {
	    }
		
	    public void   rLoadHistory  (Rengine re, String filename) {
	    }			
	    
	    public void   rSaveHistory  (Rengine re, String filename) {
	    }			
	}
	
	

}
