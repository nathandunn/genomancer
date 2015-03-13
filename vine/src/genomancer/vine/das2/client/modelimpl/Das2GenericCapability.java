package genomancer.vine.das2.client.modelimpl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import genomancer.trellis.das2.model.Das2CapabilityI;
import genomancer.trellis.das2.model.Das2CoordinatesI;
import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2VersionI;
import java.util.Map;
import org.jdom.Element;


public class Das2GenericCapability extends Identifiable implements Das2CapabilityI  {
    protected Das2VersionI version;
    protected String captype;
    protected Das2CoordinatesI coordinates;
    protected List<String> supported_extensions = new ArrayList<String>();
    protected List<Das2FormatI> formats = new ArrayList<Das2FormatI>();
    protected List<org.jdom.Element> additional_data = new ArrayList<org.jdom.Element>();
    protected Map<String, String> init_params;

    /**
     *  version is required to be non-null
     *
     *  if coordinates == null, then assume that all coordinates declared in version are supported
     */
    public Das2GenericCapability(URI base_uri, String query_uri, String captype,
				 Das2VersionI version, Das2CoordinatesI coordinates)  {
	super(base_uri, query_uri);
	this.captype = captype;
	this.coordinates = coordinates;
	this.version = version;
    }

    public void init(Map<String, String> params)  { init_params = params; }

    public String getType() { return captype; }
    public Das2CoordinatesI getCoordinates() {  return coordinates; }
    public Das2VersionI getVersion()  { return version; }

    public List<Das2FormatI> getFormats() { return formats; }
    public List<String> getSupportedExtensions() { return supported_extensions; }
    public List<org.jdom.Element> getAdditionalData() { return additional_data; }

    public void addSupportedExtension(String extension)  { supported_extensions.add(extension); }
    public void addFormat(Das2FormatI format)  { formats.add(format); }
    public void addAdditionalData(org.jdom.Element data)  { additional_data.add(data); }

    /** return negative long to indicate last modification date is unknown */
    public long getLastModified()  { return -1L; }

    
}