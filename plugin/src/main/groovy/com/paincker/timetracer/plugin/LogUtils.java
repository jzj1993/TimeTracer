package com.paincker.timetracer.plugin;


import com.paincker.timetracer.plugin.TimeTracerPlugin;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class LogUtils {

    private static final String PREFIX = "[TimeTracer]    ";
    private static final Logger LOGGER = Logging.getLogger(TimeTracerPlugin.class);

    public static void debug(String msg, Object... args) {
        System.out.println(PREFIX + format(msg, args));
    }

    public static void info(String msg, Object... args) {
        System.out.println(PREFIX + format(msg, args));
    }

    public static void error(String msg, Object... args) {
        System.err.println(PREFIX + format(msg, args));
    }

    private static String format(String msg, Object[] args) {
        return args.length > 0 ? String.format(msg, args) : msg;
    }
}
