package genomancer.trellis.das2.model;

import java.net.URI;
import java.util.List;


/**
 * 
 * 
 */
public interface Das2SegmentsResponseI {

/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public List<Das2SegmentI> getSegments();

    public Das2SegmentI getSegment(URI segment_uri);
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public URI getBaseURI();
/**
 * <p>Does ...</p>
 * 
 */
//    public void getQuery();

/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public List<Das2LinkI> getLinks();

    public List<Das2FormatI> getFormats();
}


