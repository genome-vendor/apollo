package org.bdgp.io;

import java.util.EventObject;

public class DataAdapterUIEvent extends EventObject {

    protected DataAdapterUI ui;

    public DataAdapterUIEvent(Object source,
			      DataAdapterUI ui) {
	super(source);
	this.ui = ui;
    }

    public DataAdapterUI getUI() {
	return ui;
    }
}
