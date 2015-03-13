package genomancer.ivy.das2.proxy;

import genomancer.ivy.das.client.modelimpl.Das1Location;
import genomancer.ivy.das.client.modelimpl.Das1Segment;
import genomancer.ivy.das.model.Das1LocationRefI;
import genomancer.ivy.das.model.Das1SegmentI;
import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.ivy.das.model.Das1EntryPointsCapabilityI;
import genomancer.ivy.das.model.Das1EntryPointsResponseI;
import genomancer.ivy.das.model.Das1LocationI;
import genomancer.ivy.das.model.Das1SequenceCapabilityI;
import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2LocationRefI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2SegmentsCapabilityI;
import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import genomancer.trellis.das2.model.Strand;
import genomancer.trellis.das2.server.GenericProxyCapability;
import genomancer.vine.das2.client.modelimpl.Das2Segment;
import genomancer.vine.das2.client.modelimpl.Das2SegmentsResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  For getSegment()/getSegments() methods, 
 *     Uses a Das1EntryPointsCapabilityI to get a Das1EntryPointsResponseI,
 *       uses Das1EntryPointsResponseI to get DAS1 segment (etc.) models
 *         builds DAS2 segment (etc.) models base on DAS1 models
 *           Builds a Das2SeqmentsResponse based on DAS2 segment models
 * 
 *  For getSequence() methods,
 *     Uses a Das1SequenceCapabilityI to get a Das2SequenceResponseI
 *     etc?
 */
class Das1SegmentsProxyCapability extends GenericProxyCapability 
    implements Das2SegmentsCapabilityI  {

    boolean DEBUG = false;
    boolean initialized = false;    
    Das1EntryPointsCapabilityI das1_entry_points_cap;
    Das1SequenceCapabilityI das1_sequence_cap;
    //    Das1SegmentsResponseI das1_segments_response;
    Das2SegmentsResponseI das2_segments_response;

    Map<Das1SegmentI, Das2SegmentI> das1_to_das2_segment = new HashMap<Das1SegmentI, Das2SegmentI>();
    Map<Das2SegmentI, Das1SegmentI> das2_to_das1_segment = new HashMap<Das2SegmentI, Das1SegmentI>();
    Map<String, Das1SegmentI> id_to_das1_segment = new HashMap<String, Das1SegmentI>();
    Map<URI, Das2SegmentI> uri_to_das2_segment = new HashMap<URI, Das2SegmentI>();

    public Das1SegmentsProxyCapability(URI base_uri, 
				   String query_uri, 
				   Das1EntryPointsCapabilityI das1_entry_points_cap)  {
//				   Map<String, String> init_params) {
        super(base_uri, query_uri, "segments", null, null, das1_entry_points_cap);
	if (DEBUG)  {
	    System.out.println("called Das1SegmentsProxyCapability consctructor");
	    System.out.println("    base_uri: " + base_uri.toString());
	    System.out.println("    query_uri: " + query_uri);
	}
	this.das1_entry_points_cap = das1_entry_points_cap;
    }

    public Das2SegmentsResponseI getSegments() {
	if (! initialized)  { initSegments(); }
	return das2_segments_response;
    }

    protected boolean initSegments()  {
	Das1EntryPointsResponseI das1_segments_response = das1_entry_points_cap.getEntryPoints();
	List<Das1SegmentI> das1_segments = das1_segments_response.getEntryPoints();
	List<Das2SegmentI> das2_segments = new ArrayList<Das2SegmentI>();
	List<Das2LinkI> das2_links = null;
	List<Das2FormatI> das2_formats = null;
	for (Das1SegmentI das1_segment : das1_segments)  {
	    Das2SegmentI das2_segment = createDas2Segment(das1_segment);
	    das1_to_das2_segment.put(das1_segment, das2_segment);
	    das2_to_das1_segment.put(das2_segment, das1_segment);
	    id_to_das1_segment.put(das1_segment.getID(), das1_segment);
	    uri_to_das2_segment.put(das2_segment.getAbsoluteURI(), das2_segment);
	    das2_segments.add(das2_segment);
	}
	das2_segments_response = new Das2SegmentsResponse(base_uri, 
							  das2_segments, 
							  das2_links, 
							  das2_formats);
	initialized = true;
	return initialized;
    }

    protected Das2SegmentI createDas2Segment(Das1SegmentI das1_segment)  {
	String title = das1_segment.getLabel();
	if (title == null)  { title = das1_segment.getID(); }
	Das2SegmentI das2_segment = new Das2Segment(base_uri, 
						    das1_segment.getID(), 
						    title, 
						    null, 
						    das1_segment.getStop(), 
						    null);
	return das2_segment;
    }


    /**
     *  Note that responses use the base_uri of the remote capability, _not_ the base_uri of the proxy
     */
    public String getResidues(Das2LocationI location) {
	if (! initialized)  { initSegments(); }
	//	return das1_sequence_cap.getResidues(location);
        return null;

    }

    public String getResidues(List<Das2LocationI> locations) {
	if (! initialized)  { initSegments(); }
	//	return das1_sequence_cap.getResidues(locations);
        return null;
    }

    public Das2SegmentI getDas2Segment(Das1SegmentI das1_segment)  {
	if (! initialized)  { initSegments(); }
	return das1_to_das2_segment.get(das1_segment);
    }

    public Das1SegmentI getDas1Segment(Das2SegmentI das2_segment)  {
	if (! initialized)  { initSegments(); }
	return das2_to_das1_segment.get(das2_segment);
    }

    public Das1SegmentI getDas1Segment(String id)  {
	if (! initialized)  { initSegments(); }
	return id_to_das1_segment.get(id);
    }

    public Das2SegmentI getDas2Segment(URI uri)  {
	if (! initialized)  { initSegments(); }
	return uri_to_das2_segment.get(uri);
    }

    //    public Das2SegmentI getDas2Segment(String local_uri_string)  {  }


    /**
     *  for now try mapping full loc URI,
     *    then just last segment path, 
     *      then try something based on Coordinates?
     *        if none of the above then create new segment?
     */
    Das1LocationRefI getDas1LocationRef(Das2LocationRefI locref) {
	if (! initialized)  { initSegments(); }
	URI das2_segment_uri = locref.getSegmentURI();
	Das2SegmentI das2_segment = getDas2Segment(das2_segment_uri);
	Das1SegmentI das1_segment = getDas1Segment(das2_segment);

	int min = locref.getMin() + 1;
	int max = locref.getMax();
	Strand strand = locref.getStrand();

	Das1LocationI das1_loc = new Das1Location(das1_segment, min, max, strand);
	return das1_loc;
    }

    public Das2SegmentI getSegment(URI segment_uri) {
        return this.getDas2Segment(segment_uri);
    }


    /*
    Das2LocationRefI getDas2LocationRef(Das1LocationRefI loc)  {
	if (! initialized)  { initSegments(); }
    	return null;
    }
    */


}
