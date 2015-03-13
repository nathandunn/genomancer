package genomancer.vine.das2.client.modelimpl;

import genomancer.trellis.das2.model.Das2LinkI;

import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2TypesResponseI;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Das2TypesResponse implements Das2TypesResponseI  {
    protected URI base_uri;
    protected List<Das2TypeI> types;
    protected List<Das2LinkI> links;
    protected Map<URI, Das2TypeI> uri2type;

    public Das2TypesResponse(URI base_uri, 
			     List<Das2TypeI> types,
			     List<Das2LinkI> links)  {
	this.base_uri = base_uri;
	this.types = types;
	this.links = links;
	uri2type = new HashMap<URI, Das2TypeI>(types.size());
	for (Das2TypeI type : types)  {
	    uri2type.put(type.getAbsoluteURI(), type);
	    uri2type.put(type.getLocalURI(), type);
	}
    }

    public List<Das2TypeI> getTypes() {
	return types;
    }

    public Das2TypeI getType(URI type_uri)  {
	return uri2type.get(type_uri);
    }

    public List<Das2LinkI> getLinks() {
        return links;
    }

    public URI getBaseURI() {
        return base_uri;
    }

    //  public void getQuery() {
    //        throw new UnsupportedOperationException("Not supported yet.");
    //    }

}