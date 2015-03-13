package genomancer.trellis.das2.xml;

import java.util.List;
import genomancer.trellis.das2.model.Das2FeatureI;
import genomancer.trellis.das2.model.Das2FeaturesResponseI;
import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.trellis.das2.model.Strand;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.codehaus.staxmate.out.SMOutputDocument;
import org.codehaus.staxmate.out.SMOutputElement;
import org.jdom.JDOMException;

public class FeaturesXmlWriter extends AbstractDas2XmlWriter  {
    // xml_base inherited from AbstractDasXmlWriter
    Das2FeaturesResponseI features_holder;
    boolean segments_share_xml_base = false;
    boolean types_share_xml_base = false;

    public FeaturesXmlWriter(Das2FeaturesResponseI features_holder, XMLStreamWriter xw) throws XMLStreamException  { 
	super(xw);
	this.features_holder = features_holder;
	this.setXmlBase(features_holder.getBaseURI());
    }

    public FeaturesXmlWriter(Das2FeaturesResponseI features_holder, OutputStream ostr) throws XMLStreamException  { 
	this(features_holder, getFactory().createXMLStreamWriter(ostr));
    }

    public FeaturesXmlWriter(Das2FeaturesResponseI features_holder, Writer writ) throws XMLStreamException  { 
	this(features_holder, getFactory().createXMLStreamWriter(writ));
    }

    public void writeFeaturesDocument() throws XMLStreamException  {
	List<Das2FeatureI> features = features_holder.getFeatures();	
	XMLStreamWriter xw = getXMLStreamWriter();
	SMOutputDocument doc = getSMOutputDocument();
	SMOutputElement featsel = doc.addElement("FEATURES");

	xw.writeAttribute("xmlns", DAS2_NAMESPACE);
	if (xml_base != null)  {
	    xw.writeAttribute("xml:base", xml_base.toString());
	}
	for (Das2FeatureI feat : features)  {
            try {
                writeFeature(feat, featsel);
            } catch (JDOMException ex) {
                Logger.getLogger(FeaturesXmlWriter.class.getName()).log(Level.SEVERE, null, ex);
            }
	}
	doc.closeRoot();
	xw.close();
    }

    public void writeFeature(Das2FeatureI feat, SMOutputElement featsel) throws XMLStreamException, JDOMException  {
	SMOutputElement fel = featsel.addElement("FEATURE");
	writeCommonAttributes(feat, fel);

	Date created = feat.getCreationDate();
	Date modified = feat.getLastModifiedDate();
	String local_type_string;
	if (features_holder.typesShareBaseURI())  {
	    local_type_string = feat.getType().getLocalURIString();
	}
	else  {
	    URI local_uri = xml_base.relativize(feat.getType().getAbsoluteURI());
	    local_type_string = local_uri.toString();
	}

	fel.addAttribute("type", local_type_string);
	// need to switch to using DateFormat and ensure ISO 8601 compliance
	if (created != null)  { fel.addAttribute("created", created.toString()); }
	// need to switch to using DateFormat and ensure ISO 8601 compliance
	if (modified != null)  { fel.addAttribute("modified", modified.toString()); }	    

	if (feat.getAliases() != null)  {
	    for (String alias : feat.getAliases())  {
		fel.addElement("ALIAS").addAttribute("alias", alias);
	    }
	}
	if (feat.getLocations() != null)  {
	    for (Das2LocationI loc : feat.getLocations())  {
		writeLocation(loc, fel);
	    }
	}

	if (feat.getLinks()  != null)  {
	    for (Das2LinkI link : feat.getLinks())  {
		writeLink(link, fel);
	    }
	}

	if (feat.getParents() != null)  {
	    for (Das2FeatureI feat_parent : feat.getParents())  {
		writeParent(feat_parent, fel);
	    }
	}
	
	if (feat.getParts() != null)  {
	    for (Das2FeatureI feat_child : feat.getParts())  {
		writePart(feat_child, fel);
	    }
	}

	if (feat.getNotes() != null)  {
	    for (String note : feat.getNotes())  {
		fel.addElement("NOTE").addCharacters(note);
	    }
	}

	writeCommonElements(feat, fel);

	// recurse down into feature hierarchy to write part features 
	//   currently if part features are shared by more than one parent, 
	//       part feature will be written multiple times...
	if (feat.getParts() != null)  {
	    for (Das2FeatureI feat_child : feat.getParts())  {
		writeFeature(feat_child, featsel);
	    }
	}
    }

    protected void writeLocation(Das2LocationI loc, SMOutputElement fel) throws XMLStreamException {
	String local_segment_string;
	if (features_holder.segmentsShareBaseURI())  {
	    local_segment_string = loc.getSegment().getLocalURIString();
	}
	else  {
	    URI local_uri = xml_base.relativize(loc.getSegment().getAbsoluteURI());
	    local_segment_string = local_uri.toString();
	}
	int min = loc.getMin();
	int max = loc.getMax();
	Strand strand = loc.getStrand();
	String gap = loc.getGap();

	SMOutputElement lel = fel.addElement("LOC");
	lel.addAttribute("segment", local_segment_string);
	if ((loc.getMin() >= 0) && 
	    (loc.getMax() >=0) )  {
	    StringBuffer rangebuf = new StringBuffer();
	    rangebuf.append(Integer.toString(min));
	    rangebuf.append(":");
	    rangebuf.append(Integer.toString(max));
	    if (strand != null)  {
		if (strand == Strand.FORWARD)  {
		    rangebuf.append(":1");
		}
		else if (strand == Strand.REVERSE)  {
		    rangebuf.append(":-1");
		}
		else if (strand == Strand.BOTH)  {
		    // ??? for now not appending anything
		}
		else if (strand == Strand.UNKNOWN)  {
		    // ??? for now not appending anything
		}
	    }
	    lel.addAttribute("range", rangebuf.toString());
	}
	if (gap != null)  { lel.addAttribute("gap", gap); }
    }

    protected void writeParent(Das2FeatureI feat_parent, SMOutputElement fel) throws XMLStreamException {
	URI parent_uri = feat_parent.getLocalURI();
	fel.addElement("PARENT").addAttribute("uri", parent_uri.toString());
    }

    protected void writePart(Das2FeatureI feat_child, SMOutputElement fel) throws XMLStreamException {
	URI child_uri = feat_child.getLocalURI();
	fel.addElement("PART").addAttribute("uri", child_uri.toString());
    }

  


    
}