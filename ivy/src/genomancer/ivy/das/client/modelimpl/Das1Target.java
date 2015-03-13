package genomancer.ivy.das.client.modelimpl;

import genomancer.ivy.das.model.Das1TargetI;


public class Das1Target implements Das1TargetI  {
    String id;
    String name;
    int start;
    int stop;
    
    public Das1Target(String id, String name, int start, int stop)  {
	this.id = id;
	this.name = name;
	this.start = start;
	this.stop = stop;
    }

    public String getID() {
	return id;
    }

    public String getName()  {
	return name;
    }

    public int getStart() {
	return start;
    }

    public int getStop() {
	return stop;
    }
}