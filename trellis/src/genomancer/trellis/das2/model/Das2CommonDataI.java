package genomancer.trellis.das2.model;

import java.net.URI;
import java.net.URL;
import java.util.List;
import org.jdom.Element;

/**
 * 
 * 
 */ 
public interface Das2CommonDataI extends IdentifiableI {
    // extends Das2CommonMetaAttributesI, IdentifiableI {

/**
 * <p>Does ...</p>
 * @return 
 * value of attribute "doc_href" if present
 * null if not present
 */
    public String getInfoURL();
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
    public String getDescription();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public List<Das2PropertyI> getProperties();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public List<org.jdom.Element> getAdditionalData();

}


