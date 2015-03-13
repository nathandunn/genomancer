package genomancer.trellis.das2.model;

/**
 *  
 *  Should look into more extensive representation of mime-type subparts, etc.
 *
 *  Also hould look at representing as javax.activation.MimeType from Java EE
 *    (though may be overkill to bring in Java EE stuff for just one class)
 */
public interface Das2FormatI {
/**
 * <p>Does ...</p>
 * 
 * 
 * @return 
 */
    public String getName();
/**
 * @return 
 */
    public String getMimeType();
}


