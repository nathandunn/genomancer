package genomancer.vine.das2.client.modelimpl;

import genomancer.trellis.das2.model.Das2LocationRefI;
import genomancer.trellis.das2.model.Strand;
import java.net.URI;


public class Das2LocationRef implements Das2LocationRefI  {

    URI segment_uri;
    int min;
    int max;
    Strand strand;

    public Das2LocationRef(URI segment_uri)  {
	this(segment_uri, Strand.UNKNOWN);
    }
    
    public Das2LocationRef(URI segment_uri, Strand strand)  {
	this(segment_uri, START_OF_UNKNOWN_SEQUENCE, END_OF_UNKNOWN_SEQUENCE, strand);
    }

    public Das2LocationRef(URI segment_uri, int min, int max)  {
	this(segment_uri, min, max, Strand.UNKNOWN);  // or should it be Strand.BOTH ??
    }


    public Das2LocationRef(URI segment_uri, int min, int max, Strand strand)  {
	this.segment_uri = segment_uri;
	this.min = min;
	this.max = max;
	this.strand = strand;
    }

    public URI getSegmentURI()  { return segment_uri; }
    public int getMin()  { return min; }
    public int getMax()  { return max; }
    public Strand getStrand()  { return strand; }

    public boolean coversEntireSegment()  {
	return (min == START_OF_UNKNOWN_SEQUENCE && 
		max == END_OF_UNKNOWN_SEQUENCE);
    }

}