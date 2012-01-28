package apollo.external;

import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.util.MultiException;

import apollo.config.Config;

import org.apache.log4j.*;

/** this class kicks off the servlet. modeled after igb's UnibrowControlServer 
 * maybe this should go in dataadapter? as its gonna end up loading new data 
 * actually for now it just scrolls/zooms to range */

public class ApolloControlServer {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(ApolloControlServer.class);

  private static final int DEFAULT_SERVER_PORT = 8085; // igb is 7085
  private static final String SERVLET_NAME = "ApolloControl";
  
  public ApolloControlServer() {

    // for now just setting port to default - eventually should test if port
    // is free, and if not try other ports (like igb does)
    int serverPort = DEFAULT_SERVER_PORT;
    
    HttpServer httpServer = new HttpServer();
    // Create a port listener
    SocketListener listener = new SocketListener();
    listener.setPort(serverPort);
    httpServer.addListener(listener);
    // Create a context
    HttpContext context = new HttpContext();
    context.setContextPath("/");
    // Create a servlet container
    ServletHandler servlets = new ServletHandler();
    context.addHandler(servlets);

    String servletClassName = "apollo.external.ApolloControlServlet";

    // Map a servlet onto the container
    ServletHolder sholder = servlets.addServlet(SERVLET_NAME, "/"+SERVLET_NAME+"/*",
                                                servletClassName);
    sholder.setInitOrder(1);
    httpServer.addContext(context);
    
    try {
      // Start the http server
      httpServer.start(); // throws MultiException
    }
    catch (MultiException e) {
      logger.debug("http server wont start", e);
    }

    String s =  "http://localhost:"+serverPort+"/"+SERVLET_NAME;
    logger.debug("http server started at "+s);
    //ApolloControlServlet apolloController = 
    // (ApolloControlServlet)sholder.getServlet();
  }

}
