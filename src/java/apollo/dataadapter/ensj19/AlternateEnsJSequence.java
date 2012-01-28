package apollo.dataadapter.ensj19;

import java.util.*;
import java.io.IOException;
import java.lang.String;

import apollo.gui.event.*;
import apollo.gui.*;
import apollo.datamodel.*;
import apollo.datamodel.seq.AbstractLazySequence;
import apollo.datamodel.seq.LazySequenceI;
import apollo.seq.io.*;

import org.ensembl19.*;
import org.ensembl19.driver.*;
import org.ensembl19.datamodel.*;


public class AlternateEnsJSequence extends AbstractLazySequence implements LazySequenceI {

  private LinearLocation location;
  private Properties driverProperties;
  
  //
  //The driver properties are passed into the sequence, and are used to construct
  //the driver, which is cached. If the sequence is serialised, this driver will be
  //lost (it's transient). BUT we'll just recreate it when we need it.
  private transient Driver driver;

  public AlternateEnsJSequence(String id, Controller c, LinearLocation loc, Properties driverProperties) {
    super(id,c);
    this.location = loc;
    setRange(new Range("dummy",loc.getStart(), loc.getEnd()));
    setDriverProperties(driverProperties);
    setLength(loc.getLength());
  }

  public void setDriverProperties(Properties driverProperties) {
    this.driverProperties = driverProperties;
  }


  public Properties getDriverProperties() {
    return driverProperties;
  }

  private Driver getDriver(){
    return driver;
  }//end getDriver
  
  private void setDriver(Driver newValue){
    driver = newValue;
  }//end setDriver
  
  /**
   * @return new location based on current one but with different start and
   * end.  */
  private LinearLocation derivedLocation(int start, int end) {
    
    LinearLocation subLoc = (LinearLocation)location.copy();
    subLoc.setStart( start );
    subLoc.setEnd( end );
    return subLoc;
  }

  public SequenceI getSubSequence(int start, int end) {
    return new AlternateEnsJSequence(
      getName(),
      llco.getController(),
      derivedLocation(start, end),
      getDriverProperties()
    );
  }


  /* Note: low and high are relative coordinates */
  protected String getResiduesFromSourceImpl(int start, int end) {
    SequenceAdaptor sa = null;
    try {
      
      //
      //If the driver hasn't been loaded before, or it was loaded and then
      //lost when this sequence was serialised, then we need to reload it.
      if(getDriver()== null){
        setDriver(DriverManager.load(getDriverProperties()));
      }//end if
      
      sa = (SequenceAdaptor)getDriver().getAdaptor("sequence");
      LinearLocation loc = derivedLocation(start, end);
      //
      //we reset the strand of the location to 1 (instead of 0)
      //just for sequence retrieval, because there's a bug in ensj that retrieves
      //reverse-complemented sequence for -1 oriented contigs. Not worth fixing
      //in ensj, because all that will be overhauled with introduction of new
      //schema in 2004
      loc.setStrand(1);
      org.ensembl19.datamodel.Sequence seq = sa.fetch(loc);
      String s = seq.getString();
      return s;

    } catch (AdaptorException e) {
      throw new apollo.dataadapter.NonFatalDataAdapterException("failed to get sequence due to data problem: "+e.getMessage());
    } catch (ConfigurationException e) {
      throw new apollo.dataadapter.NonFatalDataAdapterException("failed to get sequence due to configuration problem: "+e.getMessage());
    }
  }
}









