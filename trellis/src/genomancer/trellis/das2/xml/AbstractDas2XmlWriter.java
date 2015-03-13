package genomancer.trellis.das2.xml;

import genomancer.trellis.das2.model.Das2CommonDataI;
import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2PropertyI;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.codehaus.staxmate.SMOutputFactory;
import org.codehaus.staxmate.out.SMOutputDocument;
import org.codehaus.staxmate.out.SMOutputElement;
import org.jdom.JDOMException;
import org.jdom.output.StAXOutputter;

public abstract class AbstractDas2XmlWriter  {
    static protected XMLOutputFactory xfactory;
    static protected String DAS2_NAMESPACE = "http://biodas.org/documents/das2";

    protected XMLStreamWriter stream_writer;
    protected StAXOutputter jdom_writer;
    protected SMOutputDocument staxmate_doc;
    protected URI xml_base = null;

    static  {
	xfactory = XMLOutputFactory.newInstance();
    }

    protected static XMLOutputFactory getFactory()  { return xfactory; }

    public AbstractDas2XmlWriter(XMLStreamWriter xw) throws XMLStreamException  {
	this.stream_writer = xw;
	jdom_writer = new StAXOutputter(stream_writer);
	staxmate_doc = SMOutputFactory.createOutputDocument(stream_writer, "1.0", "UTF-8", true);
	staxmate_doc.setIndentation("\n                       ", 1, 3);     
    }

    public AbstractDas2XmlWriter(OutputStream ostr) throws XMLStreamException  {
	this(xfactory.createXMLStreamWriter(ostr));
    }

    public AbstractDas2XmlWriter(Writer writ) throws XMLStreamException  {
	this(xfactory.createXMLStreamWriter(writ));
    }

    protected void setXmlBase(URI xml_base)  { this.xml_base = xml_base; }
    protected URI getXmlBase()  { return xml_base; }

    protected static XMLOutputFactory getXMLOutputFactory()  { return xfactory; }
    protected XMLStreamWriter getXMLStreamWriter()  { return stream_writer; }
    protected StAXOutputter getStAXOutputter()  { return jdom_writer; }
    protected SMOutputDocument getSMOutputDocument()  { return staxmate_doc; }
 
    public void writeCommonAttributes(Das2CommonDataI data, SMOutputElement parent) throws XMLStreamException  {
	String uri = data.getLocalURIString();
	String title = data.getTitle();
	String info_url = data.getInfoURL();  // doc_href
	String desc = data.getDescription();
	parent.addAttribute("uri", uri);
	if (title != null)  { parent.addAttribute("title", title); }
	if (info_url != null)  { parent.addAttribute("doc_href", info_url); }
	if (desc != null)  { parent.addAttribute("description", desc); }
    }

    public void writeCommonElements(Das2CommonDataI data, SMOutputElement parent) throws XMLStreamException, JDOMException  {
	List<Das2PropertyI> props = data.getProperties();
	for (Das2PropertyI prop : props)  {
	    writeProperty(prop, parent);
	}
	List<org.jdom.Element> extra_doc_fragments = data.getAdditionalData();
	writeAdditionalData(extra_doc_fragments, parent);
    }

    public void writeProperty(Das2PropertyI prop, SMOutputElement parent) throws XMLStreamException  {
	SMOutputElement soe = parent.addElement("PROP");
	soe.addAttribute("key", prop.getKey());
	soe.addAttribute("value", prop.getValue());
    }

    public void writeFormat(Das2FormatI format, SMOutputElement parent) throws XMLStreamException  {
	SMOutputElement soe = parent.addElement("FORMAT");
	soe.addAttribute("name", format.getName());
	// may soon make mimetype attribute required, but for now not even in spec...
	String mimetype = format.getMimeType();
	if (mimetype != null)  { soe.addAttribute("mimetype", format.getMimeType()); }
    }

    public void writeLink(Das2LinkI link, SMOutputElement parent)  throws XMLStreamException  {
	SMOutputElement soe = parent.addElement("LINK");
	String title = link.getTitle();
	String href = link.getHref();
	String type = link.getMimeType();
	String rel = link.getRelationship();
	String rev = link.getReverseRelationship();
	if (title != null)  { soe.addAttribute("title", title); }
	if (href != null)  { soe.addAttribute("href", href); }
	if (type != null)  { soe.addAttribute("type", type); }
	if (rel != null)  { soe.addAttribute("rel", rel);  }
	if (rev != null)  { soe.addAttribute("rev", rev);  }
    }

    public void writeAdditionalData(List<org.jdom.Element> doc_fragments, SMOutputElement parent) 
	throws JDOMException, XMLStreamException  {
	if ((doc_fragments != null) && (doc_fragments.size() > 0))  {
	    XMLStreamWriter xw = getXMLStreamWriter();	    
	    xw.writeCharacters("\n");
	    jdom_writer.outputFragment(doc_fragments);
	    xw.writeCharacters("\n");
	}
    }

   
}
