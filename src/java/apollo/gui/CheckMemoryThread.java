package apollo.gui;

import javax.swing.JOptionPane;
import apollo.datamodel.CurationSet;
import apollo.main.Apollo;

import org.apache.log4j.*;

public class CheckMemoryThread extends Thread {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(CheckMemoryThread.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  long minMemory;
  long maxMemory;
  long interval = 30*1000; // in milliseconds
  private boolean halt = false;

  // memoryAllocation is the memory value in apollo.cfg, which was presumably
  // set as the max heap size for launching Apollo, although that is not
  // absolutely guaranteed to be true, because it depends on how the memory was
  // set in the installer.  But I don't know any other way to ask the size of
  // the max heap.
  public CheckMemoryThread(long memoryAllocation) {
    maxMemory = memoryAllocation;
    // Complain if free memory goes below 5% of memory allocation set in apollo.cfg
    minMemory = memoryAllocation/(long)20;
    setDaemon(true);
  }

  public void checkFreeMemory() {
    // Using freeMemory() is not a good way to check free memory, because the heap might
    // still be at its initial smaller size.  We might have almost used up that initial
    // heap, but then the JVM can allocate more memory, up to the max.
    //    long freeMemory = Runtime.getRuntime().freeMemory();

    // It seems like we're able to allocate more than maxMemory memory.
    // Not sure how much more.  But it's possible to get freeMemory values
    // below 0!
    long memoryUsed = Runtime.getRuntime().totalMemory();
    long freeMemory = maxMemory - memoryUsed;
    logger.debug("checkFreeMemory: free memory = " + freeMemory + ", total memory used = " + memoryUsed);

    if (freeMemory < minMemory) {
      // Try garbage collecting first and see if that helps.
      try {
	System.gc();
	sleep(2000);
	// Do it twice because it seems to get more the second time.
	System.gc();
	sleep(2000);
      } catch (InterruptedException e) {}

      memoryUsed = Runtime.getRuntime().totalMemory();
      freeMemory = maxMemory - memoryUsed;
      logger.debug("checkFreeMemory: After garbage collecting, free memory = " + freeMemory);
      if (freeMemory < minMemory) {
	String m = "WARNING: you are almost out of memory (" + freeMemory + " bytes left).\nIf you run out of memory, Apollo could crash and you could lose your work.\nWe recommend saving now, then exiting Apollo and restarting.";
	logger.warn(m);
	JOptionPane.showMessageDialog(null,m);
	logger.debug("checkFreeMemory: free memory = " + freeMemory + ", total memory used = " + memoryUsed);
	logger.debug("NOT halting memory checking thread.");
	// We've warned once--don't warn again
	this.halt();
      }
    }
  }

  public void halt() {
    halt = true;
    interrupt();
  }

  public void run() {
    Apollo.setLog4JDiagnosticContext();
    while(!halt) {
      try {
        sleep(interval);
	checkFreeMemory();
      } catch (InterruptedException e) {}
    }
    Apollo.clearLog4JDiagnosticContext();
  }
}
