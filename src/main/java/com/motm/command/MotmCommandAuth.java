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

    public static String devToolsDisabledMessage(boolean internalTestBuild, String configFileName) {
        if (!internalTestBuild) {
            return "[MOTM] Dev tools are not included in this public release build.\n"
                    + "Use an internal tester build to access /motm dev and live automation commands.";
        }
        return "[MOTM] Dev tools are disabled on this build/server.\n"
                + "To enable them, set dev_tools_enabled=true in "
                + configFileName + " inside the mod data folder and restart Hytale.";
    }
}
