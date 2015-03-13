package genomancer.trellis.das2.server;

import genomancer.trellis.das2.model.Das2SourcesCapabilityI;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public interface TrellisSourcesPluginI  {
    public Das2SourcesCapabilityI getSourcesCapability();
    public boolean init(ServletConfig config);
    public boolean init(Map<String, String> params);

    /**
     *   addHeaders allows plugin to add metadata to HTTP response via headers
     *   headers are also set in TrellisDas2Servlet, so when making a plugin it 
     *     is best to use response.addHeader() rather than response.setHeader() so 
     *     plugin is appending extra values to headers rather than replacing ones set by servlet
     */
    public void addHeaders(HttpServletRequest request, HttpServletResponse response);
}