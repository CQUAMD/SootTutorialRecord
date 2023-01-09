package org.example.analsys.npAnalysis;

import soot.Local;
import soot.Unit;
import soot.jimple.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import soot.toolkits.scalar.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ForwardFlowAnalysis 是 Soot 中的一个抽象类，它定义了一组方法，用于实现向前流分析（forward flow analysis）。
 * 向前流分析是一种流程分析方法，它按照程序的执行顺序，从头到尾逐个分析程序中的单元（如指令）。
 * 在分析过程中，会从分析的前一个set处继承一些信息，并根据当前的unit和定义的规则进行更新或转移。最后，结果存储在输出的set中，传递给下一个单元。
 */
public class NullPointerAnalysis extends ForwardFlowAnalysis<Unit, NullFlowSet> {

    enum AnalysisMode {
        MUST,
        MAY_P,
        MAY_O
    }
    AnalysisMode analysisMode;
    public Map<Local,String> record;
    public NullPointerAnalysis(DirectedGraph graph, AnalysisMode analysisMode) {
        super(graph);
        this.analysisMode = analysisMode;
        this.record = new HashMap<>();
        // 进行向前流分析继承自ForwardFlowAnalysis
        doAnalysis();
    }


    public AnalysisMode getAnalysisMode(){
        return this.analysisMode;
    }

    /**
     * 从出口进来，对Unit进行处理，根据规则产生合适的出口集合
     * @param inSet 入口
     * @param unit 某个代码语句
     * @param outSet 出口
     */
    @Override
    protected void flowThrough(NullFlowSet inSet, Unit unit, NullFlowSet outSet) {
        // 将inSet传递给outSet
        inSet.copy(outSet);
        // 根据unit，在outSet中删除assign unit左边的操作数。
        // 因为我们需要根据generate中定义的规则，添加左操作数
        kill(inSet, unit, outSet);
        // 根据规则，将unit的信息放入outSet
        Pair<Local,String> temp = generate(inSet, unit, outSet);
        this.record.put(temp.getO1(),temp.getO2());
    }

    @Override
    protected NullFlowSet newInitialFlow() {
        return new NullFlowSet();
    }


    @Override
    protected void merge(NullFlowSet inSet1, NullFlowSet inSet2, NullFlowSet outSet) {
        System.out.println("--------------------------------now merge------------------------------------");
        if(analysisMode != AnalysisMode.MUST)
            inSet1.union(inSet2, outSet);
        else
            inSet1.intersection(inSet2, outSet);
    }

    @Override
    protected void copy(NullFlowSet source, NullFlowSet dest) {
        source.copy(dest);
    }

    /**
     * 根据unit对输出进行删除，去掉assign的unit中的等号左边的Local。
     * @param inSet 输入，这里并没用到
     * @param unit
     * @param outSet 输出集合
     */
    protected void kill(NullFlowSet inSet, Unit unit, NullFlowSet outSet){
        unit.apply(new AbstractStmtSwitch() {
            @Override
            public void caseAssignStmt(AssignStmt stmt) {
                Local leftOp = (Local) stmt.getLeftOp();
                outSet.remove(leftOp);
            }
        });
    }

    /**
     * 根据特定规则、输入，生成输出集合
     * @param inSet 输入
     * @param unit
     * @param outSet 输出
     */
    protected Pair<Local,String> generate(NullFlowSet inSet, Unit unit, NullFlowSet outSet){
        final String[] reason = {"no case crash"};
        Pair<Local,String> res = new Pair<>();
        final Local[] locals = {null};
        unit.apply(new AbstractStmtSwitch() {
            /*
             * AssignStmt和IdentityStmt的区别是：
             *      IdentityStmt将特殊值，如参数、this或被捕获的异常，分配给一个Local
             *      AssignStmt是所有 “正常 “的赋值，例如从一个Local到另一个Local，或者从一个Constant到一个Local，都是用AssignStmt表示的。
             *
             */
            @Override
            public void caseAssignStmt(AssignStmt stmt) {
                Local leftOp = (Local) stmt.getLeftOp();
                stmt.getRightOp().apply(new AbstractJimpleValueSwitch() {
                    /*
                     * 如果assign的右操作数是局部变量，且包含在inSet里，那么添加左操作进OutSet里
                     */
                    @Override
                    public void caseLocal(Local v) {
                        if (inSet.contains(v)){
                            outSet.add(leftOp);
                            reason[0] = "assign->local";
                            locals[0] = leftOp;
                        }

                    }

                    /*
                     * 如果assign的右操作数是null，那么添加左操作数进OutSet里
                     */
                    @Override
                    public void caseNullConstant(NullConstant v) {
                        outSet.add(leftOp);
                        reason[0] = "assign->null";
                        locals[0] = leftOp;
                    }

                    /*
                     * 如果assign的右操作数是接口的表达式的话，在analysisMode=MAY.P模式下，那么添加左操作进OutSet里
                     *
                     */
                    @Override
                    public void caseInterfaceInvokeExpr(InterfaceInvokeExpr v) {
                        if(analysisMode == AnalysisMode.MAY_P){
                            outSet.add(leftOp);
                            reason[0] = "assign->interfaceInvoke";
                            locals[0] = leftOp;
                        }

                    }

                    @Override
                    public void caseStaticInvokeExpr(StaticInvokeExpr v) {
                        if(analysisMode == AnalysisMode.MAY_P){
                            outSet.add(leftOp);
                            reason[0] = "assign->interfaceInvoke";
                            locals[0] = leftOp;
                        }

                    }

                    @Override
                    public void caseVirtualInvokeExpr(VirtualInvokeExpr v) {
                        if(analysisMode == AnalysisMode.MAY_P){
                            outSet.add(leftOp);
                            reason[0] = "assign->virtualInvoke";
                            locals[0] = leftOp;
                        }

                    }
                });
            }

            /*
             *
             * 之所以这么写是因为像参数这种，可能会传递一个NULL，所以只要不是this指针都是有可能的
             */
            @Override
            public void caseIdentityStmt(IdentityStmt stmt) {

                Local leftOp = (Local) stmt.getLeftOp();
                if(analysisMode == AnalysisMode.MAY_P)
                    if(!(stmt.getRightOp() instanceof ThisRef)){
                        outSet.add(leftOp);
                        reason[0] = "identity";
                        locals[0] = leftOp;
                    }

            }
        });
        res.setPair(locals[0],reason[0] );
        return res;
    }


}