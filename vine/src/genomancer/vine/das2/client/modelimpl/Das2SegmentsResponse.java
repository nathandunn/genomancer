package genomancer.vine.das2.client.modelimpl;

import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Das2SegmentsResponse implements Das2SegmentsResponseI  {

    protected URI base_uri;
    // protected List<? extends Das2SegmentI> segments;
    protected List<Das2SegmentI> segments;
    protected List<Das2LinkI> links;
    protected List<Das2FormatI> formats;
    protected Map<URI, Das2SegmentI> uri2segment;

    public Das2SegmentsResponse(URI base_uri, 
				// List<? extends Das2SegmentI> segments,
                        List<Das2SegmentI> segments,
				List<Das2LinkI> links, 
				List<Das2FormatI> formats)  {
	this.base_uri = base_uri;
	this.segments = segments;
	this.links = links;
	this.formats = formats;
	uri2segment = new HashMap<URI, Das2SegmentI>(segments.size());
	for (Das2SegmentI segment : segments)  {
	    uri2segment.put(segment.getAbsoluteURI(), segment);
	}
    }

    // public List<? extends Das2SegmentI> getSegments() {
    public List<Das2SegmentI> getSegments() {
	return segments;
    }

    public Das2SegmentI getSegment(URI segment_uri)  {
	return uri2segment.get(segment_uri);
    }

    public URI getBaseURI() {
	return base_uri;
    }

    public List<Das2LinkI> getLinks() {
	return links;
    }

    public List<Das2FormatI> getFormats() {
	return formats;
    }

}