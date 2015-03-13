package genomancer.ivy.das.client.xml;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jdom.input.XmlFragmentBuilder;


public class AbstractDas1XmlReader  {
    public boolean DEBUG = false;
    //    static String DAS1_NAMESPACE = "http://biodas.org/documents/das";
    static String DAS1_NAMESPACE = "http://biodas.org/documents/das1";
    static String XML_BASE_NAMESPACE = "http://www.w3.org/XML/1998/namespace";
    static String DEFAULT_NAMESPACE = null;

    static XMLInputFactory ifactory;

    //    InputStream istream;
    //    XMLInputFactory ifactory;
    protected XMLStreamReader xreader;
    protected XmlFragmentBuilder frag_builder;

    //    List<String> namespaces;
    URI xml_base = null;
    String ns = DEFAULT_NAMESPACE;

    // may need to turn off namespace handling for sources docs that don't currently use namespaces
    //    (like DAS registry)
    static boolean USE_NAMESPACES = true;

    /** 
     *  reusing factory as recommended in:
     *     http://woodstox.codehaus.org/Performance
     *     http://www.cowtowncoder.com/blog/archives/2006/06/entry_2.html
     *
     *   same factory object is also shared by every instance of subclasses:
     *     SourcesXmlReader, TypesXmlReader, SegmentsXmlReader, FeaturesXmlReader
     */
    static {
	ifactory = XMLInputFactory.newInstance();
	ifactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, USE_NAMESPACES);
    }

    public AbstractDas1XmlReader(InputStream istream, URI doc_uri) throws XMLStreamException  {
	xml_base = doc_uri;    
	xreader = ifactory.createXMLStreamReader(istream);
    }

    public void setNamespaces()  {
	// figure out which namespace to use for DAS1 XML elements
	//    if this is a DAS/2 types doc, will usually be the default 
	//    (declared in "xmlns" attribute): if so then pass as null to 
	//    xreader.getAttributeValue() etc. methods 
	//    [for whatever reason (at least with Woodstox 3.2.7), 
	//         passing DAS1_XML_NAMESPACE in this case will 
	//        not return correct values, will get null returned instead]
	// 
	// if DAS1 is not default namespace, then should pass DAS1_XML_NAMESPACE 
	//    to getAttributeValue() etc. methods 
	int ncount = xreader.getNamespaceCount();
	for (int i=0; i<ncount; i++)  {
	    String nsuri = xreader.getNamespaceURI(i);
	    String nsprefix = xreader.getNamespacePrefix(i);
	    if (nsuri.equals(DAS1_NAMESPACE))  {
		if (nsprefix == null)  {
		    ns = DEFAULT_NAMESPACE;
		    // set default namespace
		}
		else  {
		    ns = DAS1_NAMESPACE;
		}
		System.out.println("namespace for das2: " + ns);
	    }
	}
	reportNamespaces();
	reportAttributes();
    }

    public void setXmlBase() throws URISyntaxException  {
	String xml_base_att;
	if (USE_NAMESPACES) { xml_base_att = xreader.getAttributeValue(XML_BASE_NAMESPACE, "base");  }
	else  { xml_base_att = xreader.getAttributeValue(null, "xml:base"); }
	if (xml_base_att != null)  {
	    URI local_xml_base = new URI(xml_base_att);
	    xml_base = xml_base.resolve(local_xml_base);
	    System.out.println("new xml base: " + xml_base.toString());
	}
    }


    public void close() throws XMLStreamException  {
	xreader.close();
    }


    public void reportNamespaces()  {
	int count = xreader.getNamespaceCount();
	System.out.println("********  Namespaces: " + count + "  *********");
	for (int i = 0; i<count; i++)  {
	    System.out.println(xreader.getNamespacePrefix(i) + ", " + 
			       xreader.getNamespaceURI(i) );
	    //  xreader.getNamespaceContext().toString());
	}
    }

    public void reportAttributes()  {
	int count = xreader.getAttributeCount();
	System.out.println("********  Attributes: " + count + "  *********");
	for (int i = 0; i<count; i++)  {
	    System.out.println(xreader.getAttributeLocalName(i) + ",  " +
			       xreader.getAttributeValue(i) + ", " + 
			       xreader.getAttributeName(i) + ",  " +
			       xreader.getAttributePrefix(i) + ", " + 
			       xreader.getAttributeNamespace(i) + ",  " + 
			       xreader.getAttributeType(i) + ", " + 
			       xreader.isAttributeSpecified(i) );
	}
    }


}