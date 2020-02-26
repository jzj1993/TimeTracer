package com.paincker.timetracer.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.*
import javassist.bytecode.AccessFlag
import org.gradle.api.Project

import java.util.concurrent.ForkJoinPool

class TimeTracerTransform extends Transform {

    Project project
    TimeTracerExtension ext = null

    TimeTracerTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "TimeTracer"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs,
                   TransformOutputProvider outputProvider, boolean isIncremental)
            throws IOException, TransformException, InterruptedException {

        if (ext == null) {
            ext = project.extensions.TimeTracer
            LogUtils.info("ext = ${ext.processClassesRegex},  ${ext.codeBeforeMethod} , ${ext.codeAfterMethod}")
        }

        if (ext == null || !ext.enable ||
                (isEmpty(ext.codeBeforeMethod) && isEmpty(ext.codeAfterMethod))) {
            LogUtils.info("plugin disabled, copy all files")
            TransformUtils.copy(inputs, outputProvider)
            return
        }

        ClassPool pool = ClassPool.getDefault()
        TransformUtils.appendClassPathToPool(pool, project, inputs)

        outputProvider.deleteAll()
        File outDir = outputProvider.getContentLocation("main", outputTypes, scopes, Format.DIRECTORY)

        ArrayList<CtClass> classes = new ArrayList<>()
        TransformUtils.traversalClasses(inputs, new TransformUtils.Callback() {
            @Override
            void process(String className) {
                addCtClass(classes, pool, className)
            }
        })

        new ForkJoinPool().submit {
            classes.each { ctClass ->
                processClass(ctClass, outDir.absolutePath)
            }
        }.get()
    }

    private static void addCtClass(List<CtClass> classes, ClassPool pool, String className) {
        try {
//            LogUtils.debug("add class '${className}'")
            CtClass c = pool.getCtClass(className)
            classes.add(c)
        } catch (NotFoundException e) {
            LogUtils.error("can not find class '${className}'!\n${e.getMessage()}")
        }
    }

    private void processClass(CtClass c, String outDir) {
        try {
            if (!acceptCtClass(c)) {
                if (ext.enableClassLog) {
                    LogUtils.info("ignore class '${c.name}'")
                }
                c.writeFile(outDir)
                c.detach()
                return;
            }
            if (ext.enableClassLog) {
                LogUtils.info("process class '${c.name}'")
            }
            if (c.isFrozen()) {
                c.defrost()
            }
            // 所有方法和构造函数
            c.declaredBehaviors.findAll { CtBehavior behavior ->
                return acceptCtBehavior(behavior)
            }.each { CtBehavior method ->
                if (ext.enableClassLog && ext.enableMethodLog) {
                    LogUtils.info("\tprocess method '${method.name}'")
                }
                String before = replaceVar(ext.codeBeforeMethod, c, method)
                String after = replaceVar(ext.codeAfterMethod, c, method)
                if (!isEmpty(before)) {
                    method.insertBefore(before)
                }
                if (!isEmpty(after)) {
                    method.insertAfter(after)
                }
            }
            c.writeFile(outDir)
            c.detach()
        } catch (CannotCompileException e) {
            LogUtils.error("can not compile code ! \n${e.getMessage()}")
        } catch (Exception e) {
            LogUtils.error("process class '${c.name}' failed!")
            if (ext.enableStackLog) {
                e.printStackTrace()
            }
        }
    }

    private boolean acceptCtClass(CtClass ctClass) {
        if (ctClass.isInterface()) {
            return false
        }
        // 不处理Android生成的类
        if (TransformUtils.isAndroidGeneratedClasses(ctClass)) {
            return false
        }
        // 不处理Tracer自身所在的包。否则会导致App无法运行。
        if (ctClass.name.startsWith(Constants.TRACER_PACKAGE)) {
            return false
        }
        if (ext.processClassesRegex != null) {
            return ctClass.name.matches(ext.processClassesRegex);
        }
        // 默认：处理所有类
        return true
    }

    private boolean acceptCtBehavior(CtBehavior it) {
        if (it.getMethodInfo().isStaticInitializer()) {
            return false
        }
        if (it.isEmpty()) {
            return false
        }
        // 跳过synthetic方法，例如AsyncTask会生成同名synthetic方法
        if ((it.getModifiers() & AccessFlag.SYNTHETIC) != 0) {
            return false
        }
        if ((it.getModifiers() & AccessFlag.ABSTRACT) != 0) {
            return false
        }
        if ((it.getModifiers() & AccessFlag.NATIVE) != 0) {
            return false
        }
        if ((it.getModifiers() & AccessFlag.INTERFACE) != 0) {
            return false
        }
        if (ext.skipConstructor && it.methodInfo.isConstructor()) { // 跳过构造函数
            return false
        }
        if (it instanceof CtConstructor && JavassistUtils.isEmptyConstructor((CtConstructor) it)) {
            // 跳过空构造函数
            return false
        }
        if (ext.skipStaticMethod && (it.getModifiers() & AccessFlag.STATIC) != 0) { // 跳过静态方法
            return false
        }
        if (ext.skipSimpleMethod && it instanceof CtMethod && isSimpleMethod((CtMethod) it)) {
            // 跳过简单方法
            return false
        }
        return true
    }

    private static String replaceVar(String s, CtClass c, CtBehavior m) {
        if (s == null || s.length() == 0) {
            return null
        }
        return s.replace(Constants.VAR_CLASS_NAME, c.name)
                .replace(Constants.VAR_SIMPLE_CLASS_NAME, c.simpleName)
                .replace(Constants.VAR_METHOD_NAME, m.name)
    }

    private boolean isSimpleMethod(CtMethod method) {
        return JavassistUtils.getMethodLength(method) < Math.max(ext.simpleMethodLength, JavassistUtils.EMPTY_METHOD_LENGTH)
    }

    private static boolean isEmpty(CharSequence s) {
        return s == null || s.length() == 0
    }
}