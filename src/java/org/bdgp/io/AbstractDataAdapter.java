package org.bdgp.io;

import org.bdgp.util.*;
import java.util.Vector;

/**
 * An implementation of VisualDataAdapter that provides implementations of
 * the listener code.
 *
 * @see org.bdgp.swing.widget.DataAdapterChooser
 * @see VisualDataAdapter
 */
public abstract class AbstractDataAdapter implements VisualDataAdapter {

    protected Vector listeners = new Vector();

    public void addProgressListener(ProgressListener listener) {
	listeners.addElement(listener);
    }

    public void removeProgressListener(ProgressListener listener) {
	listeners.removeElement(listener);
    }

    public void fireProgressEvent(ProgressEvent e) {
	for(int i=0; i < listeners.size(); i++) {
	    ProgressListener pl = (ProgressListener) listeners.get(i);
	    pl.progressMade(e);
	}
    }

    public String toString() {
	return getName();
    }
}
