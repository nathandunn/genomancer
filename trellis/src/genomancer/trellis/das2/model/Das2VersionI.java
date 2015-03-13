package genomancer.trellis.das2.model;

import java.util.List;
import java.util.Map;

/**
 * 
 *   base interface for both DAS1 (for versioned sources with DAS 1.53E capabilities) 
 *       and Das2 (for versioned sources with DAS/2.0 capabilities)
 *
 *   it's possible to have versioned sources with both DAS 1.53E and DAS 2.0 capabilities
 *       (the DAS1-->DAS2 transformational proxy may do this)
 *
 *   it's also possible that in the future there will be versioned sources that support neither 
 *        DAS 1.53E nor DAS/2.0 capabilities but only currently unknown extensions -- VersionI should 
 *        be able to represent these versioned sources as well
 *  
 */
public interface Das2VersionI extends Das2CommonDataI {

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
    public java.util.Date getCreationDate();

/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public java.util.Date getLastModifiedDate();

    public String getMaintainerEmail();

/**
 * List of all capabilities
 * 
 * @return 
 */
    public List<Das2CapabilityI> getCapabilities();


    public Das2CapabilityI getCapability(String type);

/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public List<Das2CoordinatesI> getCoordinates();


    /** 
     *  convenience method to get DAS/2 segments capability, 
     *   just syntactic sugar on top of getCapabilities(Das2Constants.DAS2_FEATURES_CAPABILITY)
     */
    //  public Das2SegmentsCapabilityI getDas2SegmentsCapability();

    /** 
     *  convenience method to get DAS/2 types capability, 
     *   just syntactic sugar on top of getCapabilities().get()
     */
    // public Das2TypesCapabilityI getDas2TypesCapability();

    /** 
     *  convenience method to get DAS/2 features capability, 
     *   just syntactic sugar on top of getCapabilities().get()
     */
    // public Das2FeaturesCapabilityI getDas2FeaturesCapability();

}


