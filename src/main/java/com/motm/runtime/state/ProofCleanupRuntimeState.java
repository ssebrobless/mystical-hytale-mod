package com.motm.runtime.state;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Owns temporary proof artifacts awaiting cleanup.
 */
public final class ProofCleanupRuntimeState {

    private final Queue<TemporaryProofSelection> selections = new ConcurrentLinkedQueue<>();
    private final Queue<TemporaryProofProxy> proxies = new ConcurrentLinkedQueue<>();

    public void addSelection(TemporaryProofSelection selection) {
        if (selection != null) {
            selections.add(selection);
        }
    }

    public void addProxy(TemporaryProofProxy proxy) {
        if (proxy != null) {
            proxies.add(proxy);
        }
    }

    public List<TemporaryProofSelection> selections() {
        return List.copyOf(selections);
    }

    public List<TemporaryProofProxy> proxies() {
        return List.copyOf(proxies);
    }

    public boolean removeSelection(TemporaryProofSelection selection) {
        return selection != null && selections.remove(selection);
    }

    public boolean removeProxy(TemporaryProofProxy proxy) {
        return proxy != null && proxies.remove(proxy);
    }

    public int selectionCount() {
        return selections.size();
    }

    public int proxyCount() {
        return proxies.size();
    }
}
