package genomancer.ivy.das.client.modelimpl;

import genomancer.ivy.das.model.Das1FeatureI;
import genomancer.ivy.das.model.Das1GroupI;
import genomancer.ivy.das.model.Das1LinkI;
import genomancer.ivy.das.model.Das1TargetI;
import genomancer.ivy.das.model.Das1TypeI;
import java.util.ArrayList;
import java.util.List;


public class Das1Group implements Das1GroupI  {
    String id;
    String label;
    Das1TypeI type ;
    List<Das1TargetI> targets;
    List<Das1LinkI> links;
    List<String> notes;
    List<Das1FeatureI> features = null;

    public Das1Group(String id,
		     String label, 
                 Das1TypeI type, 
		     List<Das1TargetI> targets, 
		     List<Das1LinkI> links,
		     List<String> notes) {
	this.id = id;
	this.label = label;
	this.type = type;
	this.targets = targets;
	this.links = links;
	this.notes = notes;
    }


    public String getID() {
	return id;
    }

    public String getLabel() {
	return label;
    }

    public Das1TypeI getType() {
	return type;
    }

    public List<String> getNotes() {
	return notes;
    }

    public List<Das1LinkI> getLinks() {
	return links;
    }

    public List<Das1TargetI> getTargets() {
	return targets;
    }

    public List<Das1FeatureI> getFeatures() {
        return features;
    }

    public void addFeature(Das1FeatureI feature) {
        if (features == null)  { features = new ArrayList<Das1FeatureI>(); }
        features.add(feature);
    }
}