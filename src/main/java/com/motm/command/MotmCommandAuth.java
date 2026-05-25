package com.motm.command;

/**
 * Named authorization boundary for command surfaces. The mod still owns config
 * loading, but commands and runtime entry points should ask this class to make
 * intent explicit when a command is test-only or build-gated.
 */
public final class MotmCommandAuth {

    private MotmCommandAuth() {
    }

    public static boolean canUseDevTools(boolean internalTestBuild, boolean serverEnabled) {
        return internalTestBuild && serverEnabled;
    }

    public static String deniedMessage(boolean allowed, String disabledMessage) {
        return allowed ? null : disabledMessage;
    }
}
