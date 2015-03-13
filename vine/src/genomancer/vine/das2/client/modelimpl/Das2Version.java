package genomancer.vine.das2.client.modelimpl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;  

import java.util.List;
import genomancer.trellis.das2.model.Das2CapabilityI;
import genomancer.trellis.das2.model.Das2CoordinatesI;
import genomancer.trellis.das2.model.Das2FeaturesCapabilityI;
import genomancer.trellis.das2.model.Das2SegmentsCapabilityI;
import genomancer.trellis.das2.model.Das2TypesCapabilityI;
import genomancer.trellis.das2.model.Das2VersionI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Das2Version extends Das2CommonData implements Das2VersionI {
    Das2Source source;
    Date creation_date;
    Date last_modified_date;
    String maintainer_email;
    List<Das2CapabilityI> capabilities = new ArrayList<Das2CapabilityI>();
    List<Das2CoordinatesI> coordinates = new ArrayList<Das2CoordinatesI>();
    Map<URI, Das2CoordinatesI> uri2coords = new LinkedHashMap<URI, Das2CoordinatesI>();
    Das2FeaturesCapabilityI features_cap;
    Das2TypesCapabilityI types_cap;
    Das2SegmentsCapabilityI segments_cap;
    Map<String, Das2CapabilityI> capmap = new HashMap<String, Das2CapabilityI>();

    public Das2Version(Das2Source source,
		       String local_uri_string, 
		       String title, 
		       String description, 
		       String info_url,
		       Date creation_date, 
		       Date last_modified_date)  {
	super(source.getBaseURI(), local_uri_string, title, description, info_url);
	this.source = source;
	if (creation_date != null)  {
	    this.creation_date = creation_date;
	}
	if (last_modified_date != null)  {
	    this.last_modified_date = last_modified_date;
	}
    }

    public void addCapability(Das2CapabilityI capability)  {
	capabilities.add(capability);
	String type = capability.getType();
	capmap.put(type, capability);

	if (type.equals("features"))  {
	    features_cap = (Das2FeaturesCapabilityI)capability;
	}
	else if (type.equals("types"))  {
	    types_cap = (Das2TypesCapabilityI)capability;
	}
	else if (type.equals("segments"))  {
	    segments_cap = (Das2SegmentsCapabilityI)capability;
	}
    }

    public Das2CapabilityI getCapability(String captype)  {
	return capmap.get(captype);
    }

    public void addCoordinates(Das2CoordinatesI coordinate)  {
	coordinates.add(coordinate);
	uri2coords.put(coordinate.getAbsoluteURI(), coordinate);
    }

    public Date getCreationDate() { return creation_date; }
    public Date getLastModifiedDate() { return last_modified_date; }
    public List<Das2CapabilityI> getCapabilities() { return capabilities; }
    public List<Das2CoordinatesI> getCoordinates() { return coordinates; }

    public Das2CoordinatesI getCoordinates(URI uri)  {
	return uri2coords.get(uri);
    }

    /*
    public Das2FeaturesCapabilityI getDas2FeaturesCapability()  { 
	return features_cap;
    }
    public Das2TypesCapabilityI getDas2TypesCapability()  {
	return types_cap;
    }
    public Das2SegmentsCapabilityI getDas2SegmentsCapability()  {
	return segments_cap;
    }
    */

    public void setMaintainerEmail(String email)  {
	this.maintainer_email = email;
    }

    public String getMaintainerEmail() {
        return maintainer_email;
    }

    public Das2Source getSource()  {
	return source;
    }

}