/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package genomancer.vine.das2.proxy;

import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2SegmentsCapabilityI;
import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import genomancer.trellis.das2.server.GenericProxyCapability;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 *
 * @author gregg
 */
class Das2SegmentsProxyCapability extends GenericProxyCapability 
    implements Das2SegmentsCapabilityI  {
    
    Das2SegmentsCapabilityI remote_seg_cap;

    public Das2SegmentsProxyCapability(URI base_uri, 
				   String query_uri, 
				   Das2SegmentsCapabilityI remote_seg_cap)  {
//				   Map<String, String> init_params) {
        super(base_uri, query_uri, "segments", null, null, remote_seg_cap);
	this.remote_seg_cap = remote_seg_cap;
    }

    /**
     *  Note that responses use the base_uri of the remote capability, _not_ the base_uri of the proxy
     */
    public Das2SegmentsResponseI getSegments() {
	return remote_seg_cap.getSegments(); 
    }

    public String getResidues(Das2LocationI location) {
	return remote_seg_cap.getResidues(location);
    }

    public String getResidues(List<Das2LocationI> locations) {
	return remote_seg_cap.getResidues(locations);
    }

    public Das2SegmentI getSegment(URI segment_uri) {
        return remote_seg_cap.getSegment(segment_uri);
    }

}
