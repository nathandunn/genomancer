package genomancer.vine.das2.client.modelimpl;

import genomancer.trellis.das2.model.Das2FeaturesQueryI;
import genomancer.trellis.das2.model.Status;
import java.util.List;
import java.net.URI;

import genomancer.trellis.das2.model.Das2FeaturesResponseI;
import genomancer.trellis.das2.model.Das2FeatureI;
import genomancer.trellis.das2.model.Das2LinkI;

public class Das2FeaturesResponse implements Das2FeaturesResponseI  {

    URI base_uri;
    List<Das2FeatureI> features;
    List<Das2LinkI> links;
    boolean types_share_base_uri;
    boolean segments_share_base_uri;
    Status status;

    public Das2FeaturesResponse(URI base_uri, 
				List<Das2FeatureI> features,
				List<Das2LinkI> links )  {
	this(base_uri, features, links, false, false);
    }

    public Das2FeaturesResponse(URI base_uri, 
				List<Das2FeatureI> features,
				List<Das2LinkI> links, 
				boolean types_share_base_uri, 
				boolean segments_share_base_uri)  {
        this(base_uri, features, links, 
            types_share_base_uri, segments_share_base_uri, Status.OK);
    }
        
    public Das2FeaturesResponse(URI base_uri,
				List<Das2FeatureI> features,
				List<Das2LinkI> links, 
				boolean types_share_base_uri, 
				boolean segments_share_base_uri,
                        Status status)  {

	this.base_uri = base_uri;
	this.features = features;
	this.links = links;
	this.types_share_base_uri = types_share_base_uri;
	this.segments_share_base_uri = segments_share_base_uri;
      this.status = status;
    }
    
    public Das2FeaturesResponse(Status status)  {
        this(null, null, null, false, false, status);
    }

    public List<Das2FeatureI> getFeatures() {
	return features;
    }

    public URI getBaseURI() {
	return base_uri;
    }

    public List<Das2LinkI> getLinks() {
	return links;
    }

    public Das2FeaturesQueryI getQuery() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean segmentsShareBaseURI() {
        return segments_share_base_uri;
    }

    public boolean typesShareBaseURI() {
        return types_share_base_uri;
    }

    public Status getStatus() {
        return status;
    }

}