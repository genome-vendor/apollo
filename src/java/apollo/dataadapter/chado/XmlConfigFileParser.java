package apollo.dataadapter.chado;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import apollo.config.Config;
import apollo.dataadapter.chado.jdbc.ChadoInstance;
import apollo.dataadapter.chado.jdbc.ChadoProgram;

/** Parses xml config file for chado, conf/chado-adapter.xml. Creates an array of ChadoDatabase
    which have chado instances. */

class XmlConfigFileParser {
    
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(XmlConfigFileParser.class);

  /**
   * Read a list of available Chado databases from an adapter-specific configuration file.
   * The Chado adapter will look for a configuration file named "chado-adapter.xml" in all of
   * the locations that Apollo usually looks for configuration files.  It expects to find it
   * in the "conf" subdirectory but will also look in the parent directory (in all the 
   * standard locations.)
   chado-adapter.cfg has been changed to a xml file chado-adapter.xml, which is in the
   conf directory (by default)
   
   should we have a separate class to parse this? definitely!
   *
   * @see apollo.util.IOUtil.findFile
   * @return An array of chado database descriptors.
   **/
  ChadoDatabase[] readConfigFile() {

    List databases = new ArrayList();
    String configFileName = Config.getChadoJdbcAdapterConfigFile();
    logger.debug("XmlConfigFileParser.readConfigFile: configFileName = " + configFileName);
    //String configFileName = apollo.util.IOUtil.findFile("conf" +
    //                              File.separator + "chado-adapter.xml");
    //if (configFileName == null) {
    //String configFileName = apollo.util.IOUtil.findFile("chado-adapter.xml");
      //}
    if (configFileName == null) {
      // return null? throw exception?
      return (ChadoDatabase[])databases.toArray(new ChadoDatabase[0]);
    } 


    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder builder = dbf.newDocumentBuilder();
      Document doc = null;
      doc = builder.parse(configFileName);
      Element root = doc.getDocumentElement();
      NodeList list = root.getChildNodes();
      int size = list.getLength();
      
      // parse chadodbs & chadoInstances
      for (int i = 0; i < size; i++) {
        Node node = list.item(i);
        
        // CHADO DB
        if (node.getNodeName().equals("chadodb")) {
          ChadoDatabase chadoDb = extractDBInfo(node);
          databases.add(chadoDb);
          // this saves ChadoAdapter the trouble of having to dig for the default db
          // ChadoAdapter now does this
          //if (chadoDb.isDefaultDatabase()) setDefaultDatabase(chadoDb);
        }
        
        // CHADO INSTANCE
        else if (node.getNodeName().equals("chadoInstance")) {
          String id = ((Element)node).getAttribute("id");
          ChadoInstance instance = generateChadoInstance((Element)node);
          //chadoInstanceMap.put(id, instance);
          instance.setId(id);
          addChadoInstance(id,instance);
        }
      }
    }
    catch (ParserConfigurationException e) {
      logger.error("ChadoAdapter.readConfigFile() 1: " + e, e);
    }
    catch (SAXException e) {
      logger.error("ChadoAdapter.readConfigFile() 2: " + e, e);
    }
    catch (IOException e) {
      logger.error("ChadoAdapter.readConfigFile() 3: " + e, e);
    }

    return (ChadoDatabase[])databases.toArray(new ChadoDatabase[0]);
  }
  
  private Map chadoInstanceMap = new HashMap();
  private void addChadoInstance(String id, ChadoInstance instance) {
    chadoInstanceMap.put(id,instance);
  }
  
  private ChadoInstance getChadoInstance(String id) {
    ChadoInstance instance = (ChadoInstance)chadoInstanceMap.get(id);
    if (instance == null) // this is a runtime, throw non-runtime?
      throw new IllegalStateException("Chado instance "+id+" not defined.");
    return instance;
  }

  /** parse chadoDB element into ChadoDatabase object */
  private ChadoDatabase extractDBInfo(Node dbNode) {
    ChadoDatabase db = new ChadoDatabase();
    boolean commandLineDefaultIsSet = false;
    NodeList list = dbNode.getChildNodes();
    int size = list.getLength();
    Node tmp = null;
    String nodeName = null;
    for (int i = 0; i < size; i++) {
      tmp = list.item(i);
      nodeName = tmp.getNodeName();
      if (nodeName.equals("name"))
        db.setName(getTextNodeValue(tmp));
      else if (nodeName.equals("adapter"))
        db.setAdapterClassName(getTextNodeValue(tmp));
      else if (nodeName.equals("url"))
        db.setJdbcUrl(getTextNodeValue(tmp));
      else if (nodeName.equals("dbName")) 
        db.setChadoDb(getTextNodeValue(tmp));
      else if (nodeName.equals("dbUser"))
        db.setLogin(getTextNodeValue(tmp));
      else if (nodeName.equals("dbPassword"))
        db.setPassword(getTextNodeValue(tmp));
      else if (nodeName.equals("allowDbUserInput"))
        db.setAllowLoginInput(getTextNodeBoolean(tmp));
      else if (nodeName.equals("allowDbPasswordInput"))
        db.setAllowPasswordInput(getTextNodeBoolean(tmp));
      else if (nodeName.equals("dbInstance")) {
        String instanceId = getTextNodeValue(tmp);
        //ChadoInstance instance = (ChadoInstance) chadoInstanceMap.get(instanceId);
        //if (instance == null)
        //throw new IllegalStateException("ChadoAdapter.extractDBInfo(): Referred ChadoInstance is not defined in config file.");
        db.setChadoInstance(getChadoInstance(instanceId)); // throws IllegalStateEx
      }
      else if (nodeName.equals("style"))
        db.setStyleFileName(getTextNodeValue(tmp));

      // seqType should go to chadoInstance
      else if (nodeName.equals("sequenceTypes")) {
        logger.error("sequenceTypes now go in chado instance, please correct your "+
                     "chado xml config file (" + Config.getChadoJdbcAdapterConfigFile() + ")");
        //extractSeqTypes(tmp, db);
      }

      // --> chadoInstance?
      else if (nodeName.equals("chromosomes")) {
        logger.error("chromosomes now go in chado instance, please correct your "+
                     "chado xml config file (" + Config.getChadoJdbcAdapterConfigFile() + ")");
        //extractChromosomes(tmp, db);
      }
      else if (nodeName.equals("default-command-line-db")) {
        if (getTextNodeValue(tmp).equals("true")) {
          if (!commandLineDefaultIsSet) {
            db.setIsDefaultDatabase(true);
            commandLineDefaultIsSet = true;
          }
          else 
            logger.warn("Default cmd line db set more than once, ignoring for "+ db.getName());
        }
      }
    }
    return db;
  }
  

  /**
   * A helper to create a ChadoInstance based on an XML element. This is an example:
   * <chadoInstance>
   *   <clsName>apollo.dataadapter.chado.jdbc.RiceChadoInstance</clsName>
   *   <oneLevelAnnotTypes>
   *     <type>promoter</type>
   *     <type>transposable element</type>
   *     <type>transposable_element_insertion_site</type>
   *     <type>remark</type>
   *     <type>repeat</type>
   *   </oneLevelAnnotTypes>
   *   <threeLevelAnnotTypes>
   *     <type>gene</type>
   *     <type>pseudogene</type>
   *     <type>tRNA</type>
   *     <type>snRNA</type>
   *     <type>snoRNA</type>
   *     <type>ncRNA</type>
   *     <type>rRNA</type>
   *     <type>miRNA</type>
   *   </threeLevelAnnotTypes>
   * </chadoInstance>
   * @param dbInstElm
   * @return
   */
  private ChadoInstance generateChadoInstance(Element dbInstElm) {
    NodeList list = dbInstElm.getChildNodes();
    int size = list.getLength();
    String className = null;
    String inheritedInstance = null;

    // JDBCTransactionWriter
    String writebackTemplate = null;

    // PureJDBCTransactionWriter
    Boolean pureJDBCWriteMode = null;
    Boolean pureJDBCCopyOnWrite = null;
    Boolean pureJDBCNoCommit = null;
    Boolean pureJDBCUseCDS = null;
    
    Boolean queryFeatureIdWithUniquename = null;
    Boolean queryFeatureIdWithName = null;
    String logDirectory = null;
    String featureCV = null;
    String relationshipCV = null;

    // feature cvterm names
    String polypeptideType = null;

    // relationship cvterm names used for central dogma features (gene, transcript, CDS, etc.)
    String partOfCvTerm = null;
    String transProtRelTerm = null;
    String transGeneRelTerm = null;
    String exonTransRelTerm = null;
    String cdsTransRelTerm = null;
    String polypeptideCdsRelTerm = null;
    String polypeptideTransRelTerm = null;
    String propertyTypeCV = null;
    String syntenyRelationshipType = null;

    // property cvterms
    String featureOwnerPropertyTerm = null;
    String featureCreateDatePropertyTerm = null;
    String commentPropertyTerm = null;

    // sequence description cvterm
    String seqDescriptionCVName = null;
    String seqDescriptionTerm = null;

    List oneLevelTypes = null;
    List threeLevelTypes = null;
    ChadoProgram[] genePredictionPrograms = null;
    ChadoProgram[] searchHitPrograms = null;
    ChadoProgram[] oneLevelResultPrograms = null;
    Boolean searchHitsHaveFeatLocs = null;
    Boolean cacheAnalysisTable = null;
    Boolean retrieveAnnotations = null;
    Boolean copyGeneModelsIntoResultTier = null;
    List seqTypeList = null;
    Boolean useSynonyms = null;

    for (int i = 0; i < size; i++) {
      Node node = list.item(i);
      String nodeName = node.getNodeName();

      if (nodeName.equals("#text") || nodeName.equals("#comment"))
        continue;

      // CLASS NAME
      if (nodeName.equals("clsName")) {
        className = getTextNodeValue(node);
      }

      // INHERITED INSTANCE
      else if (nodeName.equals("inheritsInstance")) {
        inheritedInstance = getTextNodeValue(node);
      }

      else if (nodeName.equals("sequenceTypes")) {
        seqTypeList = extractSeqTypes(node);
      }

      // WRITEBACK TEMPLATE FILE
      else if (nodeName.equalsIgnoreCase("writebackXmlTemplateFile")) {
        writebackTemplate = getTextNodeValue(node);
      }

      // PURE JDBC WRITE MODE
      else if (nodeName.equalsIgnoreCase("pureJDBCWriteMode")) {
        pureJDBCWriteMode = getTextNodeBooleanObj(node);
      }

      // PURE JDBC - COPY ON WRITE
      else if (nodeName.equalsIgnoreCase("pureJDBCCopyOnWrite")) {
        pureJDBCCopyOnWrite = getTextNodeBooleanObj(node);
      }

      // PURE JDBC - NO COMMITS
      else if (nodeName.equalsIgnoreCase("pureJDBCNoCommit")) {
        pureJDBCNoCommit = getTextNodeBooleanObj(node);
      }
      
      // PURE JDBC - USE CDS
      else if (nodeName.equalsIgnoreCase("pureJDBCUseCDS")) {
        pureJDBCUseCDS = getTextNodeBooleanObj(node);
      }
      
      // SQL UPDATE LOG DIRECTORY
      else if (nodeName.equalsIgnoreCase("logDirectory")) {
        logDirectory = getTextNodeValue(node);
      }

      // USE CHADO feature.uniquename IN feature_id LOOKUPS
      else if (nodeName.equalsIgnoreCase("queryFeatureIdWithUniquename")) {
        queryFeatureIdWithUniquename = getTextNodeBooleanObj(node);
      }

      // USE CHADO feature.name IN feature_id LOOKUPS
      else if (nodeName.equalsIgnoreCase("queryFeatureIdWithName")) {
        queryFeatureIdWithName = getTextNodeBooleanObj(node);
      }

      // FEATURE CV
      else if (nodeName.equals("featureCV")) {
        featureCV = getTextNodeValue(node);
      }

      else if (nodeName.equals("polypeptideType")) {
        polypeptideType = getTextNodeValue(node);
      }

      // RELATIONSHIP CV
      else if (nodeName.equals("relationshipCV")) {
        relationshipCV = getTextNodeValue(node);
      }

      else if (nodeName.equals("partOfCvTerm")) {
        partOfCvTerm = getTextNodeValue(node);
      }
      
      // producedBy is pase
      else if (nodeName.equals("producedByCvTerm") 
               || nodeName.equals("transProtRelationTerm")) {
        transProtRelTerm = getTextNodeValue(node);
      }

      else if (nodeName.equals("transGeneRelationTerm")) {
        transGeneRelTerm = getTextNodeValue(node);
      }

      else if (nodeName.equals("exonTransRelationTerm")) {
        exonTransRelTerm = getTextNodeValue(node);
      }

      else if (nodeName.equals("cdsTransRelationTerm")) {
        cdsTransRelTerm = getTextNodeValue(node);
      }

      else if (nodeName.equals("polypeptideCdsRelationTerm")) {
        polypeptideCdsRelTerm = getTextNodeValue(node);
      }

      else if (nodeName.equals("polypeptideTransRelationTerm")) {
        polypeptideTransRelTerm = getTextNodeValue(node);
      }

      // PROPERTY TYPE CV
      else if (nodeName.equals("propertyTypeCV")) {
        propertyTypeCV = getTextNodeValue(node);
      }
      
      else if (nodeName.equals("syntenyRelationshipType")) {
        syntenyRelationshipType = getTextNodeValue(node);
      }

      else if (nodeName.equals("featureOwnerPropertyTerm")) {
        featureOwnerPropertyTerm = getTextNodeValue(node);
      }

      else if (nodeName.equals("featureCreateDatePropertyTerm")) {
        featureCreateDatePropertyTerm = getTextNodeValue(node);
      }

      else if (nodeName.equals("commentPropertyTerm")) {
        commentPropertyTerm = getTextNodeValue(node);
      }    
        
      // SEQUENCE DESCRIPTION CV(TERM)
      else if (nodeName.equals("seqDescriptionCVName")) {
       seqDescriptionCVName = getTextNodeValue(node);
      }
      else if (nodeName.equals("seqDescriptionTerm")) {
       seqDescriptionTerm = getTextNodeValue(node);
      }
      
      else if (nodeName.equals("retrieveAnnotations")) {
        retrieveAnnotations = getTextNodeBooleanObj(node);
      }

      else if (nodeName.equals("copyGeneModelsIntoResultTier")) {
        copyGeneModelsIntoResultTier = getTextNodeBooleanObj(node);
      }

      else if (nodeName.equals("cacheAnalysisTable")) {
        cacheAnalysisTable = getTextNodeBooleanObj(node);
      }

      // ONE LEVEL ANNOT TYPES
      else if (nodeName.equals("oneLevelAnnotTypes")) {
        oneLevelTypes = extractAnnotTypes(node);
      }

      // 3 LEVEL ANNOT TYPES
      else if (nodeName.equals("threeLevelAnnotTypes")) {
        threeLevelTypes = extractAnnotTypes(node);
      }

      // GENE PREDICTION PROGRAMS
      else if (nodeName.equals("genePredictionPrograms")) {
        genePredictionPrograms = extractRichPrograms(node);//PredictedCdsRetrieval
      }

      // SEARCH HIT PROGRAMS
      else if (nodeName.equals("searchHitPrograms")) {
        searchHitPrograms = extractRichPrograms(node);
      }

      // SEARCH HITS HAVE FEAT LOCS (boolean)
      else if (nodeName.equals("searchHitsHaveFeatLocs")) {
        searchHitsHaveFeatLocs = getTextNodeBooleanObj(node);
      }

      // ONE LEVEL RESULT PROGRAMS
      else if (nodeName.equals("oneLevelResultPrograms")) {
        oneLevelResultPrograms = extractRichPrograms(node);
      }
      
      // USE SYNONYMS FOR LOOKING UP FEATURES
      else if (nodeName.equals("useSynonyms")) {
        useSynonyms = getTextNodeBooleanObj(node);
      }
      
      else {
        logger.error("Unknown xml element in chado config file: "+nodeName);
      }
    }
    // Need to construct ChadoInstance
    ChadoInstance inst = null;
    // should there be a default if no className? FlybaseChadoInstance?
    // DefaultChadoInstance?
    if (className != null || inheritedInstance != null) {
      try {
        if (inheritedInstance != null) {
          ChadoInstance superClass = getChadoInstance(inheritedInstance);
          inst = superClass.cloneInstance();
        }
        else {
          Class cls = Class.forName(className);
          inst = (ChadoInstance) cls.newInstance();
        }
        if (oneLevelTypes != null)
          inst.setOneLevelAnnotTypes(oneLevelTypes);
        if (seqTypeList != null)
          inst.setSeqTypeList(seqTypeList);
        if (threeLevelTypes != null)
          inst.setThreeLevelAnnotTypes(threeLevelTypes);
        if (writebackTemplate != null)
          inst.setWritebackTemplateFile(writebackTemplate);
        if (featureCV != null)
          inst.setFeatureCVName(featureCV);
        if (polypeptideType != null)
          inst.setPolypeptideType(polypeptideType);
        if (relationshipCV != null)
          inst.setRelationshipCVName(relationshipCV);
        if (partOfCvTerm != null)
          inst.setPartOfCvTerm(partOfCvTerm);
        if (transProtRelTerm != null)
          inst.setTransProtRelationTerm(transProtRelTerm);
        if (transGeneRelTerm != null)
          inst.setTransGeneRelationTerm(transGeneRelTerm);
        if (exonTransRelTerm != null)
          inst.setExonTransRelationTerm(exonTransRelTerm);
        if (cdsTransRelTerm != null)
          inst.setCdsTransRelationTerm(cdsTransRelTerm);
        if (polypeptideCdsRelTerm != null)
          inst.setPolypeptideCdsRelationTerm(polypeptideCdsRelTerm);
        if (polypeptideTransRelTerm != null)
          inst.setPolypeptideTransRelationTerm(polypeptideTransRelTerm);
        if (propertyTypeCV != null)
          inst.setPropertyTypeCVName(propertyTypeCV);
        if (syntenyRelationshipType != null)
          inst.setSyntenyRelationshipType(syntenyRelationshipType);	  
        if (seqDescriptionCVName != null)
          inst.setSeqDescriptionCVName(seqDescriptionCVName);
        if (seqDescriptionTerm != null)
          inst.setSeqDescriptionTerm(seqDescriptionTerm);
        if (featureOwnerPropertyTerm != null) 
          inst.setFeatureOwnerPropertyTerm(featureOwnerPropertyTerm);
        if (featureCreateDatePropertyTerm != null) 
          inst.setFeatureCreateDatePropertyTerm(featureCreateDatePropertyTerm);
        if (commentPropertyTerm != null) 
          inst.setCommentPropertyTerm(commentPropertyTerm);	  
        if (genePredictionPrograms != null)
          inst.setGenePredictionPrograms(genePredictionPrograms);
        if (searchHitPrograms != null)
          inst.setSearchHitPrograms(searchHitPrograms);
        if (oneLevelResultPrograms != null)
          inst.setOneLevelResultPrograms(oneLevelResultPrograms);

        if (searchHitsHaveFeatLocs != null)
          inst.setSearchHitsHaveFeatLocs(searchHitsHaveFeatLocs.booleanValue());
        if (retrieveAnnotations != null)
          inst.setRetrieveAnnotations(retrieveAnnotations.booleanValue());
        if (copyGeneModelsIntoResultTier != null)
          inst.setCopyGeneModelsIntoResultTier(copyGeneModelsIntoResultTier.booleanValue());
        if (cacheAnalysisTable != null)
          inst.setCacheAnalysisTable(cacheAnalysisTable.booleanValue());
        if (pureJDBCWriteMode != null)
          inst.setPureJDBCWriteMode(pureJDBCWriteMode.booleanValue());
        if (pureJDBCCopyOnWrite != null)
          inst.setPureJDBCCopyOnWrite(pureJDBCCopyOnWrite.booleanValue());
        if (pureJDBCNoCommit != null) 
          inst.setPureJDBCNoCommit(pureJDBCNoCommit.booleanValue());
        if (pureJDBCUseCDS != null) 
          inst.setPureJDBCUseCDS(pureJDBCUseCDS.booleanValue());
        if (logDirectory != null) 
          inst.setLogDirectory(logDirectory);
        if (queryFeatureIdWithUniquename != null)
          inst.setQueryFeatureIdWithUniquename(queryFeatureIdWithUniquename.booleanValue());
        if (queryFeatureIdWithName != null)
          inst.setQueryFeatureIdWithName(queryFeatureIdWithName.booleanValue());
        
        if (useSynonyms != null) {
          inst.setUseSynonyms(useSynonyms.booleanValue());
        }
      }
      catch (UnsupportedOperationException e) {
        logger.error("ChadoAdapter.generateChadoInstance(): " + e, e);
      }
      catch (Exception e) {
        logger.error("ChadoAdapter.generateChadoInstance(): " + e, e);
      }
    }
    return inst;
  }

  private List extractAnnotTypes(Node typeNode) {
    NodeList children = typeNode.getChildNodes();
    int size = children.getLength();
    List rtn = new ArrayList(size);
    for (int i = 0; i < size; i++) {
      Node node = children.item(i);
      if (node.getNodeName().equals("type")) {
        rtn.add(getTextNodeValue(node));
      }
    }
    return rtn;
  }

  /** Parses comma delimited node text into a List */
  private List extractValues(Node node) {
    String text = getTextNodeValue(node);
    StringTokenizer tokenizer = new StringTokenizer(text, ", ");
    List list = new ArrayList(tokenizer.countTokens());
    while (tokenizer.hasMoreTokens())
      list.add(tokenizer.nextToken());
    //db.setChromosomes(list);
    return list;
  }
  
  /** Returns a List of SeqTypes */
  private List extractSeqTypes(Node node) { //,ChadoDatabase db){
    List seqTypes = new ArrayList();
    NodeList list = node.getChildNodes();
    int size = list.getLength();
    for (int i = 0; i < size; i++) {
      Node childNode = list.item(i);
      if (childNode.getNodeName().equals("type")) {
        SeqType st = extractSeqType(childNode); // db
        seqTypes.add(st);
      }
    }
    return seqTypes;
  }

  private SeqType extractSeqType(Node typeNode) { //ChadoDatabase db
    NodeList nodeList = typeNode.getChildNodes();

    // if just one child then its a text node of the seq type
    if (nodeList.getLength() == 1) {
      //db.addSeqType(getTextNodeValue(typeNode));
      return new SeqType(getTextNodeValue(typeNode));
    }

    // otherwise there is other stuff specified (name,startEnd,query...)
    //ChadoDatabase.SeqType seqType = db.createSeqType();
    SeqType seqType = new SeqType();
    for (int i=0; i<nodeList.getLength(); i++) {
      Node childNode = nodeList.item(i);
      if (childNode.getNodeName().equals("name"))
        //db.addSeqType(getTextNodeValue(childNode));
        seqType.setName(getTextNodeValue(childNode));
      // should this be renamed useLocation?
      else if (childNode.getNodeName().equals("useStartAndEnd"))
        seqType.setHasStartAndEnd(getTextNodeBoolean(childNode));
      else if (childNode.getNodeName().equals("queryForValueList"))
        seqType.setQueryForValues(getTextNodeBoolean(childNode));
      else if (childNode.getNodeName().equals("isTopLevel"))
        seqType.setIsTopLevelFeatType(getTextNodeBoolean(childNode));
      else if (childNode.getNodeName().equals("values"))
        seqType.setValues(extractValues(childNode));
    }
    //db.addSeqType(seqType);
    return seqType;
  }
  
  /**
   * Wrapper for ChadoProgram[] extractRichPrograms(Node programsNode) that returns
   * the 'program' fields as an array of Strings but throws away everything else.
   * 
   * @param programsNode
   * 
   * 
   * @return String[]
   * @see #extractRichPrograms(Node)
   */
  private String[] extractPrograms(Node programsNode) {
    
    ChadoProgram[] prg=extractRichPrograms( programsNode);
    String[] ret = new String [prg.length];
    for (int i = 0; i< prg.length;i++){
      ret[i]=prg[i].getProgram();
    }
    return ret;
  }
  
  // PredictedCdsRetrieval : working with ChadoProgram rather than String[]
  private ChadoProgram[] extractRichPrograms(Node programsNode) {
    NodeList children = programsNode.getChildNodes();
    int size = children.getLength();
    List programList = new ArrayList();
    
    for (int i = 0; i < size; i++) {
      Node progNode = children.item(i);

      // PROGRAM not all children are "program"
      if (progNode.getNodeName().equals("program")) {
        NodeList progTree = progNode.getChildNodes();

        // rich program node (ie exist subtree)
        if (progTree.getLength()>1) {
          String program = null;
          String programversion = null;
          String sourcename = null;
          boolean retrievecds = true; // for reverse compatibility
          
          // PROGRAM SUB ELEMENTS - name, retrieveCDS, source, version
          for (int j = 0; j < progTree.getLength(); j++) {
            Node progSubNode = progTree.item(j);
            if (progSubNode.getNodeName().equals("retrieveCDS")) {
              retrievecds = getTextNodeBoolean(progSubNode);
            }
            else if (progSubNode.getNodeName().equals("name")) {
              program = getTextNodeValue(progSubNode);
            }
            else if (progSubNode.getNodeName().equals("source")) {
              sourcename = getTextNodeValue(progSubNode);
            }
            else if (progSubNode.getNodeName().equals("version")) {
              programversion = getTextNodeValue(progSubNode);
            }
          }
          programList.add(new ChadoProgram(program, programversion, sourcename, retrievecds));

        } else {// no child, this program node only specifies the
          programList.add( new ChadoProgram(getTextNodeValue(progNode)));
        }
      } // end of PROGRAM

    }
    ChadoProgram[] programs = new ChadoProgram[programList.size()];
    programList.toArray(programs);
    return programs;
  }
  
  private String getTextNodeValue(Node node) {
    Node textNode = node.getFirstChild();
    return textNode.getNodeValue().trim();
  }

  private boolean getTextNodeBoolean(Node node) {
    return getTextNodeValue(node).equals("true");
  }

  private Boolean getTextNodeBooleanObj(Node node) {
    return getTextNodeValue(node).equals("true") ? Boolean.TRUE : Boolean.FALSE;
  }

}
