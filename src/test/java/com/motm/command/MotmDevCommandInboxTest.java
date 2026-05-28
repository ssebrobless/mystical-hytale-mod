package com.motm.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MotmDevCommandInboxTest {

    @Test
    void normalizeCommandAcceptsShellAndChatForms() {
        assertEquals("dev observe status", MotmDevCommandInbox.normalizeCommand("motm dev observe status"));
        assertEquals("dev observe status", MotmDevCommandInbox.normalizeCommand("/motm dev observe status"));
        assertEquals("style terra", MotmDevCommandInbox.normalizeCommand("\uFEFF /MOTM style terra "));
        assertEquals("", MotmDevCommandInbox.normalizeCommand(null));
        assertEquals("", MotmDevCommandInbox.normalizeCommand("   "));
    }
}
