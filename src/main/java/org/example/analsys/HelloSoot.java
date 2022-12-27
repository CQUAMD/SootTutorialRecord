package org.example.analsys;

import org.example.visual.Visualizer;
import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.internal.JIfStmt;
import soot.options.Options;
import soot.toolkits.graph.ClassicCompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;
import java.io.File;

public class HelloSoot {

    public static String sourceDirectory = System.getProperty("user.dir") + File.separator + "demo" + File.separator + "HelloSoot";
    public static String clsName = "FizzBuzz";
    public static String methodName = "printFizzBuzz";

    public static void setupSoot() {
        G.reset();
        Options.v().set_prepend_classpath(true);//与其用命令行上给出的 classpath 替换默认的 soot classpath， 不如用该 classpath 来预置它。如果开启whole-program模式，还会添加jce.jar
        //Options.v().set_allow_phantom_refs(true);允许出现找不到引用的class，但是仍可以处理当前class。不推荐的原因是因为它开启后可能会导致结果是错误的。
        Options.v().set_soot_classpath(sourceDirectory);//用于搜寻class
        SootClass sc = Scene.v().loadClassAndSupport(clsName);//加载类
        sc.setApplicationClass();//将这个类作为Application
        Scene.v().loadNecessaryClasses();//加载soot需要的classes，包括那些在命令行中指定的class。这是初始化soot应该使用的classes list的标准方法.

    }

    public static void main(String[] args) {
        setupSoot();

        // Retrieve printFizzBuzz's body
        SootClass mainClass = Scene.v().getSootClass(clsName);
        SootMethod sm = mainClass.getMethodByName(methodName);
        JimpleBody body = (JimpleBody) sm.retrieveActiveBody();

        // Print some information about printFizzBuzz
        System.out.println("Method Signature: " + sm.getSignature());
        System.out.println("--------------");
        System.out.println("Argument(s):");
        for (Local l : body.getParameterLocals()) {
            System.out.println(l.getName() + " : " + l.getType());
        }
        System.out.println("--------------");
        System.out.println("This: " + body.getThisLocal());
        System.out.println("--------------");
        System.out.println("Units:");
        int c = 1;
        for (Unit u : body.getUnits()) {
            System.out.println("(" + c + ") " + u.toString());
            c++;
        }
        System.out.println("--------------");

        // Print statements that have branch conditions
        System.out.println("Branch Statements:");
        for (Unit u : body.getUnits()) {
            if (u instanceof JIfStmt)
                System.out.println(u.toString());
        }

        // Draw the control-flow graph of the method if 'draw' is provided in arguments
        boolean drawGraph = true;
//        if (args.length > 0 && args[0].equals("draw"))
//            drawGraph = true;
        if (drawGraph) {
            UnitGraph ug = new ClassicCompleteUnitGraph(sm.getActiveBody());
            Visualizer.v().addUnitGraph(ug);
            Visualizer.v().draw();
        }
    }
}