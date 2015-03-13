/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package genomancer.tengcha;

import org.apache.commons.io.FileUtils;
import java.io.File;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import java.net.URI;
import java.util.List;
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
public class SegmentsCapabilityTest {
    
    static ServletTester tester = new ServletTester();
    static HttpTester request = new HttpTester();
    static HttpTester response = new HttpTester();
    
    public SegmentsCapabilityTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
	tester.setContextPath("/trellis");
	ServletHolder sh = tester.addServlet("genomancer.trellis.das2.server.TrellisDas2Servlet", "/*");
	sh.setInitParameter("sources_plugin_class", "genomancer.tengcha.SourcesPlugin" );
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
    
    @Test
    public void segmentsUriShouldRespondWith200() {
        request.setMethod("GET");
	request.setHeader("Host","tester");
	request.setURI("/trellis/A.mellifera/segments");
	request.setVersion("HTTP/1.0");
        
        try {
	    response.parse(tester.getResponses(request.generate()));
	    assertTrue(response.getMethod()==null);
	    assertEquals(200,response.getStatus());          
	}
	catch (Exception ex) {
	}        
    }
    
    @Test
    public void segmentsUriShouldRespondWithCorrectXml() {
        File correctResponse = new File(genomancer.tengcha.Config.TEST_SUPPORT_PATH + "correct_trellis_A.mellifera_segments_response.xml");
        
        request.setMethod("GET");
	request.setHeader("Host","tester");
	request.setURI("/trellis/A.mellifera/segments");
	request.setVersion("HTTP/1.0");
        
        try {            
            File responseOutFile = new File("correct_trellis_A.mellifera_segments_response.xml.response");
            String responseString = response.getContent();
            
            Boolean writeOutFile = false;
            if ( writeOutFile ){
                FileUtils.writeStringToFile(responseOutFile, responseString, "UTF-8");
            }
            
            response.parse(tester.getResponses(request.generate()));
            assertTrue(response.getMethod() == null);
            assertEquals("Response to /trellis/A.mellifera/segments differs from what I expected:",
                    replaceLeadingTrailingSpace( FileUtils.readFileToString(correctResponse, "utf-8") ),
                    replaceLeadingTrailingSpace( responseString ) );
        } catch (Exception ex) {
            ex.printStackTrace();
        } 
    
    }
        
    @Test
    public void segmentsUriOnNonexistentOrganismShouldRespondWith400() {
        request.setMethod("GET");
	request.setHeader("Host","tester");
	request.setURI("/trellis/E.unicornus/segments"); // no such organism in db
	request.setVersion("HTTP/1.0");
        
        try {
	    response.parse(tester.getResponses(request.generate()));
	    assertTrue(response.getMethod()==null);
	    assertEquals(400,response.getStatus());          
	}
	catch (Exception ex) {
	}        
    }
    
    public String replaceLeadingTrailingSpace(String thisString){        
        thisString = thisString.replaceAll("\\s+$", "");
        thisString = thisString.replaceAll("^\\s+", "");
        return thisString;
    }
    
}
