package org.bdgp.io;

/**
 * A Generic starter interface for DataAdapter components. This interface
 * does NOT contain any methods for actual data manipulation. Those methods
 * should be determined by the application that will use the data adapter.
 * Most applications will define another interface that extends DataAdapter
 * and specifies the application-specific data manipulation methods.
 *
 * @see AbstractDataAdapter
 * @see VisualDataAdapter
 * @see org.bdgp.swing.widget.DataAdapterChooser
 */
public interface DataAdapter {
    /**
     * Returns the name of this data adapter
     */
    public String getName();

    /**
     * Returns the type of data handled by this data adapter
     */
    public String getType();

    /**
     * Returns a list of all operations supported by this data adapter
     */
    public IOOperation [] getSupportedOperations();

    /**
     * For initialization, like opening files or database connections.
     */
    public void init();
}
