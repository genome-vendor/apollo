package apollo.dataadapter.otter;

import java.util.*;
import java.io.*;
import apollo.seq.io.*;
import apollo.datamodel.*;
import apollo.config.Config;

import org.bdgp.io.IOOperation;
import org.bdgp.io.DataAdapterUI;
import org.bdgp.util.*;
import java.util.Properties;
import org.bdgp.swing.widget.*;
import apollo.dataadapter.*;
import apollo.dataadapter.otter.parser.*;

import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
import org.xml.sax.helpers.DefaultHandler;


public class OtterXMLAdapter extends AbstractApolloAdapter {
    private String seqfile;
    private String gfffile;

  IOOperation []
    supportedOperations = {
       ApolloDataAdapterI.OP_READ_DATA,
       ApolloDataAdapterI.OP_READ_SEQUENCE,
       ApolloDataAdapterI.OP_WRITE_DATA
    };

  private InputStream  inputStream;
  private OutputStream outputStream;

  public OtterXMLAdapter() {

  }

  public InputStream getInputStream(){
    return inputStream;
  }

  public void setInputStream(InputStream newValue){
    inputStream = newValue;
  }

  public OutputStream getOutputStream(){
    return outputStream;
  }

  public void setOutputStream(OutputStream newValue){
    outputStream = newValue;
  }

  public String getName() {
    return "Otter XML ";
  }

  public String getType() {
    return "Otter XML Annotations";
  }

  public DataInputType getInputType() {
    return DataInputType.FILE;
  }

  public String getInput() {
    return null;
  }

  public IOOperation [] getSupportedOperations() {
    return supportedOperations;
  }

  public DataAdapterUI getUI(IOOperation op) {
    return new OtterXMLAdapterGUI(op);
  }

  public void setRegion(String region) throws ApolloAdapterException {
    throw new NotImplementedException("Not yet implemented");
  }

  public Properties getStateInformation() {
    Properties props = new Properties();
    return props;
  }

  public void setStateInformation(Properties props) {
  }

  private StrandedFeatureSetI getAnnotations(){
    XMLReader            parser;
    OtterContentHandler  handler;
    InputSource          theFileReader;
    AnnotatedFeatureI theSet = null;
    Iterator             featureIterator;
    SeqFeatureI          theResultFeature;
    CurationSet          curationSet;
    StrandedFeatureSet   returnSet = new StrandedFeatureSet(new FeatureSet(),
                                                            new FeatureSet());

    curationSet = new CurationSet();

    int low  = -1;
    int high = -1;

    try {
	parser = XMLReaderFactory.createXMLReader();
	handler = new OtterContentHandler();
	handler.setCurationSet(curationSet);
	theFileReader = new InputSource(getInputStream());
	parser.setContentHandler(handler);
	parser.parse(theFileReader);

	Iterator returnedObjects = handler.getReturnedObjects().iterator();
	String   message;
	Object   returnedObject;

	AnnotatedFeatureI set = null;

	while(returnedObjects.hasNext()){

	    returnedObject = returnedObjects.next();

	    theSet = (AnnotatedFeatureI)returnedObject;

	}

	if (theSet != null) {
	    featureIterator = theSet.getFeatures().iterator();

	    while(featureIterator.hasNext()){
		theResultFeature = (SeqFeatureI)featureIterator.next();
		if(low > theResultFeature.getLow() || low < 0){
		    low = theResultFeature.getLow();
		}
		if(high < theResultFeature.getHigh() || high < 0){
		    high = theResultFeature.getHigh();
		}

		returnSet.addFeature(theResultFeature);
	    }

	    returnSet.setLow(low);
	    returnSet.setHigh(high);

	}
    } catch(IOException theException){
	theException.printStackTrace();
	//throw new apollo.dataadapter.DataAdapterException("IO Error parsing input xml-stream", theException);
    } catch(SAXException theException){
	theException.printStackTrace();
	//throw new apollo.dataadapter.DataAdapterException("SAX Error parsing input xml-stream", theException);
    }

    return returnSet;
  }

  public CurationSet getCurationSet() throws ApolloAdapterException {
    CurationSet         curationSet = new CurationSet();
    StrandedFeatureSetI annotations = getAnnotations();

    curationSet.setAnnots(annotations);
    curationSet.setResults(new StrandedFeatureSet(new FeatureSet(), new FeatureSet()));

    curationSet.setLow (annotations.getLow());
    curationSet.setHigh(annotations.getHigh());

    return curationSet;
  }


  private void setSequence(SeqFeatureI sf, CurationSet curationSet) {
    throw new NotImplementedException("Not yet implemented");
  }

  public SequenceI getSequence(String id) throws ApolloAdapterException {
    throw new NotImplementedException();
  }
  public SequenceI getSequence(DbXref dbxref) throws ApolloAdapterException{
    throw new NotImplementedException("Not yet implemented");
  }

  public SequenceI getSequence(
    DbXref dbxref,
    int start,
    int end
  ) throws ApolloAdapterException {
    throw new NotImplementedException();
  }

  public Vector getSequences(DbXref[] dbxref) throws ApolloAdapterException{
    throw new NotImplementedException();
  }

  public Vector getSequences(
    DbXref[] dbxref,
    int[] start,
    int[] end
  ) throws ApolloAdapterException{
    throw new NotImplementedException();
  }

  public void commitChanges(CurationSet curationSet) throws ApolloAdapterException {
    BufferedOutputStream      buffer  = new BufferedOutputStream(getOutputStream());
    OutputStreamWriter        writer  = new OutputStreamWriter(buffer);
    OtterXMLRenderingVisitor  visitor = new OtterXMLRenderingVisitor();
    FeatureSetI               theSet  = curationSet.getAnnots();

    theSet.accept(visitor);

    String                    outputString = visitor.getReturnBuffer().toString();

    try{

	// I don't like doing this but I can't see whereelse to get them from
	writer.write("<otter>\n");
	writer.write("<sequenceset>\n");

	// Do the sequence fragment tags get written? Are they being set properly from the server?

	writer.write(outputString);
	writer.write("</sequenceset>\n");
	writer.write("</otter>\n");
	writer.flush();

    } catch(IOException theException){
      throw new ApolloAdapterException("Error writing annotations",theException);
    }

    GFFAdapter gffadapter = new GFFAdapter();

    gffadapter.setSequenceFilename(seqfile);
    gffadapter.setFilename        (gfffile);
    try {
      gffadapter.commitChanges(curationSet);
      gffadapter.saveSequence(curationSet.getRefSequence());
    }
    catch (Exception e) {}
  }

    public void setSequenceFilename(String seqfile) {
	this.seqfile = seqfile;
    }
    public void setGFFFilename(String gfffile) {
	this.gfffile = gfffile;
    }
  public String getRawAnalysisResults(String id) throws ApolloAdapterException{
    throw new NotImplementedException();
  }

  public void init() {
  }
}
