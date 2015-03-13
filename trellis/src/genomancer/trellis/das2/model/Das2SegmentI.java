package genomancer.trellis.das2.model;

import java.net.URI;
import java.net.URL;

/**
 * 
 * 
 */
public interface Das2SegmentI extends IdentifiableI  {
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public int getLength();
/**
 * <p>Does ...</p>
 * 
 * 
 *   or should this return a Das2SegmentI ??
 */
    public URI getReference();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public String getTitle();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 * value of "doc_href" attribute if present
 */
    public String getInfoURL();

}


