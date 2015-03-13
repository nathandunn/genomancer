package genomancer.ivy.das.model;

import genomancer.trellis.das2.model.Strand;
import java.util.List;

/**
 * 
 * 
 */
public interface Das1FeatureI {
    static public double NO_SCORE = Double.MIN_VALUE;
    static public int NO_START = -1;
    static public int NO_END = -1;

    public String getID();
    public String getLabel();
    public Das1TypeI getType();
    public Das1SegmentI getSegment();
    public int getStart();
    public int getEnd();
    public Strand getOrientation();
    public boolean hasScore();
    public double getScore();
    public Das1Phase getPhase();
    public Das1MethodI getMethod();
    public List<Das1GroupI> getGroups();
    public List<Das1TargetI> getTargets();
    public List<Das1LinkI> getLinks();
    public List<String> getNotes();

}


