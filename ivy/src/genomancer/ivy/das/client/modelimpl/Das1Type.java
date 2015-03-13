package genomancer.ivy.das.client.modelimpl;

import genomancer.ivy.das.model.Das1TypeI;

public class Das1Type implements Das1TypeI  {
    protected String id;
    protected String label;
    protected String category;
    protected String method;
    protected boolean is_reference = false;
    protected int feature_count = -1;  // default is no feature count

    public Das1Type(String id, String category, String method)  {
	this(id, null, category, method);
    }

    public Das1Type(String id, String label, String category, String method)  {
	this(id, label, category, method, false);
    }

    public Das1Type(String id, String label, String category, String method, boolean is_reference)  {
	this.id = id;
	this.label = label;
	this.category = category;
	this.method = method;
	this.is_reference = is_reference;
    }

    public Das1Type(String id, String label, String category, String method, boolean is_reference, 
		    int feature_count)  {
	this(id, label, category, method, is_reference);
	this.feature_count = feature_count;
    }


    public String getID() {
	return id;
    }

    public String getLabel() {
	return label;
    }

    public String getCategory() {
	return category;
    }

    public String getMethod()  { return method; }

    public boolean isReference() {
	return is_reference;
    }

    public boolean hasFeatureCount() { return (feature_count >= 0); }
    public int getFeatureCount()  {  return feature_count; }



}