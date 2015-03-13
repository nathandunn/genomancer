package org.jdom.input;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.JDOMFactory;
import org.jdom.Namespace;
import org.jdom.UncheckedJDOMFactory;


public class XmlFragmentBuilder extends StAXBuilder  {

    /**
     *  Takes a <code>XMLStreamReader</code> and builds up an XML fragment rooted at 
     *    the returned Element
     *
     *  XmlStreamReader cursor must be at START_ELEMENT for Element that is root of 
     *     XML fragment that is to be built
     *
     *  when Element is returned, XmlStreamReader will be at END_ELEMENT for Element 
     *     that is root of XML fragment that was built 
     *     
     *   mostly copied from StAXBuilder.buildTree(), but modified to build 
     *      XML fragment with current Element as root, rather than a whole Document
     */
    public Element buildXmlFragment(XMLStreamReader r) throws XMLStreamException {
	JDOMFactory f = getFactory();
	if (f == null)  { f = new UncheckedJDOMFactory(); }
	StAXTextModifier tmod = textModifier;

        /* Only relevant when trying to trim indentation. But if so, let's
         * just always allow modifications in prolog/epilog.
         */
        boolean allowTextMods = (tmod != null);
        int evtType = r.getEventType();
	if (evtType != XMLStreamConstants.START_ELEMENT)  {
	    // something went wrong
	    throw new UnsupportedOperationException("ERROR in XmlFragmentBuilder: " + 
			  "XmlStreamReader cursor must already be positioned at START_ELEMENT event");
	}
	boolean initial_pass = true;
        Element current = null; // At top level

        main_loop:

        while (true) {
            int prevEvent = evtType;
	    // do not move to next event during first pass through main_loop, 
	    //     stay on initial START_ELEMENT
	    if (initial_pass)  {
		initial_pass = false;
	    }
	    else  {
		// not initial pass, so move to next event
		evtType = r.next();
	    }

            /* 11-Dec-2004, TSa: We may want to trim (indentation) white
             *    space... and it's easiest to do as a completely separate
             *    piece of logic, before the main switch.
             */
            if (allowTextMods) {
                // Ok; did we get CHARACTERS to potentially modify?
                if (evtType == XMLStreamConstants.CHARACTERS) {
                    // Mayhaps we could be interested in modifying it?
                    if (tmod.possiblyModifyText(r, prevEvent)) {
                        /* Need to get text before iterating to see the
                         * following event (as that'll lose it)
                         */
                        String txt = r.getText();
                        evtType = r.next();
                        // So how should the text be modified if at all?
                        txt = tmod.textToIncludeBetween(r, prevEvent, evtType,
                                                        txt);
                        // Need to output if it's non-empty text, then:
                        if (txt != null && txt.length() > 0) {
                            /* See discussion below for CHARACTERS case; basically
                             * we apparently can't add anything in epilog/prolog,
                             * not even white space.
                             */
                            if (current != null) {
                                f.addContent(current, f.text(txt));
                            }
                        }
                        prevEvent = XMLStreamConstants.CHARACTERS;
                        // Ok, let's fall down to handle new current event
                    }
                }
                // And then can just fall back to the regular handling
            }

            Content child;

            switch (evtType) {
            case XMLStreamConstants.CDATA:
                child = f.cdata(r.getText());
                break;

            case XMLStreamConstants.SPACE:
                if (cfgIgnoreWS) {
                    continue main_loop;
                }
                // fall through

            case XMLStreamConstants.CHARACTERS:
                /* Small complication: although (ignorable) white space
                 * is allowed in prolog/epilog, and StAX may report such
                 * event, JDOM barfs if trying to add it. Thus, let's just
                 * ignore all textual stuff outside the tree:
                 */
                if (current == null) {
                    continue main_loop;
                }

                child = f.text(r.getText());
                break;

            case XMLStreamConstants.COMMENT:
                child = f.comment(r.getText());
                break;

            case XMLStreamConstants.END_ELEMENT:
		Element parent = current.getParentElement();
		if (parent == null)  {
		    // current element has no parent, and therefore is the root element of the XML fragment
		    //   have reached END_ELEMENT event for the root element of the fragment, 
		    //   therefore done building fragment, so exit main loop
		    break main_loop;
		}
		current = parent;
                if (tmod != null) {
                    allowTextMods = tmod.allowModificationsAfter(r, evtType);
                }
                continue main_loop;

            case XMLStreamConstants.ENTITY_DECLARATION:
            case XMLStreamConstants.NOTATION_DECLARATION:
                /* Shouldn't really get these, but maybe some stream readers
                 * do provide the info. If so, better ignore it -- DTD event
                 * should have most/all we need.
                 */
                continue main_loop;

            case XMLStreamConstants.ENTITY_REFERENCE:
                child = f.entityRef(r.getLocalName());
                break;

            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                child = f.processingInstruction(r.getPITarget(), r.getPIData());
                break;

            case XMLStreamConstants.START_ELEMENT:
                // Ok, need to add a new element...
                {
                    Element newElem = null;
                    String nsURI = r.getNamespaceURI();
                    String elemPrefix = r.getPrefix(); // needed for special handling of elem's namespace
                    String ln = r.getLocalName();

                    if (nsURI == null || nsURI.length() == 0) {
                        if (elemPrefix == null || elemPrefix.length() == 0) {
                            newElem = f.element(ln);
                        } else {
                            /* Happens when a prefix is bound to the default
                             * (empty) namespace...
                             */
                            newElem = f.element(ln, elemPrefix, "");
                        }
                    } else {
                        newElem = f.element(ln, elemPrefix, nsURI);
                    }

                    /* Let's add element right away (probably have to do
                     * it to bind attribute namespaces, too)
                     */
                    if (current == null) {
			// at root: do nothing, root element should remain unattached
			// doc.setRootElement(newElem);
                    } else {
                        f.addContent(current, newElem);
                    }

                    // Any declared namespaces?
                    for (int i = 0, len = r.getNamespaceCount(); i < len; ++i) {
                        String prefix = r.getNamespacePrefix(i);
                        if (prefix == null) {
                            prefix = "";
                        }
                        Namespace ns = Namespace.getNamespace(prefix, r.getNamespaceURI(i));

                        // JDOM has special handling for element's "own" ns:
                        if (prefix.equals(elemPrefix)) {
                            ; // already set by when it was constructed...
                        } else {
                            f.addNamespaceDeclaration(newElem, ns);
                        }
                    }

                    // And then the attributes:
                    for (int i = 0, len = r.getAttributeCount(); i < len; ++i) {
                        String prefix = r.getAttributePrefix(i);
                        Namespace ns;

                        if (prefix == null || prefix.length() == 0) {
                            // Attribute not in any namespace
                            ns = Namespace.NO_NAMESPACE;
                        } else {
                            ns = newElem.getNamespace(prefix);
                        }
                        Attribute attr = f.attribute(r.getAttributeLocalName(i),
                                                     r.getAttributeValue(i),
                                                     resolveAttrType2(r.getAttributeType(i)),
                                                     ns);
                        f.setAttribute(newElem, attr);
                    }
                    // And then 'push' new element...
                    current = newElem;
                }
                
                if (tmod != null) {
                    allowTextMods = tmod.allowModificationsAfter(r, evtType);
                }

                // Already added the element, can continue
                continue main_loop;

            // since building a fragment, should never see START_DOCUMENT, END_DOCUMENT, DTD		
            // case XMLStreamConstants.START_DOCUMENT:
            // case XMLStreamConstants.DTD:
	    // case XMLStreamConstants.END_DOCUMENT:
            
	    // Should never get these, from a stream reader:
            //case XMLStreamConstants.ATTRIBUTE:
            //case XMLStreamConstants.NAMESPACE:
            default:
                throw new XMLStreamException("Unrecognized iterator event type: " + r.getEventType() + 
					     "; should not receive such types (broken stream reader?)");
            }  // end switch(evtType)

            if (child != null) {
                if (current == null) {
                    // f.addContent(doc, child);
                } else {
                    f.addContent(current, child);
                }
            }
        }  // end main_loop

	return current;
    }


    /**
     *  reimplementing method here becuase StAXBilder.resolveAttrType() is declared private 
     *  renamed to resolveAttrType2() to avoid any ambiguity
     */
    protected static int resolveAttrType2(String typeStr) {
        if (typeStr != null && typeStr.length() > 0) {
            Integer I = (Integer) attrTypes.get(typeStr);
            if (I != null) {
                return I.intValue();
            }
        }
        return Attribute.UNDECLARED_TYPE;
    }

    public static void main(String[] args)  throws Exception  {
	String filename = "./data/das2_registry_sources.mod.xml";
	String[] test_args = new String[1];
	test_args[0] = filename;
	//	StAXBuilder.main(test_args);

        java.io.Reader r = new java.io.FileReader(filename);
        javax.xml.stream.XMLInputFactory f = javax.xml.stream.XMLInputFactory.newInstance();
        XMLStreamReader sr = f.createXMLStreamReader(r);
	XmlFragmentBuilder builder = new XmlFragmentBuilder();
	int frag_count = 0;
	Element source_frag = null;
	Element version_frag = null;
	while (sr.hasNext())  {
	    int eventid = sr.next();
	    if (eventid == XMLStreamConstants.START_ELEMENT)  {
		String elname = sr.getLocalName();
		if ((frag_count < 1) && (elname.equals("SOURCE")))  {
		    source_frag = builder.buildXmlFragment(sr);
		    frag_count++;
		}
		else if ((frag_count < 2) && (elname.equals("VERSION")))  {
		    version_frag = builder.buildXmlFragment(sr);
		    frag_count++;
		}
	    }
	}

        org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
        java.io.PrintWriter pw = new java.io.PrintWriter(System.out);
	System.out.println("***************");
        outputter.output(source_frag, pw);
	pw.println();
        pw.flush();	
	System.out.println("***************");
        outputter.output(version_frag, pw);
	pw.println();
        pw.flush();	
	System.out.println("***************");
    }

}