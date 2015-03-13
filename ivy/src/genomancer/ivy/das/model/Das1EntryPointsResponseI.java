
package genomancer.ivy.das.model;

import java.net.URL;
import java.util.List;

/**
 * 
 * 
 */
public interface Das1EntryPointsResponseI extends Das1ResponseI  {

    public List<Das1SegmentI> getEntryPoints();
    public Das1SegmentI getEntryPoint(String id);

}

