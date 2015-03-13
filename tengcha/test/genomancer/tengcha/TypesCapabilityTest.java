/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package genomancer.tengcha;

import java.io.File;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.commons.io.FileUtils;

/**
 *
 * @author jtr4v
 */
public class TypesCapabilityTest {
    
    // http://docs.codehaus.org/display/JETTY/ServletTester
    static ServletTester tester = new ServletTester();
    static HttpTester request = new HttpTester();
    static HttpTester response = new HttpTester();
    
    public TypesCapabilityTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        tester.setContextPath("/trellis");
	ServletHolder sh = tester.addServlet("genomancer.trellis.das2.server.TrellisDas2Servlet", "/*");
        
        // the following is sort of the initial call to trellis to get sources, then 
        // we can do follow-up queries for types capability
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
    
    /**
     * Test of getTypes method, of class TypesCapability.
    */
    @Test
    public void typesUriShouldRespondWith200() {
        request.setMethod("GET");
	request.setHeader("Host","tester");
	request.setURI("/trellis/A.mellifera/types");
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
    public void typesUriOnNonexistentOrganismShouldRespondWith400() {
        request.setMethod("GET");
	request.setHeader("Host","tester");
	request.setURI("/trellis/E.unicornus/types"); // no such organism in db
	request.setVersion("HTTP/1.0");
        
        try {
	    response.parse(tester.getResponses(request.generate()));
	    assertTrue(response.getMethod()==null);
	    assertEquals(400,response.getStatus());          
	}
	catch (Exception ex) {
	}        
    }
       
    @Test
    public void typesUriShouldRespondWithCorrectXml() {
        File correctResponse = new File(genomancer.tengcha.Config.TEST_SUPPORT_PATH + "correct_trellis_A.mellifera_types_response.xml");
        
        request.setMethod("GET");
	request.setHeader("Host","tester");
	request.setURI("/trellis/A.mellifera/types");
	request.setVersion("HTTP/1.0");
        
        try {
            File responseOutFile = new File("correct_trellis_A.mellifera_types_response.xml.response");
            String responseString = response.getContent();
            
            Boolean writeOutFile = true;
            if ( writeOutFile ){
                FileUtils.writeStringToFile(responseOutFile, responseString, "UTF-8");
            }
            
            response.parse(tester.getResponses(request.generate()));
            assertTrue(response.getMethod() == null);
            assertEquals("Response to /trellis/A.mellifera/types differs from what I expected:",
                    replaceLeadingTrailingSpace( FileUtils.readFileToString(correctResponse, "utf-8") ),
                    replaceLeadingTrailingSpace( responseString ) );            
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
