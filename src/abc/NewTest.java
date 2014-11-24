package abc;
import instrument.BuffPoolInstrument;
import instrument.InsUtil;
import instrument.ReplaceInsPrevious;

import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import Heuristics.BetweenessCentrality;
import Heuristics.OptMethodCallInLoop;

import soot.G;
import soot.Local;
import soot.PackManager;
import soot.PhaseOptions;
import soot.PointsToAnalysis;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.infoflow.android.SetupApplication;
import soot.options.JBOptions;
import soot.options.JBTROptions;
import soot.options.Options;
import soot.options.PurityOptions;
import soot.toolkits.graph.DirectedGraph;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.callgraph.CallGraph;

public class NewTest {
	
	static Map<SootMethod, Map<Unit, Loop>> map;
	static public Set<SootClass> app_classes;	
	static PointsToAnalysis pa;
	static Map<SootMethod, Map<Local, Set<Unit>>> instrument_map;
	static public Map<SootMethod, InsUtil> result_map;
	static public Set<Type> newTypeSet;
	static public CallGraph cg;
	static public DirectedGraph dg;
	static public Map<SootMethod, Set<Loop>> methodToLoop_map = new HashMap<SootMethod, Set<Loop>>();
	public static Map<SootMethod, Set<Unit>> nonEscNewAlloc = new HashMap<SootMethod, Set<Unit>>();
	
			
	public static void main(String[] args) {	
		// TODO Auto-generated method stub	
		
		args = new String[0];
		SetupApplication app = new SetupApplication("C:\\Program Files (x86)\\Android\\android-sdk\\platforms","C:\\Users\\Ani\\Desktop\\myfolder\\CallWithinLoop.apk");//Dropbox\\ObjectPool\\PointObjectPool_before opt\\CallWithinLoop\\bin\\CallWithinLoop.apk");//C:\\Users\\Ani\\Dropbox\\ObjectPool\\p and point different\\CallWithinLoop.apk");//"C:\\Users\\Ani\\Dropbox\\ObjectPool\\PointObjectPool_before opt\\CallWithinLoop\\bin\\CallWithinLoop.apk");//"C:\\Users\\Ani\\Desktop\\AndroidObjectPool\\bin\\AndroidObjectPool.apk");//"C:\\Users\\Ani\\Dropbox\\ObjectPool\\compiler_optimization\\CallWithinLoop.apk");//AndroidObjectPoolBefore.apk");//"C:\\Users\\Ani\\Desktop\\AndroidObjectPool\\bin\\AndroidObjectPool.apk");//"C:\\Users\\Ani\\Desktop\\new try\\ToyExample.apk");//"C:\\Users\\Ani\\Desktop\\AndroidObjectPool.apk");
		try {		
			app.calculateSourcesSinksEntrypoints("E:\\eclipseprojects\\git\\Flowdroid_Test\\SourcesAndSinks.txt");
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		//soot.G.reset();		
		Options.v().set_src_prec(Options.src_prec_apk);		
		
		Options.v().set_process_dir(Collections.singletonList("C:\\Users\\Ani\\Desktop\\myfolder\\CallWithinLoop.apk"));//Dropbox\\ObjectPool\\PointObjectPool_before opt\\CallWithinLoop\\bin\\CallWithinLoop.apk"));//"C:\\Users\\Ani\\Dropbox\\ObjectPool\\p and point different\\CallWithinLoop.apk"));//"C:\\Users\\Ani\\Dropbox\\ObjectPool\\PointObjectPool_before opt\\CallWithinLoop\\bin\\CallWithinLoop.apk"));//"C:\\Users\\Ani\\Desktop\\AndroidObjectPool\\bin\\AndroidObjectPool.apk"));//"C:\\Users\\Ani\\Dropbox\\ObjectPool\\compiler_optimization\\CallWithinLoop.apk"));//AndroidObjectPoolBefore.apk"));//"C:\\Users\\Ani\\Desktop\\AndroidObjectPool\\bin\\AndroidObjectPool.apk"));//"C:\\Users\\Ani\\Desktop\\AndroidObjectPoolBefore.apk"));//"C:\\Users\\Ani\\Desktop\\new try\\ToyExample.apk"));//"C:\\Users\\Ani\\Desktop\\AndroidObjectPool.apk"));//"C:\\Users\\Ani\\Desktop\\new try\\ToyExample.apk"));//"E:\\Program Analysis Workspace\\test apk repo\\toytest\\ToyExample.apk"));
		Options.v().set_android_jars("C:\\Program Files (x86)\\Android\\android-sdk\\platforms");		
		Options.v().set_whole_program(true);		
		Options.v().set_allow_phantom_refs(true);
		
		
		Options.v().set_soot_classpath(System.getProperty("java.class.path"));
		//System.out.println(System.getProperty("java.class.path"));
		Options.v().set_prepend_classpath(true);
		
		
		Options.v().set_output_dir("E:\\Program Analysis Workspace\\MemoryOptimization\\sootOutput\\");
		Options.v().set_output_format(Options.output_format_jimple);
		
		Options.v().setPhaseOption("cg.spark","on");

		Options.v().set_whole_program(true);
		
		Scene.v().addBasicClass("util.objectpool.BufferPool");
		Scene.v().addBasicClass("util.objectpool.ObjectPool");
		
		Scene.v().loadNecessaryClasses();
	//	soot.PhaseOptions.v().setPhaseOptionIfUnset("cg.spark", "verbose");
	//	soot.PhaseOptions.v().setPhaseOptionIfUnset("cg.spark", "simple-edges-bidirectional");
		//soot.PhaseOptions.v().setPhaseOptionIfUnset("cg.spark", "set-impl");
	//	Options.v().setPhaseOption("jb.tr", "ignore-wrong-staticness:true");
	//	soot.PhaseOptions.v().setPhaseOption("jb.tr", "ignore-wrong-staticness");
	///	Options.v().setPhaseOption(phase, option)
		
		SootMethod entryPoint = app.getEntryPointCreator().createDummyMain();		
		Options.v().set_main_class(entryPoint.getSignature());		
		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));		
		System.out.println(entryPoint.getActiveBody());
		PackManager.v().runPacks();	
		
		System.out.println(Scene.v().getClasses());
		
		SootClass mainActivity_class = null;
		SootClass activity_class = Scene.v().getSootClass("android.app.Activity");
		
		app_classes = new HashSet<SootClass>();
		for( SootClass c: Scene.v().getClasses()){
			String class_name = c.getName();
			
			if(class_name.startsWith("java")|| class_name.startsWith("sun.")||class_name.startsWith("android.")||class_name.startsWith("com.sun.")|| class_name.startsWith("org.") || class_name.startsWith("dummyMain")|| class_name.startsWith("org.xmlpull.")|| class_name.substring(class_name.lastIndexOf(".")+1).startsWith("R$") ||class_name.endsWith(".R") || class_name.endsWith(".BuildConfig") )
				continue;
			else{
				app_classes.add(c);
				//check if it is MainActivity Class
				if(c.hasSuperclass()){
					if(c.getSuperclass().equals(activity_class)){
						mainActivity_class = c;
					}
				}
				
			}	
		}
		System.out.println(app_classes);
		
		if(mainActivity_class  == null){
			return;
		}
		
		
		cg = Scene.v().getCallGraph();
		//SparkTransformer.v().transform(phaseName, options)
		
		pa = Scene.v().getPointsToAnalysis();
		
		result_map = new HashMap<SootMethod, InsUtil>();
		
		Map options = new HashMap();
		PurityOptions opt = new PurityOptions(options);
		
		EscapeInterProc p =
			    new EscapeInterProc(cg, Scene.v().getEntryPoints().iterator(), opt); 
		
		instrument_map = p.getResultMap();
		
		
		 System.out.println("Instrument map is: "+instrument_map);
		
		//instrument Reset methods
		for(SootMethod method : instrument_map.keySet()){
				instrument.Instrumenter ins = new instrument.Instrumenter(instrument_map.get(method).keySet());		//Scene.v().getMethod(method.getSignature()), instrument_map.get(method).keySet());		
		}
		
		
		//instrument buffer field and initialization in MainActivity class
		BuffPoolInstrument ins = new BuffPoolInstrument(mainActivity_class);
	//	k =ins;
		
		newTypeSet = new HashSet<Type>();
		
		for(Map.Entry<SootMethod, InsUtil> e: result_map.entrySet()){
			if(!app_classes.contains(e.getKey().getDeclaringClass()))
				continue;
			InsUtil util = e.getValue();
			if(util == null || util.getSaveToBuf() == null)
				continue;
			for(Local l : util.getSaveToBuf().keySet()){
				newTypeSet.add(l.getType());
			}
			
		//	instrument.ReplaceIns rep = new instrument.ReplaceIns(e.getKey(), util.getNewExprMap(), util.getSaveToBuf());		
		}
		
		for(Map.Entry<SootMethod, InsUtil> e: result_map.entrySet()){
			if(!app_classes.contains(e.getKey().getDeclaringClass()))
				continue;
			InsUtil util = e.getValue();
			if(util.getNewExprMap() == null){
				if(util.getSaveToBuf() == null)
					continue;
				else{
					instrument.ReplaceIns rep = new instrument.ReplaceIns(e.getKey(), null, util.getSaveToBuf());
				}
			}
			else{
				if(util.getSaveToBuf() == null){
					instrument.ReplaceIns rep = new instrument.ReplaceIns(e.getKey(), util.getNewExprMap(), null);
				}
				else{

					instrument.ReplaceIns rep = new instrument.ReplaceIns(e.getKey(), util.getNewExprMap(), util.getSaveToBuf());		
				}
			}
		}
		
		System.out.println(NewTest.methodToLoop_map );
		System.out.println(NewTest.nonEscNewAlloc);
		
	//	BetweenessCentrality btwn = new BetweenessCentrality();
	//	btwn.calcMeasure();
		
		//OptMethodCallInLoop optLoop = new OptMethodCallInLoop();
		//optLoop.init();

	//	System.out.println(Scene.v().getClasses());
	//	PackManager.v().writeClass(Scene.v().getSootClass("android.graphics.Point"));
	//	PackManager.v().writeClass(Scene.v().getSootClass("com.devahead.androidobjectpool.poolobjects.PointPoolObjectFactory"));
	//	PackManager.v().writeClass(Scene.v().getSootClass("java.lang.Object"));
		PackManager.v().writeOutput();
		G.v().out.println("Done!!");
		
	}

	
}