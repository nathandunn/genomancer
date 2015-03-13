package genomancer.vine.das2.client.modelimpl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import genomancer.trellis.das2.model.Das2SourceI;
import genomancer.trellis.das2.model.Das2VersionI;

public class Das2Source extends Das2CommonData implements Das2SourceI {

    List<Das2VersionI> versions = new ArrayList<Das2VersionI>();
    String maintainer_email;
    
    public Das2Source(URI absolute_uri, String title, String description, String info_url)  {
	this(absolute_uri, absolute_uri.toString(), title, description, info_url);
    }

    public Das2Source(URI base_uri, 
		      String local_uri_string, 
		      String title, 
		      String description, 
		      String info_url)  { 
	super(base_uri, local_uri_string, title, description, info_url);
    }

    /** Das2SourceI implementation */
    public List<Das2VersionI> getVersions() { return versions; }

    /** Das2SourceI implementation */
    public String getMaintainerEmail() { return maintainer_email; }

    public void addVersion(Das2VersionI version)  { versions.add(version); }

    public void setMaintainerEmail(String email)  { maintainer_email = email; }

 
}