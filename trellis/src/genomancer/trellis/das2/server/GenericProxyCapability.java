package genomancer.trellis.das2.server;

import genomancer.trellis.das2.model.Das2CapabilityI;
import genomancer.trellis.das2.model.Das2CoordinatesI;
import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2VersionI;
import genomancer.vine.das2.client.modelimpl.Identifiable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.jdom.Element;

/**
 *    a generic identity proxy for Das2CapabilityI capabilities -- 
 *        everything is pass-through, no modifications
 *        (except proxy is at different URI endpoint (determined by base_uri.resolve(query_uri))
 */
public class GenericProxyCapability extends Identifiable implements Das2CapabilityI  {
    protected Das2CapabilityI remote_cap;
    protected Map<String, String> init_params;
    protected String type;

    public GenericProxyCapability(URI base_uri, String query_uri, String type, 
				  Das2VersionI version, Das2CoordinatesI coordinates, 
				  Das2CapabilityI remote_cap)  { 
	super(base_uri, query_uri);
	this.remote_cap = remote_cap;
      this.type = type;
	//	this.init(params);
    }

    public void init(Map<String, String> params)  {
	init_params = params;
	if (remote_cap != null)  {
          remote_cap.init(init_params);
      }
    }

    public Das2CapabilityI getRemoteCapability()  { return remote_cap; }


    public String getType() {
	//	return remote_cap.getType();
	return type;
    }

    public Das2CoordinatesI getCoordinates() {
	return remote_cap.getCoordinates();
    }

    public Das2VersionI getVersion() {
	return remote_cap.getVersion();
    }

    public List<Das2FormatI> getFormats() {
	return remote_cap.getFormats();
    }

    public List<String> getSupportedExtensions() {
	return remote_cap.getSupportedExtensions();
    }

    public List<Element> getAdditionalData() {
	return remote_cap.getAdditionalData();
    }

    public long getLastModified()  {
        return remote_cap.getLastModified();
    }


    
}