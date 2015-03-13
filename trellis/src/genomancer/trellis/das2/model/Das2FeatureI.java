package genomancer.trellis.das2.model;

import java.util.List;

/**
 * 
 * 
 */
public interface Das2FeatureI extends Das2CommonDataI {
/**
 * <p>Does ...</p>
 * 
 */
    public Das2TypeI getType();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public List<Das2LocationI> getLocations();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public List<Das2FeatureI> getParts();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public List<Das2FeatureI> getParents();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public List<String> getAliases();
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
    public List<String> getNotes();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public java.util.Date getCreationDate();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public java.util.Date getLastModifiedDate();


}


