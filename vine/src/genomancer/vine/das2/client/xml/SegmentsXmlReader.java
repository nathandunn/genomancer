package genomancer.vine.das2.client.xml;

import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import genomancer.vine.das2.client.modelimpl.Das2Segment;
import genomancer.vine.das2.client.modelimpl.Das2SegmentsResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import org.jdom.input.XmlFragmentBuilder;


public class SegmentsXmlReader extends AbstractDas2XmlReader {
    /**
     *  doc_uri SHOULD be an absolute URI
     *  need the doc_uri for determining XmlBase if no "xml:base" attribute, or if "xml:base" is relative
     *  could leave this out if:
     *     a) "xml:base" required
     *     b) "query" required attribute added to SEGMENTS whose value is original query URL
     */ 
    public SegmentsXmlReader(InputStream istream, URI doc_uri) throws XMLStreamException  {
	super(istream, doc_uri);  // sets xml_base = doc_uri, creates xreader based on istream
    }

    public static Das2SegmentsResponseI readSegmentsDocument(InputStream istream, URI doc_uri) throws XMLStreamException, URISyntaxException    {
	SegmentsXmlReader segments_reader = new SegmentsXmlReader(istream, doc_uri);
	Das2SegmentsResponseI response = segments_reader.readSegmentsDocument();
	segments_reader.close();
	return response;
    }


    public Das2SegmentsResponseI readSegmentsDocument() throws XMLStreamException, URISyntaxException  {

       List<Das2SegmentI> segments = new ArrayList<Das2SegmentI>();
       List<Das2FormatI> formats = new ArrayList<Das2FormatI>();
       List<Das2LinkI> links = new ArrayList<Das2LinkI>();
       
	System.out.println("XMLInputFactory: " + ifactory);
	System.out.println("XMLStreamReader: " + xreader);
	System.out.println("initial xml_base: " + xml_base);

	frag_builder = new XmlFragmentBuilder();

	while (xreader.hasNext())  {
	    int eventid = xreader.next();
	    if (eventid == XMLStreamConstants.START_ELEMENT)  {
		String elname = xreader.getLocalName();
		if (elname.equals("SEGMENT"))  {
		    Das2SegmentI segment = parseSegmentElement();
		    segments.add(segment);
		}
		else if (elname.equals("SEGMENTS"))  {
		    // currently not doing anything with segments_local_uri
		    String segments_local_uri = xreader.getAttributeValue(ns, "uri");
		    setNamespaces();
		    setXmlBase();
		}
		if (elname.equals("FORMAT"))  {
		    Das2FormatI format = parseFormatElement();
		    formats.add(format);
		}
		else if (elname.equals("LINK"))  {
		    Das2LinkI link = parseLinkElement();
		    links.add(link);
		}
	    }
	    else if (eventid == XMLStreamConstants.START_DOCUMENT)  {
		
	    }	
	    else if (eventid == XMLStreamConstants.END_DOCUMENT)  {

	    }
	}
	Das2SegmentsResponseI response = new Das2SegmentsResponse(xml_base, segments, links, formats);
	return response;
    }

    public Das2SegmentI parseSegmentElement() throws XMLStreamException {
	String local_uri = xreader.getAttributeValue(ns, "uri");
	if (DEBUG)  { System.out.println("SEGMENT: " + local_uri); }
	String title = xreader.getAttributeValue(ns, "title");
	//	String description = xreader.getAttributeValue(ns, "description");
	String info_url = xreader.getAttributeValue(ns, "doc_href");
        URI reference_uri = null;
	String reference = xreader.getAttributeValue(ns, "reference");
        if (reference != null)  {
            reference_uri = xml_base.resolve(reference);
        }
	String len = xreader.getAttributeValue(ns, "length");
	int length = Integer.parseInt(len);

	Das2Segment segment = new Das2Segment(xml_base, local_uri, title,
					      reference_uri, length, info_url);
	return segment;
    }


    public static void main(String[] args) throws XMLStreamException, FileNotFoundException, URISyntaxException  {
	String test_file = "./data/netaffx_das2_segments.xml";
	FileInputStream fis = new FileInputStream(new File(test_file));
	readSegmentsDocument(fis, new URI("file:" + test_file));
    }

}