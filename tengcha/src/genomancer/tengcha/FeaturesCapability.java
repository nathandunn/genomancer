package genomancer.tengcha;

import genomancer.trellis.das2.model.*;
import genomancer.vine.das2.client.modelimpl.Das2FeaturesResponse;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.gmod.gbol.simpleObject.Analysis;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;
import org.gmod.gbol.simpleObject.io.impl.HibernateHandler;

public class FeaturesCapability extends Das2GenericCapability implements Das2FeaturesCapabilityI {

    URI response_base_uri = null;
    HibernateHandler handler = null;
    
    Das2TypesCapabilityI types_cap;  // don't use directly, use this.getSegmentsCapability()
    Das2SegmentsCapabilityI segs_cap;  // don't use directly, use this.getSegmentsCapability()

    List<Analysis> analyses = new ArrayList();
            
    Das2TypesResponseI types_response;
    Das2SegmentsResponseI segs_response;

    URI base_uri = this.getAbsoluteURI().resolve("./");
    
    long last_modified_time = -1L;

    //
    // As of 2/29/2012, the plan is for Tengcha to only support type, segment and overlap filters
    //    Other filters described below may be supported in the future.
    //
    // name	takes	matches features ...
    // link	URI	which have the given link
    // type	URI	with exactly the given type
    // segment	URI	on the given segment
    // coordinates	URI	which are part of the given coordinate system
    // overlaps	region	which overlap the given region
    // excludes	region	which have no overlap to the given region
    // inside	region	which are contained inside the given region
    // name	string	with a "title" or "alias" matching the given string
    // note	string	with a "note" matching the given string
    // prop-*	string	with the property "*" matching the given string
    public FeaturesCapability(Das2VersionI version, Das2CoordinatesI coords) {
        super(version.getBaseURI(), (version.getLocalURIString() + "/features"), "features", version, coords);
	System.out.println("called genomancer.tengcha.FeaturesCapability() constructor");
        response_base_uri = this.getAbsoluteURI().resolve("./");
        Das2VersionI das2_version = version;
	handler = ((TengchaDas2Version)this.getVersion()).getHibernateHandler();
	System.out.println("finished call to  genomancer.tengcha.FeaturesCapability() constructor");
    }

    protected synchronized Das2SegmentsCapabilityI getSegmentsCapability() {
        if (segs_cap == null) {
            segs_cap = (Das2SegmentsCapabilityI) version.getCapability("segments");
        }
        return segs_cap;
    }

    protected synchronized Das2TypesCapabilityI getTypesCapability() {
        if (types_cap == null) {
            types_cap = (Das2TypesCapabilityI) version.getCapability("types");
        }
        return types_cap;
    }

    protected synchronized List<Analysis> getAllAnalyses() {
        if (analyses.isEmpty()) {
            String orgName = getOrganismFromURI(base_uri); // assuming http://foo/A.mellifera/, return A.mellifera
            Organism thisOrg = handler.getOrganismByAbbreviation(orgName);
            for (Iterator<? extends Analysis> analysesIterator = handler.getAnalysesForOrganism(thisOrg); analysesIterator.hasNext();) {
                Analysis thisAnalysis = analysesIterator.next();
                analyses.add( thisAnalysis );
            }
        }
        return analyses; 
    }    

    protected Analysis getAnalysisFromType( Das2TypeI type ){
        Analysis thisAnalysis = null;
        List<Analysis> allAnalyses = this.getAllAnalyses();

        // iterate through all analyses for this organism and find the one corresponding to 'type'
        for ( int i = 0; i < allAnalyses.size() ; i++){
            Analysis thisA = allAnalyses.get(i);
            String thisAURI;
            try {
                thisAURI = URLEncoder.encode(
                        thisA.getProgram() + "_" + thisA.getProgramVersion() + "_" + thisA.getSourceName(),
                        "UTF-8");
                if (thisAURI.equals(type.getLocalURIString() ) ) {
                    thisAnalysis = thisA;
                    break;
                }
            } catch (Exception E) {
            }

        }      
        
	if ( thisAnalysis == null) {
	   System.out.println("!!!! problem retrieving Analysis for Chado type " + type.toString() + " " + type.getTitle() );
	}

        return thisAnalysis;
    }
    
    protected String getOrganismFromURI(URI thisUri) {
        String[] items = thisUri.toString().split("/");
        String lastItem = items[items.length - 1];
        return lastItem;
    }

    protected String getSegmentNameFromURI(URI thisUri) {
        String[] items = thisUri.toString().split("/");
        String lastItem = items[items.length - 1];
        return lastItem;
    }
    
    @Override
    public long getLastModified(Das2FeaturesQueryI query) {
        return last_modified_time;
    }

    /**
     *  Assumes one (and only one) segment in query
     *  Assumes one (and only one) type in query
     */
    @Override
    public Das2FeaturesResponseI getFeatures(Das2FeaturesQueryI query) {

        // features to be returned
	List<Das2FeatureI> features = new ArrayList<Das2FeatureI>();

        URI typeuri;
        if (query.getTypes().size() <= 0) { 
	    System.out.println("in FeaturesCapability, could not determine type for filter");
	}
	else  {

	typeuri = query.getTypes().get(0);
        
	// TypesCapability typescap = (TypesCapability)version.getCapability("types");
	//	Das2TypeI type = typescap.getType(typeuri);
	Das2TypeI type = this.getTypesCapability().getType(typeuri);

	System.out.println("in tengcha FeaturesCapability.getFeatures(), querying for feature type: " + type.getLocalURIString());
	

        // get segment requested
        //    todo - need to add something here to throw exception if URI contains >1 segment (need a test too)
	//        URI requestedSegmentUri = query.getOverlaps().get(0).getSegmentURI();

	Das2LocationRefI loc = query.getOverlaps().get(0);
        URI requestedSegmentUri = loc.getSegmentURI();
        Das2SegmentI segment = (Das2SegmentI) this.getSegmentsCapability().getSegment(requestedSegmentUri);
        Feature segmentFeature = null;
        // todo - return empty set of features here if segment is null
        
        //
        // make featurelocation object from segment (and later by coordinates)
        // 

        FeatureLocation floc = new FeatureLocation();        
        try {
            //
            // to get feature object for this segment, we need organism, 
	    //    cvterm for reference sequence and uniquename of reference sequence to get feature object
            // 
            // first get organism
            String orgName = getOrganismFromURI(base_uri); // assuming http://foo/A.mellifera/, return A.mellifera
            Organism thisOrg = handler.getOrganismByAbbreviation(orgName);

            // now get cvterm object for segment
            String refSoTermString = Config.REFERENCE_SEQUENCE_SO_TERM;
            String refCvTermString = Config.REFERENCE_SEQUENCE_CV_NAME;
            CVTerm refCvTerm = handler.getCVTerm(refSoTermString, refCvTermString);

            // now uniquename of reference sequence
            URI refURI = requestedSegmentUri;
            String refUniquename = this.getSegmentNameFromURI( refURI );
            segmentFeature = handler.getFeature(thisOrg, refCvTerm, refUniquename);

            // now construct FeatureLocation
            // floc.setFmin(0);
	    // floc.setFmax(segment.getLength()); // set fmax to length of segment for now, to pass segments filter test
	    floc.setFmin(loc.getMin());
            floc.setFmax(loc.getMax());
            floc.setSourceFeature(segmentFeature);
        } catch (Exception ex) {
	    ex.printStackTrace();
        }
        
        // return blank feature set if we couldn't retrieve segmentFeature (source feature) that user requested from db
        
        // get types (i.e. analysis) items requested
        //    todo - get types requested, or else get features of all types 
        //    for now we'll just get all types, so we can pass segment filter and coord filter tests
	//        types_response = this.getTypesCapability().getTypes();
	//        List<Das2TypeI> types = types_response.getTypes();
        
        int feat_count = 0;
        int child_count = 0;
        
	// find this analysis item using type
	Analysis thisAnalysis = getAnalysisFromType( type );

	// System.out.println("+++ in types loop, analysis item: " + thisAnalysis.getName());
	// System.out.println("=== typesloop type: " + type.getTitle() );

	//
	// use the following method to get top level features:
	// handler.getTopLevelFeaturesByOverlappingRangeAndAnalysis(loc, analysis)
	//
	try {
	    for (Iterator<? extends Feature> featureIter =
		     handler.getTopLevelFeaturesByOverlappingRangeAndAnalysis(floc, thisAnalysis);
		 featureIter.hasNext();) {
		Feature gbolFeat = featureIter.next();

		// the following 1) retrieves all children of gbolFeat and 2) converts gbolFeat and children
		// into one uber Das2Feature family
		Das2FeatureI f = GbolFeatureConverter.convertGbolFeature(gbolFeat, segmentFeature, segment, type);
		feat_count++;
		features.add(f);
	    }
                
	} catch (Exception ex) {
	    ex.printStackTrace();                
	}
	System.out.println("/// found: " + feat_count + " top-level features for analysis: " + thisAnalysis.getName());
        
        }  // end types.size() > 0 conditional

        Das2FeaturesResponse response = new Das2FeaturesResponse(response_base_uri, features, null, true, true);
        return response;
    }

    @Override
    public int getFeaturesCount(Das2FeaturesQueryI query) {
        return -1;  // return of -1 signifies feature count is unknown
    }

    @Override
    public boolean supportsCountFormat() {
        return false;
    }

    @Override
    public List<String> getFeaturesURI(Das2FeaturesQueryI query) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean supportsUriFormat() {
        return false;
    }

    @Override
    public InputStream getFeaturesAlternateFormat(Das2FeaturesQueryI query) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Class getFeatureClassForType(Das2TypeI type) {
        return Das2FeatureI.class;
    }

    @Override
    public int getMaxHierarchyDepth(Das2TypeI type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean supportsFullQueryFilters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
