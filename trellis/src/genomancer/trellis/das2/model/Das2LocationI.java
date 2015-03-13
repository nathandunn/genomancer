package genomancer.trellis.das2.model;

/**
 * 
 * 
 */
public interface Das2LocationI extends Das2LocationRefI  {
    public Das2SegmentI getSegment();
    public String getGap();

    /**  inherited from Das2LocationRefI
    public enum Strand  { FORWARD, REVERSE, BOTH, UNKNOWN };
    public URI getSegmentURI();
    public int getMin();
    public int getMax();
    public Strand getStrand();
    */

}


