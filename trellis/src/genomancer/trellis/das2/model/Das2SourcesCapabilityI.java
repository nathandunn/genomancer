package genomancer.trellis.das2.model;

import java.util.Map;
import java.net.URI;

/**
 *   The "sources" capability is special in that it is not associated 
 *   with a Das2Version, but rather is used as the top-level access to 
 *   Das2Sources, 
 *         which have Das2Versions, 
 *               which have all the other Das2Capabilities
 *   
 *   However, it is being modeled as a Das2Capability because it maps well to 
 *      most of the info in a Das2Capability 
 *   what's missing compared to a normal Das2Capability:
 *      has no Das2Coordinates (although coords are not required by Das2Capability anyway)
 *      currently doesn't have any formats, supported extensions, etc., although that 
 *      could change.
 *   Also, for Trellis, needs to initializable from outside the constructor, to allow for 
 *      dynamic instantiation based on Trellis configuration
 *      For dynamic instantiation, could have used reflection to call a constructor with 
 *          inititalization params as args, but then wouldn't be able to declare this 
 *          in the interface, since constructors can't be specified in interfaces
 */
public interface Das2SourcesCapabilityI extends Das2CapabilityI  {
    public void init(Map<String, String> params);
    public Das2SourcesResponseI getSources(); 
    public Das2SourceI getSource(URI source_uri);
    public Das2VersionI getVersion(URI version_uri);

    //   possible method to add later, allowing filtering etc. of sources returned
    // public Das2SourcesResponseI getSources(Das2SourcesRequestI query);
    //    public Das2SourcesResponseI getSources(Map<String, List<String>> params); 
}