/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package genomancer.tengcha;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author jtr4v
 */
public class Config {

    public static final String TENGCHA_BASEDIR = "/Users/jtr4v/projects/genomancer/tengcha/";
    public static String HIBERNATE_CFG_XML = TENGCHA_BASEDIR + "src/hibernate_cfg.xml";

    // the follow is so tengcha can find the test support files, such as expected XML output for a given URI request
    public static String TEST_SUPPORT_PATH = TENGCHA_BASEDIR + "test/support/";
    
    // this is the sequence ontology term for the reference sequences (i.e. the cvterm name for the reference sequences in the feature table)
    public static String REFERENCE_SEQUENCE_SO_TERM = "scaffold"; 

    // the following is the name of the controlled vocabulary term to which 
    // REFERENCE_SEQUENCE_SO_TERM (above) belongs.
    // Should be able to do the following select statement to determine what yours is:
    //       select cv.name from cv, cvterm where cv.cv_id = cvterm.cv_id and cvterm.name = 'YOUR REFERENCE SEQUENCE SO TERM ABOVE';
    public static String REFERENCE_SEQUENCE_CV_NAME = "sequence"; 
 
    // the following are the cvterms which tengcha uses to retrieve children of features
    public static List<String> CHILD_PARENT_RELATIONSHIP_CV_NAME = Arrays.asList("part_of", "derives_from");

}
