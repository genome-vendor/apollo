package org.bdgp.io;

import java.io.*;
import java.util.Vector;
import org.bdgp.util.*;

public abstract class ProgressableInputStream extends InputStream {

    protected long fileSize = 0;
    protected long currentPos = 0;
    protected double currentProgress = 0.0d;
    protected double percentInc = 1.0;
    protected Vector listeners = new Vector();

    protected InputStream stream;

    protected void setStream(InputStream stream) {
	this.stream = stream;
    }

    protected void setStreamSize(long fileSize) {
	this.fileSize = fileSize;
    }

    public void setPercentIncrement(double percent) {
	percentInc = percent;
    }

    public double getPercentIncremement() {
	return percentInc;
    }

    public void addProgressListener(ProgressListener in) {
	listeners.addElement(in);
    }

    public void removeProgressListener(ProgressListener in) {
	listeners.removeElement(in);
    }

    protected void fireProgressEventIfNecessary() {
	double instantProgress = 100 * ((double) currentPos) / ((double) fileSize);
	if (instantProgress - currentProgress >= percentInc ||
	    instantProgress == 100.0) {
	    currentProgress = instantProgress;
	    fireProgressEvent(new ProgressEvent(this, new Double(currentProgress),
					 "Reading file..."));
	}
    }

    protected void fireProgressEvent(ProgressEvent e) {
	for(int i=0; i < listeners.size(); i++) {
	    ProgressListener pl = (ProgressListener) listeners.elementAt(i);
	    pl.progressMade(e);
	}
    }

    public int read() throws IOException {
	currentPos = currentPos + 1;
	fireProgressEventIfNecessary();
	return stream.read();
    }

    public int read(byte [] bytes) throws IOException {
	int amtread = stream.read(bytes);
	currentPos += amtread;
	fireProgressEventIfNecessary();
	return amtread;
    }

    public int read(byte [] bytes, int off, int len) throws IOException {
	int amtread = stream.read(bytes,off,len);
	currentPos += amtread;
	fireProgressEventIfNecessary();
	return amtread;
    }

    public long skip(long amt) throws IOException {
	long amtskipped = stream.skip(amt);
	currentPos += amtskipped;
	return amtskipped;
    }

    public int available() throws IOException {
	return stream.available();
    }

    public void close() throws IOException {
	stream.close();
    }

    public void mark(int readLimit) {
	stream.mark(readLimit);
    }

    public void reset() throws IOException {
	stream.reset();
    }

    public boolean markSupported() {
	return stream.markSupported();
    }
}


