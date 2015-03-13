package genomancer.vine.das2.client.xml;

import genomancer.trellis.das2.Das2Constants;
import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2FeatureI;
import genomancer.trellis.das2.model.Das2FeaturesCapabilityI;
import genomancer.trellis.das2.model.Das2FeaturesResponseI;
import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.trellis.das2.model.Das2PropertyI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2SegmentsCapabilityI;
import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2TypesCapabilityI;
import genomancer.trellis.das2.model.Das2TypesResponseI;
import genomancer.trellis.das2.model.Das2VersionI;
import genomancer.trellis.das2.model.Strand;
import genomancer.trellis.das2.xml.FeaturesXmlWriter;
import genomancer.vine.das2.client.modelimpl.Das2Feature;
import genomancer.vine.das2.client.modelimpl.Das2FeaturesResponse;
import genomancer.vine.das2.client.modelimpl.Das2Location;
import genomancer.vine.das2.client.modelimpl.Das2MutableSegment;
import genomancer.vine.das2.client.modelimpl.Das2SegmentsCapability;
import genomancer.vine.das2.client.modelimpl.Das2Type;
import genomancer.vine.das2.client.modelimpl.Das2TypesCapability;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import org.jdom.input.XmlFragmentBuilder;


public class FeaturesXmlReader extends AbstractDas2XmlReader {
    static final Pattern url_query_splitter = Pattern.compile("[;\\&]");
    static final Pattern equals_splitter = Pattern.compile("=");
    static final Pattern range_splitter = Pattern.compile(":");

    /**
     *  Handling of parent/part feature relationships
     *  Current way:
     *      uri2feature map of all features in doc
     *      feat2parts map of each feature to list of feature's parts' local_uris
     *      feat2parents map of each feature to list of feature's parents' local_uris
     *
     *      features are Das2Feature implementation, so can add parent/part after construction
     *      create feature for every FEATURE element, but add to uri2feature map rather than 
     *           directly to features list
     *          for every PART element, add to uri2parts list for feature
     *          for every PARENT element, add to uri2parents list for feature
     *      at end of doc, iterate through uri2feature.values()
     *          find root features, those that have no parents in uri2parents map
     *          add root features to feature list (features that have no parents)
     *      iterate through root features list
     *         recurse down through part hierarchy
     *         resolve parent/part refs via uri2feature map
     *         assume document has parent/part feature closure as required by spec
     *            (maybe add check for feature closure of all roots at end)
     *     use root features list for constuction of Das2FeaturesResponse
     * 
     *   Only problem with above approach is that it doesn't support streaming parsing, at 
     *      least not relative to code calling this reader -- only get set of Features at end of parsing
     *   Could probably add this, with an event/listen model, but would need to revise above to 
     *      create fully recursively populated root feature (and notify via event) as soon as 
     *      (recursively) all parts are found 
     *      (may also need to ensure that any part with multiple parents has _all_ parents populated...)
     *      This would be more involved (and most likely slower since more tracking and checking is involved), 
     *          but it's doable -- wait and see how needed this is...
     */
    Map<String, Das2Feature> uri2feature= new LinkedHashMap<String, Das2Feature>(); // preserving feature order
    Map<Das2Feature, List<String>> feat2parturis = new HashMap<Das2Feature, List<String>>();
    Map<Das2Feature, List<String>> feat2parenturis = new HashMap<Das2Feature, List<String>>();
    
    /**
     *   features capability passed in constructor
     */
    Das2FeaturesCapabilityI feat_cap;
   
    /**
       optional root LINK element pointing to DAS/2 types capability for type refs in doc
       must have rel="types" && type=Das2Constants.TYPES_CONTENT_TYPE
    */
    Das2LinkI types_cap_link;

    /**
       optional root LINK element pointing to DAS/2 segments capability for segment refs in doc
       must have rel="segments" && type=Das2Constants.SEGMENTS_CONTENT_TYPE
    */
    Das2LinkI segments_cap_link;

    /**
       optional root LINK element pointing to _a_ DAS/2 sources capability that has a 
          source-->version-->feature_capability hierarchy capable of producing this doc
       NOT necessarily the actual source-->version-->feature_capability where this doc came from, since 
          multiple sources docs can include the same capability
       
       must have rel="sources" && type=Das2Constants.SOURCES_CONTENT_TYPE
    */
    Das2LinkI sources_cap_link;

    /**
       optional link to original URL that the doc is a representation of
       must have rel="self" && type=Das2Constants.FEATURES_CONTENT_TYPE
    */
    Das2LinkI self_link;

    Map<String, Das2TypeI> localuri2type = new HashMap<String, Das2TypeI>();
    Map<String, Das2SegmentI> localuri2segment = new HashMap<String, Das2SegmentI>();

    /**
     *  doc_uri SHOULD be an absolute URI
     *  need the doc_uri for determining XmlBase if no "xml:base" attribute, or if "xml:base" is relative
     *  could leave this out if:
     *     a) "xml:base" required
     *     b) "query" required attribute added to FEATURES whose value is original query URL
     */ 
    public FeaturesXmlReader(InputStream istream, URI doc_uri, 
			     Das2FeaturesCapabilityI feat_cap) throws XMLStreamException  {
	super(istream, doc_uri);  // sets xml_base = doc_uri, creates xreader based on istream
	this.feat_cap = feat_cap;
	DEBUG = false;
    }

    public static Das2FeaturesResponseI readFeaturesDocument(InputStream istream, URI doc_uri, 
							     Das2FeaturesCapabilityI feat_cap) 
	throws XMLStreamException, URISyntaxException    {
	FeaturesXmlReader features_reader = new FeaturesXmlReader(istream, doc_uri, feat_cap);
	Das2FeaturesResponseI response = features_reader.readFeaturesDocument();
	features_reader.close();
	return response;
    }

    public static Das2FeaturesResponseI readFeaturesDocument(InputStream istream, URI doc_uri) 
	throws XMLStreamException, URISyntaxException    {
	return readFeaturesDocument(istream, doc_uri, null);
    }


    public Das2FeaturesResponseI readFeaturesDocument() throws XMLStreamException, URISyntaxException  {

	List<Das2FeatureI> features = null;
	List<Das2FormatI> formats = new ArrayList<Das2FormatI>();
	List<Das2LinkI> links = new ArrayList<Das2LinkI>();
       
	System.out.println("XMLInputFactory: " + ifactory);
	System.out.println("XMLStreamReader: " + xreader);
	System.out.println("initial xml_base: " + xml_base);

	frag_builder = new XmlFragmentBuilder();

	while (xreader.hasNext())  {
	    int eventid = xreader.next();
	    if (eventid == XMLStreamConstants.START_ELEMENT)  {
		String elname = xreader.getLocalName();
		if (elname.equals("FEATURE"))  {
		    parseFeatureElement();
		    // Das2FeatureI feature = parseFeatureElement();
		    // features.add(feature);
		}
		else if (elname.equals("FEATURES"))  {
		    // currently not doing anything with features_local_uri
		    String features_local_uri = xreader.getAttributeValue(ns, "uri");
		    setNamespaces();
		    setXmlBase();
		}
		else if (elname.equals("LINK"))  {
		    Das2LinkI link = parseLinkElement();
		    String mime_type = link.getMimeType();
		    String rel = link.getRelationship();
		    if (mime_type != null && rel != null)  {
			if (rel.equals("types") && 
			    mime_type.equals(Das2Constants.TYPES_CONTENT_TYPE))  {
			    types_cap_link = link;
			}
			else if (rel.equals("segments") &&
				 mime_type.equals(Das2Constants.SEGMENTS_CONTENT_TYPE))  {
			    segments_cap_link = link;
			}
			else if (rel.equals("sources")  && 
				 mime_type.equals(Das2Constants.SOURCES_CONTENT_TYPE))  {
			    sources_cap_link = link;
			}
			else if (rel.equals("self") && 
				 mime_type.equals(Das2Constants.FEATURES_CONTENT_TYPE))  {
			    self_link = link;
			}
		    }
		    links.add(link);
		}
	    }
	    else if (eventid == XMLStreamConstants.START_DOCUMENT)  {
		
	    }	
	    else if (eventid == XMLStreamConstants.END_DOCUMENT)  {
		features = processFeatures();
	    }
	}
	Das2FeaturesResponseI response = new Das2FeaturesResponse(xml_base, features, links);
	return response;
    }

    protected List<Das2FeatureI> processFeatures()  {
	List<Das2Feature> root_features = new ArrayList<Das2Feature>();
	for (Das2Feature feat : uri2feature.values())  {
          if (feat2parenturis.get(feat) == null)  {
//	    if ((feat.getParents() == null) || (feat.getParents().size() == 0))  {
		root_features.add(feat);
	    }
	}
	List<Das2FeatureI> processed_features = new ArrayList<Das2FeatureI>(root_features.size());
	for (Das2Feature feat : root_features)  {
	    System.out.println("processing root feature: " + feat.getLocalURIString() + 
			       ", children: " + feat2parturis.get(feat).size());
	    boolean success = processFeature(feat);
	    if (success)  {
		processed_features.add(feat);
		// checkFeatureClosure(feat);
	    }
	}
	return processed_features;
    }

    protected boolean processFeature(Das2Feature feat)  {
	System.out.println("processFeature(): " + feat.getLocalURIString());
	List<String> parent_uris = feat2parenturis.get(feat);
	if (parent_uris != null)  {
	    for (String parent_local_uri : parent_uris)  {
		Das2Feature parent = uri2feature.get(parent_local_uri);
		if (parent == null)  {
		    System.err.println("problem in FeaturesXmlReader, feature parent not present in doc: " + 
				       parent_local_uri);
		    return false;
		}
		System.out.println("  FeaturesXmlReader.processFeature(), adding parent: feat = " + 
				   feat.getLocalURIString() + ", parent = " + parent.getLocalURIString() );
		feat.addParent(parent);
		// don't recurse up hierarchy -- started from top, parts branch is recursing down
	    }
	}
	List<String> part_uris = feat2parturis.get(feat);
	if (part_uris != null)  {
	    for (String part_local_uri : part_uris )  {
		Das2Feature part = uri2feature.get(part_local_uri);
		if (part == null)  {
		    System.err.println("problem in FeaturesXmlReader, feature part not present in doc: " + 
				       part_local_uri);
		    return false;
		}
		System.out.println("  FeaturesXmlReader.processFeature(), adding part: feat = " + 
				   feat.getLocalURIString() + ", part = " + part.getLocalURIString() );
		feat.addPart(part);
		// recurse down hierarchy
		processFeature(part);
	    }
	}
	return true;
    }

    //    protected boolean checkFeatureClosure(Das2Feature feat)  {
    // make sure all parents and parts listed in feat2partursi and feat2parenturis 
    //     are present in Das2Feature and ordered the same
    //    }
    public Das2FeatureI parseFeatureElement() throws XMLStreamException {
	String local_uri = xreader.getAttributeValue(ns, "uri");
	String title = xreader.getAttributeValue(ns, "title");
	String type_uri = xreader.getAttributeValue(ns, "type"); // local uri for type
	String info_url = xreader.getAttributeValue(ns, "doc_href");
       	String description = xreader.getAttributeValue(ns, "doc_href");
	String created = xreader.getAttributeValue(ns, "created");
	String modified = xreader.getAttributeValue(ns, "modified");

	if (DEBUG)  { System.out.println("FEATURE: " + local_uri); }	
	Das2TypeI dtype = getType(type_uri);
	//	URI type_uri = new URI(type); ???
	Date creation_date = null; // = new Date(created);
	Date modified_date = null; // = new Date(modified);

	Das2Feature feature = new Das2Feature(xml_base, local_uri, title, description, info_url, 
					      dtype, creation_date, modified_date);
	uri2feature.put(local_uri, feature);

	while (xreader.hasNext())  {
	    int eventid = xreader.next();
	    if (eventid == XMLStreamConstants.START_ELEMENT)  {
		String elname = xreader.getLocalName();

		if (elname.equals("LOC"))  {
		    Das2LocationI loc = parseLocationElement();
		    feature.addLocation(loc);
		}
		else if (elname.equals("PARENT"))  {
		    String parent_local_uri = xreader.getAttributeValue(ns, "uri");
		    //		    List<String> parents = feat2parenturis.get(parent_local_uri);
		    List<String> parents = feat2parenturis.get(feature);
		    if (parents == null)  { 
			parents = new ArrayList<String>();
			feat2parenturis.put(feature, parents);
		    }
		    parents.add(parent_local_uri);
		}
		else if (elname.equals("PART"))  {
		    String part_local_uri = xreader.getAttributeValue(ns, "uri");
		    //		    List<String> parts = feat2parturis.get(part_local_uri);
		    List<String> parts = feat2parturis.get(feature);
		    if (parts == null)  { 
			parts = new ArrayList<String>();
			feat2parturis.put(feature, parts);
		    }
		    parts.add(part_local_uri);
		}
		else if (elname.equals("PROP"))  {
		    Das2PropertyI prop = parsePropertyElement();
		    feature.addProperty(prop);
		}
		else if (elname.equals("LINK"))  {
		    Das2LinkI link = parseLinkElement();
		    feature.addLink(link);
		}
		else if (elname.equals("ALIAS"))  {
		    String alias = xreader.getAttributeValue(ns, "alias");
		    if (alias != null)  {feature.addAlias(alias); }
		}
		else if (elname.equals("NOTE"))  {
		    String note = xreader.getElementText();
		    if (note != null)  { feature.addNote(note); }
		}
		else  {
		    // additional XML fragments
		    org.jdom.Element xml_fragment = frag_builder.buildXmlFragment(xreader);
		    feature.addAdditionalData(xml_fragment);
		}
	    }
	    else if (eventid == XMLStreamConstants.END_ELEMENT)  {
		String elname = xreader.getLocalName();
		if (elname.equals("FEATURE"))  {
		    break;
		}
	    }

	}

	return feature;
    }


    protected Das2LocationI parseLocationElement()  {
	String seg_local_uri = xreader.getAttributeValue(ns, "segment");
	String range = xreader.getAttributeValue(ns, "range");
	String gap = xreader.getAttributeValue(ns, "gap");
	Das2LocationI loc = null;
	Das2SegmentI seg = getSegment(seg_local_uri);
	String[] minmax = range_splitter.split(range);
	int min = Integer.parseInt(minmax[0]);
	int max;
	if (minmax.length > 1)  {
	    max = Integer.parseInt(minmax[1]);
	}
	else  { max = min; }
	// if seg is Das2MutableSegment, stretch it to include max if needed
	if ((seg instanceof Das2MutableSegment) &&
	    (max > seg.getLength()))  {
	    ((Das2MutableSegment)seg).setLength(max);
	}
	Strand strand = Strand.UNKNOWN;
	if (minmax.length > 2)  {
	    int sint = Integer.parseInt(minmax[2]);
	    if (sint == 1)  { strand = Strand.FORWARD; }
	    else if (sint == -1)  { strand = Strand.REVERSE; }
	    else if (sint == 0)  { strand = Strand.BOTH; }
	}
	loc = new Das2Location(seg, min, max, strand, gap);
	return loc;
    }
 
    protected Das2SegmentI getSegment(String seg_local_uri) {
	Das2SegmentI segment = localuri2segment.get(seg_local_uri);
	if (segment == null)  {
	    // try to find externally
	    URI segment_uri = xml_base.resolve(seg_local_uri);

	    // try to find segment in global client uri2segment map?
	    // global client uri2segments doesn't exist yet...

	    // try to backtrack to version then back to segments capability
	    //   Das2FeatureCapability --> Das2Version --> Das2SegmentsCapability
	    Das2SegmentsCapabilityI segments_cap = null;
	    if (feat_cap != null)  {
		Das2VersionI version = feat_cap.getVersion();
		if (version != null)  {
		    segments_cap = 
                    (Das2SegmentsCapabilityI)version.getCapability(Das2Constants.DAS2_SEGMENTS_CAPABILITY);
		}
	    }
	    if (segments_cap == null)  {
		// not yet found, look for LINK to segments capability
		if (segments_cap_link != null)  {
		    // try to find segments_cap in global client uri2cap map?
		    // doesn't exist yet
		    
		    // create new minimal Das2SegmentsCapability based on segments_cap_link
		    segments_cap = new Das2SegmentsCapability(xml_base, segments_cap_link.getHref(), null, null);
		}
	    }
	    if (segments_cap != null)  {
		Das2SegmentsResponseI segments_response = segments_cap.getSegments();
		if (segments_response != null)  {
		    segment = segments_response.getSegment(segment_uri);
		}
	    }

	    if (segment == null)  {  // couldn't find segment previous local use or externally, so create new one
		segment = new Das2MutableSegment(xml_base, seg_local_uri,
						 seg_local_uri, null, 0, null);
	    }

	    // add to localuri2segment map to avoid additional URI resolving if encountered again in doc
	    localuri2segment.put(seg_local_uri, segment);
	}
	if (DEBUG)  { System.out.println("  segment: " + segment); }
	return segment;
    }

    /**
     *   attempts to find existing Das2TypeI that corresponds to the type_local_uri 
     *   if can't find existing Das2TypeI, creates a new one with minimal info, and keeps it around 
     *        for possible use elsewhere in the document
     */
    protected Das2TypeI getType(String type_local_uri)  {
	Das2TypeI type = localuri2type.get(type_local_uri); // if type uri seen before in doc, use same type
	if (type == null)  {  // couldn't find previous use in doc
	    // try to find externally
	    URI type_uri = xml_base.resolve(type_local_uri);

	    // try to find type in global client uri2type map?
	    // global client uri2types doesn't exist yet...

	    // try to backtrack to version then back to types capability
	    //   Das2FeatureCapability --> Das2Version --> Das2TypesCapability
	    Das2TypesCapabilityI types_cap = null;
	    if (feat_cap != null)  {
		Das2VersionI version = feat_cap.getVersion();
		if (version != null)  {
		    types_cap = (Das2TypesCapabilityI) version.getCapability(Das2Constants.DAS2_TYPES_CAPABILITY);
		}
	    }
	    if (types_cap == null)  {
		// not yet found, look for LINK to types capability
		if (types_cap_link != null)  {
		    // try to find types_cap in global client uri2cap map?
		    // doesn't exist yet
		    
		    //
		    types_cap = new Das2TypesCapability(xml_base, types_cap_link.getHref(), null, null);
		}
	    }
	    if (types_cap != null)  {
		Das2TypesResponseI types_response = types_cap.getTypes();
		if (types_response != null)  {
		    type = types_response.getType(type_uri);
		}
	    }

	    if (type == null)  {  // couldn't find type previous local use or externally, so create new one
		type = new Das2Type(xml_base, type_local_uri, type_local_uri,
				    null, null, Das2Constants.ROOT_TYPE_ONTOLOGY_TERM, null, true);
	    }

	    // add to localuri2type map to avoid additional URI resolving if encountered again in doc
	    localuri2type.put(type_local_uri, type);
	}
	if (DEBUG)  { System.out.println("  type: " + type); }
	return type;
    }


    public static void main(String[] args) throws XMLStreamException, FileNotFoundException, URISyntaxException  {
	String test_file = "./data/netaffx_das2_features.xml";
	FileInputStream fis = new FileInputStream(new File(test_file));
	Das2FeaturesResponseI feat_response = readFeaturesDocument(fis, new URI("file:" + test_file));
	FeaturesXmlWriter writer = new FeaturesXmlWriter(feat_response, System.out);
	writer.writeFeaturesDocument();
    }




}