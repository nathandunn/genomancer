package plugin_example;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

public class ExampleJettyServer  {

    public static void main(String[] args) {
	int portnum = 9099;
        try {
            System.out.println("Trellis Plugin Example, Jetty Server Test");
            Server server = new Server();
            Connector connector = new SocketConnector();
            connector.setPort(portnum);
            server.setConnectors(new Connector[]{connector});
            ServletHandler handler = new ServletHandler();
            server.setHandler(handler);

	    ServletHolder holder = 
		handler.addServletWithMapping("genomancer.trellis.das2.server.TrellisDas2Servlet", 
					      "/trellis/*");
	    holder.setInitParameter("sources_plugin_class", 
				    "plugin_example.SourcesPluginExample" );
	    holder.setInitParameter("sources_query", "http://localhost:" + portnum + "/trellis/sources");
	    //	    holder.setInitParameter("renewal_rate_minutes", "5");
	    //      holder.setInitParameter("renewal_delay_minutes", "2");
            server.start();
            server.join();
        } catch (Exception ex) {
            Logger.getLogger(ExampleJettyServer.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

}  