package genomancer.trellis.das2.server;


import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2PropertyI;
import genomancer.trellis.das2.model.Das2TypeI;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import org.jdom.Element;

public class TypeWrapper implements Das2TypeI  {
    Das2TypeI otype;
    LinkedHashMap<String, Das2FormatI> formats = new LinkedHashMap<String, Das2FormatI>();
    TypeWrapper(Das2TypeI otype)  {
	this.otype = otype;
	for (Das2FormatI format : otype.getFormats())  {
	    formats.put(format.getName(), format);
	}
    }
    public Collection<Das2FormatI> getFormats() {
	return formats.values();
    }

    public Das2FormatI getFormat(String format_name)  {
	return formats.get(format_name);
    }

    public void addFormat(Das2FormatI format)  {
	formats.put(format.getName(), format);
    }

    public URI getOntologyTerm() { return otype.getOntologyTerm(); }
    public String getSOAccession() { return otype.getSOAccession(); }
    public String getMethod() { return otype.getMethod(); }
    public boolean isSearchable() { return otype.isSearchable(); }
    public String getInfoURL() { return otype.getInfoURL(); }
    public String getTitle() { return otype.getTitle(); }
    public String getDescription() { return otype.getDescription(); }
    public List<Das2PropertyI> getProperties() { return otype.getProperties(); }
    public List<Element> getAdditionalData() { return otype.getAdditionalData(); }
    public URI getAbsoluteURI() { return otype.getAbsoluteURI(); }
    public URI getLocalURI() { return otype.getLocalURI(); }
    public String getLocalURIString() { return otype.getLocalURIString(); }
    public String getAbsoluteURIString() { return otype.getAbsoluteURIString(); }
    public URI getBaseURI() { return otype.getBaseURI(); }

 
}