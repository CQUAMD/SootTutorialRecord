package org.example.analsys.Android;

import soot.*;
import soot.jimple.InvokeStmt;
import soot.jimple.NewExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.sets.DoublePointsToSet;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.toolkits.scalar.Pair;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AndroidPointsToAnalysis {
    private static String androidJar = "F:\\AndroidSdk\\platforms";
    static String androidDemoPath = System.getProperty("user.dir") + File.separator + "demo" + File.separator + "Android";
    static String apkPath = androidDemoPath + File.separator + "st_demo.apk";
    static String classParentName = "dev.navids.multicomp1.ClassParent";
    static String classChildName = "dev.navids.multicomp1.ClassChild";
    static String intermediaryMethodSignature = "<dev.navids.multicomp1.MyReceiver: void intermediaryMethod()>";

    static boolean isParentChildClassLocal(Local local){
        return local.getType().toString().equals(classParentName) || local.getType().toString().equals(classChildName);
    }

    public static void main(String[] args){
        if(System.getenv().containsKey("ANDROID_HOME"))
            androidJar = System.getenv("ANDROID_HOME")+ File.separator+"platforms";

        SetupApplication app = new SetupApplication(AndroidUtil.getFlowDroidConfig(apkPath, androidJar));
        // By constructing call graph, PointsTo analysis implicitly will be executed
        app.constructCallgraph();
        // 这里解释PointsToAnalysis的含义，
        // 它的工作原理是，对于一个变量，找出它可能指向的所有对象的集合。这个集合被称为变量的 "points-to set"。
        // 例如，假设有一个变量 "x"，它可能指向的对象有 "obj1"、"obj2" 和 "obj3"。则 "x" 的 points-to set 为 { "obj1"、"obj2"、"obj3" }。
        // PointsToAnalysis可以用于进行常量折叠，找出可能存在的内存泄露情况。还可以用于优化代码性能，避免无效指针访问。
        // 可以帮助创建更精确的调用图。还可用于别名分析，即你对发现两个变量是否可能或必须具有相同的值。
        soot.PointsToAnalysis pointsToAnalysis = Scene.v().getPointsToAnalysis();
        SootMethod intermediaryMethod = Scene.v().getMethod(intermediaryMethodSignature);
        for(Local local : intermediaryMethod.getActiveBody().getLocals()){
            // 判断局部变量是否属于ClassParent或ClassChild
            if(isParentChildClassLocal(local)){
                // reachingObjects() 方法获取局部变量的 points-to set，并遍历这个 set 中的所有元素。
                ((DoublePointsToSet)pointsToAnalysis.reachingObjects(local)).getOldSet().forall(new P2SetVisitor() {
                    @Override
                    public void visit(Node n) {
                        AllocNode allocNode = (AllocNode) n;
                        // 查看节点是什么方法
                        SootMethod allocMethod = allocNode.getMethod();
                        // 获取其中的new表达式，以此来查看局部变量是怎么被创建的
                        NewExpr allocExpr = (NewExpr) allocNode.getNewExpr();
                        System.out.println(String.format("Local %s in intermediaryMethod is allocated at method %s through expression: %s", local, allocMethod, allocExpr));
                    }
                });
            }
        }
        System.out.println("----------");

        // Reporting aliases relation among all local values with type ClassParent or ClassChild
        List<Pair<Local, String>> allParentChildLocals = getParentChildClassLocals();
        String header = "\t";
        for(int i=0; i< allParentChildLocals.size(); i++) {
            Matcher matcher = Pattern.compile("([^.]+)$").matcher(allParentChildLocals.get(i).getO1().getType().toString());
            String type = "";
            if (matcher.find()){
                type = matcher.group(1);
            }
            System.out.println(String.format("|Local %d|Type:%s|Expr:%s|", i + 1,type ,allParentChildLocals.get(i).getO2()));
            header += (i+1)+"\t";
        }
        System.out.println("----------");
        System.out.println("Aliases (1 -> the locals on row and column MAY points to the same memory location, 0 -> otherwise)");
        System.out.println(header);

        for(int i=0; i< allParentChildLocals.size(); i++){
            // 获取Pair里的那个Local
            Local leftLocal = allParentChildLocals.get(i).getO1();
            String row = (i+1) + "\t";
            PointsToSet leftSet = pointsToAnalysis.reachingObjects(leftLocal);
            for(int j=0; j< allParentChildLocals.size(); j++) {
                Local rightLocal = allParentChildLocals.get(j).getO1();
                PointsToSet rightSet = pointsToAnalysis.reachingObjects(rightLocal);
                // 检查两个集合是否有重复的内存指向，所以对角线显然是都会为1
                row += (leftSet.hasNonEmptyIntersection(rightSet)? "1" : "0") +"\t";
            }
            System.out.println(row);
        }

    }

    /**
     * 遍历Class里的每个方法里的每个Unit，找到Invoke表达式，然后获取是谁Invoke的，
     * 如果属于ClassParent或ClassChild则放入容器
     * @return 由ClassParent和ClassChild对象生成的局部变量和对应的调用表达式
     */
    public static List<Pair<Local, String>> getParentChildClassLocals() {
        List<Pair<Local, String>> allParentChildLocals = new ArrayList<>();
        for(SootClass sootClass : Scene.v().getApplicationClasses()){
            for(SootMethod sootMethod : sootClass.getMethods()){
                if(!sootMethod.hasActiveBody())
                    continue;
                for(Unit unit : sootMethod.getActiveBody().getUnits()){
                    if (unit instanceof InvokeStmt){
                        InvokeStmt invokeStmt = (InvokeStmt) unit;
                        if(invokeStmt.getInvokeExpr() instanceof VirtualInvokeExpr){
                            VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) invokeStmt.getInvokeExpr();
                            // getBase() 方法返回的是调用表达式中 "." 左边的对象。
                            Local baseLocal = (Local) virtualInvokeExpr.getBase();
                            if(isParentChildClassLocal(baseLocal)){
                                String label = String.format("%s.%s {%s}", sootClass.getShortName(), sootMethod.getName(), virtualInvokeExpr);
                                allParentChildLocals.add(new Pair<>(baseLocal, label));
                            }

                        }
                    }
                }
            }
        }
        return allParentChildLocals;
    }
}
