
package genomancer.ivy.das.model;

import java.util.List;


/**
 * 
 * 
 */
public interface Das1GroupI {

    public List<Das1FeatureI> getFeatures();

    public String getID();
    public String getLabel();
    public Das1TypeI getType();
    public List<String> getNotes();
    public List<Das1LinkI> getLinks();
    public List<Das1TargetI> getTargets();

}


