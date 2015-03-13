package genomancer.ivy.das.client.modelimpl;

import genomancer.ivy.das.model.Das1MethodI;


public class Das1Method implements Das1MethodI  {
    String id;
    String label;

    public Das1Method(String id, String label)  {
	this.id = id;
	this.label = label;
    }

    public String getID() {
	return id;
    }

    public String getLabel() {
	return label;
    }

}