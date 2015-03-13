package genomancer.vine.das2.client.modelimpl;

import genomancer.trellis.das2.model.Das2SegmentI;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;

import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import genomancer.trellis.das2.model.Das2SegmentsCapabilityI;
import genomancer.vine.das2.client.xml.SegmentsXmlReader;
import java.util.HashMap;
import java.util.Map;

public class Das2SegmentsCapability extends Das2GenericCapability implements Das2SegmentsCapabilityI  {
    Das2SegmentsResponseI segments = null;
    Map<URI, Das2SegmentI> uri2segment = null;

    public Das2SegmentsCapability(URI base_uri, 
			       String query_uri, 
			       Das2Version version, 
			       Das2Coordinates coordinates)  {
        super(base_uri, query_uri, "segments", version, coordinates);
    }

   
    public Das2SegmentsResponseI getSegments() { 
	if (segments == null)  {
	    initSegments();
	}
	return segments; 
    }

    protected synchronized boolean initSegments()  {
	boolean success = false;
	InputStream istr = null;
	try {
	    URL segments_query = this.getAbsoluteURI().toURL();
	    URLConnection conn = segments_query.openConnection();
	    // check HTTP status header, etc. here
	    istr = conn.getInputStream();
	    segments = SegmentsXmlReader.readSegmentsDocument(istr, this.getAbsoluteURI());
          uri2segment = new HashMap<URI, Das2SegmentI>();
          for (Das2SegmentI seg : segments.getSegments())  {
              uri2segment.put(seg.getAbsoluteURI(), seg);
          }
	    success = true;
	} catch (IOException ex) {
	    Logger.getLogger(Das2SegmentsCapability.class.getName()).log(Level.SEVERE, null, ex);
	} catch (XMLStreamException ex) {
	    Logger.getLogger(Das2SegmentsCapability.class.getName()).log(Level.SEVERE, null, ex);
	} catch (URISyntaxException ex) {
	    Logger.getLogger(Das2SegmentsCapability.class.getName()).log(Level.SEVERE, null, ex);
	} finally {
	    try {
		istr.close();
	    } catch (IOException ex) {
		success = false;
		Logger.getLogger(Das2SegmentsCapability.class.getName()).log(Level.SEVERE, null, ex);
	    }
	}
	return success;
    }

    public static void main(String[] args) throws URISyntaxException  {
	String segments_url = "file:./data/netaffx_das2_segments.xml";
	Das2SegmentsCapability cap = new Das2SegmentsCapability(new URI(segments_url), segments_url, null, null);
	//	List<Das2SegmentI> segments = cap.getSegments();
	Das2SegmentsResponseI segments_response = cap.getSegments();
    }

    public String getResidues(Das2LocationI location) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getResidues(List<Das2LocationI> locations) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Das2SegmentI getSegment(URI segment_uri) {
        return uri2segment.get(segment_uri);
    }

} 
