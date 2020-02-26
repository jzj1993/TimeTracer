package com.paincker.timetracer.plugin;

public interface Constants {

    String VAR_CLASS_NAME = "<class-name>";
    String VAR_SIMPLE_CLASS_NAME = "<simple-class-name>";
    String VAR_METHOD_NAME = "<method-name>";

    String TRACER_PACKAGE = "com.paincker.timetracer.tracer";
    String DEFAULT_METHOD_START = "com.paincker.timetracer.tracer.TimeTracer.methodStart(\"<class-name>.<method-name>\");";
    String DEFAULT_METHOD_END = "com.paincker.timetracer.tracer.TimeTracer.methodEnd(\"<class-name>.<method-name>\");";
}
