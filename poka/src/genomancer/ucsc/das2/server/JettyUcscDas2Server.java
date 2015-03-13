package genomancer.ucsc.das2.server;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 *  A DAS/2 server front end for UCSC genome database (currently uses public MySQL server)
 *
 *  Uses the Trellis DAS/2 server framework 
 *  Uses the Poka UcscSourcesPlugin
 */
public class JettyUcscDas2Server  {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
	int portnum = 9093;
        try {
            System.out.println("Trellis/Poka UCSC DAS2 Server Test");
            Server server = new Server();
            Connector connector = new SocketConnector();
            connector.setPort(portnum);
            server.setConnectors(new Connector[]{connector});
            ServletHandler handler = new ServletHandler();
            server.setHandler(handler);

	    ServletHolder holder = 
		handler.addServletWithMapping("genomancer.trellis.das2.server.TrellisDas2Servlet", 
					      "/das2/genome/*");
	    holder.setInitParameter("sources_plugin_class", 
				    "genomancer.ucsc.das2.server.UcscSourcesPlugin");
	    holder.setInitParameter("sources_query", "http://localhost:9093/das2/genome/sources");
          // holder.setInitParameter("sources_query", "http://mship.local:9095/das2/ucsc/genome/sources");
	    // holder.setInitParameter("sources_query", "http://www.genomancer.org:9095/das2/ucsc/genome/sources");
	    holder.setInitParameter("renewal_rate_minutes", "1440");
	    holder.setInitParameter("renewal_delay_minutes", "1440");
            server.start();
            server.join();
        } catch (Exception ex) {
            Logger.getLogger(JettyUcscDas2Server.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

}  