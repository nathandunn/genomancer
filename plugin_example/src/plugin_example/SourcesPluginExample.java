package plugin_example;

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


public class SourcesPluginExample implements TrellisSourcesPluginI  {
    static String VERSION = "1.0";  // current version of SourcesPluginExample

    SourcesCapabilityExample sources_cap = null;
    Map<String, String> params = null;

    public boolean init(ServletConfig config)  {
	return init( ServerUtils.getInitParams(config) );
    }

    public boolean init(Map<String, String> init_params)   {
	boolean success = false;
        try {
	    params = init_params;
	    String sources_query = params.get("sources_query");
	    URI sources_query_uri = new URI(sources_query);
	    URI base_uri = sources_query_uri.resolve("");
	    System.out.println("sources_query_uri: " + sources_query_uri);
	    System.out.println("base_uri: " + base_uri);
	    sources_cap = new SourcesCapabilityExample(base_uri, sources_query, init_params);
	    success = true;
        } catch (URISyntaxException ex) {
            Logger.getLogger(SourcesPluginExample.class.getName()).log(Level.SEVERE, null, ex);
        }
	return success;
    }

    public Das2SourcesCapabilityI getSourcesCapability()  { return sources_cap; }

    public void addHeaders(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader("DAS2-Server", "SourcesPluginExample/" + VERSION);
    }
    
}