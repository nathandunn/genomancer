package genomancer.ivy.das2.proxy;

import genomancer.ivy.das.client.modelimpl.Das1SourcesCapability;
import genomancer.trellis.das2.model.Das2SourcesCapabilityI;
import genomancer.trellis.das2.server.ServerUtils;
import genomancer.trellis.das2.server.TrellisSourcesPluginI;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class Das1ProxySourcesPlugin implements TrellisSourcesPluginI {
    static String VERSION = "0.83"; // current version of Das1ProxySourcesPlugin

    Das1SourcesProxyCapability sources_proxy = null;
    Map<String, String> params = null;

    public boolean init(ServletConfig config)  {
	return init( ServerUtils.getInitParams(config) );
    }

    public boolean init(Map<String, String> init_params)   {
	boolean success = false;
        try {

            params = init_params;
            String remote_sources_query = 
		// "file:///Users/gregg/projects/genomancer/data/das1_registry_sources.slice.xml";
		// "file:///Users/gregg/projects/genomancer/data/das1_registry_sources.xml";
		// "http://www.dasregistry.org/das1/sources";
            params.get("das1_sources_query");
            URI remote_base_uri = new URI(remote_sources_query);
            Das2SourcesCapabilityI remote_sources_cap = 
		new Das1SourcesCapability(remote_base_uri, remote_sources_query);

	    // String sources_query = "http://localhost:9094/das2/das1_proxy/sources";
	    // String sources_query = "http://75.101.164.54/das2/das1_proxy/sources";
//	    String sources_query = "http://www.genomancer.org/das2/das1_proxy/sources";
            String sources_query = params.get("sources_query");
            URI sources_query_uri = new URI(sources_query);
            sources_proxy = new Das1SourcesProxyCapability(sources_query_uri, sources_query, remote_sources_cap);
	    success = true;
        } catch (URISyntaxException ex) {
            Logger.getLogger(Das1ProxySourcesPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
	return success;
    }

    public Das2SourcesCapabilityI getSourcesCapability()  { return sources_proxy; }

    public void addHeaders(HttpServletRequest request, HttpServletResponse response) {
	response.addHeader("DAS2-Server", "Das1ProxySourcesPlugin/" + VERSION);
    }

}