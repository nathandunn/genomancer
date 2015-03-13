package genomancer.vine.das2.client.xml;

import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2PropertyI;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2TypesResponseI;
import genomancer.vine.das2.client.modelimpl.Das2Type;
import genomancer.vine.das2.client.modelimpl.Das2TypesResponse;
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


public class TypesXmlReader extends AbstractDas2XmlReader {
    /**
     *  doc_uri SHOULD be an absolute URI
     *  need the doc_uri for determining XmlBase if no "xml:base" attribute, or if "xml:base" is relative
     *  could leave this out if:
     *     a) "xml:base" required
     *     b) "query" required attribute added to TYPES whose value is original query URL
     */ 
    public TypesXmlReader(InputStream istream, URI doc_uri) throws XMLStreamException  {
	super(istream, doc_uri);  // sets xml_base = doc_uri, creates xreader based on istream
    }

    public static Das2TypesResponseI readTypesDocument(InputStream istream, URI doc_uri) throws XMLStreamException, URISyntaxException    {
	TypesXmlReader types_reader = new TypesXmlReader(istream, doc_uri);
	Das2TypesResponseI response = types_reader.readTypesDocument();
	types_reader.close();
	return response;
    }


    public Das2TypesResponseI readTypesDocument() throws XMLStreamException, URISyntaxException  {

       List<Das2TypeI> types = new ArrayList<Das2TypeI>();
       ArrayList<Das2LinkI> links = new ArrayList<Das2LinkI>();
       
	System.out.println("XMLInputFactory: " + ifactory);
	System.out.println("XMLStreamReader: " + xreader);
	System.out.println("initial xml_base: " + xml_base);

	frag_builder = new XmlFragmentBuilder();

	while (xreader.hasNext())  {
	    int eventid = xreader.next();
	    if (eventid == XMLStreamConstants.START_ELEMENT)  {
		String elname = xreader.getLocalName();
		if (elname.equals("TYPE"))  {
		    Das2TypeI type = parseTypeElement();
		    types.add(type);
		}
		else if (elname.equals("TYPES"))  {
		    setNamespaces();
		    setXmlBase();
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
	Das2TypesResponseI response = new Das2TypesResponse(xml_base, types, links);
	return response;
    }

    public Das2TypeI parseTypeElement() throws XMLStreamException {
	String local_uri = xreader.getAttributeValue(ns, "uri");
	if (DEBUG)  { System.out.println("TYPE: " + local_uri); }
	String title = xreader.getAttributeValue(ns, "title");
	String description = xreader.getAttributeValue(ns, "description");
	String info_url = xreader.getAttributeValue(ns, "doc_href");
	String ontology_term = xreader.getAttributeValue(ns, "ontology");
	// legacy servers may use "so_accession" instead of "ontology"
	if (ontology_term == null)  { ontology_term = xreader.getAttributeValue(ns, "so_accession"); }
	String method = xreader.getAttributeValue(ns, "method");
	String searchable = xreader.getAttributeValue(ns, "searchable");
	boolean is_searchable = true;  // searchable = "true" by default
	if (searchable != null && searchable.equalsIgnoreCase("false"))  {
	    is_searchable = false;
	}
	Das2Type type = new Das2Type(xml_base, local_uri, title, description, info_url, 
				     ontology_term, method, is_searchable);

	while (xreader.hasNext())  {
	    int eventid = xreader.next();
	    if (eventid == XMLStreamConstants.START_ELEMENT)  {
		String elname = xreader.getLocalName();
		if (elname.equals("FORMAT"))  {
		    Das2FormatI format = parseFormatElement();
		    type.addFormat(format);
		}
		else if (elname.equals("PROP"))  {
		    Das2PropertyI prop = parsePropertyElement();
		    type.addProperty(prop);
		}
		else  {
		    // additional XML fragments
		    org.jdom.Element xml_fragment = frag_builder.buildXmlFragment(xreader);
		    type.addAdditionalData(xml_fragment);
		}
	    }
	    else if (eventid == XMLStreamConstants.END_ELEMENT)  {
		String elname = xreader.getLocalName();
		if (elname.equals("TYPE"))  {
		    break;
		}
	    }

	}
	return type;
    }


    public static void main(String[] args) throws XMLStreamException, FileNotFoundException, URISyntaxException  {
	String test_file = "./data/netaffx_das2_types.mod.xml";
	FileInputStream fis = new FileInputStream(new File(test_file));
	readTypesDocument(fis, new URI("file:" + test_file));
    }

}