package org.bdgp.util;

/**
 * An event indicating that some progress has been made
 *
 */
public class ProgressEvent extends java.util.EventObject {
    protected Double value;
    protected String description;

    public ProgressEvent(Object source, Double value, String description) {
	super(source);
	this.value = value;
	this.description = description;
    }

    public Double getValue() {
	return value;
    }

    public String getDescription() {
	return description;
    }
}
