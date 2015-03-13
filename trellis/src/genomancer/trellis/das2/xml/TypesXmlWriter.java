package genomancer.trellis.das2.xml;

import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2LinkI;
import java.util.List;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2TypesResponseI;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.codehaus.staxmate.out.SMOutputDocument;
import org.codehaus.staxmate.out.SMOutputElement;
import org.jdom.JDOMException;

public class TypesXmlWriter extends AbstractDas2XmlWriter  {
    // xml_base inherited from AbstractDasXmlWriter
    Das2TypesResponseI types_holder;
    
    public TypesXmlWriter(Das2TypesResponseI types_holder, XMLStreamWriter xw) throws XMLStreamException  { 
	super(xw);
	this.types_holder = types_holder;
	this.setXmlBase(types_holder.getBaseURI());
    }
    public TypesXmlWriter(Das2TypesResponseI types_holder, OutputStream ostr) throws XMLStreamException  { 
	this(types_holder, getFactory().createXMLStreamWriter(ostr));
    }
    public TypesXmlWriter(Das2TypesResponseI types_holder, Writer writ) throws XMLStreamException  { 
	this(types_holder, getFactory().createXMLStreamWriter(writ));
    }


    // public void writeTypesDocument(List<Das2TypeI> types) throws XMLStreamException, JDOMException  {
    public void writeTypesDocument() throws XMLStreamException, JDOMException  {
	List<Das2TypeI> types = types_holder.getTypes();
	List<Das2LinkI> links = types_holder.getLinks();

	XMLStreamWriter xw = getXMLStreamWriter();
	SMOutputDocument doc = getSMOutputDocument();
	SMOutputElement typesel = doc.addElement("TYPES");
	xw.writeAttribute("xmlns", DAS2_NAMESPACE);
    
	if (xml_base != null)  {
	    xw.writeAttribute("xml:base", xml_base.toString());
	}
	if (links != null)  {
	for (Das2LinkI link : links)  {
	    writeLink(link, typesel);
	}
      }
      if (types != null)  {
	for (Das2TypeI type : types)  {
	    writeType(type, typesel);
	}
      }
	doc.closeRoot();
	xw.close();
    }

    public void writeType(Das2TypeI type, SMOutputElement parent) throws XMLStreamException, JDOMException  {
	URI ontoterm = type.getOntologyTerm();
	String soterm = type.getSOAccession();
	String method = type.getMethod();
	boolean searchable = type.isSearchable();

        SMOutputElement tel = parent.addElement("TYPE");
	writeCommonAttributes(type, tel);
	if (ontoterm != null)  { tel.addAttribute("ontology", ontoterm.toString()); }
	if (soterm != null)  { tel.addAttribute("so_accession", soterm); }
	if (method != null)  { tel.addAttribute("method", method); }
	if (! searchable)  { tel.addAttribute("searchable", "false"); }

	for (Das2FormatI format : type.getFormats())  {
	    writeFormat(format, tel);
	} 
	writeCommonElements(type, tel);
    }
    
}