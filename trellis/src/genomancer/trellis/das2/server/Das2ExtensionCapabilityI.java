package genomancer.trellis.das2.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import genomancer.trellis.das2.model.Das2CapabilityI;


public interface Das2ExtensionCapabilityI extends Das2CapabilityI  {

    /**
     *   Trellis DAS/2 framework support for passthrough of capabilities it 
     *     doesn't know about
     *  
     *   Plugins that want to add extensions should implement Das2ExtensionCapabilityI
     *
     *   Just passes the HTTP request and response objects to the plugin to 
     *      do what it wants
     *
     *   returned boolean should be true if query was successfully handled, 
     *      false otherwise
     *
     *   Trellis will check header status of response, if set to OK but 
     *         success = false will reset status to indicate an error
     */
    public boolean handleQuery(HttpServletRequest request, HttpServletResponse response);
}