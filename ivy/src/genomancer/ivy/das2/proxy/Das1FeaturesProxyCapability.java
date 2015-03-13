package genomancer.ivy.das2.proxy;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import genomancer.ivy.das.model.Das1FeatureI;
import genomancer.ivy.das.model.Das1Phase;
import genomancer.ivy.das.model.Das1FeaturesCapabilityI;
import genomancer.ivy.das.model.Das1FeaturesQueryI;
import genomancer.ivy.das.model.Das1FeaturesResponseI;
import genomancer.ivy.das.model.Das1GroupI;
import genomancer.ivy.das.model.Das1LinkI;
import genomancer.ivy.das.model.Das1LocationRefI;
import genomancer.ivy.das.model.Das1MethodI;
import genomancer.ivy.das.model.Das1SegmentI;
import genomancer.ivy.das.model.Das1TargetI;
import genomancer.ivy.das.model.Das1TypeI;

import genomancer.trellis.das2.model.Das2FeatureI;
import genomancer.trellis.das2.model.Das2FeaturesCapabilityI;
import genomancer.trellis.das2.model.Das2FeaturesQueryI;
import genomancer.trellis.das2.model.Das2FeaturesResponseI;
import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.trellis.das2.model.Das2LocationRefI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Strand;

import genomancer.trellis.das2.server.GenericProxyCapability;
import genomancer.ivy.das.client.modelimpl.Das1FeaturesQuery;
import genomancer.ivy.das.client.modelimpl.Das1LocationRef;
import genomancer.vine.das2.client.modelimpl.Das2Feature;
import genomancer.vine.das2.client.modelimpl.Das2FeaturesResponse;
import genomancer.vine.das2.client.modelimpl.Das2Link;
import genomancer.vine.das2.client.modelimpl.Das2Location;
import genomancer.vine.das2.client.modelimpl.Das2MutableSegment;
import genomancer.vine.das2.client.modelimpl.Das2Property;
import genomancer.vine.das2.client.modelimpl.Das2Segment;
import genomancer.vine.das2.client.modelimpl.Das2Type;
import java.net.URLEncoder;


/**
 *  Uses a Das1FeaturesCapabilityI to get a Das1FeaturesResponseI
 *    uses Das1FeaturesResponseI to get DAS1 feature (etc.) models
 *    builds DAS2 feature (etc.) models base on DAS1 models
 *    Builds a Das2FeaturesResponse based on DAS2 feature models
 * 
 */
public class Das1FeaturesProxyCapability extends GenericProxyCapability 
    implements Das2FeaturesCapabilityI  {

    boolean DEBUG = false;
    //    type_transform;  // standard type transform is to remove all but last path segment

    //    Das2FeaturesCapabilityI das1_features_cap; 
    Das1FeaturesCapabilityI das1_features_cap;
    Das1SegmentsProxyCapability das1_segments_proxy;  // bridge between das1/entry_points and das2/segments
    Das1TypesProxyCapability das1_types_proxy;    // bridge between das1/types and das2/types

    public Das1FeaturesProxyCapability(URI base_uri,  
				       String query_uri, 
				       Das1FeaturesCapabilityI remote_features_cap)  { 
	//				Map<String, String> params) {
	super(base_uri, query_uri, "features", null, null, remote_features_cap);
	this.das1_features_cap = remote_features_cap;
    }

    public long getLastModified(Das2FeaturesQueryI query)  {
        return -1L;
    }

    /**
     *  Uses a Das1FeaturesCapabilityI to get a Das1FeaturesResponseI
     *    uses Das1FeaturesResponseI to get DAS1 feature (etc.) models
     *    builds DAS2 feature (etc.) models based on DAS1 models
     *    Builds a Das2FeaturesResponse based on DAS2 feature models
     * 
     */
    public Das2FeaturesResponseI getFeatures(Das2FeaturesQueryI das2_query) {
	// first translate Das2FeaturesQueryI to Das1FeaturesQueryI
	Das1FeaturesQueryI das1_query = transformQuery(das2_query);
	Das1FeaturesResponseI das1_response = das1_features_cap.getFeatures(das1_query);
	Das2FeaturesResponseI das2_response = tranformResponse(das1_response);
	return das2_response;
    }

    protected Das2FeaturesResponseI tranformResponse(Das1FeaturesResponseI das1_response) {
	List<Das1FeatureI> das1_features = das1_response.getFeatures();
	HashSet<Das1FeatureI> features_already_done = new HashSet<Das1FeatureI>();
	Map<Das1GroupI, Das2Feature> group2feature = new HashMap<Das1GroupI, Das2Feature>();

	boolean types_share_base_uri = true;
	boolean segments_share_base_uri = true;
	List<Das2LinkI> das2_links = new ArrayList<Das2LinkI>();
	List<Das2FeatureI> das2_features = new ArrayList<Das2FeatureI>();

	
	for (Das1FeatureI das1_feature : das1_features)  {
	    Das2Feature das2_feature = transformFeature(das1_feature);
	    List<Das1GroupI> groups = das1_feature.getGroups();
	    if (groups == null || groups.size() <= 0)  {  //
		if (das2_feature != null)  {
		    das2_features.add(das2_feature);
		}
	    }
	    else  {  // otherwise add feature to parent "group" feature, and add group feature to feature list
		for (Das1GroupI group : groups)  {
		    Das2Feature das2_group_feature = group2feature.get(group);
		    if (das2_group_feature == null)  {
			// transform group into parent feature
			das2_group_feature = transformGroup(group);
			if (das2_group_feature != null)  { 
			    das2_features.add(das2_group_feature); 
			    group2feature.put(group, das2_group_feature);
			}
		    }
		    das2_group_feature.addPart(das2_feature);
		    das2_feature.addParent(das2_group_feature);
		}
	    }
	}

	Das2FeaturesResponseI das2_response = new Das2FeaturesResponse(base_uri, das2_features, das2_links, 
								       types_share_base_uri,
								       segments_share_base_uri);
	return das2_response;
    }


    /**
     *   Unfortunately some DAS1 feature IDs cannot simply be treated as absolute or relative URIs
     *   In particular some DAS2 servers use IDs like "21:1000,2000" which is neither a relative URI 
     *   (because of the colon before any slashes) nor is it an absolute URI because schema 
     *   (part before colon) must start with a letter (not digit)
     *
     *   Feature IDs with whitespace also present a problem.
     *   There are probably others that would not correctly parse to absolute/relative URI, but rather 
     *   than trying to figure out all possibilities or write a general URI validator, 
     *   getLocalURI() uses the URI class to validate that a DAS1 ID string can be used as a DAS/2 URI.
     *   If there's a problem, getLocalURI() tries to create a valid URI by 
     *         a) appending "./" to ID (which should solve colon issue mentioned above)
     *         b) URL-encoding the ID (which should solve whitespace issue mentioned above)
     *   If URI constructor still can't use transformed ID to build a URI, then 
     *         return null 
     *   (could possibly add other attempts to transform to valid URI -- maybe base64 encode the whole URI??)
     */
    protected URI getLocalURI(String id)  {
	URI id_uri = null;
	try {
	    id_uri = new URI(id);
	}
	catch (Exception ex1)  {
	    try  {
		id_uri = new URI("./" + id);
	    }
	    catch (Exception ex2)  {
		try  {
		    id_uri = new URI(URLEncoder.encode(id)); 
		}
		catch(Exception ex3)  {
		    // could not create URI!!!
		    // returns null
		}
	    }
	}
	return id_uri;
    }


    /**
     *  does not handle feature grouping/hierarchy, that is done in calling method (this.transformResponse())
     */
    protected Das2Feature transformFeature(Das1FeatureI das1_feature)  {
	String id = das1_feature.getID();
	URI id_uri = getLocalURI(id);
	if (id_uri == null)  {  // can't transform feature if can't create a URI identifier
	    System.out.println("WARNING: could not create URI for feature id: " + id);
	    return null; 
	} 

	String title = das1_feature.getLabel();
	Das1TypeI das1_type = das1_feature.getType();
	Das1SegmentI das1_segment = das1_feature.getSegment();
	int das1_start = das1_feature.getStart();
	int das1_end = das1_feature.getEnd();
	int das2_min = das1_start - 1;
	int das2_max = das1_end;


	Strand strand = das1_feature.getOrientation();  // Strand is same for both DAS1 and DAS2
        double score = das1_feature.getScore();
	Das1Phase phase = das1_feature.getPhase();
	Das1MethodI method = das1_feature.getMethod();
	List<Das1GroupI> groups = das1_feature.getGroups();
	List<Das1TargetI> targets = das1_feature.getTargets();
	List<Das1LinkI> links = das1_feature.getLinks();
	List<String> notes = das1_feature.getNotes();

	String description = null;
	String info_url = null; 
	Das2TypeI das2_type = getDas2Type(das1_type);
	Das2SegmentI das2_segment = getDas2Segment(das1_segment);

	//Das2Feature das2_feature = new Das2Feature(base_uri, id, title, description, info_url, 
	Das2Feature das2_feature = new Das2Feature(base_uri, id_uri.toString(), title, description, info_url, 
						   das2_type, null, null);

	// das1_start = 0, das1_end = 0 
	// is convention used by some DAS1 servers (particularly UniProt) to represent features with no location -- 
	//   therefore is see this, _don't_ add location
	if (das1_start > 0 && das1_end > 0)  {
	    Das2LocationI loc = new Das2Location(das2_segment, das2_min, das2_max, strand);
	    das2_feature.addLocation(loc);
	}

	// add property for score if score is anything other than NO_SCORE
	if (score != Das1FeatureI.NO_SCORE)  {
	    das2_feature.addProperty(new Das2Property("das1:score", Double.toString(score)));
	}
	// add property for phase if phase is anthing other than NO_APPLICABLE
	if (phase != Das1Phase.NOT_APPLICABLE)  {
	    das2_feature.addProperty(new Das2Property("das1:phase", phase.toString()));
	}
	// method -- ignore?  mostly handled in typing?

	if (targets != null && targets.size() > 0)  {
	    for (Das1TargetI target : targets)  {
		Das2LocationI target_loc = getDas2Location(target);
		das2_feature.addLocation(target_loc);
	    }
	}
	if (links != null && links.size() > 0)  {
	    for (Das1LinkI das1_link : links)  {
		Das2LinkI das2_link = new Das2Link(das1_link.getHref(), das1_link.getLabel(), null, null, null);
		das2_feature.addLink(das2_link);
	    }
	}
	if (notes != null && notes.size() > 0)  {
	    for (String note : notes)  {
		das2_feature.addNote(note);
	    }
	}
	// transformFeature() does not deal with groups, 
	//    that's done in calling function (this.transformResponse())
	//	if (groups != null && groups.size() > 0)  { }
	
	return das2_feature;
    }
  
    /**
     *  Assumes all features in group are on same sequence / segment 
     *  Does not handle adding parts to resultant Das2Feature representing group, that is 
     *     done in calling method (this.transformResponse())
     */
    protected Das2Feature transformGroup(Das1GroupI group)  {
	Das2Feature group_feature = null;

	String id = group.getID();
	URI id_uri = getLocalURI(id);
	if (id_uri == null)  { // can't transform group if can't create a URI identifier
	    System.out.println("WARNING: could not create URI for group id: " + id);
	    return null; 
	}  
	String label = group.getLabel();
	//	Das1TypeI das1_type = group.getType();
	List<String> notes = group.getNotes();
	List<Das1LinkI> links = group.getLinks();
	List<Das1TargetI> targets = group.getTargets();
	List<Das1FeatureI> features = group.getFeatures();
	Das1SegmentI das1_segment = null;
	int forward_count = 0;
	int reverse_count = 0;
	int unknown_count = 0;
	int both_count = 0;
	int na_count = 0;

	int feat_count = (features == null ? 0 : features.size());
	//	if (features != null && feat_count > 0)  {
	if (feat_count > 0)  {
	    int gmin = Integer.MAX_VALUE;
	    int gmax = Integer.MIN_VALUE;
	    for (Das1FeatureI feature : features)  {
		gmin = Math.min(feature.getStart() - 1, gmin);
	 	gmax = Math.max(feature.getEnd(), gmax);
		das1_segment = feature.getSegment();
		Strand fstrand = feature.getOrientation();
		if (fstrand == Strand.FORWARD)  { forward_count++; }
		else if (fstrand == Strand.REVERSE)  { reverse_count++; }
		else if (fstrand == Strand.NOT_APPLICABLE)  { na_count++; }
		else if (fstrand == Strand.UNKNOWN)  { unknown_count++; }
		else if (fstrand == Strand.BOTH)  { both_count++; }
	    }

	    Strand strand;
	    // deciding strand based on strand of all features in group
	    if (forward_count == feat_count)  { strand = Strand.FORWARD; }
	    else if (reverse_count == feat_count)  { strand = Strand.REVERSE; }
	    else if (na_count == feat_count)  { strand = Strand.NOT_APPLICABLE; }
	    else if (unknown_count == feat_count)  { strand = Strand.UNKNOWN; }
	    else if (both_count == feat_count)  { strand = Strand.BOTH; }
	    else  {  // not all of the features in the group have same orientation
		// if no UNKNOWN or NOT_APPLICABLE, then only FORWARD, REVERSE, and BOTH, 
		///  so assign BOTH to group
		if ((unknown_count == 0) && (na_count == 0))  {
		    strand = Strand.BOTH;
		}
		else  { 
		    // too mixed to make a call, so assign UNKNOWN
		    strand = Strand.UNKNOWN;
		}
	    }
	    Das2SegmentI das2_segment = getDas2Segment(das1_segment);
	    //	    Das2TypeI das2_type = getDas2Type(das1_type);
	    Das2TypeI das2_type = getDas2GroupType(group);
	    //	    group_feature = new Das2Feature(base_uri, id, label, null, null, 
	    group_feature = new Das2Feature(base_uri, id_uri.toString(), label, null, null, 
					    das2_type, null, null);
	    Das2LocationI loc = new Das2Location(das2_segment, gmin, gmax, strand);
	    group_feature.addLocation(loc);

	    // not sure what to do when "child" features have target XYZ but "parent" group does not
	    // could do targets that group does have first, then go through and if all child features 
	    //    have target XYZ not present in group targets, make a location based on extent of all 
	    //    XYZ targets in child features??

	    if (targets != null && targets.size() > 0)  {
		for (Das1TargetI target : targets)  {
		    Das2LocationI target_loc = getDas2Location(target);
		    group_feature.addLocation(target_loc);
		}
	    }
	    if (links != null && links.size() > 0)  {
		for (Das1LinkI das1_link : links)  {
		    Das2LinkI das2_link = new Das2Link(das1_link.getHref(), das1_link.getLabel(), null, null, null);
		    group_feature.addLink(das2_link);
		}
	    }
	    if (notes != null && notes.size() > 0)  {
		for (String note : notes)  {
		    group_feature.addNote(note);
		}
	    }
	}
	return group_feature;
    }


    /**
     *  failsafe for when there is no das1_types_proxy
     *   intelligent guess at DAS1 type id based on DAS/2 type URI
     *   repeats some of what's in Das1TypesProxyCapability
     *
     *    (similar flow as getDas1LocationRef, but types instead of locations, and ids instead of ref objects)
     */
    protected String getDas1TypeID(URI das2_type_uri)  {
	String das1_type_id;
	if (das1_types_proxy == null)  {
	    // assume everything but last component of URI path is proxy stuff, just keep last component
	    das1_type_id = local_typeid_map.get(das2_type_uri);
	    if (das1_type_id == null)  {
		String das2_uri_str = das2_type_uri.toString();
		String[] path_components = das2_uri_str.split("/");
		das1_type_id = path_components[path_components.length - 1];
		System.out.println("das1_type_id: " + das1_type_id + ",   das2_type_uri: " + das2_uri_str);
		local_typeid_map.put(das2_type_uri, das1_type_id);
	    }
	}
	else  {
	    Das2TypeI das2_type = das1_types_proxy.getDas2Type(das2_type_uri);
	    Das1TypeI das1_type = das1_types_proxy.getDas1Type(das2_type);
	    das1_type_id = das1_type.getID();
	}
	return das1_type_id;
    }


    /**
     *   Das2Type of a feature cannot (or at least should not) be null
     *   On the other hand Das1GroupI type is allowed to be null 
     *   If DAS1 group has not type, then try to determine type based on 
     *     types of all feature children
     *     If child types follow sequence ontology, might be able to derive group type from that??
     *        If all children have same type, then assign that type to group as well?
     *         (or maybe [children_type]-group?)
     *        If children have different types, 
     */
    protected Das2TypeI getDas2GroupType(Das1GroupI das1_group)  {
	Das1TypeI das1_type = das1_group.getType();
	Das2TypeI das2_type = null;
	if (das1_type == null)  {
	    das2_type = DAS2_UNKNOWN_GROUP_TYPE;
	}
	else  { das2_type = getDas2Type(das1_type); }
	return das2_type;
    }


    protected Das2TypeI DAS2_UNKNOWN_GROUP_TYPE = new Das2Type(base_uri, 
					       "unknown_group_type", 
					       "unknown_group_type", 
					       "could not determine group type", 
					       null, 
					       null, 
					       "unknown_group_method", 
					       true);

    protected Das2TypeI DAS2_UNKNOWN_TYPE = new Das2Type(base_uri, 
					       "unknown_type", 
					       "unknown_type", 
					       "could not determine type", 
					       null, 
					       null, 
					       "unknown_method", 
					       true);

    /**
     *  failsafe for when there is no das1_types_proxy
     *   intelligent guess at Das2TypeI based on Das1TypeI
     *   repeats some of what's in Das1TypesProxyCapability
     *
     *    (similar flow as getDas2Segment(), but with types instead of segments)
     */
    protected Das2TypeI getDas2Type(Das1TypeI das1_type)  {
	if (das1_type == null)  { return DAS2_UNKNOWN_TYPE; }
	Das2TypeI das2_type = null;
	if (das1_types_proxy != null)  {
	    das2_type = das1_types_proxy.getDas2Type(das1_type);
	}
	if (das2_type == null)  {
	    das2_type = das1_to_das2_type.get(das1_type);
	    if (das2_type == null)  {
		das2_type = Das1TypesProxyCapability.createDas2Type(base_uri, das1_type);
		System.out.println("created Das2TypeI: " + das2_type.getAbsoluteURI());
		das1_to_das2_type.put(das1_type, das2_type);
	    }
	}
	return das2_type;
    }

    /**
     *
     *
     */  
    Map<String, Das2SegmentI> target_id_to_das2_segment = new HashMap<String, Das2SegmentI>();

    /**
     *   Used if there is no das1_types_proxy (or getting null returns from proxy)
     *   map of Das1TypeI to Das2TypeI
     *   if no das1_types_proxy, then populated as new Das2TypeI are created in getDas2Type()
     */
    Map<Das1TypeI, Das2TypeI> das1_to_das2_type = new HashMap<Das1TypeI, Das2TypeI>();

    /**
     *   Used if there is no das1_types_proxy (or getting null returns from proxy)
     *   map of DAS2 type URIs to DAS1 type ID
     *   if no das1_types_proxy, then populated as new DAS2 type URIs are encountered in getDas1TypeID()
     */
    Map<URI, String> local_typeid_map = new HashMap<URI, String>();

    /**
     *   Used if there is no das1_segments_proxy (or getting null returns from proxy)
     *   map of Das2LocationRefI segment URIs to Das1LocationRefI segment IDs
     *   if no das1_segments_proxy, then populated as new segment URIs are encountered in getDas1LocationRef() 
     */
    Map<URI, String> das2_seguri_to_das1_segid = new HashMap<URI, String>();


    /**
     *   Used if there is no das1_segments_proxy (or getting null returns from proxy)
     *   map of Das1SegmentI to Das2SegmentI
     *   if no das1_segments_proxy, then populated as new Das2SegmentI are created in getDas2Segment()
     */
    Map<Das1SegmentI, Das2SegmentI> das1_to_das2_segment = new HashMap<Das1SegmentI, Das2SegmentI>();

    protected Das2SegmentI getDas2Segment(Das1SegmentI das1_segment)  {
	Das2SegmentI das2_segment = null;
	if (das1_segments_proxy != null)  {
	    das2_segment = das1_segments_proxy.getDas2Segment(das1_segment);
	}
	if (das2_segment == null)  {
	    das2_segment = das1_to_das2_segment.get(das1_segment);
	    if (das2_segment == null)  {
		String title = das1_segment.getLabel();
		if (title == null)  { title = das1_segment.getID(); }
		das2_segment = new Das2Segment(base_uri, 
					       das1_segment.getID(), 
					       title, 
					       null, 
					       das1_segment.getStop(), 
					       null);
		if (DEBUG)  { System.out.println("created Das2SegmentI: " + das2_segment.getAbsoluteURI()); }
		das1_to_das2_segment.put(das1_segment, das2_segment);
	    }
	}
	return das2_segment;
    }

    protected Das2LocationI getDas2Location(Das1TargetI target)  {
	//    protected Das2SegmentI getDas2Segment(Das1TargetI target)  {
	Das2SegmentI das2_segment = null;
	String target_id = target.getID();
	int min = target.getStart() - 1;
	int max = target.getStop();
	if (das1_segments_proxy != null)  {
	    Das1SegmentI das1_segment = das1_segments_proxy.getDas1Segment(target_id);
	    if (das1_segment != null) {
		das2_segment = das1_segments_proxy.getDas2Segment(das1_segment);
	    }
	}
	if (das2_segment == null)  {
	    das2_segment = target_id_to_das2_segment.get(target_id);
	    if (das2_segment == null)  {
		String target_name = target.getName();
		if (target_name == null)   { target_name = target_id; }
		// creating a MutableDas2Sdsfegment because don't know actual length of segment, 
		//     so initially set to max of target, allow stretching later
		das2_segment = new Das2MutableSegment(base_uri, 
						      target_id, 
						      target_name, 
						      null, 
						      max, 
						      null);
		if (DEBUG)  { System.out.println("created Das2SegmentI: " + das2_segment.getAbsoluteURI()); }
		target_id_to_das2_segment.put(target_id, das2_segment);
	    }
	}
	if (das2_segment instanceof Das2MutableSegment)  {
	    Das2MutableSegment mutable_segment = (Das2MutableSegment)das2_segment;
	    if (das2_segment.getLength() < max)  {
		mutable_segment.setLength(max);
	    }
	}
	Das2LocationI das2_loc = new Das2Location(das2_segment, min, max, Strand.FORWARD);
	return das2_loc;
    }


    /**
     *  failsafe for when there is no das1_segments_proxy
     *   intelligent guess at Das1LocationRefI based on Das2LocationRefI
     *   repeats some of what's in Das1SegmentsProxyCapability
     */
    protected Das1LocationRefI getDas1LocationRef(Das2LocationRefI das2_loc)  {
	Das1LocationRefI das1_loc = null;
	if (das1_segments_proxy != null)  {
	    das1_loc = das1_segments_proxy.getDas1LocationRef(das2_loc);
	}
	if (das1_loc == null)  {
	    // assume everything but last component of URI path is proxy stuff, just keep last component
	    URI das2_uri = das2_loc.getSegmentURI();
	    String das1_id = das2_seguri_to_das1_segid.get(das2_uri);
	    if (das1_id == null)  {
		String das2_uri_str = das2_uri.toString();
		String[] path_components = das2_uri_str.split("/");
		das1_id = path_components[path_components.length - 1];
		System.out.println("das1_seg_id: " + das1_id + ",   das2_seg_uri: " + das2_uri_str);
		das2_seguri_to_das1_segid.put(das2_uri, das1_id);
	    }
	    Strand strand = das2_loc.getStrand();
	    if (das2_loc.coversEntireSegment())  {
		das1_loc = new Das1LocationRef(das1_id, strand);
	    }
	    else {
		int min = das2_loc.getMin() + 1;
		int max = das2_loc.getMax();
		das1_loc = new Das1LocationRef(das1_id, min, max, strand);
	    }
	}
	return das1_loc;
    }



    /**
     *  Given a Das2FeaturesQueryI, returns the corresponding Das1FeaturesQueryI
     *  all param names and values are URL-encoded
     *  currently all values that are URIs are absolute
     */
    protected Das1FeaturesQueryI transformQuery(Das2FeaturesQueryI das2_query)  {
      System.out.println("called Das1FeaturesProxyCapability.transformQuery()");
	Das1FeaturesQuery das1_query = new Das1FeaturesQuery();

	String format = das2_query.getFormat();
	List<Das2LocationRefI> overlaps = das2_query.getOverlaps();
	List<Das2LocationRefI> insides = das2_query.getInsides();
	List<Das2LocationRefI> excludes = das2_query.getExcludes();
	List<URI> types = das2_query.getTypes();
	List<URI> coords = das2_query.getCoordinates();
	List<URI> links = das2_query.getLinks();
	List<String> names = das2_query.getNames();
	List<String> notes = das2_query.getNotes();
	Map<String, List<String>> non_standard_params = das2_query.getNonStandardParams();

	if ((overlaps != null && overlaps.size() > 0) || 
	    (insides != null && insides.size() > 0) || 
	    (excludes != null && excludes.size() > 0) ) {
	    if (overlaps != null)  {
		for (Das2LocationRefI loc : overlaps)  { 
		    Das1LocationRefI das1_loc = getDas1LocationRef(loc);
		    das1_query.addLocation(das1_loc);
		}
	    }
	    for (Das2LocationRefI loc : insides)  {
		// NOT YET IMPLEMENTED 
		// handle in features response as a further restriction

	    }
	    for (Das2LocationRefI loc : excludes)  {
		// NOT SUPPORTED (but passthrough just in case?)

	    }
	}
	if (types != null && types.size() > 0)  {
	    for (URI das2_type_uri : types)  { 
		// add both "type" and "category" filters to das2_query?
		String das1_type_id = getDas1TypeID(das2_type_uri);
		das1_query.addType(das1_type_id);
	    } 
	}
	if (coords != null && coords.size() > 0)  {
	    // NOT SUPPORTED (but passthrough just in case?)
	    //	    for (URI coorduri : coords )  { }
	}
	if (links != null && links.size() > 0)  {
	    // NOT SUPPORTED (but passthrough just in case?)
	    //	    for (URI linkuri : links)  { }
	}
	if (names != null && names.size() > 0)  {
	    for (String name : names)  { 
		// add both "feature_id" and "group_id" filters to das2_query?
		das1_query.addGroup(name);
		das1_query.addFeature(name);
	    }		
	}
	if (notes != null)  {
	    // NOT SUPPORTED (but passthrough just in case?)
	    //	 for (String note : notes)  {  }
	}
	if (non_standard_params != null && non_standard_params.size() > 0)  {
	    for (String param_name : non_standard_params.keySet())  {
		List<String> param_values = non_standard_params.get(param_name);
		for (String param_val : param_values)  {  
		    // qsb.addParameter(param_name, param_val);
		}
	    }
	}
	if (format != null)   {

	}
	
	return das1_query;
    }

    public boolean supportsFullQueryFilters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean supportsCountFormat() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean supportsUriFormat() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getFeaturesCount(Das2FeaturesQueryI query) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<String> getFeaturesURI(Das2FeaturesQueryI query) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public InputStream getFeaturesAlternateFormat(Das2FeaturesQueryI query) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Class getFeatureClassForType(Das2TypeI type) {
        return Das2FeatureI.class;
    }

    public int getMaxHierarchyDepth(Das2TypeI type) {
        return Das2FeaturesCapabilityI.UNKNOWN;
    }


}
