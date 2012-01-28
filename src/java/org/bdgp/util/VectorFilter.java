package org.bdgp.util;

import java.io.*;

public interface VectorFilter extends Cloneable, Serializable {

    public boolean satisfies(Object in);
}
