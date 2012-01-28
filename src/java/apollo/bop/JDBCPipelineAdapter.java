package apollo.bop;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.sql.*;

import apollo.datamodel.*;
import apollo.dataadapter.exception.BopException;
import apollo.dataadapter.exception.NoOutputException;

import org.apache.log4j.*;

public class JDBCPipelineAdapter {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(JDBCPipelineAdapter.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  String driver = "org.gjt.mm.mysql.Driver";
  String default_host = "space.lbl.gov";
  String default_db = "pipe";
  String url;
  protected int current_job_id;
  protected JobOutput job_run;

  public JDBCPipelineAdapter(String host, String db) {
    url = ("jdbc:mysql://" +
	   (host != null ? host : default_host) + "/" +
	   (db != null ? db : default_db) +
	   "?user=jdbc");
  }

  public JDBCPipelineAdapter() {}

  //get raw output, analysis date, seq fasta for the job_id
  //3 items stored in the hash table
  public JobOutput retrieveJob(int job_id) 
    throws NoOutputException, BopException {

    job_run = new JobOutput();

    Statement stmt = null;
    Connection con = null;
    try {
      con = getConnection();
    } catch (Exception e) {
      throw new BopException("in retrieveJob: "+e.getMessage());
    }
    if (con == null)
      throw new BopException("Can not establish db connection");
      
    try { 
      stmt = con.createStatement();
      
      //get job processed date
      java.sql.ResultSet rSet = stmt.executeQuery
	("select j.time_processed, j.program, ds.name " +
	 "from job j, data_source_vrsn dsv, data_source ds " +
	 "where j.ds_vrsn_id = dsv.id and " +
	 "dsv.data_source_id = ds.id and " +
	 "j.id = " + job_id);

      if (rSet.next()) {
	// first get the time the job was run
	job_run.setRunDate (rSet.getLong(1));
	logger.info ("Job run on " + job_run.getRunDate());

	String program = rSet.getString(2);
	// finally the dataset that was compared to
	String seq_ds = rSet.getString(3).toLowerCase();
	if (program == null)
	  throw new NoOutputException("No program name for job: "+job_id);
	logger.info ("Job was " + program + ":" + seq_ds);
	job_run.setAnalysisType (program, seq_ds);
      }
          
      //get job output
      String strResult = "";
      java.sql.ResultSet rSet1 = stmt.executeQuery 
	("select output_text, is_compressed from job_output "+
	 "where job_id = "+job_id+" and output_type = 'out'");
      if (rSet1.next()) {
	String compressed = rSet1.getString(2);
	if (compressed.equalsIgnoreCase("t")) {
	  logger.info ("Have to decompress results");
	  //get job raw output (compressed)
	  java.sql.ResultSet rSet2 = stmt.executeQuery 
	    ("select output_text from job_out_compress "+
	     "where job_id = "+job_id);
	  if (rSet2.next()) {
	    String strCompressed = rSet2.getString(1);
	    ByteArrayInputStream bis
	      = new ByteArrayInputStream (strCompressed.getBytes());
	    InflaterInputStream istream 
	      = new InflaterInputStream(bis);
	    InputStreamReader r = new InputStreamReader(istream);
	    try {
	      BufferedReader bufreader = new BufferedReader(r);
	      String line = bufreader.readLine();
	      //slow, but when pass istream as InputStream obj
	      //bop parsers complaining about stream closed!
	      //could pass out as InputStreamReader obj, 
	      //but may read only once and 2 extra line problem
	      StringBuffer buf = new StringBuffer();
	      while (line != null) {
		buf.append(line + "\n");
		line = bufreader.readLine();
	      }
	      strResult = buf.toString();
	    }
	    catch (Exception e) {
	      throw new BopException("Can not decompress job: " + job_id +
				     " error: " + e.getMessage());
	    }
	  }
	}
	else {
	  strResult = rSet1.getString(1);
	}
	if (strResult == null || strResult.equals(""))
	  throw new NoOutputException("No output for job: "+job_id);
	else {
	  String extra1 = 
	    "Warning: no access to tty (Bad file descriptor).";
	  String extra2 = "Thus no job control in this shell.";
	  //manually remove 2 extra lines at beginning of output
	  if (strResult.startsWith(extra1)) {
	    logger.debug ("Trimming off " + extra1);
	    strResult = (strResult.substring(extra1.length())).trim();
	  }
	  if (strResult.startsWith(extra2)) {
	    if (strResult.length() > extra2.length()) {
	      logger.debug ("Trimming off " + extra2);
	      strResult = strResult.substring(extra2.length()).trim();
	    }
	    else
	      throw new NoOutputException("Empty job output");
	  }
	  logger.info ("Job raw output retrieved length=" +
                       strResult.length() + " " +
                       strResult.substring(0, 24));
	  job_run.setRawOutput(strResult);
	}
      }
          
      //get job seq fasta
      java.sql.ResultSet rSet2 = stmt.executeQuery
	("select seq.name, seq.description, seq.residues " +
	 "from job, job_seq, seq "+
	 "where job.id = job_seq.job_id and seq.id = job_seq.seq_id "+
	 "and job.id = "+job_id);
      if (rSet2.next()) {
	/*
	strResult = ">"+rSet2.getString(1); //description
	strResult += "\n"+rSet2.getString(2); //residues
	fasta_seq = strResult;
	*/
	SequenceI seq = new Sequence (rSet2.getString(1), // name
				      rSet2.getString(3)); //residues
	seq.setDescription (rSet2.getString(2)); //description
	job_run.setQuerySequence (seq);
	logger.info ("Job sequence retrieved: " + seq.getName());
	current_job_id = job_id;
      }
    } catch (SQLException e) {
      logger.error("Error getting raw output from db: "+e.getMessage(), e);
    }
    return job_run;
  }

  public Connection getConnection() throws Exception {
    Connection con = null;
      
    try {
      DriverManager.registerDriver
	((Driver)Class.forName(driver).newInstance());
      con = DriverManager.getConnection(url);
    } catch (InstantiationException e) {
      throw new Exception("Could not instantiate databasedriver: "+driver);
    } catch (IllegalAccessException e) {
      throw new Exception("Illege access using databasedriver: "+driver);
    } catch (ClassNotFoundException e) {
      throw new Exception("Could not locate JDBC driver class for mysql: "+driver);
    } catch (SQLException e) {
      throw new Exception(e.getMessage());
    }
    return con;
  }
}
