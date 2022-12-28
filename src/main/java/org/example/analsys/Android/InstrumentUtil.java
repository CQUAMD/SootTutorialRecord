package org.example.analsys.Android;

import soot.*;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.*;
import soot.options.Options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstrumentUtil {
    public static final String TAG = "<SOOT_TUTORIAL>";

    public static void setupSoot(String androidJar, String apkPath, String outputPath) {
        G.reset();
        Options.v().set_allow_phantom_refs(true);//允许出现找不到引用的class，但是仍可以处理当前class。不推荐的原因是因为它开启后可能会导致结果是错误的。
        Options.v().set_whole_program(true);//分析整个程序
        Options.v().set_prepend_classpath(true);//与其用命令行上给出的 classpath 替换默认的 soot classpath， 不如用该 classpath 来预置它。如果开启whole-program模式，还会添加jce.jar


        // Read (APK Dex-to-Jimple) Options
        Options.v().set_android_jars(androidJar);// The path to Android Platforms
        Options.v().set_src_prec(Options.src_prec_apk);// Determine the input is an APK
        Options.v().set_process_dir(Collections.singletonList(apkPath));// Provide paths to the APK

        Options.v().set_process_multiple_dex(true);// Inform Dexpler that the APK may have more than one .dex files
        Options.v().set_include_all(true);

        // Write (APK Generation) Options
        Options.v().set_output_format(Options.output_format_dex);
        Options.v().set_validate(true);//Validate Jimple bodies in each transofrmation pack
        Options.v().set_output_dir(outputPath);

        // Resolve required classes
        //添加这些类使得apk jimple插入代码的时候能找得到
        Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);
        Scene.v().loadNecessaryClasses();
    }

    public static List<Unit> generateLogStmts(JimpleBody b, String msg) {
        return generateLogStmts(b, msg, null);
    }

    /**
     *
     * @param b JimpleBody
     * @param msg 消息的format
     * @param value format里的%s指代的消息
     * @return 拼接后的打印消息的Jimple表达式
     */
    public static List<Unit> generateLogStmts(JimpleBody b, String msg, Value value) {
        List<Unit> generated = new ArrayList<>();
        //声明一个String constant，也就是要打印的format
        Value logMessage = StringConstant.v(msg);
        //因为是打印的类型，有点类似于Android log print里面的TAG
        Value logType = StringConstant.v(TAG);
        Value logMsg = logMessage;
        if (value != null)
            // value不为空就需要进行拼接
            logMsg = InstrumentUtil.appendTwoStrings(b, logMessage, value, generated);
        SootMethod sm = Scene.v().getMethod("<android.util.Log: int i(java.lang.String,java.lang.String)>");
        // android的log需要tag和message两个参数
        StaticInvokeExpr invokeExpr = Jimple.v().newStaticInvokeExpr(sm.makeRef(), logType, logMsg);
        generated.add(Jimple.v().newInvokeStmt(invokeExpr));
        return generated;
    }

    /**
     *
     * @param b Jimple body
     * @param s1 待拼接的字符串1
     * @param s2 待拼接的字符串2
     * @param generated 装载Jimple的容器
     * @return
     */
    private static Local appendTwoStrings(Body b, Value s1, Value s2, List<Unit> generated) {
        RefType stringType = Scene.v().getSootClass("java.lang.String").getType();
        SootClass builderClass = Scene.v().getSootClass("java.lang.StringBuilder");
        RefType builderType = builderClass.getType();
        //由StringBuilder类型创建的new表达式
        NewExpr newBuilderExpr = Jimple.v().newNewExpr(builderType);
        //创建一个StringBuilder的局部变量
        Local builderLocal = generateNewLocal(b, builderType);
        // 类似于StringBuilder local = new StringBuilder()
        generated.add(Jimple.v().newAssignStmt(builderLocal, newBuilderExpr));
        Local tmpLocal = generateNewLocal(b, builderType);
        Local resultLocal = generateNewLocal(b, stringType);
        // builderLocal.append(toString(s2))
        VirtualInvokeExpr appendExpr = Jimple.v().newVirtualInvokeExpr(builderLocal,
                builderClass.getMethod("java.lang.StringBuilder append(java.lang.String)").makeRef(), toString(b, s2, generated));
        VirtualInvokeExpr toStrExpr = Jimple.v().newVirtualInvokeExpr(builderLocal, builderClass.getMethod("java.lang.String toString()").makeRef());
        // 等效于类似于StringBuilder builderLocal = StringBuilder(s1)
        generated.add(Jimple.v().newInvokeStmt(
                Jimple.v().newSpecialInvokeExpr(builderLocal, builderClass.getMethod("void <init>(java.lang.String)").makeRef(), s1)));
        //tmpLocal = builderLocal.append(toString(s2))
        generated.add(Jimple.v().newAssignStmt(tmpLocal, appendExpr));
        // String resultLocal = tmpLocal.toString()
        generated.add(Jimple.v().newAssignStmt(resultLocal, toStrExpr));

        return resultLocal;
    }

    /**
     *
     * @param b Jimple body
     * @param value toString的主体
     * @param generated 装载Jimple的容器，因为可能存在说需要value toString的Jimple代码
     * @return
     */
    public static Value toString(Body b, Value value, List<Unit> generated) {
        SootClass stringClass = Scene.v().getSootClass("java.lang.String");
        // 本身就是String类型就不管
        if (value.getType().equals(stringClass.getType()))
            return value;
        Type type = value.getType();
        // PrimType指代 all types except void, null, reference types, and array types
        // 如果是PrimType就创建valueOf(int)类似的Jimple语句
        if (type instanceof PrimType) {
            Local tmpLocal = generateNewLocal(b, stringClass.getType());
            generated.add(Jimple.v().newAssignStmt(tmpLocal,
                    Jimple.v().newStaticInvokeExpr(stringClass.getMethod("java.lang.String valueOf(" + type.toString() + ")").makeRef(), value)));
            return tmpLocal;
        } else if (value instanceof Local){
            // 如果是局部变量那就调用value.toString()获取他的String类似
            Local base = (Local) value;
            SootMethod toStrMethod = Scene.v().getSootClass("java.lang.Object").getMethod("java.lang.String toString()");
            Local tmpLocal = generateNewLocal(b, stringClass.getType());
            generated.add(Jimple.v().newAssignStmt(tmpLocal,
                    Jimple.v().newVirtualInvokeExpr(base, toStrMethod.makeRef())));
            return tmpLocal;
        }
        else{
            throw new RuntimeException(String.format("The value %s should be primitive or local but it's %s", value, value.getType()));
        }
    }

    public static Local generateNewLocal(Body body, Type type) {
        LocalGenerator lg = new LocalGenerator(body);
        return lg.generateLocal(type);
    }
}
