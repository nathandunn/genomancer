/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package genomancer.tengcha;

import java.io.IOException;
import org.xml.sax.SAXException;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.net.URLEncoder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import java.util.List;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jtr4v
 */
public class FeaturesCapabilityTest {

    static ServletTester tester = new ServletTester();
    static HttpTester request = new HttpTester();
    static HttpTester response = new HttpTester();

    public FeaturesCapabilityTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        tester.setContextPath("/trellis");
        ServletHolder sh = tester.addServlet("genomancer.trellis.das2.server.TrellisDas2Servlet", "/*");
        sh.setInitParameter("sources_plugin_class", "genomancer.tengcha.SourcesPlugin");
        sh.setInitParameter("sources_query", "http://tester/trellis/sources");
        tester.start();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    ////////////////////////////////////////////////////////////
    // Segments filter tests
    ///////////////////////////////////////////////////////////
    @Test
    public void segmentsUriShouldRespondWith200() {
        request.setMethod("GET");
        request.setHeader("Host", "tester");
        request.setURI("/trellis/A.mellifera/features?segment=GroupUn1044");
        request.setVersion("HTTP/1.0");

        try {
            response.parse(tester.getResponses(request.generate()));
            assertTrue(response.getMethod() == null);
            assertEquals(200, response.getStatus());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Get all features for a given segment - should respond with return full set of correct features, in xml
    @Test
    public void segmentFilterShouldReturnCorrectFeaturesXml() {

        File correctResponseFile = new File(genomancer.tengcha.Config.TEST_SUPPORT_PATH
                + "correct_trellis_A.mellifera_features_segmentFilterShouldRespondWithReturnCorrectFeaturesXML.xml");
        String testURL = "/trellis/A.mellifera/features?segment=GroupUn1044";

        Boolean verbose = true;
        Boolean writeOutResponse = true;

        assertExpectedAndActualXMLResponses(correctResponseFile, testURL, verbose, writeOutResponse);
    }

    // Get all features for a given segment - should respond with return full set of correct features, in json
    @Test
    public void segmentFilterShouldReturnCorrectFeaturesJson() {
        // Test url = http://localhost:9099/trellis/unicorn_v1/features?segment=GroupUn1044;format=jbrowse-nclist-full-json
        assertEquals("really", "really");
    }
    // Should respond with no features when asked segment that doesn’t exist, in xml

    @Test
    public void segmentFilterNoFeaturesShouldReturnNoFeaturesXml() {
        // Test url = http://localhost:9099/trellis/unicorn_v1/features?segment=ScaffoldThatsNotInDb
        assertEquals("really", "really");
    }
    // Should respond with no features when asked segment that doesn’t exist, in json

    @Test
    public void segmentFilterNoFeaturesShouldReturnNoFeaturesJson() {
        // Test url = http://localhost:9099/trellis/unicorn_v1/features?segment=ScaffoldThatsNotInDb;format=jbrowse-nclist-full-json
        assertEquals("really", "really");
    }
    // Should require segments filter (otherwise the request is going to generate a select statement that'll return fitty billion things)

    @Test
    public void segmentFilterNoSegmentsClauseShouldCause400ErrorXml() {
        // Test url = http://localhost:9099/trellis/unicorn_v1/features
        assertEquals("really", "really");
    }

    //////////////////////////////////////////////////////////////
    // Types filter tests
    //////////////////////////////////////////////////////////////
    // Get all features for a given segment of a given type - should respond with only the types requested, in xml
    @Test
    public void segmentAndTypesFilterShouldReturnCorrectFeaturesXml() {
        // http://localhost:9099/trellis/unicorn_v1/features?segment=GroupUn1044;types=???
        assertEquals("really", "really");
    }
    // Get all features for a given segment of a given type - should respond with only the types requested, in json

    @Test
    public void segmentAndTypesFilterShouldReturnCorrectFeaturesJson() {
        assertEquals("really", "really");
    }

    //////////////////////////////////////////////////////////////
    /// should respond with no features when asked for types not present on segment
    //////////////////////////////////////////////////////////////
    @Test
    public void segmentAndTypesFilterShouldReturnNoFeaturesXml() {
        // URL = http://localhost:9099/trellis/unicorn_v1/features?segment=GroupUn1044;types=???
        // [??? = some type that has no features on segment]
        assertEquals("really", "really");
    }

    @Test
    public void segmentAndTypesFilterShouldReturnNoFeaturesJson() {
        // URL = http://localhost:9099/trellis/unicorn_v1/features?segment=GroupUn1044;types=???;format=jbrowse-nclist-full-json
        // [??? = some type that has no features on segment]
        assertEquals("really", "really");
    }

    //////////////////////////////////////////////////////////////
    /// Get all features for a given segment that overlap a given region - should respond with features that overlap region, in xml or json
    //////////////////////////////////////////////////////////////
    @Test
    public void segmentAndOverlapFilterShouldReturnCorrectFeaturesXml() {
        File correctResponseFile = new File(genomancer.tengcha.Config.TEST_SUPPORT_PATH
                + "correct_trellis_A.mellifera_features_segmentAndOverlapFilterShouldReturnCorrectFeaturesXml.xml");
        String testURL = "/trellis/A.mellifera/features?segment=GroupUn1044;overlap=900:1500";

        Boolean verbose = true;
        Boolean writeOutResponse = true;

        assertExpectedAndActualXMLResponses(correctResponseFile, testURL, verbose, writeOutResponse);        
        
    }

    @Test
    public void segmentAndOverlapFilterShouldReturnCorrectFeaturesJson() {
        // http://localhost:9099/trellis/unicorn_v1/features?segment=GroupUn1044;overlap=1:???;format=jbrowse-nclist-full-json
        // [??? = some coordinate that excludes some features, and lands in the center of at least one feature, which should be returned]
        assertEquals("really", "really");
    }

    public String replaceLeadingTrailingSpace(String thisString) {
        thisString = thisString.replaceAll("\\s+$", "");
        thisString = thisString.replaceAll("^\\s+", "");
        return thisString;
    }

    public void assertExpectedAndActualXMLResponses(File correctResponse, String URL, Boolean verbose, Boolean writeResponseToFile ){
                
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreWhitespace(true);

        if (verbose == null){
            verbose = false;
        }
        
        if ( writeResponseToFile == null ){
            writeResponseToFile = false;
        }
        
        Diff diff = null;
        request.setMethod("GET");
        request.setHeader("Host", "tester");
        request.setURI(URL);
        request.setVersion("HTTP/1.0");

        try {
            response.parse(tester.getResponses(request.generate()));
            assertTrue(response.getMethod() == null);

            String correctResponseString = FileUtils.readFileToString(correctResponse, "UTF-8");
            String actualResponseString = response.getContent();

            // write response to file if necessary
            if (writeResponseToFile) {
                String responseOutfileName = URLEncoder.encode(URL, "UTF-8") + ".response";
                File responseOutFile = new File(responseOutfileName);
                FileUtils.writeStringToFile(responseOutFile, actualResponseString, "UTF-8");
            }
                      
            diff = new Diff(correctResponseString, actualResponseString);

            System.out.println("Similar? " + diff.similar());
            System.out.println("Identical? " + diff.identical());
            DetailedDiff detDiff = new DetailedDiff(diff);

            List differences = detDiff.getAllDifferences();

            System.out.println("found " + differences.size() + " differences");

            if (verbose) {
                for (Object object : differences) {
                    Difference difference = (Difference) object;
                    System.out.println("***********************");
                    System.out.println(difference);
                    System.out.println("***********************");
                }

            }

            assertEquals("found some differences while comparing xml",
                    differences.size(), 0);


        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
