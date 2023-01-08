package org.example.analsys.usageFinder;

import soot.*;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeStmt;
import soot.jimple.JimpleBody;
import soot.options.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class UsageFinder {
    public static String sourceDirectory = System.getProperty("user.dir") + File.separator + "demo" + File.separator + "IntraAnalysis";
    public static String clsName = "UsageExample";

    public static String usageMethodSubsignature = "void println(java.lang.String)";

    public static String usageClassSignature = "java.io.PrintStream";

    public static void setupSoot() {
        G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_soot_classpath(sourceDirectory);
        SootClass sc = Scene.v().loadClassAndSupport(clsName);
        sc.setApplicationClass();
        Scene.v().loadNecessaryClasses();
    }

    /**
     *
     * @param u 某一句代码
     * @param methodSubsignature 调用的类的方法名
     * @param classSignature 调用的类的类名
     * @return 这句代码是否包含了这个调用
     */
    public static boolean doesInvokeTheMethod(Unit u, String methodSubsignature, String classSignature) {
        // AtomicBoolean是线程安全的，同时它也有很多可以修改Boolean值的方法
        AtomicBoolean result = new AtomicBoolean(false);
        u.apply(new AbstractStmtSwitch() {
            @Override
            public void caseInvokeStmt(InvokeStmt invokeStmt) {
                // getMethod返回的是调用的方法，也就是“.”右边的方法。
                String invokedSubsignature = invokeStmt.getInvokeExpr().getMethod().getSubSignature();
                // getDeclaringClass获取method的声明类
                String invokedClassSignature = invokeStmt.getInvokeExpr().getMethod().getDeclaringClass().getName();
                if (invokedSubsignature.equals(methodSubsignature)) {
                    if (classSignature == null || invokedClassSignature.equals(classSignature)) {
                        result.set(true);
                    }
                }

            }
        });
        return result.get();
    }

    public static void main(String[] args) {
        setupSoot();
        String classMessage = " of the class " + usageClassSignature;
        System.out.println("Searching the usages of method " + usageMethodSubsignature + classMessage + "...");
        SootClass mainClass = Scene.v().getSootClass(clsName);
        for (SootMethod sm : mainClass.getMethods()) {
            JimpleBody body = (JimpleBody) sm.retrieveActiveBody();
            List<Unit> usageFound = new ArrayList<>();
            for (Iterator<Unit> it = body.getUnits().snapshotIterator(); it.hasNext(); ) {
                Unit u = it.next();
                if (doesInvokeTheMethod(u, usageMethodSubsignature, usageClassSignature))
                    usageFound.add(u);
            }
            if (usageFound.size() > 0) {
                System.out.println(usageFound.size() + " Usage(s) found in the method " + sm.getSignature());
                for (Unit u : usageFound) {
                    System.out.println("   " + u.toString());
                }
            }

        }
    }
}
