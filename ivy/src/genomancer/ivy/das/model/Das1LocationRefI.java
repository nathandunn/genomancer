package genomancer.ivy.das.model;

import genomancer.trellis.das2.model.Strand;

public interface Das1LocationRefI  {
    public int START_OF_UNKNOWN_SEQUENCE = 0;
    public int END_OF_UNKNOWN_SEQUENCE = Integer.MAX_VALUE;

    public String getSegmentID();
    public int getMin();
    public int getMax();
    public Strand getStrand();
    public boolean coversEntireSegment();

} 