package genomancer.trellis.das2.model;

import java.net.URI;
import java.util.List;


/**
 * 
 * 
 */
public interface Das2TypesResponseI {

/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public List<Das2TypeI> getTypes();

    public Das2TypeI getType(URI type_uri);
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public List<Das2LinkI> getLinks();
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
    //  public void getQuery();
}


