package genomancer.ivy.das.model;

import java.util.List;
import java.util.Map;

/**
 * 
 *   leaving out "prop-*" queries for now (other than pass-through as non_standard_params)
 */
public interface Das1FeaturesQueryI  {
    public List<Das1LocationRefI> getLocations();
    public List<String> getTypes();
    public List<String> getCategories();
    public List<String> getGroupIds();
    public List<String> getFeatureIds();
    public boolean isCategorized();

    /** added support for non-standard params (not really in DAS1.53 spec, but used in practice) */
    public Map<String, List<String>> getNonStandardParams();

}
	
