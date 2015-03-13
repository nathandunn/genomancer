package genomancer.trellis.das2.server;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 *  a Das2Servlet running in it's own Jetty web server, 
 *     can be started up as a standalone server.  
 *  Very useful for rapid testing 
 *
 *  For production deployment the Das2Servlet should be treated as a standard 
 *    web application and set up via standard web application configuration 
 *    (for example in Tomcat getting ServletConfig params from web.xml config file)
 */
public class ExampleJettyDas2Server  {


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
	int portnum = 9096;
	String root_path = "/das2/genome";
        try {
            System.out.println("Testing, testing");
            Server server = new Server();
            Connector connector = new SocketConnector();
            connector.setPort(portnum);
            server.setConnectors(new Connector[]{connector});
            ServletHandler handler = new ServletHandler();
            server.setHandler(handler);
            ServletHolder holder = 
		handler.addServletWithMapping("genomancer.trellis.das2.server.TrellisDas2Servlet", 
					      root_path + "/*");  
	    /*
	    holder.setInitParameter("sources_plugin_class", 
				    ???);
            holder.setInitParameter("sources_capability_class", 
				    "genomancer.vine.das2.client.modelimpl.Das2SourcesCapability");
            holder.setInitParameter("sources_capability_params", 
				    "query_uri=" + 
				    "file:///Users/gregg/projects/genomancer/data/netaffx_das2_sources.mod.xml" );

            holder.setInitParameter("sources_query_uri", 
				    "http://localhost:" + portnum + root_path + "/sources");
	    */
            server.start();
            server.join();
        } catch (Exception ex) {
            Logger.getLogger(ExampleJettyDas2Server.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

}