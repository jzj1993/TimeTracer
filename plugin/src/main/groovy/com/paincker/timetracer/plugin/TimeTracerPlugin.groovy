package com.paincker.timetracer.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class TimeTracerPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create('TimeTracer', TimeTracerExtension)
        def android = project.extensions.findByType(AppExtension)
        android.registerTransform(new TimeTracerTransform(project))
        LogUtils.info("plugin applied")
    }
}