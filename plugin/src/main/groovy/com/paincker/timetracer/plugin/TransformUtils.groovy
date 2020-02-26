package com.paincker.timetracer.plugin

import com.android.SdkConstants
import com.android.build.api.transform.*
import javassist.ClassPool
import javassist.CtClass
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile

class TransformUtils {

    interface Callback {
        void process(String className);
    }

    /**
     * 将所有class直接复制到目标文件夹，不做任何操作
     */
    static void copy(Collection<TransformInput> inputs, TransformOutputProvider outputProvider) {
        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput ->
                // input = xxx/build/intermediates/classes/release
                // output = xxx/build/intermediates/transforms/TransformBuildConfig/release/folders/1/1/81a690789a26ea8c4e3e8a94e133cfa9c224f932
                String outputFileName = dirInput.name + '-' + dirInput.file.path.hashCode()
                File output = outputProvider.getContentLocation(outputFileName, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(dirInput.file, output)
//                LogUtils.debug("input = '${dirInput}', output = '${output}'")
            }

            input.jarInputs.each { JarInput jarInput ->
                String outputFileName = jarInput.name.replace(".jar", "") + '-' + jarInput.file.path.hashCode()
                File output = outputProvider.getContentLocation(outputFileName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                FileUtils.copyFile(jarInput.file, output)
//                LogUtils.debug("input = '${jarInput}', output = '${output}'")
            }
        }
    }

    /**
     * 将安卓和inputs加入ClassPool的ClassPath
     */
    static void appendClassPathToPool(ClassPool pool, Project project, Collection<TransformInput> inputs) {
        project.android.bootClasspath.each {
            pool.appendClassPath((String) it.absolutePath)
        }

        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput ->
                pool.appendClassPath(dirInput.file.absolutePath)
            }
            input.jarInputs.each { JarInput jarInput ->
                pool.insertClassPath(jarInput.file.absolutePath)
            }
        }
    }

    /**
     * 遍历所有目录和jar文件中的class
     */
    static void traversalClasses(Collection<TransformInput> inputs, Callback callback) {
        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput ->
                final int start = dirInput.file.absolutePath.length() + 1
                final int end = SdkConstants.DOT_CLASS.length()
                FileUtils.listFiles(dirInput.file, null, true).each { File f ->
                    def filePath = f.absolutePath
                    if (filePath.endsWith(SdkConstants.DOT_CLASS)) {
                        def className = filePath.substring(start, filePath.length() - end).replaceAll('/', '.')
                        callback.process(className)
                    }
                }
            }
            input.jarInputs.each { JarInput jarInput ->
                final int end = SdkConstants.DOT_CLASS.length()
                final JarFile jarFile = new JarFile(jarInput.file)
                final Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.endsWith(SdkConstants.DOT_CLASS)) {
                        def className = name.substring(0, name.length() - end).replaceAll('/', '.')
                        callback.process(className)
                    }
                }
            }
        }
    }

    /**
     * 是否为安卓生成的class
     */
    static boolean isAndroidGeneratedClasses(CtClass ctClass) {
        // com.package.demo.R
        // com.package.demo.R$layout
        // com.package.demo.R$id
        // com.package.demo.BuildConfig
        def cls = ctClass.simpleName
        return cls == 'R' || cls.startsWith('R$') || cls == 'BuildConfig'
    }
}
