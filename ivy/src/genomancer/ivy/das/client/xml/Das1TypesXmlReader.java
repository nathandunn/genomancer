package genomancer.ivy.das.client.xml;

import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2PropertyI;
import genomancer.ivy.das.model.Das1TypeI;
import genomancer.ivy.das.model.Das1TypesResponseI;
import genomancer.ivy.das.client.modelimpl.Das1Type;
import genomancer.ivy.das.client.modelimpl.Das1TypesResponse;
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
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import org.jdom.input.XmlFragmentBuilder;


public class Das1TypesXmlReader extends AbstractDas1XmlReader {
    /**
     *  doc_uri SHOULD be an absolute URI
     *  need the doc_uri for determining XmlBase if no "xml:base" attribute, or if "xml:base" is relative
     *  could leave this out if:
     *     a) "xml:base" required
     *     b) "query" required attribute added to TYPES whose value is original query URL
     */ 
    public Das1TypesXmlReader(InputStream istream, URI doc_uri) throws XMLStreamException  {
	super(istream, doc_uri);  // sets xml_base = doc_uri, creates xreader based on istream
    }

    public static Das1TypesResponseI readTypesDocument(InputStream istream, URI doc_uri) throws XMLStreamException, URISyntaxException, MalformedURLException    {
	Das1TypesXmlReader types_reader = new Das1TypesXmlReader(istream, doc_uri);
	Das1TypesResponseI response = types_reader.readTypesDocument();
	types_reader.close();
	return response;
    }


    public Das1TypesResponseI readTypesDocument() throws XMLStreamException, URISyntaxException, MalformedURLException  {

	List<Das1TypeI> types = new ArrayList<Das1TypeI>();
	String version = null;
	String href = null;

	System.out.println("XMLInputFactory: " + ifactory);
	System.out.println("XMLStreamReader: " + xreader);
	System.out.println("initial xml_base: " + xml_base);

	frag_builder = new XmlFragmentBuilder();

	while (xreader.hasNext())  {
	    int eventid = xreader.next();
	    if (eventid == XMLStreamConstants.START_ELEMENT)  {
		String elname = xreader.getLocalName();
		if (elname.equals("TYPE"))  {
		    Das1TypeI type = parseTypeElement();
		    types.add(type);
		}
		else if (elname.equals("GFF"))  {
		    version = xreader.getAttributeValue(ns, "version");
		    href = xreader.getAttributeValue(ns, "href");
		}
	    }
	    else if (eventid == XMLStreamConstants.START_DOCUMENT)  {
		
	    }	
	    else if (eventid == XMLStreamConstants.END_DOCUMENT)  {

	    }
	}

	Das1TypesResponseI response = new Das1TypesResponse(href, version, types);
	return response;
    }

    public Das1TypeI parseTypeElement() throws XMLStreamException {
	String id = xreader.getAttributeValue(ns, "id");
	// String label = xreader.getAttributeValue(ns, "title");
	String category = xreader.getAttributeValue(ns, "category");
	String method = xreader.getAttributeValue(ns, "method");
	int type_count = Das1TypeI.NO_TYPE_COUNT;
	String type_count_str = xreader.getElementText();
	if (type_count_str != null && type_count_str.length() > 0)  {  
          type_count_str = type_count_str.trim(); 
          if (type_count_str.length() > 0)  {
              type_count = Integer.parseInt(type_count_str);
          }
	}
	//	boolean is_reference = 
	if (DEBUG)  { System.out.println("TYPE: " + id); }

	Das1Type type = new Das1Type(id, category, method);
	return type;
    }


    public static void main(String[] args) throws XMLStreamException, FileNotFoundException, URISyntaxException, MalformedURLException  {
	String test_file = "./data/das1_types.xml";
	FileInputStream fis = new FileInputStream(new File(test_file));
	readTypesDocument(fis, new URI("file:" + test_file));
    }

}