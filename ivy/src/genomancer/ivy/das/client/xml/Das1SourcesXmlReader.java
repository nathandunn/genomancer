package genomancer.ivy.das.client.xml;

import genomancer.ivy.das.Das1Constants;
import genomancer.ivy.das.client.modelimpl.Das1TypesCapability;
import genomancer.ivy.das.client.modelimpl.Das1StylesheetCapability;
import genomancer.ivy.das.client.modelimpl.Das1SequenceCapability;
import genomancer.ivy.das.client.modelimpl.Das1FeaturesCapability;
import genomancer.ivy.das.client.modelimpl.Das1EntryPointsCapability;
import genomancer.trellis.das2.model.Das2CapabilityI;
import genomancer.trellis.das2.model.Das2SourcesResponseI;
import genomancer.vine.das2.client.modelimpl.Das2Coordinates;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2Version;
import genomancer.vine.das2.client.xml.SourcesXmlReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.stream.XMLStreamException;

public class Das1SourcesXmlReader extends SourcesXmlReader {

    public Das1SourcesXmlReader(InputStream istream, URI doc_uri) throws XMLStreamException  {
	super(istream, doc_uri);  // sets xml_base = doc_uri, creates xreader based on istream
    }

    public static Das2SourcesResponseI readSourcesDocument(InputStream istream, URI doc_uri)  
	throws XMLStreamException, URISyntaxException  {
	Das1SourcesXmlReader sources_reader = new Das1SourcesXmlReader(istream, doc_uri);
	Das2SourcesResponseI response = sources_reader.readSourcesDocument();
	sources_reader.close();
	return response;
    }


    protected Das2GenericCapability makeCapability(URI xml_base, String local_query_uri, String type, 
						  Das2Version version, Das2Coordinates coords)  {
	Das2GenericCapability cap = null;
	if (type.equals(Das1Constants.DAS1_FEATURES_CAPABILITY))  { // "das1:features"
	    cap = new Das1FeaturesCapability(xml_base, local_query_uri, version, coords);
	    // System.out.println("made features capability: " + cap);
	}
	else if (type.equals(Das1Constants.DAS1_STYLESHEET_CAPABILITY))  {  // "das1:stylesheet"
	    cap = new Das1StylesheetCapability(xml_base, local_query_uri, version, coords);
	}
	else if (type.equals(Das1Constants.DAS1_SEQUENCE_CAPABILITY))  {  // "das1:sequence"
	    cap = new Das1SequenceCapability(xml_base, local_query_uri, version, coords);
	}
	else if (type.equals(Das1Constants.DAS1_TYPES_CAPABILITY))  {  // "das1:types"
	    cap = new Das1TypesCapability(xml_base, local_query_uri, version, coords);
	}
	else if (type.equals(Das1Constants.DAS1_ENTRY_POINTS_CAPABILITY))  {  // "das1:entry_points"
	    cap = new Das1EntryPointsCapability(xml_base, local_query_uri, version, coords);
	    //	    System.out.println("made entry points capability: " + cap);
	}
	//	else if (type.equals("das1:link"))  {
	    // not yet present in DAS1 registry
	//	}

	/**  Oct2008 GAH: none of the DAS1.53E extension capabilties are implemented yet, 
	     treat them as unrecognized capabilities for now
	else if (type.equals("das1:alignment"))  {    // DAS1 alignment extension
	    // ignore, not yet implemented
	}
	else if (type.equals("das1:structure"))  {
	    // ignore, not yet implemented
	}
        else if (type.equals("das1:interaction"))  {
	    // ignore, not yet implemented
	}
	else if (type.equals("das1:volmap"))  {  // not yet present in DAS1 registry
	    // ignore, not yet implemented
	}
	*/
	else  {  
	    // not recognized as a DAS1 capability, pass up to superclass to 
	    //   handle DAS2 and unknown capabilities
	    cap = super.makeCapability(xml_base, local_query_uri, type, version, coords);
	}
	return cap;
    }

    public static void main(String[] args) throws XMLStreamException, FileNotFoundException, URISyntaxException  {
	//	String test_file = "./data/das2_registry_sources.mod.xml";
	String test_file = "./data/das1_registry_sources.slice.xml";
	FileInputStream fis = new FileInputStream(new File(test_file));
	readSourcesDocument(fis, new URI("file:" + test_file));
    }
}