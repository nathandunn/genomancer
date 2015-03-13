package genomancer.vine.das2.client.modelimpl;

import java.net.URI;
import genomancer.trellis.das2.model.Das2SegmentI;

public class Das2Segment extends Identifiable implements Das2SegmentI {
    String title;
    URI reference;
    int length;
    String info_url;

    public Das2Segment(URI xml_base, 
		       String local_uri_string,  
		       String title, 
		       URI reference, 
		       int length, 
		       String info_url)  {
	super (xml_base, local_uri_string);
	this.title = title;
	this.reference = reference;
	this.length = length;
	this.info_url = info_url;
    }

    public String getTitle() { return title; }
    public String getInfoURL() { return info_url; }
    public int getLength() { return length; }
    public URI getReference() { return reference;  }

}