package org.bdgp.io;

import java.util.Vector;

/**
 * A registry of data adapters. This class allows data adapters to be
 * dynamically loaded and managed at runtime.
 *
 * The DataAdapterRegistry can provide the name, type, supported operations,
 * etc. for a dynamically loaded DataAdapter. In order to get this information,
 * the data adapter must be instantiated immediately after the class is loaded.
 * Therefore, all data adapters that will be loaded by the registry MUST
 * provide a zero-argument constructor. Otherwise the installation of the
 * adapter will fail.
 */
public class DataAdapterRegistry {

    protected Vector dataAdapters;

    /**
     * Creates a new DataAdapterRegistry
     */
    public DataAdapterRegistry() {
	dataAdapters = new Vector();
    }

    /**
     * Returns the class names of all installed adapters that support the
     * given IOOperation. If visualOnly is true, only data adapters that
     * implement the VisualDataAdapter interface will be returned.
     */
    public DataAdapter [] getAdapters(IOOperation op, boolean visualOnly) {
	Vector v = new Vector();
	Class visualAdapterClass = VisualDataAdapter.class;

	for(int i=0; i < dataAdapters.size(); i++) {
	    DataAdapter adapter = (DataAdapter) dataAdapters.get(i);
	    if ((op == null || adapterSupports(adapter, op)) &&
		(!visualOnly ||
		 visualAdapterClass.isAssignableFrom(adapter.getClass()))) {
		v.add(adapter);
	    }
	}
	DataAdapter [] out = new DataAdapter[v.size()];
	v.copyInto(out);
	return out;
    }

    /**
     * Returns all installed data adapters that support the given operation
     */
    public DataAdapter [] getAdapters(IOOperation op) {
	return getAdapters(op, false);
    }

    /**
     * Returns all installed data adapters that support visual guis
     */
    public DataAdapter [] getAdapters(boolean visualOnly) {
	return getAdapters(null, visualOnly);
    }

    /**
     * Return all installed data adapters
     */
    public DataAdapter [] getAdapters() {
	return getAdapters(null, false);
    }

    /**
     * Returns a boolean indicated whether the data adapter supports
     * the given IOOperation.
     */
    public static boolean adapterSupports(DataAdapter adapter,
					  IOOperation op) {
	IOOperation [] supported = adapter.getSupportedOperations();
	for(int i=0; i < supported.length; i++)
	    if (supported[i].equals(op))
		return true;
	return false;
    }

    /**
     * Returns the number of data adapters available
     */
    public int getAdapterCount() {
	return dataAdapters.size();
    }

    public DataAdapter getAdapter(int index) {
	return (DataAdapter) dataAdapters.get(index);
    }

    /**
     * Installs a data adapter to this registry.
     */
    public synchronized void installDataAdapter(DataAdapter adapter) {
	dataAdapters.add(adapter);
    }

    /**
     * This is a dangerous method to use. Since DataAdapters are often loaded
     * dynamically, class name clashes are likely if no ClassLoader is supplied
     */
    public synchronized void installDataAdapter(String className) throws
      DataAdapterException {
      // ClassLoader.getSystemClassLoader() doesn't work from webstart
      //	installDataAdapter(className, ClassLoader.getSystemClassLoader());
      installDataAdapter(className, this.getClass().getClassLoader());
    }

    /**
     * Loads a data adapter class through the given class loader, and then
     * installs the adapter to this registry.
     */
    public synchronized void installDataAdapter(String className,
						ClassLoader loader)
	throws DataAdapterException {
	try {
          Class adapterClass = Class.forName(className, true, loader);
	    if (!org.bdgp.io.DataAdapter.class.isAssignableFrom(adapterClass))
		throw new DataAdapterException("Class "+className+" is not a "+
					       "DataAdapter");
	    installDataAdapter((DataAdapter) adapterClass.newInstance());
	} catch (ClassNotFoundException e) {
	    throw new DataAdapterException(e, "Could not find DataAdapter "+
					   className);
	} catch (IllegalAccessException e) {
	    throw new DataAdapterException(e, "Could not install "+className+
					   "because no instance of "+
					   className+" could be constructed."+
					   "Make sure that this class has a "+
					   "public zero-argument constructor "+
					   "and is declared public in its "+
					   "package.");
	} catch (InstantiationException e) {
	    throw new DataAdapterException(e, "Could not install "+className+
					   "because it cannot be constructed;"+
					   " data adapters cannot be "+
					   "interfaces or abstract classes. "+
					   "Have you provided a public, "+
					   "no-args constructor?");
	}
    }

    /**
     * Uninstalls a data adapter from this registry
     */
    public synchronized void removeDataAdapter(DataAdapter adapter) {
	dataAdapters.remove(adapter);
    }

    /**
     * Returns whether or not a data adapter is installed
     */
    public boolean isInstalled(DataAdapter adapter) {
	return dataAdapters.contains(adapter);
    }

    /**
     * Returns the name of the data adapter with the given class name. This
     * method will return the same value that would be returned by calling
     * getName() on a new instance of a data adapter of the appropriate type.
     * @deprecated Data adapters should now be addressed directly, not through
     * their names
     */
    public String getNameForAdapter(String className) {
	int index = getIndexOfAdapter(className);
	if (index != -1)
	    return ((DataAdapter) dataAdapters.elementAt(index)).getName();
	else
	    return null;
    }

    /**
     * Returns the type of the data adapter with the given class name. This
     * method will return the same value that would be returned by calling
     * getType() on a new instance of a data adapter of the appropriate type.
     * @deprecated Data adapters should now be addressed directly, not through
     * their names
     */
    public String getTypeForAdapter(String className) {
	int index = getIndexOfAdapter(className);
	if (index != -1)
	    return ((DataAdapter) dataAdapters.elementAt(index)).getType();
	else
	    return null;
    }


    /**
     * Returns the index of the given adapter
     * @deprecated Data adapters should now be addressed directly, not through
     * their names
     */
    protected int getIndexOfAdapter(String className) {
	for(int i=0; i < dataAdapters.size(); i++) {
	    DataAdapter current = (DataAdapter) dataAdapters.elementAt(i);
	    if (current.getClass().getName().equals(className)) {
		return i;
	    }
	}
	return -1;
    }

    /**
     * Uninstalls an adapter. This method does NOT unload the class
     * definition from the JVM. It simply removes the adapter from
     * the AdapterRegistry's list of available adapters.
     * @deprecated Data adapters should now be addressed directly, not through
     * their names
     */
    public synchronized void removeDataAdapter(String className) {
	int index = getIndexOfAdapter(className);
	if (index != -1) {
	    dataAdapters.removeElementAt(index);
	}
    }

    /**
     * Returns whether an adapter with the given class name has been installed
     * @deprecated Data adapters should now be addressed directly, not through
     * their names. This method is dangerous; multiple adapters with the same
     * name may be installed.
     */
    public boolean isInstalled(String className) {
	return getIndexOfAdapter(className) != -1;
    }



    /**
     * Returns the supported operations of the data adapter with the given
     * class name. This method will return the same value that would be
     * returned by calling getSupportedOperations() on a new instance of a
     * data adapter of the appropriate type.
     * @deprecated Data adapters should now be addressed directly, not through
     * their names
     */
    public IOOperation [] getSupportedOperations(String className) {
	int index = getIndexOfAdapter(className);
	if (index != -1)
	    return ((DataAdapter) dataAdapters.elementAt(index)).
		getSupportedOperations();
	else
	    return null;	
    }

    /**
     * Returns a boolean indicating whether a data adapter supports a
     * particular IOOperation.
     * @deprecated Data adapters should now be addressed directly, not through
     * their names
     */
    public boolean adapterSupports(String className, IOOperation op) {
	int index = getIndexOfAdapter(className);
	if (index != -1) {
	    return adapterSupports(getAdapter(index), op);
	} else
	    return false;
    }

    /**
     * Returns the first adapter with the given class name.
     * @deprecated Data adapters should now be addressed directly, not through
     * their names
     */
    public DataAdapter getAdapter(String className) {
	try {
	    int index = getIndexOfAdapter(className);
	    if (index != -1) {
		return (DataAdapter) dataAdapters.get(index);
	    }
	} catch (Exception e) {
	    return null;
	}
	return null;
    }

    /**
     * Returns the class names of all installed adapters.
     * @deprecated Data adapters should now be addressed directly, not through
     * their names
     */
    public String [] getAdapterNames() {
	return getAdapterNames(null);
    }

    /**
     * Returns the class names of all installed adapters that support the
     * given IOOperation. If visualOnly is true, only data adapters that
     * implement the VisualDataAdapter interface will be returned.
     * @deprecated Data adapters should now be addressed directly, not through
     * their names
     */
    public String [] getAdapterNames(IOOperation op, boolean visualOnly) {
	DataAdapter [] adapters = getAdapters(op, visualOnly);
	String [] out = new String[adapters.length];
	for(int i=0; i < adapters.length; i++)
	    out[i] = adapters[i].getClass().getName();
	return out;
    }

    /**
     * Returns the class names of all installed adapters that support the
     * given IOOperation. If the given operation is null, all data adapters
     * will be returned.
     * @deprecated Data adapters should now be addressed directly, not through
     * their names
     */
    public String [] getAdapterNames(IOOperation op) {
	return getAdapterNames(op, false);
    }
}
