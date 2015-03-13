package genomancer.ivy.das.client.modelimpl;

import genomancer.ivy.das.model.Das1SegmentI;
import genomancer.trellis.das2.model.Strand;


public class Das1Segment implements Das1SegmentI  {
    protected String id;
    protected int start;
    protected int stop;
    protected String type;
    protected Strand orientation = Strand.NOT_APPLICABLE;
    protected boolean has_subparts;
    protected String description;
    protected String version;
    protected String label;

    public Das1Segment(String id, int start, int stop, String type, String version, String label)  {
	this.id = id;
	this.start = start;
	this.stop = stop;
	this.type = type;
	this.version = version;
	this.label = label;
    }
    

    public String getID() {
	return id;
    }

    public int getStart() {
	return start;
    }

    public int getStop() {
	return stop;
    }

    public String getType() {
	return type;
    }

    public Strand getOrientation() {
	return orientation;
    }

    public boolean hasSubParts() {
	return has_subparts;
    }

    public String getDescription() {
	return description;
    }

    public String getVersion() {
	return version;
    }

    public String getLabel() {
	return label;
    }

}