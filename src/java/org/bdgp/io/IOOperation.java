package org.bdgp.io;

/**
 * An input/output operation. This class is used to represent ANY operation
 * that a data adapter might perform. This could be a common operation like
 * "read" or "write", or it could be something very specific to a particular
 * data source, like "reindex" or "page all database users"
 * @see DataAdapter
 * @see org.bdgp.swing.widget.DataAdapterChooser
 */
public class IOOperation {
    /**
     * A predefined IOOperation for "read"
     */
    public static final IOOperation READ = new IOOperation("read");

    /**
     * A predifined IOOperation for "write"
     */
    public static final IOOperation WRITE = new IOOperation("write");

    protected String name;

    /**
     * Creates a named IOOperation
     */
    public IOOperation(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this IOOperation
     */
    public String getName() {
	return name;
    }

    /**
     * Indicates whether this IOOperation equals another. IOOperations
     * are only equal if they are the IDENTICAL OBJECT. Two IOOperations
     * with the same name are not equal unless they are actually the same
     * object.
     */
    public boolean equals(Object o) {
	return this == o;
    }
}
