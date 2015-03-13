package genomancer.trellis.das2.model;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 
 *   leaving out "prop-*" queries for now (other than pass-through as non_standard_params)
 */
public interface Das2FeaturesQueryI  {
    public String getFormat();

    /** implementation MUST return a list -- null returns not allowed, use empty list instead */
    public List<Das2LocationRefI> getOverlaps();
    /** implementation MUST return a list -- null returns not allowed, use empty list instead */
    public List<Das2LocationRefI> getInsides();
    /** implementation MUST return a list -- null returns not allowed, use empty list instead */
    public List<Das2LocationRefI> getExcludes();
    /** implementation MUST return a list -- null returns not allowed, use empty list instead */
    public List<URI> getTypes();
    /** implementation MUST return a list -- null returns not allowed, use empty list instead */
    public List<URI> getCoordinates();
    /** implementation MUST return a list -- null returns not allowed, use empty list instead */
    public List<URI> getLinks();
    /** implementation MUST return a list -- null returns not allowed, use empty list instead */
    public List<String> getNames();
    /** implementation MUST return a list -- null returns not allowed, use empty list instead */
    public List<String> getNotes();

    //    public List<> getProperties();
    //    public Das2VersionI getVersion();

    /**
     *  Support for arbitrary query parameters that are not part of the DAS/2 spec
     *    (DAS/2 allows this)
     *  In the returned Map each key is a query parameter name, and each key 
     *      is a List of that parameter's values (to support multiple instances of 
     *      the same parameter)
     *
     *  implementation MUST return a map -- null returns not allowed, use empty map instead
     */
    public Map<String, List<String>> getNonStandardParams();

}
	
