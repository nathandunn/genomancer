package genomancer.trellis.das2.server;

import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2TypesResponseI;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypesResponseWrapper implements Das2TypesResponseI  {
    
    Das2TypesResponseI oresponse;
    List<Das2TypeI> types;
    Map<URI, TypeWrapper> uri2type;
     

    public TypesResponseWrapper(Das2TypesResponseI original_response)  {
	this.oresponse = original_response;
	List<Das2TypeI> otypes = oresponse.getTypes();
	int numtypes = otypes.size();
	types = new ArrayList(numtypes);
	uri2type = new HashMap<URI, TypeWrapper>(numtypes);
	for (Das2TypeI type : otypes)  {
	    TypeWrapper newtype = new TypeWrapper(type);
	    types.add(newtype);
	    uri2type.put(type.getAbsoluteURI(), newtype);
	}
    }

    public List<Das2TypeI> getTypes() { return types; }

    public Das2TypeI getType(URI type_uri) { return uri2type.get(type_uri); }

    public List<Das2LinkI> getLinks() { return oresponse.getLinks(); }

    public URI getBaseURI() { return oresponse.getBaseURI(); }
}