package genomancer.vine.das2.client.modelimpl;

import genomancer.trellis.das2.model.Das2FormatI;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;

import genomancer.trellis.das2.model.Das2TypesResponseI;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2TypesCapabilityI;
import genomancer.vine.das2.client.xml.TypesXmlReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  Assumes a format name uniquely identifies a Das2FormatI within this Das2TypesCapabilityI 
 */
public class Das2TypesCapability extends Das2GenericCapability implements Das2TypesCapabilityI  {
    Das2TypesResponseI types = null;

    Map<String, Das2FormatI> name2format = new HashMap<String, Das2FormatI>();
    Map<URI, Das2TypeI> uri2type = null;

    public Das2TypesCapability(URI base_uri, 
			       String query_uri, 
			       Das2Version version, 
			       Das2Coordinates coordinates)  {
        super(base_uri, query_uri, "types", version, coordinates);
    }

   
    public Das2TypesResponseI getTypes() { 
	if (types == null)  {
	    initTypes();
	}
	return types; 
    }

    protected boolean initTypes()  {
	boolean success = false;
	InputStream istr = null;
      uri2type = new HashMap<URI, Das2TypeI>();
	try {
	    URL types_query = this.getAbsoluteURI().toURL();
	    URLConnection conn = types_query.openConnection();
	    // check HTTP status header, etc. here
	    istr = conn.getInputStream();
	    types = TypesXmlReader.readTypesDocument(istr, this.getAbsoluteURI());
          List<Das2TypeI> typeslist = types.getTypes();
	    for (Das2TypeI local_type : typeslist)  {
            uri2type.put(local_type.getAbsoluteURI(), local_type);
		Collection<Das2FormatI> local_formats = local_type.getFormats();
		for (Das2FormatI format : local_formats)  {
		    String format_name = format.getName();
		    if (! name2format.containsKey(format_name)) {
			name2format.put(format_name, format);
		    }
		}
	    }
	    success = true;
	} catch (IOException ex) {
	    Logger.getLogger(Das2TypesCapability.class.getName()).log(Level.SEVERE, null, ex);
	} catch (XMLStreamException ex) {
	    Logger.getLogger(Das2TypesCapability.class.getName()).log(Level.SEVERE, null, ex);
	} catch (URISyntaxException ex) {
	    Logger.getLogger(Das2TypesCapability.class.getName()).log(Level.SEVERE, null, ex);
	} finally {
	    try {
		istr.close();
	    } catch (IOException ex) {
		success = false;
		Logger.getLogger(Das2TypesCapability.class.getName()).log(Level.SEVERE, null, ex);
	    }
	}
	return success;
    }

    public Das2FormatI getFormat(String format_name)  {
	if (types == null)  { initTypes(); }
	return name2format.get(format_name);
    }

    public static void main(String[] args) throws URISyntaxException  {
	String types_url = "file:./data/netaffx_das2_types.mod.xml";
	Das2TypesCapability cap = new Das2TypesCapability(new URI(types_url), types_url, null, null);
	//	List<Das2TypeI> types = cap.getTypes();
	Das2TypesResponseI types_response = cap.getTypes();
    }

    public Das2TypeI getType(URI type_uri) {
        return uri2type.get(type_uri);
    }

} 
