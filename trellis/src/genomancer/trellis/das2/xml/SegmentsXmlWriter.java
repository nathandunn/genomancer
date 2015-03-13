package genomancer.trellis.das2.xml;

import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import java.util.List;
import genomancer.trellis.das2.model.Das2SegmentI;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.codehaus.staxmate.out.SMOutputDocument;
import org.codehaus.staxmate.out.SMOutputElement;

public class SegmentsXmlWriter extends AbstractDas2XmlWriter  {
    // xml_base inherited from AbstractDasXmlWriter
    Das2SegmentsResponseI segments_holder;

    public SegmentsXmlWriter(Das2SegmentsResponseI segments_holder, XMLStreamWriter xw) 
	throws XMLStreamException {
	super(xw);
	this.segments_holder = segments_holder;
	this.setXmlBase(segments_holder.getBaseURI());
    }

    public SegmentsXmlWriter(Das2SegmentsResponseI segments_holder, OutputStream ostr) 
	throws XMLStreamException  {
	this(segments_holder, getFactory().createXMLStreamWriter(ostr));
    }

    public SegmentsXmlWriter(Das2SegmentsResponseI segments_holder, Writer writ) 
	throws XMLStreamException  {
	this(segments_holder, getFactory().createXMLStreamWriter(writ));
    }

    public void writeSegmentsDocument() throws XMLStreamException {
	//    public void writeSegmentsDocument(List<Das2SegmentI> segments) throws XMLStreamException  {
	List<Das2SegmentI> segments = segments_holder.getSegments();
	XMLStreamWriter xw = getXMLStreamWriter();
	SMOutputDocument doc = getSMOutputDocument();
	SMOutputElement segs = doc.addElement("SEGMENTS");
	xw.writeAttribute("xmlns", DAS2_NAMESPACE);
	if (xml_base != null)  {
	    xw.writeAttribute("xml:base", xml_base.toString());
	}
	for (Das2SegmentI segment : segments)  {
	    writeSegment(segment, segs);
	}
	doc.closeRoot();
	xw.close();
    }

    public void writeSegment(Das2SegmentI segment, SMOutputElement parent) throws XMLStreamException  {
	String  local_uri = segment.getLocalURIString();
	String title = segment.getTitle();
	String info_url = segment.getInfoURL();
	URI reference = segment.getReference();
	int length = segment.getLength();
	
	SMOutputElement segel = parent.addElement("SEGMENT");
	segel.addAttribute("uri", local_uri);
	segel.addAttribute("title", title);
	segel.addAttribute("length", Integer.toString(length));
	if (info_url != null)  { segel.addAttribute("doc_href", info_url); }
	if (reference != null)  { segel.addAttribute("reference", reference.toString()); }
    }


    
}