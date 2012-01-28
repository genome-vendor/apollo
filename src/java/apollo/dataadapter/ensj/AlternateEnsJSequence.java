package apollo.dataadapter.ensj;

import java.util.*;
import java.io.IOException;
import java.lang.String;

import apollo.gui.event.*;
import apollo.gui.*;
import apollo.datamodel.*;
import apollo.datamodel.seq.AbstractLazySequence;
import apollo.datamodel.seq.LazySequenceI;
import apollo.seq.io.*;

import org.ensembl.*;
import org.ensembl.driver.*;
import org.ensembl.datamodel.*;


public class AlternateEnsJSequence extends AbstractLazySequence implements LazySequenceI {

  private Location _location;
  private Properties _driverProperties;
  
  //
  //The driver properties are passed into the sequence, and are used to construct
  //the driver, which is cached. If the sequence is serialised, this driver will be
  //lost (it's transient). BUT we'll just recreate it when we need it.
  private transient Driver driver;

  public AlternateEnsJSequence(String id, Controller controller, Location location, Properties driverProperties) {
    super(id,controller);
    _location = location;
    _driverProperties = driverProperties;
    setRange(new Range("dummy",getLocation().getStart(), getLocation().getEnd()));
    setLength(location.getLength());
  }

  public void setDriverProperties(Properties driverProperties) {
    _driverProperties = driverProperties;
  }


  public Properties getDriverProperties() {
    return _driverProperties;
  }

  private Driver getDriver(){
    return driver;
  }//end getDriver
  
  private void setDriver(Driver newValue){
    driver = newValue;
  }//end setDriver
  
  private Location getLocation(){
    return _location;
  }
  
  /**
   * @return new location based on current one but with different start and
   * end.  */
  private Location derivedLocation(int start, int end) {
    Location subLocation = getLocation().copy();
    subLocation.setStart(start);
    subLocation.setEnd(end);
    return subLocation;
  }

  public SequenceI getSubSequence(int start, int end) {
    return new AlternateEnsJSequence(
      getName(),
      //llco is the LazyLoadControlledObject stored as an instance variable in the superclass
      llco.getController(),
      derivedLocation(start, end),
      getDriverProperties()
    );
  }


  /* Note: low and high are relative coordinates */
  protected String getResiduesFromSourceImpl(int start, int end) {
    SequenceAdaptor sequenceAdaptor = null;
    Location location;
    org.ensembl.datamodel.Sequence sequence;
    String sequenceString;
    try {
      //
      //If the driver hasn't been loaded before, or it was loaded and then
      //lost when this sequence was serialised, then we need to reload it.
      if(getDriver()== null){
        setDriver((Driver)DriverManager.load(getDriverProperties()));
      }//end if
      
      sequenceAdaptor = (SequenceAdaptor)getDriver().getSequenceAdaptor();
      
      location = derivedLocation(start, end);
      
      //
      //we reset the strand of the location to 1 (instead of 0)
      //just for sequence retrieval. The API just requires that
      //the input strand be +1 when seq-fetching
      location.setStrand(1);
      
      sequence = sequenceAdaptor.fetch(location);
      sequenceString = sequence.getString();

      if(sequenceString.length() <= location.getNodeLength()){
        //return null; 
        StringBuffer buf;
        buf = new StringBuffer(sequenceString);
        for (int g = sequenceString.length(); g <location.getNodeLength(); ++g) buf.append('N');

        sequenceString = buf.toString();
      }

      
      return sequenceString;
    } catch (AdaptorException e) {
      throw new apollo.dataadapter.NonFatalDataAdapterException("failed to get sequence due to data problem: "+e.getMessage());
    }
  }
}
