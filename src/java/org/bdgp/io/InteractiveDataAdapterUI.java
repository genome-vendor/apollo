package org.bdgp.io;

import org.bdgp.util.*;

public interface InteractiveDataAdapterUI extends DataAdapterUI {

    public void addDataAdapterUIListener(DataAdapterUIListener listener);
    public void removeDataAdapterUIListener(DataAdapterUIListener listener);
    public void setControllingObject(Commitable controller);
}
