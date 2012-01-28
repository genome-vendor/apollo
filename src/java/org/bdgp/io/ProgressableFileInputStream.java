package org.bdgp.io;

import java.io.*;
import java.util.Vector;
import org.bdgp.util.*;

public class ProgressableFileInputStream extends ProgressableInputStream {

    public ProgressableFileInputStream(File in) throws FileNotFoundException {
	stream = new FileInputStream(in);
	fileSize = in.length();
    }

    public ProgressableFileInputStream(String name) throws FileNotFoundException {
	this(new File(name));
    }
}
