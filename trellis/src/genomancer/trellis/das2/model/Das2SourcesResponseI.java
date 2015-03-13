package genomancer.trellis.das2.model;

import java.net.URI;
import java.util.List;


/**
 * 
 * 
 */
public interface Das2SourcesResponseI {
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public List<Das2SourceI> getSources();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public URI getBaseURI();

    public List<Das2LinkI> getLinks();

    public String getMaintainerEmail();

    // not sure yet about getQuery() method
    //    public Das2SourcesQueryI getQuery();

}


