package plugin_example;

import genomancer.trellis.das2.Das2Constants;
import genomancer.trellis.das2.model.Das2FormatI;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import genomancer.trellis.das2.model.Das2CapabilityI;
import genomancer.trellis.das2.model.Das2SourcesCapabilityI;
import genomancer.trellis.das2.model.Das2SegmentsCapabilityI;
import genomancer.trellis.das2.model.Das2TypesCapabilityI;
import genomancer.trellis.das2.model.Das2FeaturesCapabilityI;

import genomancer.trellis.das2.model.Das2SourceI;
import genomancer.trellis.das2.model.Das2VersionI;
import genomancer.trellis.das2.model.Das2CoordinatesI;
import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import genomancer.trellis.das2.model.Das2SourcesResponseI;

import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2TypesResponseI;
import genomancer.trellis.das2.xml.SegmentsXmlWriter;
import genomancer.trellis.das2.xml.SourcesXmlWriter;
import genomancer.trellis.das2.xml.TypesXmlWriter;
import genomancer.vine.das2.client.modelimpl.Das2Coordinates;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2Source;
import genomancer.vine.das2.client.modelimpl.Das2SourcesCapability;
import genomancer.vine.das2.client.modelimpl.Das2SourcesResponse;
import genomancer.vine.das2.client.modelimpl.Das2TypesCapability;
import genomancer.vine.das2.client.modelimpl.Das2Version;
import java.util.LinkedHashMap;
import java.util.HashMap;
import javax.xml.stream.XMLStreamException;
import org.jdom.Element;
import org.jdom.JDOMException;

public class SourcesCapabilityExample extends Das2GenericCapability implements Das2SourcesCapabilityI {
    Das2SourcesResponseI sources = null;
    Map<URI, Das2SourceI> uri2source = null;
    Map<URI, Das2VersionI> uri2version = null;


    public SourcesCapabilityExample(URI base_uri, String query_uri, Map<String,String> params) throws URISyntaxException  {
        super(base_uri, query_uri, "sources", 
	      null, // Das2SourceCapabilities are not assigned a version (they contain version(s)
	      null  // Das2SourceCapabilities are not assigned coordinates (they contain coordinate(s)
	      );
	initSources();
    }


    public Das2SourcesResponseI getSources() {
	return sources;
    }

    public Das2SourceI getSource(URI source_uri)  {
	return uri2source.get(source_uri);
    }

    public Das2VersionI getVersion (URI version_uri)  {
	return uri2version.get(version_uri);
    }

    protected void initSources()   {
	List<Das2SourceI> sources_list = new ArrayList<Das2SourceI>();
	Das2Source usource = new Das2Source(base_uri, "unicorn", "unicorn", null, null);
	sources_list.add(usource);
	
	Das2Version uversion = new Das2Version(usource, "unicorn_v1", "Unicorn Genome Version 1.0", 
					       null, null, null, null);

	Das2CoordinatesI ucoords = new Das2Coordinates(base_uri, "unicorn_v1/coords", "unicornus_equs", 
						       "chromosome", "authority_placeholder", 
						       "version1", null, null);
	usource.addVersion(uversion);
	uversion.addCoordinates(ucoords);
				
	uversion.addCapability(new SegmentsCapabilityExample(uversion, ucoords));
	uversion.addCapability(new TypesCapabilityExample(uversion, ucoords));
	uversion.addCapability(new FeaturesCapabilityExample(uversion, ucoords));

	/*
	UcscSegmentsCapability segcap = new UcscSegmentsCapability(new_version, coords, rs);
	new_version.addCapability(segcap);
	UcscTypesCapability typecap = new UcscTypesCapability(new_version, coords, rs);
	new_version.addCapability(typecap);
	UcscFeaturesCapability featcap = new UcscFeaturesCapability(new_version, coords, rs);
	*/

	sources = new Das2SourcesResponse(getBaseURI(), sources_list, "maintainer_email@wherever", null);
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
    }


  /*  public static void main(String[] args) throws URISyntaxException, XMLStreamException, JDOMException  {
	// String sources_url = "file:./data/das2_registry_sources.mod.xml";
	Das2SourcesCapability sources_cap = new Das2SourcesCapability();
	URI base_uri = new URI("http://localhost/das2/proxy/test/");
	String query_uri = "sources";
	SourcesCapabilityExample sources_cap = new SourcesCapabilityExample(base_uri.toString());

	//	List<Das2SourceI> sources = cap.getSources();
	Das2SourcesResponseI sources_response = sources_cap.getSources();
	SourcesXmlWriter sources_writer = new SourcesXmlWriter(sources_response, System.out);
        sources_writer.writeSourcesDocument();

	Das2SourceI source = sources_response.getSources().get(0);
	Das2VersionI version = source.getVersions().get(0);
	System.out.println();
	System.out.println();

	System.out.println("getting types for assembly version: " + version.getAbsoluteURI());
	Das2TypesCapabilityI types_cap = 
            (Das2TypesCapabilityI) version.getCapability(Das2Constants.DAS2_TYPES_CAPABILITY);
	Das2TypesResponseI types_response = types_cap.getTypes();
	Das2TypeI type = types_response.getTypes().get(0);
	TypesXmlWriter types_writer = new TypesXmlWriter(types_response, System.out);
	types_writer.writeTypesDocument();
	System.out.println();
	System.out.println();

	System.out.println("getting segments for version: " + version.getAbsoluteURI());
	Das2SegmentsCapabilityI segments_cap = 
            (Das2SegmentsCapabilityI) version.getCapability(Das2Constants.DAS2_SEGMENTS_CAPABILITY);
	Das2SegmentsResponseI segments_response = segments_cap.getSegments();
	Das2SegmentI segment = segments_response.getSegments().get(0);
	SegmentsXmlWriter segments_writer = new SegmentsXmlWriter(segments_response, System.out);
	segments_writer.writeSegmentsDocument();
	System.out.println();
	System.out.println();
	
    }
*/
   

}