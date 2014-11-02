package abc;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import soot.G;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.PointsToAnalysis;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.InfoflowResults;
import soot.jimple.infoflow.android.SetupApplication;
import soot.options.Options;
import soot.options.PurityOptions;
import soot.toolkits.graph.BriefUnitGraph;
import soot.util.dot.DotGraph;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;

public class NewTest {
	
	private static DotGraph dot = new DotGraph("CallGraph");
	static HashMap<String,Boolean> visited = new HashMap<String, Boolean>();
	
	static Map<SootMethod, Map<Unit, Loop>> map;
	
	static Set<SootClass> app_classes;
	
	static PointsToAnalysis pa;
			
	public NewTest() {	
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args) {	
		// TODO Auto-generated method stub	
		
		args =new String[0];
		SetupApplication app = new SetupApplication("C:\\Program Files (x86)\\Android\\android-sdk\\platforms","C:\\Users\\Ani\\Desktop\\AndroidObjectPool.apk");
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
		soot.G.reset();		
		Options.v().set_src_prec(Options.src_prec_apk);		
		
		Options.v().set_process_dir(Collections.singletonList("C:\\Users\\Ani\\Desktop\\AndroidObjectPool.apk"));//"C:\\Users\\Ani\\Desktop\\new try\\ToyExample.apk"));//"E:\\Program Analysis Workspace\\test apk repo\\toytest\\ToyExample.apk"));
		Options.v().set_android_jars("C:\\Program Files (x86)\\Android\\android-sdk\\platforms");		
		Options.v().set_whole_program(true);		
		Options.v().set_allow_phantom_refs(true);
	//	Options.v().set_soot_classpath(System.getProperty("java.class.path"));
	//	System.out.println(System.getProperty("java.class.path"));
	//	Options.v().set_prepend_classpath(true);
		Options.v().set_output_dir("E:\\Program Analysis Workspace\\MemoryOptimization\\sootOutput\\");
		Options.v().set_output_format(Options.output_format_jimple);
		
		Options.v().setPhaseOption("cg.spark", "on");
		Options.v().set_whole_program(true);	
		

//		Scene.v().addBasicClass("HelloWorld");	
		Scene.v().loadNecessaryClasses();
		
		SootMethod entryPoint = app.getEntryPointCreator().createDummyMain();		
		Options.v().set_main_class(entryPoint.getSignature());		
		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));		
		System.out.println(entryPoint.getActiveBody());
		PackManager.v().runPacks();	
		
		app_classes = new HashSet<SootClass>();
		for( SootClass c: Scene.v().getClasses()){
			String class_name = c.getName();
			
			if(class_name.startsWith("java")|| class_name.startsWith("sun.")||class_name.startsWith("android.")||class_name.startsWith("com.sun.")||class_name.startsWith("dummyMain")|| class_name.startsWith("org.xmlpull.")|| class_name.substring(class_name.lastIndexOf(".")+1).startsWith("R$") ||class_name.endsWith(".R") || class_name.endsWith(".BuildConfig") )
				continue;
			else
				app_classes.add(c);
		}
		System.out.println(app_classes);
		
		CallGraph cg = Scene.v().getCallGraph();
		
		pa = Scene.v().getPointsToAnalysis();
		
		
		Map options = new HashMap();
		
		PurityOptions opt = new PurityOptions(options);
		
		EscapeInterProc p =
			    new EscapeInterProc(cg, Scene.v().getEntryPoints().iterator(), opt); 
		
		map = p.loop_reset;
		
		System.out.println("Map is.."+ map)	;

		for(SootMethod method : map.keySet()){
			AndroidAnalysis a = new AndroidAnalysis(Scene.v().getMethod(method.getSignature()));
		}
		
		PackManager.v().writeOutput();
		G.v().out.println("Done!!");
		
	}
	
	//Utility to generate DOT format file for call graph visualization
	private static void visit(CallGraph cg, SootMethod k)
	{
		String identifier = k.getName();
		
		visited.put(k.getSignature(), true);
		
		dot.drawNode(identifier);
		
		//iterate over unvisited parents
		Iterator<MethodOrMethodContext> ptargets = new Targets(cg.edgesInto(k));
		
		if(ptargets != null){
			while(ptargets.hasNext())
			{
				SootMethod p = (SootMethod) ptargets.next();
				
				if(p == null) System.out.println("p is null");
				
				if(!visited.containsKey(p.getSignature()))
					visit(cg,p);
			}
		}
		
		//iterate over unvisited children
		Iterator<MethodOrMethodContext> ctargets = new Targets(cg.edgesOutOf(k));
		
		if(ctargets != null){
			while(ctargets.hasNext())
			{
				SootMethod c = (SootMethod) ctargets.next();
				if(c == null) System.out.println("c is null");
				dot.drawEdge(identifier, c.getName());
				
				if(!visited.containsKey(c.getSignature()))
					visit(cg,c);
			}
		}
	}
	
}