package com.paincker.timetracer.plugin

import javassist.CtBehavior
import javassist.CtConstructor
import javassist.CtMethod
import javassist.bytecode.CodeAttribute
import javassist.bytecode.LineNumberAttribute

class JavassistUtils {

    public static final int EMPTY_METHOD_LENGTH = 2
    public static final int EMPTY_CONSTRUCTOR_LENGTH = 4

    static int getBehaviourLength(CtBehavior behavior) {
        CodeAttribute ca = behavior.getMethodInfo().getCodeAttribute()
        if (ca == null) return -1
        LineNumberAttribute info = (LineNumberAttribute) ca.getAttribute(LineNumberAttribute.tag)
        if (info == null) return -1
        return info.tableLength()
    }

    static int getConstructorLength(CtConstructor constructor) {
        return getBehaviourLength(constructor)
    }

    static int getMethodLength(CtMethod method) {
        return getBehaviourLength(method)
    }

    static boolean isEmptyMethod(CtMethod method) {
        return getMethodLength(method) < EMPTY_METHOD_LENGTH
    }

    static boolean isEmptyConstructor(CtConstructor constructor) {
        return getConstructorLength(constructor) < EMPTY_CONSTRUCTOR_LENGTH
    }
}
