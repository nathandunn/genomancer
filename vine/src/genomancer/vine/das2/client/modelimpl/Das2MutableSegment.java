package genomancer.vine.das2.client.modelimpl;

import java.net.URI;



/**
 *  subclass of Das2Segment that allows setting of all non-identity fields
 *  This class is mainly meant for when Das2SegmentI references are encountered that 
 *      can't immediately be resolved to existing Das2SegmentI objects, so make a 
 *      mutable standin -- needs to be mutable mostly so can expand length later, 
 *      since length is unknown
 */
public class Das2MutableSegment extends Das2Segment  {

    public Das2MutableSegment(URI xml_base, 
		       String local_uri_string,  
		       String title, 
		       URI reference, 
		       int length, 
		       String info_url)  {
	super(xml_base, local_uri_string, title, reference, length, info_url);
    }

    public void setTitle(String title)  { this.title = title; }
    public void setInfoUrl(String info_url)  { this.info_url = info_url; }
    public void setLength(int length)  { this.length = length; }
    public void setReference(URI reference)  { this.reference = reference; }
    
}