package genomancer.vine.das2.client.modelimpl;

import genomancer.trellis.das2.Das2Constants;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2TypeI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;



public class Das2Type extends Das2CommonData implements Das2TypeI {
    
    URI ontology_term;
    String method;
    boolean is_searchable;
    LinkedHashMap<String, Das2FormatI> formats = new LinkedHashMap<String, Das2FormatI>();
    
    public Das2Type(URI base_uri, 
		    String local_uri, 
		    String title, 
		    String description, 
		    String info_url, 
		    String ontology_term_name, 
		    String method, 
		    boolean is_searchable)  {
	super(base_uri, local_uri, title, description, info_url);
	this.base_uri = base_uri;
	//	System.out.println("Type base_uri: " + base_uri.toString());
	//	System.out.println("Type ontology term: " + ontology_term_name);
	if (ontology_term_name == null)  { 
	    ontology_term = Das2Constants.ROOT_TYPE_ONTOLOGY_URI;
	}
        else if (this.getBaseURI() == null) {
            try {
                this.ontology_term = new URI(ontology_term_name);
            } catch (URISyntaxException ex) {
                Logger.getLogger(Das2Type.class.getName()).log(Level.SEVERE, null, ex);
            }
	}
        else  {
            this.ontology_term = getBaseURI().resolve(ontology_term_name);
        }
	this.method = method;
	this.is_searchable = is_searchable;
    }


    /** 
     *  Das2TypeI implementation 
     *  @deprecated
     *  deprecating getSOAccession() since "SO:00001234" style accessions 
     *  can be used as URIs for ontology term
     */
    public String getSOAccession() { return ontology_term.toString(); }

    /**  Das2TypeI implementation */
    public URI getOntologyTerm() { return ontology_term; }

    /**  Das2TypeI implementation */
    public String getMethod() { return method; }

    /**  Das2TypeI implementation */
    public boolean isSearchable() { return is_searchable; }

    /**  Das2TypeI implementation */
    public Collection<Das2FormatI> getFormats() { return formats.values(); }

    public void addFormat(Das2FormatI format)  { formats.put(format.getName(), format); }

    public Das2FormatI getFormat(String format_name) {
        return formats.get(format_name);
    }

}