package genomancer.vine.das2.proxy;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 *  A DAS/2 server that acts as a proxy for other DAS/2 servers
 *
 *  Uses the Trellis DAS/2 server framework 
 *  Uses the Vine DAS/2 client to access a DAS/2 server being proxied
 *  Then just wraps the DAS/2 client Capabilities with ProxyCapabilities that 
 *      rewrite URI endpoints in the Das2***ResponseI objects passed back from queries
 *  
 */
public class JettyDas2ProxyServer  {


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
	int portnum = 9099;
        try {
            System.out.println("Vine DAS/2-->DAS/2 Proxy Server Test");
            Server server = new Server();
            Connector connector = new SocketConnector();
            connector.setPort(portnum);
            server.setConnectors(new Connector[]{connector});
            ServletHandler handler = new ServletHandler();
            server.setHandler(handler);

	    ServletHolder holder = 
		handler.addServletWithMapping("genomancer.trellis.das2.server.TrellisDas2Servlet", 
					      "/das2/proxy/genome/*");
	    holder.setInitParameter("sources_plugin_class", 
				    "genomancer.vine.das2.proxy.Das2ProxySourcesPlugin");
	    holder.setInitParameter("renewal_rate_minutes", "5");
	    holder.setInitParameter("renewal_delay_minutes", "2");
            server.start();
            server.join();
        } catch (Exception ex) {
            Logger.getLogger(JettyDas2ProxyServer.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

}  