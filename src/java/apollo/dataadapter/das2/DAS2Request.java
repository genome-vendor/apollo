package apollo.dataadapter.das2;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.parsers.*;
import org.apache.log4j.*;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;

import apollo.dataadapter.NonFatalDataAdapterException;
import apollo.dataadapter.das.*;
import apollo.dataadapter.das.simple.*;
import apollo.config.Config;

/**
 * Adapted from das/simple/SimpleDASServer
 * I don't understand why that class was called "server", since it's client-side. 
 *
 *  Updating to handle DAS/2.
 * $PREFIX/sequence - a "sources" request  
 *   This is the top-level entry point to a DAS 2 server.  It returns a
 *   list of the available genomic sequence and their versions.
 *
 * $PREFIX/sequence/$SOURCE - a "source" request
 *   Returns the available versions of the given genomic sequence.

 * $PREFIX/sequence/$SOURCE/$VERSION - a "versioned source" request
 *   Returns information about a given version of a genomic sequence.
 *   Clients may assume that the sequence and assembly are constant for a
 *   given version of a source. Note that annotation data on a server
 *   with curational write-back support may change without changing the
 *   version.
 *
 * In DAS1, the "dsn" request was equivalent to 
 * $PREFIX/sequence/$SOURCE/$VERSION - it gave you a complete list of
 * versioned genome dbs, e.g., "ensembl_Anopheles_gambiae_core_28_2c",
 * "ensembl_Anopheles_gambiae_core_29", etc.
 **/

public class DAS2Request
  implements DASServerI {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(DAS2Request.class);

  private static final float version = 1.0f;
  private static String SOURCES_QUERY = "sequence";

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private String  url = "";
  private String server;
  private List dsnList;

  /**
   * You <em>must</em> pass in the URL of a DAS-reference server, used
   * in all further requests.
   * e.g.  http://das.biopackages.net/codesprint
  **/
  public DAS2Request(String url) {
    setURL(url);
  }

  public void setURL (String url) {
    if (url.indexOf("http://") != 0) {
      url = "http://" + url;
    }//end if

    //    if (url.substring(url.length()-1).equals("/") == false) {
    //      url = url + "/";
    //    }//end if

    this.url = url;
    logger.debug("DAS2Request.setURL = " + url);
  }

  public XMLReader setupXMLReader(DefaultHandler parser) {
    try {
      XMLReader reader = new org.apache.xerces.parsers.SAXParser();
      reader.setContentHandler(parser);
      reader.setFeature("http://xml.org/sax/features/validation", false);
      reader.setFeature("http://apache.org/xml/features/validation/dynamic", false);
      reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      return reader;
    } catch(SAXException theException) {
      throw new apollo.dataadapter.NonFatalDataAdapterException("SAX Problem setting up parser: "+theException.getMessage());
    }
  }

  public String getURL () {
    return url;
  }//end getURL

  /**
   * Gets a list of versioned sources from the server.
   * This actually flattens the tree that is the result of two queries:
   * URL/sequence/ returns a list of the available genomes and their versions, e.g.
   *  <SOURCE id="human" title="human genome" writeable="no" doc_href="" taxon="Human">
   *      <VERSION id="human/17" title="Hsa" created="" modified="">
   * ...
   *  </SOURCE>
   *  <SOURCE id="mouse" title="mouse genome" writeable="no" doc_href="" taxon="Mouse">
   *      <VERSION id="mouse/6" title="Mmu" created="" modified="">
   *  </SOURCE>
   *  <SOURCE id="yeast" title="yeast genome" writeable="no" doc_href="" taxon="Yeast">
   *      <VERSION id="yeast/S228C" title="Sce" created="" modified="">
   *
   * I populate the returned list with instances of <code>SimpleDASDsn</code>.
   * 
   * I wanted to call this getVersionedSources, but it needs to be called getDSNs
   * for now for compatibility with DASServerI
  **/
  public List getDSNs() {
    return getVersionedSources();
  }

  public List getVersionedSources() {
      dsnList = new ArrayList();  // global
    String queryString;

    //This is the tag-handler for the SAX parser - it has all the callbacks
    //as the various tags are hit.
    SourceParser sourceParser = new SourceParser();
    //    sourceParser.setURL(getURL());
    // sourceParser sets its own base url from the xml:base tag

    // SOURCES_QUERY = "sequence"
    queryString = getURL()+"/" + SOURCES_QUERY;
    logger.debug("DAS2Request.getVersionedSources: source query string = " + queryString);

    try {
      //Do the parse - the output is cached on the handler itself
      //      XMLReader parser = XMLReaderFactory.createXMLReader();
      XMLReader reader = setupXMLReader(sourceParser);
      reader.parse(queryString);
     } catch(SAXException theException) {
       logger.error("Error parsing from " + queryString, theException);
       throw new NonFatalDataAdapterException(theException.getMessage());
     }
     catch(IOException theException) {
       throw new NonFatalDataAdapterException(theException.getMessage());
     }//end try

    dsnList = sourceParser.getDsns();
    return dsnList;
  }//end getDsns

    /** Given a URI, return the DSN (if any) for that URI.
     *  (If it can't find it, should it make a new one??) */
    public DASDsn getDSN(String uri) {
	if (dsnList == null)
	    getDSNs();  // fills in dsnList

	Iterator dsnIterator = dsnList.iterator();

	while (dsnIterator.hasNext()) {
	    SimpleDASDsn dsn = (SimpleDASDsn)dsnIterator.next();
	    if (dsn.getSourceId().equals(uri))
		return dsn;
	}
	logger.warn("DAS2Request.getDSN: couldn't find DSN for " + uri);
	return null;
    }

  /**
   * I return all entry-points from the input SimpleDASDsn instance passed in. 
   * The list consists of objects implementing
   * the <code>apollo.dataadapter.das.DASSegment</code> interface. In this case,
   * I populate the returned list with instances of <code>SimpleDASSegment</code>.
  **/
  public List getEntryPoints(DASDsn dsn) {
    List entryPoints = new ArrayList();
    String queryString;

    //This is the tag-handler for the SAX parser - it has all the callbacks
    //as the various tags are hit.
    SegmentParser segmentParser = new SegmentParser();

    // Get this from the type="segments" CAPABILITY record:
    //      <CAPABILITY type="types" query_id="yeast/S228C/type"/>
    SimpleDASDsn source = (SimpleDASDsn)dsn;
    queryString = source.getMapMaster() + "/" + source.getCapabilityURI("segments");
    logger.debug("DAS2Request.getEntryPoints: queryString = " + queryString);

    //    // FOR DEBUGGING--use canned response
    //    queryString = "http://toy.lbl.gov:8094/cgi-bin/annot/segments.pl";
    //    logger.debug("DAS2Request.getEntryPoints: FOR DEBUGGING, queryString is " + queryString);

    try {
      //Do the parse - the output is cached on the handler itself
      XMLReader parser = setupXMLReader(segmentParser);
      parser.parse(queryString);
    } catch(SAXException theException) {
      throw new apollo.dataadapter.NonFatalDataAdapterException("SAX Problem fetching Entry Points: "+theException.getMessage());
    } catch(IOException theException) {
      throw new apollo.dataadapter.NonFatalDataAdapterException("IO Problem fetching Entry Points: "+theException.getMessage());
    }

    entryPoints = segmentParser.getSegments();
    logger.debug("Got " + entryPoints.size() + " segments");
    return entryPoints;

  }//end getEntryPoints

  /**
   * <p> I am the call used by apollo to fetch annotations (features) from
   * the input dsn, for the entry-points and ranges given by the input segments</p>
   *
   * <p>The return list consists of objects implementing
   * the <code>apollo.dataadapter.das.DASFeature</code> interface. In this case,
   * I populate the returned list with instances of <code>SimpleDASFeature</code>.</p>
   *
   *  NOT CURRENTLY USED (but required by interface)
  **/
   public List getFeatures(DASDsn dsn, DASSegment[] segmentSelection
   ) {
       return null;
   }
//     HashMap segmentMap = new HashMap();
//     for(int i=0; i<segmentSelection.length; i++) {
//       segmentMap.put(segmentSelection[i].getId(), segmentSelection[i]);
//     }//end for

//     return
//       getFeatures(
//         dsn,
//         segmentMap,
//         new HashMap(),
//         Long.valueOf(segmentSelection[0].getStart()).longValue(),
//         Long.valueOf(segmentSelection[0].getStop()).longValue()
//       );
//   }//end getFeatures

//   /**
//    * <p> I am the <code>getFeatures</code> signature called internally by the 
//    * DASFeatureContentHandler. Since I exist for the purposes of a recursive-descent
//    * (for assembly) in this implementation, I am <em>not</em> a part of a DASServerI 
//    * interface.</p> 
//    *
//    * <p> I am invoked directly from DASFeatureContentHandler when the features 
//    * returned have 'children' - that is, they contain further nested features. In that
//    * case, the handler will collect all such features of interest, create a new 
//    * DASSegment for each such feature, and re-invoke this method (recursion!) passing
//    * in both a set of segments corresponding to the parent features, <em>and</em> 
//    * the parent features themselves. </p>
//    *
//    * @param dsn               The dsn of the das data source (for example 'ensembl729')
//    * @param segmentSelection  The DAS-segments of interest indexed by segment id
//    * @param parentFeatures    If any particular segment has a parent feature (i.e. 
//    * we are invoking the method because we want to find the children of features from
//    * a prior invocation) then the parent features for each requested segment are
//    * stored here, indexed by segment id (thus each feature corresponds 1-1 to the
//    * segments input above). The most important thing contained in each parent is the 
//    * global start and end of the requested range in the input segments.
//    * @param globalStart       The global start position of the (very first) requested range
//    * @param globalEnd         Global end position of the (very first) requested range.
//    * NOT CURRENTLY USED
//   **/
   public List getFeatures(
     DASDsn dsn,
     HashMap segmentSelection, //
     HashMap parentFeatures, //indexed by segment id (correspond to the segment ids passed in above).
     long globalStart,
     long globalEnd
   ) {
       return null;
   }
//     List dasFeatures;
//     String dsnId = dsn.getSourceId();
//     Iterator segmentIterator = segmentSelection.values().iterator();
//     DASSegment aSegment;
//     String segmentId;
//     String start;
//     String stop;
//     String queryString;

//     //
//     //This is the tag-handler for the SAX parser - it has all the callbacks
//     //as the various tags are hit.
//     DASFeatureContentHandler theSAXHandler =
//       new DASFeatureContentHandler(
//         this,
//         dsn,
//         segmentSelection,
//         parentFeatures,
//         globalStart,
//         globalEnd
//       );

//     queryString = getURL()+dsnId+"/features?";

//     //
//     // We have a set of segments passed in - need to set up an addition to the
//     //query string for each.
//     while(segmentIterator.hasNext()) {
//       aSegment = (DASSegment)segmentIterator.next();
//       segmentId = aSegment.getId();
//       start = aSegment.getStart();
//       stop = aSegment.getStop();

//       queryString += "segment="+segmentId+":"+start+","+stop+";";
//     }//end while

//     DataInputStream xmlStream = null;

//     try {
//       //Do the parse - the output is cached on the handler itself
//       XMLReader reader = setupXMLReader(theSAXHandler);
//       reader.parse(queryString);
//     }catch(SAXException theException) {
//       throw new apollo.dataadapter.NonFatalDataAdapterException("SAX Problem fetching features: "+theException.getMessage());
//     }catch(IOException theException) {
//       throw new apollo.dataadapter.NonFatalDataAdapterException("IO Problem fetching features: "+theException.getMessage());
//     }//end try

//     return theSAXHandler.getFeatures();
//   }//getFeatures

  /**
   * <p> I am the call used by apollo to fetch dna (sequence) from
   * the input dsn, for the entry-points and ranges given by the input segments</p>
   *
   * <p>The return list consists of objects implementing
   * the <code>apollo.dataadapter.das.DASSequence</code> interface. In this case,
   * I populate the returned list with instances of <code>SimpleDASSequence</code>.</p>
   * 
   * @param segmentSelection A list of segments containing entry points and ranges
   * of interest. NOTE: I only pay attention to the FIRST element of the list - 
   * all subsequence elements are IGNORED!!! Therefore, my return value is a list
   * of size 1 under all circumstances.
   *  NOT CURRENTLY USED
  **/
  public List getSequences(DASDsn dsn, DASSegment[] segmentSelection) {
      return null;

//     List sequences = null;

//     String dsnId = dsn.getSourceId();
//     String segmentId = segmentSelection[0].getId();
//     String start = segmentSelection[0].getStart();
//     String stop = segmentSelection[0].getStop();
//     String queryString;

//     //
//     //This is the tag-handler for the SAX parser - it has all the callbacks
//     //as the various tags are hit.
//     DASSequenceContentHandler theSAXHandler =
//       new DASSequenceContentHandler(
//         this,
//         dsn,
//         segmentSelection
//       );

//     queryString = getURL()+dsnId+"/dna?segment="+segmentId+":"+start+","+stop;

//     DataInputStream xmlStream = null;
    
//     //Quick hack to get around Wormbase oddity
//     URL sequenceURL;
//     DataInputStream content;
//     StringBuffer sequenceString = new StringBuffer();
//     String dataLine;
//     StringBufferInputStream stringReader;
//     String lastCharacter;

//     //A very odd hack to account for the fact that wormbase's response
//     //starts <xml... with NO closing ">" on the first line.
//     //If the closing > is missing we put it back in.
//     try{
//       sequenceURL = new URL(queryString);
//       content = new DataInputStream(sequenceURL.openStream());
//       dataLine = content.readLine();
//       lastCharacter = dataLine.substring(dataLine.length()-1);
      
//       if(!lastCharacter.equals(">")){
//         dataLine+=">";
//       }

//       sequenceString.append(dataLine);
//       dataLine = content.readLine();

//       while(dataLine != null){
//         sequenceString.append(dataLine);
//         dataLine = content.readLine();
//       }
      
//       stringReader = new StringBufferInputStream(sequenceString.toString());
//     }catch(IOException exception){
//       throw new apollo.dataadapter.NonFatalDataAdapterException("attempt to read sequence failed:"+exception.getMessage());
//     }//end try
    
//     try {
//       //
//       //Do the parse - the output is cached on the handler itself
//       XMLReader parser = XMLReaderFactory.createXMLReader();
//       parser.setContentHandler(theSAXHandler);
//       InputSource inputSource = new InputSource(stringReader);
//       inputSource.setSystemId(getURL());
//       parser.parse(inputSource);
//     } catch(SAXException theException) {
//       throw new apollo.dataadapter.NonFatalDataAdapterException("SAX Problem fetching sequence: "+theException.getMessage());
//     }catch(IOException theException) {
//       throw new apollo.dataadapter.NonFatalDataAdapterException("IO Problem fetching sequence: "+theException.getMessage());
//     }//end try

//     return theSAXHandler.getSequences();
  }//end getSequences

  /** For testing--supply server URL on command line */
  public static void main(String[] args) {
    String url = args[0];
    logger.info("Set das server to " + url);
    DAS2Request sources = new DAS2Request(url);
    List theDSNList = sources.getDSNs();
    Iterator dsnIterator = theDSNList.iterator();

    while (dsnIterator.hasNext()) {
      SimpleDASDsn dsn = (SimpleDASDsn)dsnIterator.next();
      dsn.setMapMaster(url);
      logger.info("Adding versioned source to list: " + dsn);
    }//end while
  }

}//end SimpleDASServer
