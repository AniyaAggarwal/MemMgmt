package abc;
import java.io.IOException;
import java.util.*;

import org.xmlpull.v1.XmlPullParserException;

import soot.*;
import soot.Singletons.Global;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.*;
import soot.options.Options;
import soot.options.PurityOptions;

/**
 * Purity analysis phase.
 */

/**
 * TODO:
 *  - test, test, and test (and correct the potentially infinite bugs)
 *  - optimise PurityGraph, especially methodCall)
 *  - find a better abstraction for exceptions (throw & catch)
 *  - output nicer graphs (especially clusters!)
 */

public class EscapeAnalysis extends SceneTransformer
{
    Singletons.Global g;
    
    static EscapeAnalysis instance = new EscapeAnalysis();

    public EscapeAnalysis(Singletons.Global g ) { this.g = g; }

    public EscapeAnalysis() {
		// TODO Auto-generated constructor stub
	}

	public static EscapeAnalysis v() {
		return instance;
    }

    protected void internalTransform(String phaseName, Map options)	{
    //	PurityOptions opts = new PurityOptions(options);

    //	G.v().out.println("[AM] Analysing purity")
    	SetupApplication app = new SetupApplication("C:\\Program Files (x86)\\Android\\android-sdk\\platforms","E:\\Program Analysis Workspace\\Flowdroid_Test\\Messaging.apk" );
		try {		
			app.calculateSourcesSinksEntrypoints("E:\\Program Analysis Workspace\\Flowdroid_Test\\SourcesAndSinks.txt");
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
    	
    	Scene.v().loadNecessaryClasses();		
		SootMethod entryPoint = app.getEntryPointCreator().createDummyMain();		
		Options.v().set_main_class(entryPoint.getSignature());		
		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));		
		System.out.println(entryPoint.getActiveBody());	
		PackManager.v().runPacks();		
		System.out.println(Scene.v().getCallGraph().size());
		
		
		CallGraph cg = Scene.v().getCallGraph();
		System.out.println(cg +"\n\n");
		
		


    	// launch the analysis
    //	EscapeInterProc p =
	  //  new EscapeInterProc(cg, Scene.v().getEntryPoints().iterator(), options); 
    }
}