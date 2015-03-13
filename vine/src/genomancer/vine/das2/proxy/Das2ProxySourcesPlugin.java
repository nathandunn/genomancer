package genomancer.vine.das2.proxy;

import genomancer.trellis.das2.model.Das2SourcesCapabilityI;
import genomancer.trellis.das2.server.ServerUtils;
import genomancer.trellis.das2.server.TrellisSourcesPluginI;
import genomancer.vine.das2.client.modelimpl.Das2SourcesCapability;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class Das2ProxySourcesPlugin implements TrellisSourcesPluginI  {
    static String VERSION = "0.8";  // current version of Das2ProxySourcesPlugin

    Das2SourcesProxyCapability sources_proxy = null;
    Map<String, String> params = null;

    public boolean init(ServletConfig config)  {
	return init( ServerUtils.getInitParams(config) );
    }

    public boolean init(Map<String, String> init_params)   {
	boolean success = false;
        try {

            params = init_params;
            String remote_sources_query = "file:///Users/gregg/projects/genomancer/data/netaffx_das2_sources.mod.xml";
            URI remote_base_uri = new URI(remote_sources_query);
            Das2SourcesCapabilityI remote_sources_cap = 
		new Das2SourcesCapability(remote_base_uri, remote_sources_query);

            String sources_query = "http://localhost:9099/das2/proxy/genome/sources";
            URI sources_query_uri = new URI(sources_query);
            sources_proxy = new Das2SourcesProxyCapability(sources_query_uri, sources_query, remote_sources_cap);
	    success = true;
        } catch (URISyntaxException ex) {
            Logger.getLogger(Das2ProxySourcesPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
	return success;
    }

    public Das2SourcesCapabilityI getSourcesCapability()  { return sources_proxy; }

    public void addHeaders(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader("DAS2-Server", "Das2ProxySourcesPlugin/" + VERSION);
    }
    
}