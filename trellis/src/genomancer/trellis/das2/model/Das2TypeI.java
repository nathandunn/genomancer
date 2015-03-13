package genomancer.trellis.das2.model;

import java.net.URI;
import java.util.Collection;

/**
 * 
 * 
 */
public interface Das2TypeI extends Das2CommonDataI {
/**
 * <p>Does ...</p>
 * 
 */
    public URI getOntologyTerm();
/**
 * Should stop using SOAccession and just have SO accession in OntologyTerm, 
 *   since accessions are "SO:00001234" and therefore legal URIs
 * 
 */
    public  String getSOAccession();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public String getMethod();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public boolean isSearchable();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public Collection<Das2FormatI> getFormats();

    public Das2FormatI getFormat(String format_name);

}


