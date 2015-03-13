package genomancer.ivy.das.client.xml;

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

import genomancer.trellis.das2.model.Das2VersionI;

import genomancer.ivy.das.Das1Constants;
import genomancer.ivy.das.model.Das1FeaturesCapabilityI;
import genomancer.ivy.das.model.Das1FeaturesResponseI;
import genomancer.ivy.das.model.Das1LocationI;
import genomancer.ivy.das.model.Das1SegmentI;
import genomancer.ivy.das.model.Das1EntryPointsCapabilityI;
import genomancer.ivy.das.model.Das1EntryPointsResponseI;
import genomancer.ivy.das.model.Das1TypeI;
import genomancer.ivy.das.model.Das1TypesCapabilityI;
import genomancer.ivy.das.model.Das1TypesResponseI;

import genomancer.ivy.das.client.modelimpl.Das1Feature;
import genomancer.ivy.das.client.modelimpl.Das1FeaturesResponse;
import genomancer.ivy.das.client.modelimpl.Das1Group;
import genomancer.ivy.das.client.modelimpl.Das1Link;
import genomancer.ivy.das.client.modelimpl.Das1Location;
// import genomancer.ivy.das.client.modelimpl.Das1MutableSegment;

import genomancer.ivy.das.client.modelimpl.Das1Method;
import genomancer.ivy.das.client.modelimpl.Das1Segment;
import genomancer.ivy.das.client.modelimpl.Das1Target;
import genomancer.ivy.das.client.modelimpl.Das1Type;
import genomancer.ivy.das.client.modelimpl.Das1TypesCapability;

// import genomancer.ivy.das.xml.Das1FeaturesXmlWriter;
import genomancer.ivy.das.model.Das1FeatureI;
import genomancer.ivy.das.model.Das1Phase;
import genomancer.ivy.das.model.Das1LinkI;
import genomancer.ivy.das.model.Das1GroupI;
import genomancer.ivy.das.model.Das1MethodI;
import genomancer.ivy.das.model.Das1TargetI;
import genomancer.trellis.das2.model.Strand;

public class Das1FeaturesXmlReader extends AbstractDas1XmlReader {

    static final Pattern url_query_splitter = Pattern.compile("[;\\&]");
    static final Pattern equals_splitter = Pattern.compile("=");
    static final Pattern range_splitter = Pattern.compile(":");
    static boolean DEBUG = false;

    Map<String, Das1FeatureI> id2feature = new LinkedHashMap<String, Das1FeatureI>(); // preserving feature order
    Map<String, Das1SegmentI> id2segment = new HashMap<String, Das1SegmentI>();
    Map<String, Das1TypeI> id2type = new HashMap<String, Das1TypeI>();
    Map<String, Das1Group> id2group = new LinkedHashMap<String, Das1Group>();
    Map<String, Das1LinkI> url2link = new LinkedHashMap<String, Das1LinkI>();

    Das1SegmentI current_segment = null;
    
    /**
     *   features capability passed in constructor
     */
    Das1FeaturesCapabilityI feat_cap;
   
    /**
     *  doc_uri SHOULD be an absolute URI
     *  need the doc_uri for determining XmlBase if no "xml:base" attribute, or if "xml:base" is relative
     *  could leave this out if:
     *     a) "xml:base" required
     *     b) "query" required attribute added to FEATURES whose value is original query URL
     */ 
    public Das1FeaturesXmlReader(InputStream istream, URI doc_uri, 
				 Das1FeaturesCapabilityI feat_cap) throws XMLStreamException  {
	super(istream, doc_uri);  // sets xml_base = doc_uri, creates xreader based on istream
	this.feat_cap = feat_cap;
	DEBUG = false;
    }

    public static Das1FeaturesResponseI readFeaturesDocument(InputStream istream, URI doc_uri, 
							     Das1FeaturesCapabilityI feat_cap) 
	throws XMLStreamException, URISyntaxException    {
	Das1FeaturesXmlReader features_reader = new Das1FeaturesXmlReader(istream, doc_uri, feat_cap);
	Das1FeaturesResponseI response = features_reader.readFeaturesDocument();
	features_reader.close();
	return response;
    }

    public static Das1FeaturesResponseI readFeaturesDocument(InputStream istream, URI doc_uri) 
	throws XMLStreamException, URISyntaxException    {
	return readFeaturesDocument(istream, doc_uri, null);
    }


    public Das1FeaturesResponseI readFeaturesDocument() throws XMLStreamException, URISyntaxException  {

	List<Das1FeatureI> features = new ArrayList<Das1FeatureI>();
	String version = null;
	String href = null;
       
	System.out.println("XMLInputFactory: " + ifactory);
	System.out.println("XMLStreamReader: " + xreader);
	System.out.println("initial xml_base: " + xml_base);

	frag_builder = new XmlFragmentBuilder();

	while (xreader.hasNext())  {
	    int eventid = xreader.next();
	    if (eventid == XMLStreamConstants.START_ELEMENT)  {
		String elname = xreader.getLocalName();
		if (elname.equals("FEATURE"))  {
		    Das1FeatureI feature = parseFeatureElement();
		    if (DEBUG)  { System.out.println(feature); }
		    features.add(feature);
                
		}
		else if (elname.equals("SEGMENT"))  {
		    current_segment = getSegment();
		}
		else if (elname.equals("GFF"))  {
		    version = xreader.getAttributeValue(ns, "version");
		    href = xreader.getAttributeValue(ns, "href");
		}
	    }
	    else if (eventid == XMLStreamConstants.START_DOCUMENT)  {
		
	    }	
	    else if (eventid == XMLStreamConstants.END_DOCUMENT)  {

          }
	}
	System.out.println("feature count: " + features.size());
	Das1FeaturesResponseI response = new Das1FeaturesResponse(href, version, features);
	return response;
    }

    protected Das1SegmentI getSegment()  {
	String id = xreader.getAttributeValue(ns, "id");
	int start = Integer.parseInt(xreader.getAttributeValue(ns, "start"));
	int stop = Integer.parseInt(xreader.getAttributeValue(ns, "stop"));
	String type = xreader.getAttributeValue(ns, "type");

	String version = xreader.getAttributeValue(ns, "version");
	String label = xreader.getAttributeValue(ns, "label");

	Das1SegmentI segment = new Das1Segment(id, start, stop, type, version, label);
	Das1SegmentI full_segment = getFullSegment(segment);

	return full_segment;
    }


    protected Das1SegmentI getFullSegment(Das1SegmentI segment)  {  
	// NOT YET IMPLEMENTED: map partial segment to a full sequence segment if possible
	// retrieve segments from MAPMASTER?
	return segment;
    }


    public Das1FeatureI parseFeatureElement() throws XMLStreamException {
        Das1FeatureI feature = null;
        
	String id = xreader.getAttributeValue(ns, "id");
	String label = xreader.getAttributeValue(ns, "label");
	Das1TypeI type = null;
	Das1SegmentI segment = null;
	int start = Das1FeatureI.NO_START;
	int end = Das1FeatureI.NO_END;
	Strand orientation = Strand.NOT_APPLICABLE;
	double score = Das1FeatureI.NO_SCORE;
	Das1Phase phase = Das1Phase.NOT_APPLICABLE;
	Das1MethodI method = null;
	List<String> notes = null;
	List<Das1LinkI> links = null;
	List<Das1TargetI> targets = null;
	List<Das1GroupI> groups = null;

	if (DEBUG)  { System.out.println("FEATURE: " + id); }	
	// Das1TypeI dtype = getType(type_uri);


	while (xreader.hasNext())  {
	    int eventid = xreader.next();
	    if (eventid == XMLStreamConstants.START_ELEMENT)  {
		String elname = xreader.getLocalName();
		if (elname.equals("TYPE"))  { 
		    type = getType();
		}
		else if (elname.equals("METHOD"))  {
		    String methid = xreader.getAttributeValue(ns, "id");
		    String methlabel = xreader.getElementText().trim();
		    method = new Das1Method(methid, methlabel);
		}
		else if (elname.equals("START"))  {
		    try {
			start = Integer.parseInt(xreader.getElementText().trim());
		    }
		    catch (Exception ex)  {  // support for GeneDAS extension -- no start or start irrelevant
			start = Das1FeatureI.NO_START;
		    }
		}
		else if (elname.equals("END"))  {
		    try {
			end = Integer.parseInt(xreader.getElementText().trim());
		    }
		    catch (Exception ex)  {  // support for GeneDAS extension -- no end or end irrelevant
			end = Das1FeatureI.NO_END;
		    }
		}
		else if (elname.equals("SCORE"))  {
		    String scorestr = xreader.getElementText().trim();
		    if ((scorestr == null) || (scorestr.equals("-")) || (scorestr.length() == 0) )  {  
			score = Das1FeatureI.NO_SCORE;
		    }
		    else {
			try  {
			    score = Double.parseDouble(scorestr);
			}
			catch(NumberFormatException ex)  {
			    score = Das1FeatureI.NO_SCORE;
			}
		    }
		}
		else if (elname.equals("ORIENTATION"))  {
		    String orientstr = xreader.getElementText().trim();
		    if (orientstr.equals("0"))  { orientation = Strand.NOT_APPLICABLE; }
		    else if (orientstr.equals("+"))  { orientation = Strand.FORWARD; }
		    else if (orientstr.equals("-"))  { orientation = Strand.REVERSE; }
		    else  { orientation = Strand.UNKNOWN; }
		}
		else if (elname.equals("PHASE"))  {
		    String phasestr = xreader.getElementText().trim();
		    phase = Das1Phase.getPhase(phasestr);
		    if (phase == null)  { phase = Das1Phase.NOT_APPLICABLE; }
		}
		else if (elname.equals("NOTE"))  {
		    if (notes == null)  { notes = new ArrayList<String>(); }
		    notes.add(xreader.getElementText().trim());
		}
		else if (elname.equals("LINK"))  {
		    if (links == null)  { links = new ArrayList<Das1LinkI>(); }
		    Das1LinkI link = getLink();
		    links.add(link);
		}
		else if (elname.equals("TARGET"))  {
		    if (targets == null)  { targets = new ArrayList<Das1TargetI>(); }
		    Das1TargetI target = parseTarget();
		    targets.add(target);
		}
		else if (elname.equals("GROUP"))  {
		    if (groups == null)  { groups = new ArrayList<Das1GroupI>(); }
		    Das1Group group = getGroup();
		    groups.add(group);
		}
		else  {
		    // additional XML fragments
		    // org.jdom.Element xml_fragment = frag_builder.buildXmlFragment(xreader);
		    //		    feature.addAdditionalData(xml_fragment);
		}
	    }
	    else if (eventid == XMLStreamConstants.END_ELEMENT && 
		     xreader.getLocalName().equals("FEATURE"))  {
		feature = new Das1Feature(id, label, type, method,  
					  current_segment, start, end, score, orientation, 
					  phase, notes, links, targets, groups);
		if (groups != null)  {
		    for (Das1GroupI group : groups)  {
			((Das1Group)group).addFeature(feature);
		    }
		}
		id2feature.put(id, feature);
		break;
	    }
	}
	return feature;
    }


    protected Das1LinkI getLink() throws XMLStreamException  {
	String linkref = xreader.getAttributeValue(ns, "href");
	Das1LinkI link = url2link.get(linkref);
	if (link == null)  {
	    String linklabel = xreader.getElementText().trim();
	    link = new Das1Link(linkref, linklabel);
	    url2link.put(linkref, link);
	}
	return link;
    }

    protected Das1TargetI parseTarget() throws XMLStreamException  {
	String id = xreader.getAttributeValue(ns, "id");
	int start = Integer.parseInt(xreader.getAttributeValue(ns, "start"));
	int stop = Integer.parseInt(xreader.getAttributeValue(ns, "stop"));
	String name = xreader.getElementText();
	if (name != null)  { name = name.trim(); }
	Das1TargetI target = new Das1Target(id, name, start, stop);
	return target;
    }


    /** 
     *  if multiple GROUP elements with same id are encountered, 
     *      assumes that any TARGET, LINK, NOTE elements appear in first GROUP element
     *      ignores TARGET, LINK, NOTE elements in subsequent occurences of GROUP with same id
     */
    protected Das1Group getGroup() throws XMLStreamException  {
	Das1Group group = null;
	String id = xreader.getAttributeValue(ns, "id");
	group = id2group.get(id);
	if (group == null)  {
	    List<String> notes = null;
	    List<Das1TargetI> targets = null;
	    List<Das1LinkI> links = null;
	    String label = xreader.getAttributeValue(ns, "label");
	    String typeid = xreader.getAttributeValue(ns, "type");
	    Das1TypeI type = null;
	    if (typeid != null)  {
		type = id2type.get(typeid);
		if (type == null)  {
		    type = new Das1Type(typeid, typeid, null, null);
		    id2type.put(typeid, type);
		}
	    }
	    while (xreader.hasNext())  {
		int eventid = xreader.next();
		if (eventid == XMLStreamConstants.START_ELEMENT)  {
		    String elname = xreader.getLocalName();
		    if (elname.equals("TARGET"))  {
			if (targets == null)  { targets = new ArrayList<Das1TargetI>(); }
			targets.add(parseTarget());
		    }
		    else if (elname.equals("LINK"))  {
			if (links == null)  { links = new ArrayList<Das1LinkI>(); }
			links.add(getLink());
		    }
		    else if (elname.equals("NOTE"))  {
			if (notes == null)  { notes = new ArrayList<String>(); }
			notes.add(xreader.getElementText().trim());
		    }
		}
		else if (eventid == XMLStreamConstants.END_ELEMENT && 
			 xreader.getLocalName().equals("GROUP"))  {
		    // WARNING: type may be null
		    group = new Das1Group(id, label, type, targets, links, notes);
		    id2group.put(id, group);
		    break;
		}
	    }
	}
	else  {
	    scanToEndElement("GROUP");
	}
	return group;
    }

    protected boolean scanToEndElement(String elname) throws XMLStreamException  {
	boolean success = false;
	if ( (xreader.getEventType() == XMLStreamConstants.END_ELEMENT) &&
	     (xreader.getLocalName().equals(elname) ) )  {
	    success = true;
	}
	else  {
	    while (xreader.hasNext())  {
		int eventid = xreader.next();
		// System.out.println(xreader.getLocalName());
		if ((eventid == XMLStreamConstants.END_ELEMENT) && 
		    (xreader.getLocalName().equals(elname) ) )  {
		    success = true;
		    break;
		}
	    }
	}
	return success;
    }

 
    protected Das1TypeI getType() throws XMLStreamException  {
        String id = xreader.getAttributeValue(ns, "id");

	// System.out.println("type text label: " + label);
        
	Das1TypeI type = id2type.get(id); // if type uri seen before in doc, use same type
	if (type == null)  {  

	    // try to find externally
	    Das1TypesCapabilityI types_cap = null;
	    if (feat_cap != null)  {
		Das2VersionI version = (Das2VersionI) feat_cap.getVersion();
		if (version != null)  {
		    types_cap = (Das1TypesCapabilityI) version.getCapability(Das1Constants.DAS1_TYPES_CAPABILITY);
		}
	    } 
	    if (types_cap != null)  {
		Das1TypesResponseI types_response = types_cap.getTypes();
		if (types_response != null)  {
		    type = types_response.getType(id);
		}
	    }

	    if (type == null)  {  // couldn't find type previous local use or externally, so create new one
		String category = xreader.getAttributeValue(ns, "category");
		String label = xreader.getElementText().trim();
		type = new Das1Type(id, label, category, null); 
		System.out.println("new type: " + id + ", " + type);
		// add type to id2type map to avoid additional searching if encountered again in doc
		id2type.put(id, type);
	    }
	}
	else  {
	    scanToEndElement("TYPE");
	}
	if (DEBUG)  { System.out.println("  type: " + type); }
	return type;
    }

    public static void main(String[] args) throws XMLStreamException, FileNotFoundException, URISyntaxException  {
	// String test_file = "./data/das1_features_ucsc_genscan.slice.xml";
	String test_file = "./data/das1_features_ensembl.slice.xml";
	FileInputStream fis = new FileInputStream(new File(test_file));
	Das1FeaturesResponseI feat_response = readFeaturesDocument(fis, new URI("file:" + test_file));
	//	Das1FeaturesXmlWriter writer = new Das1FeaturesXmlWriter(feat_response, System.out);
	//	writer.writeFeaturesDocument();
    }


   /* protected Das1SegmentI getSegment(String seg_local_uri) {
	Das1SegmentI segment = localuri2segment.get(seg_local_uri);
	if (segment == null)  {
	    // try to find externally
	    URI segment_uri = xml_base.resolve(seg_local_uri);

	    // try to find segment in global client uri2segment map?
	    // global client uri2segments doesn't exist yet...

	    // try to backtrack to version then back to segments capability
	    //   Das1FeatureCapability --> Das1Version --> Das1SegmentsCapability

	    if (segment == null)  {  // couldn't find segment previous local use or externally, so create new one
		segment = new Das1MutableSegment(xml_base, seg_local_uri,
						 seg_local_uri, null, 0, null);
	    }

	    // add to localuri2segment map to avoid additional URI resolving if encountered again in doc
	    localuri2segment.put(seg_local_uri, segment);
	}
	if (DEBUG)  { System.out.println("  segment: " + segment); }
	return segment;
    }
    */

}