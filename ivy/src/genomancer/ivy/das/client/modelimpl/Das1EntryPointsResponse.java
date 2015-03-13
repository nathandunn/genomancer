package genomancer.ivy.das.client.modelimpl;

import genomancer.ivy.das.model.Das1EntryPointsResponseI;
import genomancer.ivy.das.model.Das1SegmentI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Das1EntryPointsResponse extends Das1Response implements Das1EntryPointsResponseI {
    List<Das1SegmentI> entry_points = new ArrayList<Das1SegmentI>();
    Map<String, Das1SegmentI> id2entry = new HashMap<String, Das1SegmentI>();

    public Das1EntryPointsResponse(String request_url, String version, List<Das1SegmentI> entry_points)  {
	super(request_url, version);
	this.entry_points = entry_points;
	for (Das1SegmentI segment : entry_points)  {
	    id2entry.put(segment.getID(), segment);
	}
    }

    public List<Das1SegmentI> getEntryPoints() {
	return entry_points;
    }

    public Das1SegmentI getEntryPoint(String id) {
	return id2entry.get(id);
    }
    
}