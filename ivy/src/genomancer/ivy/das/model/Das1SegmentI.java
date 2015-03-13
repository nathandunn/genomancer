
package genomancer.ivy.das.model;

import genomancer.trellis.das2.model.Strand;



/**
 *  union of attributes from <SEGMENT> element in /segments XML doc and 
 *   attributes from <SEGMENT> element in /features XML doc
 * 
 */
public interface Das1SegmentI {
    public String getID();  // shared by /segments and /features <SEGMENT>
    public int getStart();  // shared by /segments and /features <SEGMENT>
    public int getStop();   // shared by /segments and /features <SEGMENT>
    public String getType();  // shared by /segments and /features <SEGMENT>
    
    public Strand getOrientation();  // only present in /segments <SEGMENT>
    public boolean hasSubParts();    // only present in /segments <SEGMENT>
    public String getDescription();  // text of element, only in /segments <SEGMENT>
    
    public String getVersion(); // only present in /features <SEGMENT>
    public String getLabel();   // only present in /features <SEGMENT>

    //    public List<DasFeatureI> getFeatures();
}


