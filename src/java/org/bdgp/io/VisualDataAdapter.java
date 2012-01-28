package org.bdgp.io;

import org.bdgp.util.*;

/**
 * A data adapter that provides some sort of user interface. This user
 * interface does not necessarily need to be a gui component. It can be
 * any object that extends the DataAdapterUI interface.
 *
 * @see org.bdgp.swing.widget.DataAdapterChooser
 * @see AbstractDataAdapter
 */
public interface VisualDataAdapter extends DataAdapter {

    /**
     * Adds a progress listener to this adapter.
     */
    public void addProgressListener(ProgressListener listener);

    /**
     * Remove a progress listener to this adapter.
     */
    public void removeProgressListener(ProgressListener listener);

    /**
     * Fires a progress event. This method will usually be called to
     * indicate that the data adapter has made progress in performing
     * some IOOperation.
     */
    public void fireProgressEvent(ProgressEvent e);

    /**
     * Returns a user interface for the requested IOOperation. Most
     * VisualDataAdapters will return a different user interface for each
     * supported IOOperation.
     */
    public DataAdapterUI getUI(IOOperation op);
}
