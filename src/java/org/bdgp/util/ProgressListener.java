package org.bdgp.util;

/**
 * Listener for ProgressEvents.
 */
public interface ProgressListener extends java.util.EventListener {
    /**
     * Fired when some sort of progress has been made on the object
     * to which we are listening
     */
    public void progressMade(ProgressEvent e);
}
