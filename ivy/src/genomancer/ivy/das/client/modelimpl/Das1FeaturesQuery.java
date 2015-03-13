package genomancer.ivy.das.client.modelimpl;


import genomancer.ivy.das.model.Das1FeaturesQueryI;
import genomancer.ivy.das.model.Das1LocationRefI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Das1FeaturesQuery implements Das1FeaturesQueryI  {
    List<Das1LocationRefI> locations = new ArrayList<Das1LocationRefI>();
    List<String> types = new ArrayList<String>();
    List<String> categories = new ArrayList<String>();
    List<String> groups = new ArrayList<String>();
    List<String> features = new ArrayList<String>();
    boolean is_categorized = false;
    Map<String, List<String>> non_standard_params = new LinkedHashMap<String, List<String>>();

    public List<Das1LocationRefI> getLocations() { return locations; }
    public List<String> getTypes() { return types; }
    public List<String> getCategories() { return categories; }
    public List<String> getGroupIds() { return groups; }
    public List<String> getFeatureIds() { return features; }
    public boolean isCategorized() { return is_categorized; }
    public Map<String, List<String>> getNonStandardParams() { return non_standard_params; }

    public void addLocation(Das1LocationRefI loc)  { locations.add(loc); }
    public void addType(String typeid)  { types.add(typeid); }
    public void addCategory(String category)  { categories.add(category); }
    public void addGroup(String groupid)  { groups.add(groupid); }
    public void addFeature(String featureid)  { features.add(featureid); }
    public void setIsCategorized(boolean is_categorized)  { this.is_categorized = is_categorized; }

    public void addNonStandardParam(String param, String value)  {
	System.out.println("    adding nonstandard param: name = " + param + ", value = " + value);
	List<String> values = non_standard_params.get(param);
	if (values == null)  {
	    values = new ArrayList<String>();
	    non_standard_params.put(param, values);
	}
	values.add(value);
    }

}