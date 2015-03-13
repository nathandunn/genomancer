package genomancer.ivy.das.client.modelimpl;

import genomancer.ivy.das.model.Das1LocationI;
import genomancer.ivy.das.model.Das1SegmentI;
import genomancer.trellis.das2.model.Strand;

public class Das1Location extends Das1LocationRef implements Das1LocationI {
    Das1SegmentI segment;

    public Das1Location(Das1SegmentI segment, int min, int max, Strand strand)  {
	super(segment.getID(), min, max, strand);
	this.segment = segment;
    }

    public Das1SegmentI getSegment() {
	return segment;
    }

    public boolean coversEntireSegment()  { 
	return ((min == segment.getStart()) && (max == segment.getStop()));
    }

}
