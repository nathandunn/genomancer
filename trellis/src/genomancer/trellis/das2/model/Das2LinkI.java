package genomancer.trellis.das2.model;

import java.net.URI;


/**
 *   essentially a subset of HTML <LINK> element
 * 
 */
public interface Das2LinkI extends Das2CommonMetaAttributesI {
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public String getHref();
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
 */
    public String getMimeType();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 * value of attribute "rel" if present
 * null if not present
 */
    public String getRelationship();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 * valud of attribute "rev" if present
 * null if not present
 */
    public String getReverseRelationship();

}


