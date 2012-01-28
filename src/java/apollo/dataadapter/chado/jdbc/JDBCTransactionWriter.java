/*
 * Created on Nov 18, 2004
 *
 */
package apollo.dataadapter.chado.jdbc;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.*;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import apollo.config.Config;
import apollo.dataadapter.TransactionOutputAdapter;
import apollo.dataadapter.chado.ChadoDatabase;
import apollo.dataadapter.chado.ChadoTransaction;
import apollo.dataadapter.chado.ChadoTransactionTransformer;
import apollo.dataadapter.chado.ChadoUpdateTransaction;
import apollo.dataadapter.chadoxml.ChadoTransactionXMLTemplate;
import apollo.util.IOUtil;

/**
 * This class is used to write/commit a List of ChadoTransactions
 * into a chado database using JDBC.
 *
 * @author wgm
 */
public class JDBCTransactionWriter extends TransactionOutputAdapter {
    
  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(JDBCTransactionWriter.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private String dbUrl;
  private String dbUser;
  private String dbPwd;

  private Connection conn;

  private Map idMap;

  private String writebackTemplateFilename = null;
  private List chadoTransMacrosFromConfig = null;

  // If true then print insert/update/delete SQL to stdout instead of updating the DB
  // The updates will be run as usual (in order to correctly simulate what would have
  // happened during a real commit), but then a rollback() will be issued to undo them.
  private boolean printOnly = false;
  // JC: printOnly isn't such a good name for this.  Could split it into two parameters,
  // one that specifies a rollback() should always be done ("noCommit") and one that 
  // specifies logging all SQL to console/file.

  // -----------------------------------------------------------------------
  // Constructors
  // -----------------------------------------------------------------------
  
  /** This is only used by test classes, should change test classes to chado db
      need template file as well which comes with db */
  public JDBCTransactionWriter(String dbHost,
			       String dbName,
			       String dbUser,
			       String pwd,
			       int port) {
    this("jdbc:postgresql://" + dbHost + ":" + port + "/" + dbName, 
         dbUser, 
         pwd);
  }
  
  /**
   *@param chadoDB  ChadoDatabase to which changes should be written.
   */
  public JDBCTransactionWriter(ChadoDatabase chadoDB) {
    this(chadoDB.getJdbcUrl(),chadoDB.getLogin(),chadoDB.getPassword());
    chadoTransMacrosFromConfig = chadoDB.getChadoInstance().getChadoTransMacros();
    writebackTemplateFilename = chadoDB.getChadoInstance().getWritebackTemplateFile();
  }

  /**
   * @param url    JDBC URL of the chado database to which changes should be written.
   * @param dbUser Name of a login with insert/update/delete privileges
   * @param pwd    Password for the login <code>dbUser</code>
   */
  private JDBCTransactionWriter(String url,
				String dbUser,
				String pwd) {
    this.dbUser = dbUser;
    this.dbPwd = pwd;
    this.dbUrl = url;

    // JC: this is still a hack, but slightly less so than before...
    String driverClass = null;
    if (isPostgreSQL()) driverClass = "org.postgresql.Driver";

    // JC: It shouldn't be necessary to load the driver class if we've already used
    // the JDBC chado adapter to *read* info. from this same database.  However, 
    // that may not always be the case, e.g. converting data from GAME XML to 
    // chado JDBC?  In any case, the real problem here is that we're duplicating
    // functionality in the writeback adapter that's already been covered in the
    // read adapter.

    try {
	if (driverClass != null) Class.forName(driverClass);
	conn = getConnection();
    }
    catch(Exception e) {
      logger.error("Exception in JDBCTransactionWriter.init()", e);
    }
    idMap = new HashMap();    
  }

  // -----------------------------------------------------------------------
  // TransactionOutputAdapter
  // -----------------------------------------------------------------------
  
  /** This takes a list of chado transactions (not apollo transactions) and commits
      them */
  protected void commitTransformedTransactions(List transformedTn) throws Exception {
    if (transformedTn == null || transformedTn.size() == 0) { 
      logger.info("JDBCTransactionWriter.commitTransformedTransactions(): no transactions to commit.");
      return;
    }

    List macros = new ArrayList();
    // add macros from config - hafta do first - has ontologies...
    if (chadoTransMacrosFromConfig != null)
      macros.addAll(chadoTransMacrosFromConfig);
    // Have to load transactions in the template file.
    macros.addAll(loadTnsFromTemplate());

    // Add another transaction for src feature
    logger.debug("Creating srcfeature ID transaction with mapID=" + mapID + " mapType=" + mapType);
    ChadoTransaction srcTn = 
      ((ChadoTransactionTransformer)transformer).createSrcFeatureIDTransaction(mapID, mapType);
    macros.add(srcTn);
    List tns = new ArrayList();
    tns.addAll(macros);
    tns.addAll(transformedTn);
    // Wrap all SQL operations in one transaction in case anything is wrong
    Connection connection = null;
    try {
      connection = getConnection();
    }
    catch(SQLException e) { // Popup the exception
      throw e;
    }
    try {
      connection.setAutoCommit(false);
      ChadoTransaction tn = null;
      ChadoTransaction.Operation op = null;
      for (Iterator it = tns.iterator(); it.hasNext();) {
        tn = (ChadoTransaction) it.next();
        op = tn.getOperation();
	logger.debug("ChadoTransaction: " + tn);

        if (op == ChadoTransaction.LOOKUP) {
          commitLookup(tn, connection);
        }
        else if (op == ChadoTransaction.INSERT) {
          commitInsert(tn, connection);
        }
        else if (op == ChadoTransaction.FORCE) {
          commitForce(tn, connection);
        }
        else if (op == ChadoTransaction.DELETE) {
          commitDelete(tn, connection);
        }
        else if (op == ChadoTransaction.UPDATE) {
          commitUpdate(tn, connection);
        }
      }

      // If running in printOnly mode do a rollback()
      if (printOnly) {
	try {
	  connection.rollback();
          logger.debug("JDBCTransactionWriter.commitTransaction(): " +
                       "Successful rollback() due to running in printOnly mode");

	}
	catch(SQLException e1) {
	  logger.debug("JDBCTransactionWriter.commitTransaction(): " +
                       "FAILED rollback() in printOnly mode.  Please clean up the mess you just made.");
	}
      }
      // Otherwise (not in printOnly mode) do a commit()
      else {
	connection.commit();
      }
      connection.close();
      logger.debug("JDBCTransactionWriter.commitTransaction(): " +
                   "Transactions saved to the database successfully.");
    }
    catch (Exception e) {

      logger.error("Exception in commitTransformedTransactions", e);

      // Have to roll back if anything went wrong
      try {
        connection.rollback();
        logger.debug("JDBCTransactionWriter.commitTransaction(): " +
                     "Successful rollback() in response to a caught exception");

      }
      catch(SQLException e1) {
        logger.error("JDBCTransactionWriter.commitTransaction(): " +
                     "Cannot roll back changes during exception throwing. " +
                     "Please contact DBA ASAP!!!", e1);
      }

      // only popup exception if not in printonly mode; this allows
      // ChadoAdapter to think that transactions were committed 
      // correctly and allow it to clear the transaction log 
      // (which is desirable for interactive debugging)
      if (!printOnly) {
	throw e; // popup any exception.
      }
    }
  }

  // -----------------------------------------------------------------------
  // JDBCTransactionWriter - private methods
  // -----------------------------------------------------------------------
  
  private Connection getConnection() throws SQLException {
    if (conn != null &&  !conn.isClosed())
      return conn;
    // Have to initialize
    conn = DriverManager.getConnection(dbUrl, dbUser, dbPwd);
    target = conn;
    return conn;
  }

  private String getWritebackTemplateFilename() {
    if (writebackTemplateFilename == null) // didnt get set from chado db, use config
      writebackTemplateFilename = Config.getChadoTemplateName();
    return writebackTemplateFilename;
  }
  
  private List loadTnsFromTemplate() {
    List tns = new ArrayList();
    String name = "conf" + File.separator + getWritebackTemplateFilename();
    String tmpFileName = IOUtil.findFile(name);
    if (tmpFileName == null) {
      String m="JDBCTransactionWriter.loadTnsFromTemplate(): Cannot find xml template"+
        "for chado transaction. filename: "+name;
      throw new IllegalStateException(m);
    }
    ChadoTransactionXMLTemplate template = new ChadoTransactionXMLTemplate(tmpFileName);    
    Element elm = template.getElement("preamble");
    NodeList children = elm.getChildNodes();
    int size = children.getLength();
    for (int i = 0; i < size; i++) {
      Node tmp = children.item(i);
      if (tmp.getNodeType() == Node.ELEMENT_NODE) 
        tns.add(loadOpElement((Element)tmp));
    }
    return tns;
  }
  
  private ChadoTransaction loadOpElement(Element elm) {
    String op = elm.getAttribute("op");
    ChadoTransaction tn = null;
    if (op.equals("update"))
      tn = new ChadoUpdateTransaction();
    else
      tn = new ChadoTransaction();
    String tableName = elm.getNodeName();
    tn.setTableName(tableName);
    if (op == null || op.length() == 0)
      tn.setOperation(ChadoTransaction.FORCE);
    else
      tn.setOperation(ChadoTransaction.Operation.getOperation(op));
    String id = elm.getAttribute("id");
    if (id != null && id.length() > 0)
      tn.setID(id);
    // Get properties
    NodeList children = elm.getChildNodes();
    int size = children.getLength();
    if (tn instanceof ChadoUpdateTransaction) {
      for (int i = 0; i < size; i++) {
        Node tmp = children.item(i);
        if (tmp.getNodeType() == Node.ELEMENT_NODE) {
          String propName = tmp.getNodeName();
          String update = ((Element)tmp).getAttribute("update");
          String value = getTextValue(tmp);
          if (update == null || update.length() == 0)
            tn.addProperty(propName, value);
          else
            ((ChadoUpdateTransaction)tn).addUpdateProperty(propName, value);
        }
      }
    }
    else {
      for (int i = 0; i < size; i++) {
        Node tmp = children.item(i);
        if (tmp.getNodeType() == Node.ELEMENT_NODE) {
          String propName = tmp.getNodeName();
          String value = getTextValue(tmp);
          tn.addProperty(propName, value);
        }
      }
    }
    return tn;
  }
  
  private String getTextValue(Node elm) {
    return elm.getFirstChild().getNodeValue();
  }
  
  private void commitLookup(ChadoTransaction tn, Connection conn) 
          throws SQLException {
    long id = lookup(tn, conn);

    // if id < 0, its -1 and it failed to return a real id
    if (id < 0) {
      // Nothing found
      String m = "Error in JDBCTransactionWriter.commitLookup()\nLookup query "+
        "failed to return anything:\n "+makeLookupQuery(tn);
      //throw new IllegalStateException("JDBCTransactionWriter.commitLookup(): "+id+" cannot be found.");
      throw new IllegalStateException(m);
    }
  }
  
  /** If found then tn.getID() is put in idMap to "tableName"_id for future
      use */
  private long lookup(ChadoTransaction tn, Connection conn) throws SQLException {
//     String tableName = tn.getTableName();
//     StringBuffer query = new StringBuffer();
//     query.append("SELECT ");
//     query.append(tableName);
//     query.append("_id FROM ");
//     query.append(tableName);
//     Map prop = tn.getProperties();
//     constructWhereClause(prop, query);
    String query = makeLookupQuery(tn);
    logger.debug("Lookup Query: " + query);
    Statement stat = conn.createStatement();
    ResultSet result = stat.executeQuery(query);
    int count = 0;
    // Need to get id
    long id1 = -1;
    String id = tn.getID();
    while (result.next()) {
      count ++;
      id1 = result.getLong(1);
      idMap.put(id, new Long(id1));
    }
    cleanUp(result, stat);
    if (count > 1)
      throw new IllegalStateException("JDBCTransactionWriter.lookup(): more than one row matches the query.");
    return id1;
  }

  /** Uses ChadoTransaction.getUniqueKeyProps for lookup query - different for 
      exons (those dam shared exons) */
  private String makeLookupQuery(ChadoTransaction tn) {
    String tableName = tn.getTableName();
    //String id = tn.getID();
    StringBuffer query = new StringBuffer();
    query.append("SELECT ");
    query.append(tableName);
    query.append("_id FROM ");
    query.append(tableName);
    //Map prop = tn.getProperties();
    // for lookup only want to use props that are part of unique key. this is
    //currently only truly implemented for synonyms at this point, the rest just
    // return all props
    Map prop = tn.getUniqueKeyProps();
    constructWhereClause(prop, query);
    return query.toString();
  }
  
  private void constructWhereClause(Map prop, StringBuffer query) {
    query.append(" WHERE ");
    for (Iterator it = prop.keySet().iterator(); it.hasNext();) {
      String key = (String) it.next();
      String value = (String) prop.get(key); 
      query.append(key);
      query.append("=");
      appendValue(key, value, query);
      if (it.hasNext())
        query.append(" AND ");
    }    
  }
    
  private void cleanUp(ResultSet rs) throws SQLException {
    cleanUp(rs,rs.getStatement());
  }

  private void cleanUp(ResultSet result, Statement stat) throws SQLException {
    if (result != null)
      result.close();
    if (stat != null)
      stat.close();
  }
  
  private void commitInsert(ChadoTransaction tn, Connection conn) 
           throws SQLException {

    String tableName = tn.getTableName();
    String tempId = tn.getID(); // temp id
    long idLong = -1;

    // In PostgreSQL we query for the id first.  This is far more efficient 
    // (at least in postgres) than querying for max id afterwards 
    // which can take a whole minute (even though it's indexed!)
    //
    if (isPostgreSQL()) {
      String idQuery = "SELECT nextval('public."+tableName+"_"+tableName+"_id_seq')";
      logger.debug("commitInsert PostgreSQL idQuery SQL: " + idQuery);
      ResultSet rs = conn.createStatement().executeQuery(idQuery);
      rs.next();
      idLong = rs.getLong(1);
    
      if (tempId != null)
        idMap.put(tempId, new Long(idLong)); // map temp id to new real id
      cleanUp(rs); // close result set & statement
    }

    // In Sybase we'll do the max(id)+1 query first to determine the new id.
    // We don't have any sequences to rely on, as in PostgreSQL, and the
    // id columns aren't defined as Sybase IDENTITY types, so the database
    // won't assign an id automatically.  Luckily this query is very fast
    // in Sybase.
    //
    else if (isSybase()) {
      String idQuery = "SELECT MAX(" + tableName + "_id)+1 FROM " + tableName;
      logger.debug("commitInsert Sybase idQuery SQL: " + idQuery);
      ResultSet rs = conn.createStatement().executeQuery(idQuery);
      rs.next();
      idLong = rs.getLong(1);
      if (tempId != null)
	idMap.put(tempId, new Long(idLong)); // map temp id to new real id
      cleanUp(rs); // close result set & statement
    }
    else {
      throw new SQLException("Unknown DBMS type for JDBC URL '" + dbUrl + "'");
    }

    // HACK to deal with non-NULLable columns in Sybase chado feature table
    // Should be in the table defs in the transactionXMLTemplate file, but this
    // information is currently hard-coded and not read from the file.
    if (isSybase() && tableName.equals("feature")) {
      tn.addProperty("is_analysis", "0");
      tn.addProperty("is_obsolete", "0");
      tn.addProperty("timeaccessioned", "getdate()");
      tn.addProperty("timelastmodified", "getdate()");
    }
    if (isSybase() && tableName.equals("featureloc")) {
      tn.addProperty("is_fmin_partial", "0");
      tn.addProperty("is_fmax_partial", "0");
      tn.addProperty("rank", "0");
      // HACK - this will be a problem in the context of redundant featurelocs
      tn.addProperty("locgroup", "0");
    }
    // END HACK

    StringBuffer query = new StringBuffer();
    query.append("INSERT INTO ");
    query.append(tableName);
    query.append(" (");
    query.append(tableName + "_id, ");
    Map prop = tn.getProperties();
    for (Iterator it = prop.keySet().iterator(); it.hasNext();) {
      query.append(it.next());
      if (it.hasNext())
        query.append(", ");
    }
    query.append(") VALUES (");
    query.append(idLong + ", ");
    for (Iterator it = prop.keySet().iterator(); it.hasNext();) {
      String key = (String) it.next();
      String value = (String) prop.get(key);
      appendValue(key, value, query);
      if (it.hasNext())
        query.append(", ");
    }
    query.append(")");
    logger.debug("insert SQL: " + query.toString());
    Statement stat = conn.createStatement();
    int row = 0;

    try {
      row = stat.executeUpdate(query.toString());
    }
    catch (SQLException e) {
//       System.out.println("Next exception "+e.getNextException()+" err cod "+ e.getErrorCode()+ " state "+ e.getSQLState()
//                          +" conn warning:"+conn.getWarnings()
//         +"\nstmnt warning\n "+stat.getWarnings()+"\n"+" nex warning "+stat.getWarnings().getNextWarning() );
//       throw new SQLException(e.getMessage() + "\nProblematic query: "+query);
      processException(e,stat,query.toString());
    }

    if (row == 0)
      throw new IllegalStateException("JDBCTransactionWriter.commitInsert(): "
                                      + "insert cannot work.");
//     // Need id
//     if (id != null) {
//       //ResultSet result = stat.getGeneratedKeys(); not supported yet!
//       String sql = "SELECT max(" + tableName + "_id) FROM " + tableName;
//       debugMsgAndTime(sql);
//       ResultSet result = stat.executeQuery(sql);
//       if (result.next()) {
//         long idLong = result.getLong(1); // new db id
//         idMap.put(id, new Long(idLong)); // map temp id to new real id
//         cleanUp(result, stat);
//       }
//       else {
//         cleanUp(result, stat);
//         throw new IllegalStateException("JDBCTransactionWriter.commitInsert(): Cannot get id for inserting.");
//       }
//     }
//     else
    cleanUp(null, stat);
  }

  private void processException(SQLException e,Statement s, String sql) 
    throws SQLException {

    // Trigger raise notices come out in the statement warnings
    SQLWarning warning = s.getWarnings();
    while (warning != null) {
      logger.warn(warning.getMessage());
      warning = warning.getNextWarning();
    }
    
    throw new SQLException(e.getMessage() + "\nProblematic query: "+sql);
  }
  
  private void commitForce(ChadoTransaction tn, Connection conn) 
           throws SQLException {
    // Lookup first
    long id = lookup(tn, conn);
    // Insert if cannot find
    if (id < 0) { // Nothing found. Do insert
      logger.debug(this + " lookup failed to find " + tn + ", doing forced insert");
      commitInsert(tn, conn);
    }
  }

  private void commitUpdate(ChadoTransaction tn, Connection conn) 
           throws SQLException {
    String tableName = tn.getTableName();
    String id = tn.getID();

    // so whats up with this? is this just to check that in fact the id/uniquename is 
    // in the database - this wont work for exons as exon the uniquename is ignored
    // what if id is null? im a little confused about this
    /// well anyways shouldnt do this for exons - do we know if this is an exon
    // at this point?? - no we dont - im just gonna take this out then
    // presumably if we dont have a db id the update below will fail anyways right?
    // if in fact we need this then need to find way to work out exon issue - note
    // it in ChadoTransaction or something - ah there is isExon
    // this has to happen for exon too - this is where id gets added to idMap
    // for future lookups! - hmmmmmmmm.....
    // actually its ok to do exons just that isExon has to be true so it uses
    // theexon uniqueKey defined in ChadoTransaction!
    if (id != null) { // Need id
      logger.debug("id is not null - looking up id for update id: "+id);
      long idLong = lookup(tn, conn);
      if (idLong < 0)
        throw new IllegalStateException("JDBCTransactionWriter.commitUpdate(): cannot find record for updating.");
    }

    StringBuffer query = new StringBuffer();
    query.append("UPDATE ");
    query.append(tableName);
    query.append(" SET ");
    Map updateProp = ((ChadoUpdateTransaction)tn).getUpdateProperies();
    for (Iterator it = updateProp.keySet().iterator(); it.hasNext();) {
      String propName = (String) it.next();
      String value = (String) updateProp.get(propName);
      query.append(propName);
      query.append("=");
      appendValue(propName, value, query);
      if (it.hasNext())
        query.append(", ");
    }
    Map prop = tn.getProperties();
    constructWhereClause(prop, query);
    logger.debug("Update Query: " + query.toString());
    Statement stat = conn.createStatement();
    int row = stat.executeUpdate(query.toString());
    cleanUp(null, stat);
    if (row == 0) { // update failed to update a row - bad where clause
      String m="JDBCTransactionWriter.commitUpdate(): Cannot update. Update attempted "+
        "but no row was effected. Check where clause.";
      throw new IllegalStateException(m);
    }
    else if (row > 1)
      throw new IllegalStateException("JDBCTransactionWriter.commitUpdate(): More than one row is updated.");    
  }
  
  private void commitDelete(ChadoTransaction tn, Connection conn) throws SQLException {
    String tableName = tn.getTableName();
    String id = tn.getID();
    StringBuffer query = new StringBuffer();
    query.append("DELETE FROM ");
    query.append(tableName);
    Map prop = tn.getProperties();
    constructWhereClause(prop, query);
    logger.debug("Delete Query: " + query.toString());
    Statement stat = conn.createStatement();
    int row = stat.executeUpdate(query.toString());
    cleanUp(null, stat);
    if ((!printOnly) && (row == 0))
      throw new IllegalStateException("JDBCTransactionWriter.commitDelete(): Cannot delete a row.");
    else if (row > 1)
      throw new IllegalStateException("JDBCTransactionWriter.commitDelete(): More than one row is deleted.");
  }
  
  private void appendValue(String key, String value, StringBuffer query) {
    // Try to fetch id
    if (key.endsWith("_id") && idMap.containsKey(value))
      query.append(idMap.get(value));
    else {
      //if _id is not in idMap that probably means trouble! - it would be nice if we
      // could automatically try to lookup id (like for type_ids) but it would be hard
      // here as we have absolutely no context - for type_ids need to know which cv to
      // use
      if (key.endsWith("_id"))
        logger.debug("ERROR: no id mapped for " +key+" with value "+value+" for query "
                     +query+" Will try to do query without it but will probably fail");

      boolean addQuotes = true;

      // JC: HACK - don't quote values we know aren't character values
      if (isSybase()) {
	if (key.startsWith("is_") || key.startsWith("time")) {
	  addQuotes = false;
	}
	// featureloc
	if (key.equals("fmin") || key.equals("fmax") || key.equals("strand") || key.equals("rank") || key.equals("locgroup") ||
	    key.equals("seqlen")) {
	  addQuotes = false;
	}
	// going out on a limb here..
	if ((value == null) || (value.equals("null"))) {
	  addQuotes = false;
	}
      }

      if (addQuotes) {
	query.append("'");
	query.append(value);
	query.append("'");
      } else {
	query.append(value);
      }
    }
  }
  
  /**
   * Examines the JDBC URL to determine whether the target database is PostgresQL.
   */
  private boolean isPostgreSQL() {
    return (dbUrl != null) && (dbUrl.startsWith("jdbc:postgresql"));
  }

  /**
   * Examines the JDBC URL to determine whether the target database is Sybase.
   */
  private boolean isSybase() {
    return (dbUrl != null) && (dbUrl.startsWith("jdbc:sybase"));
  }
}
