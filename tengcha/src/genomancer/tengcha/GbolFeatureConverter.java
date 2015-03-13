package genomancer.tengcha;

import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2TypeI;
import java.net.*;
import java.util.*;

import genomancer.trellis.das2.model.Strand;
import genomancer.vine.das2.client.modelimpl.Das2Feature;
import genomancer.vine.das2.client.modelimpl.Das2Location;
import genomancer.vine.das2.client.modelimpl.Das2Segment;
import genomancer.vine.das2.client.modelimpl.Das2Type;
import org.gmod.gbol.simpleObject.Analysis;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureRelationship;

/**
 * Takes a GBOL Feature (or FeatureLoc) and creates a Trellis Das2Feature
 * @author gregg
 */
public class GbolFeatureConverter {

    static Map<CVTerm, Das2TypeI> gbol2das_type = new HashMap();
    /**
     *    feat is Feature to convert
     *    srcFeature is coordinate system that location coords are specified on
     */
    public static Das2Feature convertGbolFeature(Feature gbolfeat, Feature gbolseq, Das2SegmentI seq, Das2TypeI type)  {
    // public static Das2Feature convertGbolFeature(Feature gbolfeat, Feature gbolseq, Analysis analys)  {
        String gap = null; // not worrying about cigar strings yet
        URI base_uri = seq.getBaseURI();
        Integer id = gbolfeat.getFeatureId();
        String name = gbolfeat.getName();
        String uname = gbolfeat.getUniqueName();
	CVTerm gbol_type = gbolfeat.getType();
	Das2TypeI das_type  = GbolFeatureConverter.convertGbolType(gbol_type);
	//	System.out.println("feature gbol type: " + gbol_type.getName());
	//	System.out.println("feature das  type: " + das_type.getTitle());

	// gbolfeat.get
	// Das2TypeI das_type = getDasType(gbolfeat, analys);
	// Das2SegmentI das_seq = getDasSegment(gbolseq);
        String locuri = null;
        if (id != null) { locuri = id.toString(); }
        else if (uname != null)  { locuri = uname; }
        else { locuri = name; }
        if (locuri == null)  {
            System.err.println("ERROR: in GbolFeatureConverter.convertGbolFeature(), GBOL Feature being converted has no id, name, or uniqueName");
        }
        String title = null;
        if (uname != null)  { title = uname; }
        else if (name != null)  { title = name; }
        
        Das2Feature dasFeat = new Das2Feature(base_uri,   // URI base_uri
					      locuri, // String local_uri_string, 
                                                  // gbolfeat.getUniqueName(), // possible instead of feat id?
					      title, // String title, 
					      null, // String description, 
					      null, // String info_url, 
					      das_type, // das_type, // Das2TypeI type, 
					      null, // Date creation_date, 
					      null // Date last_modified_date) 
                );
	Set<FeatureLocation> locs = gbolfeat.getFeatureLocations();
	for (FeatureLocation loc : locs)  {
	    Feature locSource = loc.getSourceFeature();
            // for now only using location that is relative to srcFeature
            if (gbolseq.equals(locSource))  {
               // if (locSource.getFeatureId() == srcFeature.getFeatureId())  {  }     
                Das2Location dloc = 
                        new Das2Location(seq, 
                        loc.getFmin(), 
                        loc.getFmax(), 
                        convertGbolStrand(loc.getStrand()), 
                        gap);
                dasFeat.addLocation(dloc);
                break;
            }
           
	}
        
        Set<FeatureRelationship> rels = gbolfeat.getChildFeatureRelationships();
        // find "partOf" relations
        for (FeatureRelationship rel : rels)  {

            CVTerm reltype = rel.getType();
            // Set Config.CHILD_PARENT_RELATIONSHIP_CV_NAME in Config.java            
            if (Config.CHILD_PARENT_RELATIONSHIP_CV_NAME.contains(reltype.getName() ) ){
		                       
                // child feature is subject, parent feature is object
                //   based on subject-predicate-object sentence structure
                //   CHILD is PART_OF PARENT
                Feature subfeat = rel.getSubjectFeature();
		CVTerm gbol_subtype = subfeat.getType();
		
		// System.out.println(" subfeat rel: " + reltype.getName()  + ", CV = " + reltype.getCv().getName());
		// System.out.println(" subfeat type: " + gbol_subtype.getName() + ", CV = " + gbol_subtype.getCv().getName());    
		reltype.getCvtermId();
		reltype.getName();
		reltype.getCv().getName();
		
                // Das2Feature das_subfeat = convertGbolFeature(subfeat, gbolseq, seq, type, null);
                Das2Feature das_subfeat = convertGbolFeature(subfeat, gbolseq, seq, type);
                dasFeat.addPart(das_subfeat);
                das_subfeat.addParent(dasFeat);
            }
        }
        return dasFeat;
    }
    
    public static Strand convertGbolStrand(int gbolStrand)  {
	if (gbolStrand == 1)  {  return Strand.FORWARD; }
	else if (gbolStrand == -1)  { return Strand.REVERSE; }
	else  { return Strand.UNKNOWN; }  // includes Chado allowable 0, plus any non-standard values
    }

    public static Das2TypeI convertGbolType(CVTerm gtype)  {
	//    public static Das2TypeI convertGbolType(CVTerm gtype, Analysis gmethod)  {
	Das2TypeI dtype = gbol2das_type.get(gtype);
	if (dtype == null)  {
            String name = gtype.getName();
            String onto = gtype.getCv().getName();
            Integer id = gtype.getCvtermId();
            String onto_term = onto + ":" + id;
            dtype = new Das2Type(
				 null, // base_uri
				 name, // local_uri
				 name, // title
				 null, // description
				 null, // info_url
				 onto_term, // ontology_term_name
				 null, // method
				 true);          // is_searchable
	    System.out.println("created new Das2TypeI: " + dtype.getTitle());
	    gbol2das_type.put(gtype, dtype);
	}
	return dtype;
    }
    
    /*
    public static Das2Feature convertGbolLocation(FeatureLocation loc) {
        return null;
    }
    */
    
}
