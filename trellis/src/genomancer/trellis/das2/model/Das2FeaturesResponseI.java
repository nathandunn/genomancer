package genomancer.trellis.das2.model;

import java.net.URI;
import java.util.List;


/**
 *  only valid for formats where it makes sense to return list of Das2FeatureI objects
 */
public interface Das2FeaturesResponseI  {
    public List<Das2FeatureI> getFeatures();   
    public URI getBaseURI();
    public List<Das2LinkI> getLinks();
    public Status getStatus();
    
    /**
     *  the Das2FeaturesQueryI that was argument in Das2FeaturesCapabilityI.getFeatures() call 
     *     that produced this Das2FeaturesResponse
     */
    public Das2FeaturesQueryI getQuery();


    /**  to allow for speed optimizations */
    public boolean segmentsShareBaseURI();

    /** to allow for speed optimizations */
    public boolean typesShareBaseURI();

}