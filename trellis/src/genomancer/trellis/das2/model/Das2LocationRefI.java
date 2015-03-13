package genomancer.trellis.das2.model;

import java.net.URI;


public interface Das2LocationRefI  {
    public int START_OF_UNKNOWN_SEQUENCE = 0;
    public int END_OF_UNKNOWN_SEQUENCE = Integer.MAX_VALUE;

    public URI getSegmentURI();
    public int getMin();
    public int getMax();
    public Strand getStrand();
    public boolean coversEntireSegment();

}