/* Jalview - a java multiple alignment editor
 * Copyright (C) 1998  Michele Clamp
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package apollo.dataadapter;

import apollo.main.Apollo;

import java.net.*;
import java.io.*;
import java.util.*;

public class CGI implements Runnable {
  protected String      server;
  protected int         port = 80;
  protected String      location;
  protected String      data;
  protected String      method = "GET";
  protected PrintStream statout;

  protected Hashtable   variables;

  protected URLConnection   connection;
  protected PrintStream     out;
  protected DataInputStream in;

  protected int             timeout = 5000;

  public CGI(String server,int port,String location,Hashtable variables,
             PrintStream statout, String method) {
    this.server    = server;
    this.port      = port;
    this.location  = location;
    this.variables = variables;
    this.method    = method;
    this.statout   = statout;
  }

  public CGI(String server,int port,String location,Hashtable variables,
             PrintStream statout) {
    this(server,port,location,variables,statout,"GET");
  }

  public CGI(String server, int port, String location,PrintStream statout) {
    this(server,port,location,(Hashtable)null,statout);
  }

  public CGI(String server,String location,PrintStream statout) {
    this(server,80,null,statout);
  }
  public void setServer(String server) {
    this.server = server;
  }
  public String getServer() {
    return server;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getPort() {
    return port;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }
  public int getTimeout() {
    return timeout;
  }

  public static boolean test(String server, int port,int timeout) {
    try {

      Socket socket = new Socket(server,port);
      //Set the server connect timeout to 5 seconds
      socket.setSoTimeout(timeout);
    } catch(UnknownHostException e) {
      System.out.println(e);
      return false;
    }
    catch (SocketException e) {
      System.out.println("Socket Exception " + e);
      return false;
    }
    catch (InterruptedIOException e) {
      System.out.println("Read to server timed out " + e);
      return false;
    }
    catch(IOException e) {
      System.out.println("IOException " + e);
      return false;
    }
    return true;
  }

  public void run() {
    Apollo.setLog4JDiagnosticContext();
    URL cgiServer = null;
    try {

      /* Try without test
            if (test(server,port,getTimeout())) {
      */
      if (method.equals("POST")) {
        cgiServer = new URL("http://" + server + ":" + port + "/" + location);

        connection = cgiServer.openConnection();

        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        //statout.println("Connection is " + connection);

        connection.setDoOutput(true);

        out = new PrintStream(connection.getOutputStream());

        //out.println("FORM=" + URLEncoder.encode("SERVERSELECT"));
        //System.out.print(data);
        //out.print(data);

        if (variables != null) {
          out.print(getVariablesString());
        }
      } else { /* GET */
        cgiServer = new URL("http://" + server + ":" + port + "/" + location + "?" + getVariablesString());
	//	System.out.println("CGI: cgiServer = " + cgiServer); // DEL

	// We're going to handle possibly multiple redirects, but don't keep trying forever
	for (int attempts = 0; attempts < 10; attempts++) {
	  connection = cgiServer.openConnection();
	  connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	  if (connection instanceof HttpURLConnection) {
	    //	    System.out.println("CGI: setting request method to GET");
	    ((HttpURLConnection)connection).setRequestMethod("GET");
	    // The Ensembl CGI URL is redirecting to a filename with no host,
	    // which seems to confuse certain versions of Java (JDK1.4 on Mac),
	    // so we'll do the redirect ourselves.
	    ((HttpURLConnection)connection).setInstanceFollowRedirects(false);
	  }
	  connection.setAllowUserInteraction(false);
	  connection.connect();
	  String redirectedTo = connection.getHeaderField("Location");
	  // If we weren't redirected, then we're done--we already have a good connection.
	  if (redirectedTo == null) {
	    //	    System.out.println("CGI.run: URL " + cgiServer + " didn't redirect");  // DEL
	    break;
	  }
	  //	  System.out.println("CGI.run: URL " + cgiServer + " redirects to " + redirectedTo); // DEL
	  // Construct a new URL from the old URL and the redirected path
	  cgiServer = new URL(cgiServer, redirectedTo);
	  //	  System.out.println("CGI.run: redirected URL = " + cgiServer);
	}
      }
      //statout.println("Transferred data to server");
      //statout.println("Waiting for output data...");
      in = new DataInputStream(connection.getInputStream());

    } catch (MalformedURLException ex) {
      System.out.println("CGI.run: URL = " + cgiServer + ".  Caught malformed URL exception " + ex);
    }
    catch (IOException ioex) {
      System.out.println("CGI.run: URL = " + cgiServer + ".  Caught io exception " + ioex);
    }
    catch (Exception ex) {
      System.out.println("CGI.run: URL = " + cgiServer + ".  Caught exception " + ex);
    }

    // When we catch an exception, should we maybe return something to indicate
    // that all is not well?
    Apollo.clearLog4JDiagnosticContext();
  }

  private String getVariablesString() {
    StringBuffer buf = new StringBuffer();
    Enumeration en = variables.keys();
    String sep = "";

    while (en.hasMoreElements()) {
      String name  = (String)en.nextElement();
      String value = (String)variables.get(name);

      buf.append(sep + name + "=" + URLEncoder.encode(value));
      sep = "&";
    }
    // System.out.println("buf = " + buf);
    return buf.toString();
  }

  public void readInput(DataInputStream in) {

    String data = "";

    try {
      while ((data = in.readLine()) != null) {
        statout.println(data);


      }

    } catch (IOException ioex) {
      System.out.println("Exception " + ioex);
    }
  }
  public DataInputStream getInput() {
    return in;
  }
  public void close() {
    ((HttpURLConnection)connection).disconnect();
  }
}
