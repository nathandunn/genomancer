package genomancer.tengcha;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

public class TengchaJettyServer  {

    public static void main(String[] args) {
	int portnum = 9099;
        try {
            System.out.println("Tengcha Plugin, Jetty Server Test");
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
				    "genomancer.tengcha.SourcesPlugin" );
	    holder.setInitParameter("sources_query", "http://localhost:" + portnum + "/trellis/sources");
            server.start();
            server.join();
        } catch (Exception ex) {
            Logger.getLogger(TengchaJettyServer.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

}  