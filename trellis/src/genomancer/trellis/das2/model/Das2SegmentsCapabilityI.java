package genomancer.trellis.das2.model;

import java.net.URI;
import java.util.List;

/**
 * 
 * 
 */
public interface Das2SegmentsCapabilityI extends Das2CapabilityI {

    public Das2SegmentsResponseI getSegments();
    public Das2SegmentI getSegment(URI segment_uri);
    public String getResidues(Das2LocationI location);
    public String getResidues(List<Das2LocationI> locations);

}


