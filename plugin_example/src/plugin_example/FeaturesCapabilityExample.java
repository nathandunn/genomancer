/** 
 *  This example is still very incomplete!!
 */

package plugin_example;

import genomancer.trellis.das2.model.Das2CoordinatesI;
import genomancer.trellis.das2.model.Das2FeaturesCapabilityI;
import genomancer.trellis.das2.model.Das2FeaturesQueryI;
import genomancer.trellis.das2.model.Das2FeaturesResponseI;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2VersionI;
import genomancer.trellis.das2.Das2Constants;
import genomancer.trellis.das2.model.Das2FeatureI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2SegmentsCapabilityI;
import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import genomancer.trellis.das2.model.Das2TypesCapabilityI;
import genomancer.trellis.das2.model.Das2TypesResponseI;
import genomancer.trellis.das2.model.Strand;
import genomancer.trellis.das2.server.GenericProxyCapability;
import genomancer.vine.das2.client.modelimpl.Das2Feature;
import genomancer.vine.das2.client.modelimpl.Das2FeaturesResponse;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2Location;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeaturesCapabilityExample extends Das2GenericCapability implements Das2FeaturesCapabilityI  {
    URI response_base_uri = null;
    Das2TypesCapabilityI types_cap;
    Das2SegmentsCapabilityI segs_cap;
    Das2TypesResponseI types_response;
    Das2SegmentsResponseI segs_response;
    List<Das2FeatureI> features;
    long last_modified_time;

    public FeaturesCapabilityExample(Das2VersionI version, Das2CoordinatesI coords)  {
       super(version.getBaseURI(), (version.getLocalURIString()+"/features"), "features", version, coords);
       response_base_uri = this.getAbsoluteURI().resolve("./");
       types_cap = (Das2TypesCapabilityI)version.getCapability(Das2Constants.DAS2_TYPES_CAPABILITY);
       segs_cap = (Das2SegmentsCapabilityI)version.getCapability(Das2Constants.DAS2_SEGMENTS_CAPABILITY);
       types_response = types_cap.getTypes();
       segs_response = segs_cap.getSegments();
       
       List<Das2TypeI> types = types_response.getTypes();
       List<Das2SegmentI> segments = segs_response.getSegments();
       
       // generating fake features
       features = new ArrayList<Das2FeatureI>();
       int feat_count = 0;
       for (Das2TypeI type : types)  {
	   for (Das2SegmentI segment : segments)  {
	       int slength = segment.getLength();
	       for (int i=10000; i<slength; i+=10000)  {
		   String featid = "feat"+feat_count;
		   Das2Location ploc = new Das2Location(segment, i, i+4000, Strand.FORWARD);
		   Das2Feature feat = new Das2Feature(response_base_uri, featid, type);
		   feat.addLocation(ploc);
		   int child_count = 0;
		   for (int k=i; k<i+4000; k+=1000)  {
		       Das2Location cloc = new Das2Location(segment, k, k+300, Strand.FORWARD);
		       Das2Feature child = new Das2Feature(response_base_uri, featid+"."+child_count, type);
		       child.addLocation(cloc);
		       child.addParent(feat);
		       feat.addPart(child);
		       child_count++;
		   }
		   feat_count++;
		   features.add(feat);
	       }
	   }
       }
       // since collection of features is not modified after constructed, can determine an unchanging
       //    last_modified_time right after features are constructed
       //
       last_modified_time = System.currentTimeMillis();
    }

    public long getLastModified(Das2FeaturesQueryI query) {
	return last_modified_time;
    }

    /** 
     *  Very preliminary, just returns ALL the features on every request
     */
    public Das2FeaturesResponseI getFeatures(Das2FeaturesQueryI query)  {
	List<URI> type_uris = query.getTypes();
	if (type_uris != null && type_uris.size()== 1)  {
	    Das2TypeI query_type = types_response.getType(query.getTypes().get(0));
	}

	Das2FeaturesResponse response = new Das2FeaturesResponse(response_base_uri, features, null, true, true);
	return response;
    }

    public int getFeaturesCount(Das2FeaturesQueryI query) {
	throw new UnsupportedOperationException("Not supported yet.");
    }
    public boolean supportsCountFormat() {
	return false;
    }

    public List<String> getFeaturesURI(Das2FeaturesQueryI query) {
	throw new UnsupportedOperationException("Not supported yet.");
    }
    public boolean supportsUriFormat() {
	return false;
    }

    public InputStream getFeaturesAlternateFormat(Das2FeaturesQueryI query) {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    public Class getFeatureClassForType(Das2TypeI type) {
	return Das2FeatureI.class;
    }

    public int getMaxHierarchyDepth(Das2TypeI type) {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean supportsFullQueryFilters() {
	throw new UnsupportedOperationException("Not supported yet.");
    }





}
