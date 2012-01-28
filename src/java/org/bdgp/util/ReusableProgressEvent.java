package org.bdgp.util;

/**
 * An event indicating that some progress has been made. A single instance of
 * this class should be used in cases where many progress events will be
 * fired in a row, and the cost of instantiating a destroying many
 * ProgressEvents would be wasteful. In those cases, a single reusable progress
 * event can be instatiated, reset with new progress values, and be refired
 * by listeners each time a progress event is needed.
 *
 */
public class ReusableProgressEvent extends ProgressEvent {
    protected int fastVal;

    public ReusableProgressEvent(Object source) {
	super(source, null, null);
	fastVal = -1;
    }

    public int getFastVal() {
	return fastVal;
    }

    public void setFastVal(int fastVal) {
	this.fastVal = fastVal;
    }

    public Double getValue() {
	return new Double(fastVal);
    }

    public void setDescription(String description) {
	this.description = description;
    }

    public String getDescription() {
	return description;
    }
}
