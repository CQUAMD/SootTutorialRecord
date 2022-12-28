package org.example.visual;

import org.example.analsys.Android.AndroidUtil;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.List;

public class AndroidCallGraphFilter implements CallGraphFilter{
    public List<SootClass> getValidClasses() {
        return validClasses;
    }

    private List<SootClass> validClasses = new ArrayList<>();

    /**
     *
     * @param appPackageName app的package 名称
     * 用于过滤R.class和BuildConfig，所以这里可以自定义一些规则实现class过滤
     */
    public AndroidCallGraphFilter(String appPackageName) {
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (!sootClass.getName().contains(appPackageName))
                continue;
            if (sootClass.getName().contains(appPackageName + ".R") || sootClass.getName().contains(appPackageName + ".BuildConfig"))
                continue;
            validClasses.add(sootClass);
        }
    }

    private boolean isValidMethod(SootMethod sootMethod){
        if(AndroidUtil.isAndroidMethod(sootMethod))
            return false;
        if(sootMethod.getDeclaringClass().getPackageName().startsWith("java"))
            return false;
        if(sootMethod.toString().contains("<init>") || sootMethod.toString().contains("<clinit>"))
            return false;
        if(sootMethod.getName().equals("dummyMainMethod"))
            return false;
        return true;
    }

    @Override
    public boolean isValidEdge(soot.jimple.toolkits.callgraph.Edge sEdge) {
        if(!sEdge.src().getDeclaringClass().isApplicationClass())// || sEdge.tgt().getDeclaringClass().isApplicationClass())
            return false;
        if(!isValidMethod(sEdge.src()) || !isValidMethod(sEdge.tgt()))
            return false;
        boolean flag = validClasses.contains(sEdge.src().getDeclaringClass());
        flag |= validClasses.contains(sEdge.tgt().getDeclaringClass());
        return flag;
    }
}
