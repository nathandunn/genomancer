/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package genomancer.vine.das2.proxy;

import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2TypesCapabilityI;
import genomancer.trellis.das2.model.Das2TypesResponseI;
import genomancer.trellis.das2.server.GenericProxyCapability;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 *
 * @author gregg
 */
public class Das2TypesProxyCapability extends GenericProxyCapability implements Das2TypesCapabilityI  {
    
    Das2TypesCapabilityI remote_types_cap;

    public Das2TypesProxyCapability(URI base_uri, 
				String query_uri, 
				Das2TypesCapabilityI remote_types_cap)  {
//				Map<String, String> params) {
	super(base_uri, query_uri, "types", null, null, remote_types_cap);
	this.remote_types_cap = remote_types_cap;
    }

    public Das2TypesResponseI getTypes() {
	return remote_types_cap.getTypes();
    }

    public Das2FormatI getFormat(String format_name)  {
	return remote_types_cap.getFormat(format_name);
    }

    public Das2TypeI getType(URI type_uri) {
       return remote_types_cap.getType(type_uri);
    }

}
