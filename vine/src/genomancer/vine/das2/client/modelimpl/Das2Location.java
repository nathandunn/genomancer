package genomancer.vine.das2.client.modelimpl;

import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Strand;
import java.net.URI;

public class Das2Location implements Das2LocationI {

    Das2SegmentI segment;
    int min;
    int max;
    Strand strand;
    String gap;

    public Das2Location(Das2SegmentI seg, int min, int max)  {
	this(seg, min, max, Strand.UNKNOWN);  // or should it be Strand.BOTH ??
    }


    public Das2Location(Das2SegmentI seg, int min, int max, Strand strand)  {
	this(seg, min, max, strand, null);
    }

    public Das2Location(Das2SegmentI seg, int min, int max, Strand strand, String gap)  {
	this.min = min;
	this.max = max;
	this.segment = seg;
	this.strand = strand;
	this.gap = gap;
    }

    public int getMin()  { return min; }
    public int getMax()  { return max; }
    public Das2SegmentI getSegment()  { return segment; }
    public URI getSegmentURI()  { return segment.getAbsoluteURI(); }
    public Strand getStrand()  { return strand; }
    public String getGap() { return gap; }

    public boolean coversEntireSegment()  { 
	return ((min == 0) && (max == segment.getLength()));
    }
}