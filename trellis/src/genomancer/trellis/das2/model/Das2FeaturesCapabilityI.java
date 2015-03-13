package genomancer.trellis.das2.model;

import java.io.InputStream;
import java.util.List;

/**
 * 
 * 
 */
public interface Das2FeaturesCapabilityI extends Das2CapabilityI {
    public static int UNKNOWN = -1;

    /** 
     *  return most recent time that features in response are OR could have been modified, 
     *    or -1 if unknown
     */
    public long getLastModified(Das2FeaturesQueryI query);

    /**
     *  if query is empty or null then return all features
     */
    public Das2FeaturesResponseI getFeatures(Das2FeaturesQueryI query);
    //    public Das2FeatureI getFeature(URI feat_uri);
    
    /** 
     * return count of number of features query would return
     *   if implementation is unable or unwilling to determine feature count, 
     *      then should return -1 to indicate unknown feature count 
     */
    public int getFeaturesCount(Das2FeaturesQueryI query);
    
    public List<String> getFeaturesURI(Das2FeaturesQueryI query);
    public Das2VersionI getVersion();

    /**
     *  returns an InputStream of bytes that represent Das2Features in alternative 
     *     content formats
     *
     *  should probably throw a das2.exception.FormatNotSupportedException if 
     *      the data source can't represent any part of the Das2Features in 
     *      the specified format
     *
     *  could probably subclass Das2FeaturesCapabilityI for specific popular formats too?
     */
    public InputStream getFeaturesAlternateFormat(Das2FeaturesQueryI query);


    /**
     *   This is to help Trellis format plugins decide whether they can support 
     *      outputting features of the given type in a particular format
     *   Note that the returned Class can be an interface (Class objects 
     *      in Java represent both classes and interfaces)
     *   Unless feature capability plugins are optimizing for efficient representations, this 
     *      should usually just return Das2FeatureI
     *   If optimizing, then should return most concrete class (furthest down in extends/implements hierarchy) 
     *        that all features of the given type returned from this features capabiltiy can share
     *   if returns null, then caller can assume Das2FeatureI, since this is enforced 
     *        by features_response.getFeatures()
     */
    public Class getFeatureClassForType(Das2TypeI type);

    /**
     *  This is to help Trellis format plugins decide whether they can support 
     *      outputting features of the given type in a particular format 
     *      (since many formats can only support 1 or 2 levels)
     *  Returns maximum depth of feature hierarchy
     *  if unknown, return -1
     */
    public int getMaxHierarchyDepth(Das2TypeI type);
    
    public boolean supportsFullQueryFilters();
    public boolean supportsCountFormat();
    public boolean supportsUriFormat();

    /** TEMPORARY: standin for FEATURES --> LINK subelement */
    //    public List<Das2LinkI> getLinks();

}


