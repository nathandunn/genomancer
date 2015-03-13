package genomancer.trellis.das2.model;

import java.util.List;

/**
 * 
 * 
 */
public interface Das2SourceI extends Das2CommonDataI {

/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public List<Das2VersionI> getVersions();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public String getMaintainerEmail();

}


