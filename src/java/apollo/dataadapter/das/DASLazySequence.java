package apollo.dataadapter.das;

import java.util.*;
import java.io.IOException;
import java.lang.String;

import apollo.datamodel.*;
import apollo.datamodel.seq.AbstractLazySequence;
import apollo.datamodel.seq.LazySequenceI;
import apollo.gui.event.*;
import apollo.gui.*;
import apollo.dataadapter.*;
import apollo.dataadapter.das.*;
import apollo.seq.io.*;

/**
 * <p>I am a Sequence class for implementing lazy load using a DASServer</p>
**/
public class 
    DASLazySequence 
extends 
    AbstractLazySequence 
implements 
    LazySequenceI 
{
    DASServerI server;
    DASDsn dsn;
    DASSegment segment;
    
    public DASLazySequence(
        String id, 
        Controller c, 
        RangeI loc, 
        DASServerI theServer,
        DASDsn theDsn,
        DASSegment theSegment
    ){
        super(id,c);
        setRange(loc);
        setLength(loc.length());
        server = theServer;
        dsn = theDsn;
        segment = theSegment;
    }//end DASLazySequence

    public DASServerI getServer(){
        return server;
    }//end getServer
    
    public DASDsn getDsn(){
        return dsn;
    }//end getDsn
    
    public DASSegment getSegment(){
        return segment;
    }//end getSegment
    
    public SequenceI getSubSequence(int start, int end) {
        Range subLoc = 
            new Range(
                getRange().getName(),
                getRange().getStart()+start-1,
                getRange().getStart()+end-1
            );
        
        //
        //Adjust the segment to reflect the change in the location
        getSegment().setStart(String.valueOf(getRange().getStart()+start-1));
        getSegment().setStop(String.valueOf(getRange().getStart()+end-1));
        
        return new DASLazySequence(
            getName(), 
            llco.getController(), 
            subLoc,
            getServer(),
            getDsn(),
            getSegment()
        );
    }//end getSubSequence

    /**
     * Returns the sequence string
    **/
    protected String getResiduesFromSourceImpl(int low, int high){

        List theSequenceList = 
            getServer().getSequences(
                getDsn(),
                new DASSegment[]{getSegment()}
            );
            
        DASSequence theSequence;
        String sequenceString;
        
        if(theSequenceList.size()>0){
          theSequence = (DASSequence)theSequenceList.iterator().next();
          sequenceString = theSequence.getDNA();
        }else{
          throw new apollo.dataadapter.NonFatalDataAdapterException("sequence fetch returned no sequences");
        }//end if
        
        return sequenceString;
    }//end getResiduesFromSourceImpl
}
