package com.motm.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MotmCommandAuthTest {

    @Test
    void publicBuildMessagePointsToInternalTesterBuild() {
        String message = MotmCommandAuth.devToolsDisabledMessage(false, "motm-server.json");

        assertTrue(message.contains("public release build"));
        assertTrue(message.contains("internal tester build"));
    }

    @Test
    void internalBuildMessagePointsToServerConfig() {
        String message = MotmCommandAuth.devToolsDisabledMessage(true, "motm-server.json");

        assertTrue(message.contains("dev_tools_enabled=true"));
        assertTrue(message.contains("motm-server.json"));
    }
}
