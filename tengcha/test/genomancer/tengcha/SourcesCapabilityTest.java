/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package genomancer.tengcha;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

// testing stuff
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import org.custommonkey.xmlunit.Diff;

import org.mortbay.jetty.testing.HttpTester; 
import org.mortbay.jetty.testing.ServletTester;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 *
 * @author jtr4v
 */
public class SourcesCapabilityTest {

    // http://docs.codehaus.org/display/JETTY/ServletTester
    static ServletTester tester = new ServletTester();
    static HttpTester request = new HttpTester();
    static HttpTester response = new HttpTester();

    public SourcesCapabilityTest() {
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
        tester.stop();
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void sourcesUriShouldRespondWith200() {       
	request.setMethod("GET");
	request.setHeader("Host","tester");
	request.setURI("/trellis/sources");
	request.setVersion("HTTP/1.0");

	try {
	    response.parse(tester.getResponses(request.generate()));
	    assertTrue(response.getMethod()==null);
	    assertEquals(200,response.getStatus());
        }
	catch (Exception ex) {
	    ex.printStackTrace();
	}
    }
            
    @Test
    public void sourcesUriShouldRespondWithCorrectXml() throws IOException {
        File correctResponse = new File(genomancer.tengcha.Config.TEST_SUPPORT_PATH + "correct_trellis_A.mellifera_sources_response.xml");
        FileReader fr = null;
        Diff diff = null;
        
        request.setMethod("GET");
	request.setHeader("Host","tester");
	request.setURI("/trellis/sources");
	request.setVersion("HTTP/1.0");
                
        try {
            response.parse(tester.getResponses(request.generate()));
            assertTrue(response.getMethod() == null);
            assertEquals("Response to /trellis/A.mellifera/sources differs from what I expected:",
                    replaceLeadingTrailingSpace( FileUtils.readFileToString(correctResponse, "utf-8") ),
                    replaceLeadingTrailingSpace( response.getContent() ) );
        } catch (Exception ex) {
            ex.printStackTrace();
        }         
    }
    
    public String replaceLeadingTrailingSpace(String thisString){        
        thisString = thisString.replaceAll("\\s+$", "");
        thisString = thisString.replaceAll("^\\s+", "");
        return thisString;
    }  
            
}
