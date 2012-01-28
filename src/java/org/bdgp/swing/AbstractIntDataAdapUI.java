package org.bdgp.swing;

import javax.swing.*;
import org.bdgp.io.*;
import org.bdgp.util.*;
import java.util.*;

public abstract class AbstractIntDataAdapUI extends AbstractDataAdapterUI
    implements InteractiveDataAdapterUI {

    protected Vector listeners = new Vector();
    protected Commitable controllingObject;

    public abstract Object doOperation(Object values)
	throws DataAdapterException;


    public void setControllingObject(Commitable controllingObject) {
	this.controllingObject = controllingObject;
    }

    public void setProperties(Properties in) {
    }
    
    public Properties getProperties() {
	return null;
    }
    
    public void addDataAdapterUIListener(DataAdapterUIListener listener) {
	listeners.addElement(listener);
    }
    
    public void removeDataAdapterUIListener(DataAdapterUIListener listener) {
	listeners.removeElement(listener);
    }

    protected void fireDataAdapterUIEvent(DataAdapterUIEvent event) {
	for(int i=0; i < listeners.size(); i++) {
	    DataAdapterUIListener listener = (DataAdapterUIListener)
		listeners.elementAt(i);
	    listener.setNewUI(event);
	}
    }

}
