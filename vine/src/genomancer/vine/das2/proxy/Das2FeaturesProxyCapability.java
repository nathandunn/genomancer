/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package genomancer.vine.das2.proxy;

import genomancer.trellis.das2.model.Das2FeaturesCapabilityI;
import genomancer.trellis.das2.model.Das2FeaturesQueryI;
import genomancer.trellis.das2.model.Das2FeaturesResponseI;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.server.GenericProxyCapability;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 *
 * @author gregg
 */
public class Das2FeaturesProxyCapability extends GenericProxyCapability 
    implements Das2FeaturesCapabilityI  {
    Das2FeaturesCapabilityI remote_features_cap; 

    public Das2FeaturesProxyCapability(URI base_uri, 
				String query_uri, 
				Das2FeaturesCapabilityI remote_features_cap)  { 
//				Map<String, String> params) {
	super(base_uri, query_uri, "features", null, null, remote_features_cap);
	this.remote_features_cap = remote_features_cap;
    }

    public boolean supportsFullQueryFilters() {
	return remote_features_cap.supportsFullQueryFilters();
    }

    public boolean supportsCountFormat() {
	return remote_features_cap.supportsCountFormat();
    }

    public boolean supportsUriFormat() {
	return remote_features_cap.supportsUriFormat();
    }

    public Das2FeaturesResponseI getFeatures(Das2FeaturesQueryI query) {
	return remote_features_cap.getFeatures(query);
    }

    public int getFeaturesCount(Das2FeaturesQueryI query) {
	return remote_features_cap.getFeaturesCount(query);
    }

    public List<String> getFeaturesURI(Das2FeaturesQueryI query) {
	return remote_features_cap.getFeaturesURI(query);
    }

    public InputStream getFeaturesAlternateFormat(Das2FeaturesQueryI query) {
	return remote_features_cap.getFeaturesAlternateFormat(query);
    }

    public Class getFeatureClassForType(Das2TypeI type) {
        return remote_features_cap.getClass();
    }

    public int getMaxHierarchyDepth(Das2TypeI type) {
        return remote_features_cap.getMaxHierarchyDepth(type);

    }

    public long getLastModified(Das2FeaturesQueryI query) {
        return remote_features_cap.getLastModified(query);
    }

}
