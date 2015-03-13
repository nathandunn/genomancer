package genomancer.ivy.das.client.modelimpl;

import genomancer.ivy.das.model.Das1LocationRefI;
import genomancer.trellis.das2.model.Strand;

public class Das1LocationRef implements Das1LocationRefI  {
    String id;
    int min;
    int max;
    Strand strand;

    public Das1LocationRef(String id)  {
	this(id, Strand.UNKNOWN);
    }
    
    public Das1LocationRef(String id, Strand strand)  {
	this(id, START_OF_UNKNOWN_SEQUENCE, END_OF_UNKNOWN_SEQUENCE, strand);
    }

    public Das1LocationRef(String id, int min, int max, Strand strand)  {
	this.id = id;
	this.min = min;
	this.max = max;
	this.strand = strand;
    }

    public String getSegmentID() {
	return id;
    }

    public int getMin() {
	return min;
    }

    public int getMax() {
	return max;
    }

    public Strand getStrand() {
	return strand;
    }

    public boolean coversEntireSegment()  {
	return (min == START_OF_UNKNOWN_SEQUENCE && 
		max == END_OF_UNKNOWN_SEQUENCE);
    }

    

}