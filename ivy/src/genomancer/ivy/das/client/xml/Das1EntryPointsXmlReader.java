package genomancer.ivy.das.client.xml;


import genomancer.ivy.das.client.modelimpl.Das1EntryPointsResponse;
import genomancer.ivy.das.client.modelimpl.Das1Segment;
import genomancer.ivy.das.model.Das1EntryPointsResponseI;
import genomancer.ivy.das.model.Das1LinkI;
import genomancer.ivy.das.model.Das1SegmentI;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import org.jdom.input.XmlFragmentBuilder;


public class Das1EntryPointsXmlReader extends AbstractDas1XmlReader {
    /**
     *  doc_uri SHOULD be an absolute URI
     *  need the doc_uri for determining XmlBase if no "xml:base" attribute, or if "xml:base" is relative
     *  could leave this out if:
     *     a) "xml:base" required
     *     b) "query" required attribute added to SEGMENTS whose value is original query URL
     */ 
    public Das1EntryPointsXmlReader(InputStream istream, URI doc_uri) throws XMLStreamException  {
	super(istream, doc_uri);  // sets xml_base = doc_uri, creates xreader based on istream
    }

    public static Das1EntryPointsResponseI readEntryPointsDocument(InputStream istream, URI doc_uri) throws XMLStreamException, URISyntaxException    {
	Das1EntryPointsXmlReader entry_points_reader = new Das1EntryPointsXmlReader(istream, doc_uri);
	Das1EntryPointsResponseI entry_points_response = entry_points_reader.readEntryPointsDocument();
	entry_points_reader.close();
	return entry_points_response;
    }


    public Das1EntryPointsResponseI readEntryPointsDocument() throws XMLStreamException, URISyntaxException  {
        Das1EntryPointsResponseI response = null;

	List<Das1SegmentI> segments = new ArrayList<Das1SegmentI>();
	String version = null;
	String href = null;

	System.out.println("XMLInputFactory: " + ifactory);
	System.out.println("XMLStreamReader: " + xreader);
	System.out.println("initial xml_base: " + xml_base);

	frag_builder = new XmlFragmentBuilder();

	while (xreader.hasNext()) {
	    int eventid = xreader.next();
	    if (eventid == XMLStreamConstants.START_ELEMENT) {
		String elname = xreader.getLocalName();
		if (elname.equals("SEGMENT")) {
		    Das1SegmentI segment = parseSegmentElement();
		    segments.add(segment);
		} else if (elname.equals("ENTRY_POINTS")) {
		    version = xreader.getAttributeValue(ns, "version");
		    href = xreader.getAttributeValue(ns, "href");
		}
	    } else if (eventid == XMLStreamConstants.START_DOCUMENT) {
	    } else if (eventid == XMLStreamConstants.END_DOCUMENT) {
	    }
	}
	response = new Das1EntryPointsResponse(href, version, segments);

        return response;
    }

    public Das1SegmentI parseSegmentElement() throws XMLStreamException {
	String id = xreader.getAttributeValue(ns, "id");
	int start = Integer.parseInt(xreader.getAttributeValue(ns, "start"));
	int stop = Integer.parseInt(xreader.getAttributeValue(ns, "stop"));
	String type = xreader.getAttributeValue(ns, "type");
	String orientation = xreader.getAttributeValue(ns, "orientation");
	String description = xreader.getElementText().trim();
	// not sure what to do about "description", "version", and "label"
	Das1SegmentI segment = new Das1Segment(id, start, stop, type, null, null);
	System.out.println("segment: " + id + ", " + stop + ", " + type);
	return segment;

    }


    public static void main(String[] args) throws XMLStreamException, FileNotFoundException, URISyntaxException  {
	String test_file = "./data/das1_entry_points.xml";
	FileInputStream fis = new FileInputStream(new File(test_file));
	Das1EntryPointsResponseI entry_points_response = readEntryPointsDocument(fis, new URI("file:" + test_file));
	List<Das1SegmentI> entry_points = entry_points_response.getEntryPoints();
	System.out.println("Das1EntryPointsReader, entry point count: " + entry_points.size());
    }

}