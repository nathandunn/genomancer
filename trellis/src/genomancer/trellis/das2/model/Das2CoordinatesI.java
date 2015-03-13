package genomancer.trellis.das2.model;

import java.net.URI;

/**
 * 
 * 
 */
public interface Das2CoordinatesI extends IdentifiableI  {
    // extends Das2CommonMetaAttributesI {

/**
 * <p>Does ...</p>
 * required
 */
    public String getTaxonomyID();
/**
 * <p>
 * From the sources documentation:
 * Returns the coordinate type.  
 * This refers to the "physical dimension" of the data. 
 * Currently the following categories are available: 
 * Chromosome, Clone, Contig, Gene_ID, NT_Contig, Protein Sequence, Protein Structure 
 * </p>
 *
 * required
 *
 * @return 
 * value of "source" attribute if present
 * null if not present
 */
    public String getCoordinateType();
/**
 * <p>Does ...</p>
 * required
 * 
 * @return 
 */
    public String getAuthority();
/**
 * <p>Does ...</p>
 * version - (optional) for genome assemblies the version of the build.
 * 
 * @return 
 * value of "version" attribute if present
 * null if not present
 */
    public String getBuildVersion();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public java.util.Date getCreated();
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public String getTestRange();

}


