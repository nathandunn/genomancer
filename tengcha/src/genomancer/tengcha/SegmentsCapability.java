package genomancer.tengcha;

import genomancer.trellis.das2.model.Das2CoordinatesI;
import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2SegmentsCapabilityI;
import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import genomancer.trellis.das2.model.Das2VersionI;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2Segment;
import genomancer.vine.das2.client.modelimpl.Das2SegmentsResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gmod.gbol.simpleObject.io.impl.HibernateHandler;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.Organism;
        
public class SegmentsCapability extends Das2GenericCapability implements Das2SegmentsCapabilityI  {
    Das2SegmentsResponse response = null;
    boolean initialized = false;
    
    public SegmentsCapability(Das2VersionI version, Das2CoordinatesI coords)  {
        super(version.getBaseURI(), (version.getLocalURIString()+"/segments"), "segments", version, coords);
    }
    
    @Override
    public Das2SegmentsResponseI getSegments() {
	if (!initialized)  { initSegments(); }
        return response;
    }

    public Das2SegmentI getSegment(URI segment_uri) {
	if (!initialized)  { initSegments(); }
        return response.getSegment(segment_uri);
    }
    
    protected synchronized void initSegments()  {
	// only initialized once -- if want effect of re-initializing, must create new SegmentsCapability instead
	if (initialized)  { return; }
	System.out.println("called SegmentsCapability.initSegments(): " + version.getLocalURIString());
	Runtime rt = Runtime.getRuntime();
	HibernateHandler handler = ((TengchaDas2Version)this.getVersion()).getHibernateHandler();

        List<Das2SegmentI> segments_list = new ArrayList<Das2SegmentI>();
        List<Das2LinkI> links = new ArrayList<Das2LinkI>();
        List<Das2FormatI> formats = new ArrayList<Das2FormatI>();
        
        URI segment_base_uri = this.getAbsoluteURI().resolve("./");

        String orgName = getOrganismFromURI(segment_base_uri); // assuming http://foo/A.mellifera/, return A.mellifera
        Organism thisOrg = handler.getOrganismByAbbreviation(orgName);
        CVTerm thisRefSeqCvterm = handler.getCVTerm(Config.REFERENCE_SEQUENCE_SO_TERM, Config.REFERENCE_SEQUENCE_CV_NAME);
	int segcount = 0;
	int maxlength = 0;
	Feature maxfeat = null;
        
        // query hibernate and ask for all features with cvterm == REFERENCE_SEQUENCE_SO_TERM and organism == thisOrg
	System.out.println("in SegmentsCapability.initSegments(), looping through segments as GBOL features");
        for (Iterator<? extends Feature> features = 
                handler.getFeaturesByCVTermAndOrganism(thisRefSeqCvterm, thisOrg); features.hasNext();) {  

	     Feature feature = features.next();
	     feature.getName();

	     maxlength = Math.max(maxlength, feature.getSequenceLength());
	     if (feature.getSequenceLength() == maxlength)  { maxfeat = feature; }

	    Das2Segment thisSegment = new Das2Segment(                    
                       segment_base_uri,
		       // new String(feature.getUniqueName()),  
		       new String(feature.getName()),  
		       new String(feature.getName()),
		       null, 
		       feature.getSequenceLength(), 
		       null
                    );
            segments_list.add(thisSegment);
	    segcount++;
	    if (segcount % 100 == 0)  {
		System.out.println("SEQ COUNT: " + segcount + ", name: " + feature.getUniqueName());
	    }
	    // feature.setResidues(null);
        }

	System.out.println("done with segment construction, segment count: " + segcount);
	System.out.println("longest seq: " + maxlength + ", uname: " + maxfeat.getName());
	System.out.println("Used memory: " + ((rt.totalMemory() - rt.freeMemory())/1000));
        response = new Das2SegmentsResponse(segment_base_uri, segments_list, links, formats);
	System.out.println("created Das2SegmentsResponse");
        initialized = true;        
    }
    
    @Override
    public String getResidues(Das2LocationI location){
        return "foobar";
    }

    @Override
    public String getResidues(List<Das2LocationI> locations){
        return "foobar";
    }
    
    protected String getOrganismFromURI(URI thisUri) {
        String[] items = thisUri.toString().split("/");
        String lastItem = items[items.length - 1];
        return lastItem;
    }

}
