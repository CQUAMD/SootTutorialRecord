package org.example.analsys.npAnalysis;

import soot.*;
import soot.jimple.InvokeStmt;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.toolkits.graph.TrapUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 可能存在空指针错误的一个分析程序
 */
public class NPAMain {
    public static String sourceDirectory = System.getProperty("user.dir") + File.separator + "demo" + File.separator + "IntraAnalysis";
    public static String clsName = "NullPointerExample";

    public static void setupSoot() {
        G.reset();
        Options.v().set_soot_classpath(sourceDirectory);
        Options.v().set_prepend_classpath(true);
        // 设置是否保留行偏移量信息。行偏移量是指字节码文件中每行的起始位置相对于文件头的偏移量。保留行偏移量信息可以帮助你在调试或分析程序时定位代码位置。
        Options.v().set_keep_line_number(true);
        // 设置是否保留行号信息。行号是指字节码文件中每行的顺序编号。保留行号信息可以帮助你在调试或分析程序时更方便地查看代码。
        // 可以使用偏移和行号信息在代码中标记断点，或者快速定位代码位置等。但是会增加Soot运行时的开销。
        Options.v().set_keep_offset(true);
        SootClass sc = Scene.v().loadClassAndSupport(clsName);
        sc.setApplicationClass();
        Scene.v().loadNecessaryClasses();
    }


    public static void main(String[] args) {
        setupSoot();
        SootClass mainClass = Scene.v().getSootClass(clsName);
        for (SootMethod sm : mainClass.getMethods()) {
            System.out.println("Method: " + sm.getSignature());
            JimpleBody body = (JimpleBody) sm.retrieveActiveBody();
            /*
             * ExceptionalUnitGraph：表示方法体中的单元流图，包括普通流和异常流。。
             * SimpleLocalDefs：表示方法体中的单元流图，不包括异常流。
             * BriefUnitGraph：表示方法体中的单元流图，简化版本，只保留主要的控制流信息。
             * TrapUnitGraph：仅分析catch块内部的流信息 可以帮助你分析方法中的 catch 块内部的流程，不包括 catch 块外部的流信息。
             */
            UnitGraph unitGraph = new TrapUnitGraph(body);
            List<NullPointerAnalysis> npAnalyzers = new ArrayList<>();
            // 首先会通过NullPointerAnalysis构造函数进行flow分析，产生一个outSet
            // outSet放入npAnalyzers
            npAnalyzers.add(new NullPointerAnalysis(unitGraph, NullPointerAnalysis.AnalysisMode.MUST));
            npAnalyzers.add(new NullPointerAnalysis(unitGraph, NullPointerAnalysis.AnalysisMode.MAY_O));
            npAnalyzers.add(new NullPointerAnalysis(unitGraph, NullPointerAnalysis.AnalysisMode.MAY_P));
            int c = 0;
            for(Unit unit : body.getUnits()){
                c++;
                // Box类似于指针
                // useBox是指那种装着使用值的Box
                // 使用值，举个例子Unit：x=y+z，y和z都是使用值，
                for(ValueBox usedValueBox : unit.getUseBoxes()){
                    if(usedValueBox.getValue() instanceof Local){
                        Local usedLocal = (Local) usedValueBox.getValue();
                        // 下面的逻辑就是，根据NullPointerAnalysis，我们其实获得了一个可能或者一定为空指针的Local集合
                        // 现在我从NullPointerAnalysis中的集合找到和Unit的相关的local集合，如果这个unit使用的local包含在里面
                        // 这个unit是可能或者一定存在空指针的
                        for(NullPointerAnalysis npa: npAnalyzers){
                            if(npa.getFlowBefore(unit).contains(usedLocal)){
                                System.out.println("    Line " + unit.getJavaSourceStartLineNumber() +": " + npa.analysisMode + " NullPointer usage of local " + usedLocal + " in unit " + unit);
                                if(npa.record.containsKey(usedLocal)){
                                    String reason = npa.record.get(usedLocal);
                                    System.out.println(String.format("\t\tunit:%s\n\t\treason:%s\n\t\tmode:%s", unit,reason,npa.getAnalysisMode()));
                                }


                            }
                        }
                    }
                    if(unit instanceof InvokeStmt && usedValueBox.getValue().getType().equals(NullType.v())){
                        System.out.println("    Line " + unit.getJavaSourceStartLineNumber() +": MUST NullPointer usage in unit (" + c +") " + unit);
                    }
                }
            }
            System.out.println("###########################################################################################");

        }
    }
}
