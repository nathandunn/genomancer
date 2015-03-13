/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package genomancer.ivy.das.client.modelimpl;

import genomancer.ivy.das.client.xml.Das1TypesXmlReader;
import genomancer.ivy.das.model.Das1TypesCapabilityI;
import genomancer.ivy.das.model.Das1TypesResponseI;
import genomancer.vine.das2.client.modelimpl.Das2Coordinates;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2Version;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author gregg
 */
public class Das1TypesCapability extends Das2GenericCapability implements Das1TypesCapabilityI  {
    Das1TypesResponseI types_response = null;

    public Das1TypesCapability(URI xml_base, 
			       String local_query_uri, 
			       Das2Version version, 
			       Das2Coordinates coords) {
	super(xml_base, local_query_uri, "das1:types", version, coords);
    }

 
    public Das1TypesResponseI getTypes() { 
	if (types_response == null)  {
	    initTypes();
	}
	return types_response; 
    }

    protected boolean initTypes()  {
	boolean success = false;
	InputStream istr = null;
	try {
	    URL types_query = this.getAbsoluteURI().toURL();
	    URLConnection conn = types_query.openConnection();
	    // check HTTP status header, etc. here
	    istr = conn.getInputStream();
	    types_response = Das1TypesXmlReader.readTypesDocument(istr, this.getAbsoluteURI());
	    success = true;
	} catch (IOException ex) {
	    Logger.getLogger(Das1TypesCapability.class.getName()).log(Level.SEVERE, null, ex);
	} catch (XMLStreamException ex) {
	    Logger.getLogger(Das1TypesCapability.class.getName()).log(Level.SEVERE, null, ex);
	} catch (URISyntaxException ex) {
	    Logger.getLogger(Das1TypesCapability.class.getName()).log(Level.SEVERE, null, ex);
	} finally {
	    try {
		istr.close();
	    } catch (IOException ex) {
		success = false;
		Logger.getLogger(Das1TypesCapability.class.getName()).log(Level.SEVERE, null, ex);
	    }
	}
	return success;
    }



    public static void main(String[] args) throws URISyntaxException  {
	String types_url = "file:./data/das1_types.xml";
	Das1TypesCapability cap = new Das1TypesCapability(new URI(types_url), types_url, null, null);
	//	List<Das1TypeI> types_response = cap.getTypes();
	Das1TypesResponseI types_response = cap.getTypes();
    }
}
