/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package genomancer.ivy.das.client.modelimpl;

import genomancer.ivy.das.client.xml.Das1EntryPointsXmlReader;
import genomancer.ivy.das.model.Das1EntryPointsCapabilityI;
import genomancer.ivy.das.model.Das1EntryPointsResponseI;
import genomancer.ivy.das.model.Das1SegmentI;
import genomancer.vine.das2.client.modelimpl.Das2Coordinates;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2Version;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author gregg
 */
public class Das1EntryPointsCapability extends Das2GenericCapability implements Das1EntryPointsCapabilityI  {
    Das1EntryPointsResponseI entry_points_response;

    public Das1EntryPointsCapability(URI xml_base, 
				     String local_query_uri, 
				     Das2Version version, 
				     Das2Coordinates coords) {
	super(xml_base, local_query_uri, "das1:entry_points", version, coords);
    }

    public Das1EntryPointsResponseI getEntryPoints() {
	if (entry_points_response == null)  {
	    initEntryPoints();
	}
        return entry_points_response;
    }

   protected boolean initEntryPoints()  {
	boolean success = false;
	InputStream istr = null;
	try {
	    URL entry_points_query = this.getAbsoluteURI().toURL();
	    URLConnection conn = entry_points_query.openConnection();
	    // check HTTP status header, etc. here
	    istr = conn.getInputStream();
	    entry_points_response = Das1EntryPointsXmlReader.readEntryPointsDocument(istr, this.getAbsoluteURI());
	    success = true;
	} catch (IOException ex) {
	    Logger.getLogger(Das1EntryPointsCapability.class.getName()).log(Level.SEVERE, null, ex);
	} catch (XMLStreamException ex) {
	    Logger.getLogger(Das1EntryPointsCapability.class.getName()).log(Level.SEVERE, null, ex);
	} catch (URISyntaxException ex) {
	    Logger.getLogger(Das1EntryPointsCapability.class.getName()).log(Level.SEVERE, null, ex);
	} finally {
	    try {
		istr.close();
	    } catch (IOException ex) {
		success = false;
		Logger.getLogger(Das1EntryPointsCapability.class.getName()).log(Level.SEVERE, null, ex);
	    }
	}
	return success;
    }

    public static void main(String[] args) throws URISyntaxException  {
	String segments_url = "file:./data/das1_entry_points.xml";
	Das1EntryPointsCapability cap = 
	    new Das1EntryPointsCapability(new URI(segments_url), segments_url, null, null);
	Das1EntryPointsResponseI entry_points_response = cap.getEntryPoints();
	List<Das1SegmentI> entry_points = entry_points_response.getEntryPoints();
	System.out.println("Das1EntryPointsCapability, entry point count: " + entry_points.size());
    }

}