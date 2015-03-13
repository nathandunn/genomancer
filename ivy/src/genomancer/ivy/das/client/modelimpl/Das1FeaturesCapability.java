package genomancer.ivy.das.client.modelimpl;

import genomancer.ivy.das.client.xml.Das1FeaturesXmlReader;
import genomancer.ivy.das.model.Das1FeatureI;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;

import genomancer.ivy.das.model.Das1FeaturesQueryI;
import genomancer.ivy.das.model.Das1FeaturesResponseI;
import genomancer.ivy.das.model.Das1FeaturesCapabilityI;
import genomancer.ivy.das.model.Das1LocationRefI;
import genomancer.trellis.das2.model.Strand;
import genomancer.trellis.das2.server.ServerUtils;
import genomancer.vine.das2.client.modelimpl.Das2Coordinates;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2Version;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

public class Das1FeaturesCapability extends Das2GenericCapability implements Das1FeaturesCapabilityI  {

    public Das1FeaturesCapability(URI base_uri, 
			       String query_uri, 
			       Das2Version version, 
			       Das2Coordinates coordinates)  {
        super(base_uri, query_uri, "das1:features", version, coordinates);
    }

    public Das1FeaturesResponseI getFeatures()  {
	return getFeatures(new Das1FeaturesQuery());
    }

    /**
     *   getFeatures() is same as getFeatures(query), just with empty query
     */
    public Das1FeaturesResponseI getFeatures(Das1FeaturesQueryI query) {
	String query_string = getQueryString(query);
	Das1FeaturesResponseI features = null;
	InputStream istr = null;
	boolean success = false;
	try {
	    String features_query_string;
	    if (query_string == null || query_string.length() == 0)  {
		features_query_string = this.getAbsoluteURI().toString();
	    }
	    else  {
		features_query_string = this.getAbsoluteURI().toString() + "?" + query_string;
	    }

	    System.out.println("FEATURES_QUERY: " + features_query_string);
	    System.out.println("   URL-decoded: " + URLDecoder.decode(features_query_string));
	    URL features_query = new URL(features_query_string);
	    URLConnection conn = features_query.openConnection();
	    // check HTTP status header, etc. here
	    istr = conn.getInputStream();
	    features = Das1FeaturesXmlReader.readFeaturesDocument(istr, this.getAbsoluteURI());
	    success = true;
	} catch (IOException ex) {
	    Logger.getLogger(Das1FeaturesCapability.class.getName()).log(Level.SEVERE, null, ex);
	} catch (XMLStreamException ex) {
	    Logger.getLogger(Das1FeaturesCapability.class.getName()).log(Level.SEVERE, null, ex);
	} catch (URISyntaxException ex) {
	    Logger.getLogger(Das1FeaturesCapability.class.getName()).log(Level.SEVERE, null, ex);
	} finally {
	    try {
		istr.close();
	    } catch (IOException ex) {
		success = false;
		Logger.getLogger(Das1FeaturesCapability.class.getName()).log(Level.SEVERE, null, ex);
	    }
	}
	if (! success)  {
	    // report error somehow
	    //   maybe replace features with a Das1FeaturesResponse singleton that represents errors?
	}
	return features;
    }


    /**
     *  Note that returned range string is NOT URL-encoded, if needed apply URLEncoder:
     *      URLEncoder.encode( getRangeQueryString(loc) )
     */
    public static String getRangeQueryString(Das1LocationRefI loc)  {
	StringBuffer buf = new StringBuffer();
	buf.append(loc.getSegmentID());
	if (! loc.coversEntireSegment())  {
	    buf.append(":");
	    buf.append(Integer.toString(loc.getMin()));
	    buf.append(",");
	    buf.append(Integer.toString(loc.getMax()));
	}
	else  {
	    System.out.println("Das1LocationRefI covers entire segment: " + loc.getSegmentID());
	}
	return buf.toString();
    }

    //    public static String getEncodedRangeQueryString(Das1LocationRefI loc)  {
    //	return URLEncoder.encode( getRangeQueryString(loc) );
    //    }

    /**
     *  URL-encodes both the parameter name and the parameter value
     *  does not include the "?" delimiter that indicates start of URL query section
     */
    public class QueryStringBuilder  {
	String delimiter = ";";
	int param_count = 0;
	StringBuffer qbuf = new StringBuffer();
	
	void addParameter(String param_name, String param_val)  {
	    System.out.println("QueryStringBuilder.addParameter(), " + 
			       "name = " + param_name + ", val = " + param_val);
	    if (param_count > 0)  { qbuf.append(delimiter); }
	    qbuf.append(URLEncoder.encode(param_name));
	    qbuf.append("=");
	    qbuf.append(URLEncoder.encode(param_val));
	    param_count++;
	}
	String getQueryString()  { return qbuf.toString(); }
    }

    /**
     *  Given a Das1FeaturesQueryI, 
     *  construct the corresponding query string for making a features request to a DAS/2 server
     *
     *  all param names and values are URL-encoded
     *  currently all values that are URIs are absolute
     *
     */
    public String getQueryString(Das1FeaturesQueryI query)  {
	if (query == null)  { return null; }
	QueryStringBuilder qsb = new QueryStringBuilder();
	List<Das1LocationRefI> locations = query.getLocations();
	List<String> types = query.getTypes();
	List<String> categories = query.getCategories();
	List<String> group_ids = query.getGroupIds();
	List<String> feature_ids = query.getFeatureIds();
	boolean is_categorized = query.isCategorized();
	Map<String, List<String>> non_standard_params = query.getNonStandardParams();

	if (locations != null)  {
	    for (Das1LocationRefI loc : locations)  {
		qsb.addParameter("segment", getRangeQueryString(loc));
	    }
	}
	if (types != null)  {
	    for (String type_id : types)  { qsb.addParameter("type", type_id); } 
	}
	if (categories != null)  {
	    for (String category : categories )  { qsb.addParameter("category", category); }
	}
	if (is_categorized)  { qsb.addParameter("categorize", "yes"); }  // default is "no"
	if (feature_ids != null)  {
	    for (String feature_id : feature_ids)  { qsb.addParameter("feature_id", feature_id); }
	}
	if (group_ids != null)  {
	    for (String group_id : group_ids)  { qsb.addParameter("group_id", group_id); }
	}
    	if (non_standard_params != null)  {
	    for (String param_name : non_standard_params.keySet())  {
		List<String> param_values = non_standard_params.get(param_name);
		for (String param_val : param_values)  {  
		    qsb.addParameter(param_name, param_val);
		}
	    }
	}
	String query_string = qsb.getQueryString();
	return query_string;
    }


    public static void main(String[] args) throws URISyntaxException  {
	String features_url = "file:./data/das1_features_ucsc_genscan.slice.xml";
	Das1FeaturesCapability cap = new Das1FeaturesCapability(new URI(features_url), features_url, null, null);
	//	List<Das2FeatureI> features = cap.getFeatures();
	Das1FeaturesResponseI features_response = cap.getFeatures();
	List<Das1FeatureI> features = features_response.getFeatures();
	System.out.println("got Das1FeaturesResponseI, feature count = " + features.size());
    }


} 
