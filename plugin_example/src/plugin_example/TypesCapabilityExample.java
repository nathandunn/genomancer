package plugin_example;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import genomancer.trellis.das2.model.Das2CoordinatesI;
import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2TypesCapabilityI;
import genomancer.trellis.das2.model.Das2TypesResponseI;
import genomancer.trellis.das2.model.Das2VersionI;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2Type;
import genomancer.vine.das2.client.modelimpl.Das2TypesResponse;

public class TypesCapabilityExample extends Das2GenericCapability implements Das2TypesCapabilityI  {
    Das2TypesResponse response = null;
    
    public TypesCapabilityExample(Das2VersionI version, Das2CoordinatesI coords)  {
        super(version.getBaseURI(), (version.getLocalURIString()+"/types"), "types", version, coords);
	
	List<Das2TypeI> types = new ArrayList<Das2TypeI>();
	URI type_base_uri = this.getAbsoluteURI().resolve("./");
	Das2Type type1 = new Das2Type(type_base_uri,  // base_uri
				      "horse_bits",   // local_uri
 				      "Horse Bits",   // title
				      null,           // description
				      null,           // info_url
				      "unknown",      // ontology_term_name
				      null,           // method
				      true);          // is_searchable

	Das2Type type2 = new Das2Type(type_base_uri,  // base_uri
				      "narwhal_bits",   // local_uri
 				      "Narwhal Bits",   // title
				      null,           // description
				      null,           // info_url
				      "unknown",      // ontology_term_name
				      null,           // method
				      true);          // is_searchable

	types.add(type1);
	types.add(type2);
	response = new Das2TypesResponse(type_base_uri, types, null);
    }

    public Das2TypesResponseI getTypes() {
	return response;
    }

    public Das2TypeI getType(URI type_uri) {
	return response.getType(type_uri);
    }


    public Das2FormatI getFormat(String format_name)  {
        throw new UnsupportedOperationException("Not supported yet.");
    }


}
