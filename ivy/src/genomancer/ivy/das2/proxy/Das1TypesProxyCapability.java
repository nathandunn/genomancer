package genomancer.ivy.das2.proxy;

import genomancer.ivy.das.model.Das1TypeI;
import genomancer.ivy.das.model.Das1TypesCapabilityI;
import genomancer.ivy.das.model.Das1TypesResponseI;
import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2TypesCapabilityI;
import genomancer.trellis.das2.model.Das2TypesResponseI;
import genomancer.trellis.das2.server.GenericProxyCapability;
import genomancer.vine.das2.client.modelimpl.Das2Type;
import genomancer.vine.das2.client.modelimpl.Das2TypesResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Das1TypesProxyCapability extends GenericProxyCapability implements Das2TypesCapabilityI  {
    boolean initialized = false;
    Das1TypesCapabilityI das1_types_cap;
    Das2TypesResponseI das2_types_response;

    Map<Das1TypeI, Das2TypeI> das1_to_das2_type = new HashMap<Das1TypeI, Das2TypeI>();
    Map<Das2TypeI, Das1TypeI> das2_to_das1_type = new HashMap<Das2TypeI, Das1TypeI>();
    Map<String, Das1TypeI> id_to_das1_type = new HashMap<String, Das1TypeI>();
    Map<URI, Das2TypeI> uri_to_das2_type = new HashMap<URI, Das2TypeI>();

    public Das1TypesProxyCapability(URI base_uri, 
				String query_uri, 
				Das1TypesCapabilityI das1_types_cap)  {
	super(base_uri, query_uri, "types", null, null, das1_types_cap);
	this.das1_types_cap = das1_types_cap;
    }

    public Das2TypesResponseI getTypes() {
	if (! initialized)  { initTypes(); }
	return das2_types_response;
    }

    protected boolean initTypes()  {
	Das1TypesResponseI das1_types_response = das1_types_cap.getTypes();
	List<Das1TypeI> das1_types = das1_types_response.getTypes();
	ArrayList<Das2TypeI> das2_types = new ArrayList<Das2TypeI>();
	ArrayList<Das2LinkI> das2_links = null;
	for (Das1TypeI das1_type : das1_types)  {
	    Das2TypeI das2_type = createDas2Type(das1_type);
	    das1_to_das2_type.put(das1_type, das2_type);
	    das2_to_das1_type.put(das2_type, das1_type);
	    id_to_das1_type.put(das1_type.getID(), das1_type);
	    uri_to_das2_type.put(das2_type.getAbsoluteURI(), das2_type);
	    das2_types.add(das2_type);
	}
	das2_types_response = new Das2TypesResponse(base_uri, das2_types, das2_links);
	initialized = true;
	return true;
    }

    protected Das2TypeI createDas2Type(Das1TypeI das1_type)  {
	return createDas2Type(base_uri, das1_type);
    }

    public static Das2TypeI createDas2Type(URI base_uri, Das1TypeI das1_type)  {
	String das1_id = das1_type.getID();
	//	String local_uri = URLEncoder.encode(das1_id);
      // replacing whitespace, not allowed in URIs
      // should probably check for other unallowed chars
      //  could just use URLEncoder, but that will also convert chars that want
      //    to preserve, such as "/"
      String das2_local_uri = das1_id.replaceAll("\\s", "_");
      System.out.println("das2_local_uri: " + das2_local_uri);
	String title = das1_type.getLabel();
	if (title == null)  { title = das1_id; }
	String ontology_term = null;
	// if DAS1.53E, id _should_ be an ontology term URI -- 
	//    see http://www.dasregistry.org/extension_ontology.jsp for details of ontologies, 
	//   as of 11/15/2008 the possiblities ontology schemes are "SO", "BS", and "MOD"
	if (das1_id.startsWith("SO:") ||
	    das1_id.startsWith("BS:") ||
	    das1_id.startsWith("MOD:"))  {
	    ontology_term = das1_id;
	}
	Das2TypeI das2_type = new Das2Type(base_uri, // base_uri
					   das2_local_uri,  // local_uri,
					   title,     // title, 
					   null,   // description
					   null,   // info_url
					   ontology_term,   // ontology_term --> defaults to ontology root term
					   das1_type.getCategory(), // method
					   true );  // is_searchable
	return das2_type;
    }

    public Das2FormatI getFormat(String format_name)  {
	if (! initialized)  { initTypes(); }
	//	return das1_types_cap.getFormat(format_name);
	return null;
    }


    /**
     *   for now try mapping full type URI, 
     *     then just last path segment, 
     *       if neither then create new type?
     */
    //    public String transformTypeID(URI typeuri) {
    //	return typeuri.toString();
    //    }

    Das2TypeI getDas2Type(Das1TypeI das1_type) {
	if (! initialized)  { initTypes(); }
	return das1_to_das2_type.get(das1_type);
    }

    Das1TypeI getDas1Type(Das2TypeI das2_type)  {
	if (! initialized)  { initTypes(); }
	return das2_to_das1_type.get(das2_type);
    }

    Das2TypeI getDas2Type(URI typeuri) {
	if (! initialized)  { initTypes(); }
	return uri_to_das2_type.get(typeuri);
    }

    Das1TypeI getDas1Type(String id)  {
	if (! initialized)  { initTypes(); }
	return id_to_das1_type.get(id);
    }

    public Das2TypeI getType(URI type_uri) {
        return getDas2Type(type_uri);
    }


}
