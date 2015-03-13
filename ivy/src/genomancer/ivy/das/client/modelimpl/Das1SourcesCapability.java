package genomancer.ivy.das.client.modelimpl;

import genomancer.ivy.das.client.xml.Das1SourcesXmlReader;
import genomancer.trellis.das2.model.Das2VersionI;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;

import genomancer.trellis.das2.model.Das2SourceI;
import genomancer.trellis.das2.model.Das2SourcesCapabilityI;
import genomancer.trellis.das2.model.Das2SourcesResponseI;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.xml.SourcesXmlReader;
import java.util.HashMap;

public class Das1SourcesCapability extends Das2GenericCapability implements Das2SourcesCapabilityI  {
    // List<Das2SourceI> sources = null;
    Das2SourcesResponseI sources = null;
    Map<URI, Das2SourceI> uri2source = null;
    Map<URI, Das2VersionI> uri2version = null;

    public Das1SourcesCapability()  {
	// exploiting a constructor loophole to allow setting of all fields via init()
	//     maybe should have Das2SourcesCapability not extends Das2GenericCapablity
	//     (and possibly not implement Das2SourcesCapability)
	//     since only real shared stuff is from IdentifiableI, and could implement 
	//       IdentifiableI with a wrapped Identifiable field
	this(null, null);
    }

    public Das1SourcesCapability(URI base_uri, String query_uri)  {
	super(base_uri, query_uri, "sources", null, null);
    }

    public void init(Map<String, String> params) {
	super.init(params);  // populates init_params;
	if (init_params != null)  {
	    try {
		String local_query_string = init_params.get("sources_query_uri");
            if (local_query_string != null)  {
                local_uri_string = local_query_string;
                base_uri = new URI(local_uri_string);
            }
	    } catch (URISyntaxException ex) {
		Logger.getLogger(Das1SourcesCapability.class.getName()).log(Level.SEVERE, null, ex);
	    }
	}
    }


    public Das1SourcesCapability(String query_uri) throws URISyntaxException  {
        super(new URI(query_uri), query_uri, "sources", null, null);
    }
   
    public String getMaintainerEmail() {
        return null;
    }

    // public List<Das2SourceI> getSources()  { 
    public Das2SourcesResponseI getSources()  {
	if (sources == null)  {
	    initSources();
	}
	return sources; 
    }

    public Das2SourceI getSource(URI source_uri)  {
	if (sources == null)  { initSources(); }
	return uri2source.get(source_uri);
    }

    public Das2VersionI getVersion (URI version_uri)  {
	if (sources == null)  { initSources(); }
	return uri2version.get(version_uri);
    }

    protected boolean initSources()  {
	boolean success = false;
	InputStream istr = null;
	try {
	    URL sources_query = this.getAbsoluteURI().toURL();
	    URLConnection conn = sources_query.openConnection();
	    // check HTTP status header, etc. here
	    istr = conn.getInputStream();
	    sources = Das1SourcesXmlReader.readSourcesDocument(istr, this.getAbsoluteURI());
	    uri2source = new HashMap<URI, Das2SourceI>();
	    uri2version = new HashMap<URI, Das2VersionI>();
	    for (Das2SourceI source : sources.getSources()) {
		uri2source.put(source.getAbsoluteURI(), source);
		uri2source.put(source.getLocalURI(), source);
		for (Das2VersionI version : source.getVersions()) {
		    uri2version.put(version.getAbsoluteURI(), version);
		    uri2version.put(version.getLocalURI(), version);
		}
	    }
	    success = true;
	} catch (IOException ex) {
	    Logger.getLogger(Das1SourcesCapability.class.getName()).log(Level.SEVERE, null, ex);
	} catch (XMLStreamException ex) {
	    Logger.getLogger(Das1SourcesCapability.class.getName()).log(Level.SEVERE, null, ex);
	} catch (URISyntaxException ex) {
	    Logger.getLogger(Das1SourcesCapability.class.getName()).log(Level.SEVERE, null, ex);
	} finally {
	    try {
		istr.close();
	    } catch (IOException ex) {
		success = false;
		Logger.getLogger(Das1SourcesCapability.class.getName()).log(Level.SEVERE, null, ex);
	    }
	}
	return success;
    }

    public static void main(String[] args) throws URISyntaxException  {
	String sources_url = "file:./data/das1_registry_sources.slice.xml";
	//	String sources_url = "./data/das2_registry_sources.mod.xml";
	Das1SourcesCapability cap = new Das1SourcesCapability(sources_url);
	//	List<Das2SourceI> sources = cap.getSources();
	Das2SourcesResponseI sources_response = cap.getSources();
    }


} 
