package genomancer.vine.das2.client.modelimpl;

import java.net.URI;
import java.util.Date;
import genomancer.trellis.das2.model.Das2CoordinatesI;

public class Das2Coordinates extends Identifiable implements Das2CoordinatesI  {
    String taxonomy_id;
    String coordinate_type;
    String authority;
    String build_version;
    Date created;
    String test_range;

    public Das2Coordinates(URI base_uri, 
			   String local_uri_string, 
			   String taxonomy_id, 
			   String coordinate_type,
			   String authority, 
			   String build_version, 
			   Date created, 
			   String test_range)  {
	super(base_uri, local_uri_string);
	this.taxonomy_id = taxonomy_id;
	this.coordinate_type = coordinate_type;
	this.authority = authority;
	this.build_version = build_version;
	// need to figure out date parsing
	//	this.created = new Date(created);
	this.test_range = test_range;
    }

    public String getTaxonomyID() { return taxonomy_id; }
    public String getCoordinateType() { return coordinate_type; }
    public String getAuthority() { return authority; }
    public String getBuildVersion() { return build_version; }
    public Date getCreated() { return created; }
    public String getTestRange() { return test_range; }
  
}