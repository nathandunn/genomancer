package genomancer.vine.das2.client.modelimpl;

import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2SourceI;
import genomancer.trellis.das2.model.Das2SourcesResponseI;
import java.net.URI;
import java.util.List;


public class Das2SourcesResponse implements Das2SourcesResponseI  {

    List<Das2SourceI> sources;
    URI base_uri;
    String maintainer_email;
    List<Das2LinkI> links;
    //    Das2SourcesQueryI sources_query;

    public Das2SourcesResponse(URI base_uri,
			       List<Das2SourceI> sources, 
			       String maintainer_email,
			       List<Das2LinkI> links)  {
	this.base_uri = base_uri;
	this.sources = sources;
	this.maintainer_email = maintainer_email;
	this.links = links;
    }

    public List<Das2SourceI> getSources() { return sources; }

    public URI getBaseURI() { return base_uri; }

    public List<Das2LinkI> getLinks()  { return links; }

    public String getMaintainerEmail()  { return maintainer_email; }


    //  public Das2SourcesQueryI getQuery() { return sources_query; }

}