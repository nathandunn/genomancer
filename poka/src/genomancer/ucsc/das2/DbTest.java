package genomancer.ucsc.das2;

import genomancer.trellis.das2.model.Das2FeaturesCapabilityI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2SegmentsCapabilityI;
import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import genomancer.trellis.das2.model.Das2SourceI;
import genomancer.trellis.das2.model.Das2SourcesResponseI;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2TypesCapabilityI;
import genomancer.trellis.das2.model.Das2TypesResponseI;
import genomancer.trellis.das2.model.Das2VersionI;
import genomancer.ucsc.das2.modelimpl.UcscSourcesCapability;
import genomancer.util.MemCheck;
import genomancer.vine.das2.client.modelimpl.Das2FeaturesCapability;
import genomancer.vine.das2.client.modelimpl.Das2FeaturesQuery;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

public class DbTest  {
    static boolean RESTRICT_VERSIONS = false;
    static boolean REPORT_LAST_MODIFIED = false;
    static boolean REPORT_ANNOT_COUNT = false;
    static boolean USE_LOCAL_DBHOST = false;
    static HashSet version_test_set = new HashSet();
    static List<String> test_versions;
    static  { 
	//	test_versions = Arrays.asList(new String[] { "apiMel2", "bosTau4" });
	test_versions = Arrays.asList(new String[] { "hg19" });
	for (String version : test_versions)  { version_test_set.add(version); }
    }

    UcscSourcesCapability sources_cap;
    Das2SourcesResponseI sources_response;

    public static void main(String[] args)  {
	DbTest tester = new DbTest();
	tester.testDb();
    }

    public UcscSourcesCapability initialize()  {
	try  {
	Map<String, String> sources_params = new HashMap<String, String>();
	if (USE_LOCAL_DBHOST)  { sources_params.put("dbhost", "localhost:3306"); }
	else  { sources_params.put("dbhost", "genome-mysql.cse.ucsc.edu"); }
	sources_params.put("dbuser", "genomep");
	sources_params.put("dbpassword", "password");
	sources_cap = new UcscSourcesCapability(new URI("test:ucsc/das2/"), "sources", sources_params);
	}
	catch (Exception ex)  { ex.printStackTrace(); }
	return sources_cap;
    }

    public void testDb()  {
	MemCheck memer = new MemCheck();
	memer.start();
        try {
	    int source_count = 0;
	    int version_count = 0;
	    int type_count = 0;
	    int segment_count = 0;
	    initialize();

            sources_response = sources_cap.getSources();
            List<Das2SourceI> sources = sources_response.getSources();
	    source_count = sources.size();
            System.out.println("sources count: " + source_count);
	    for (Das2SourceI source : sources)  {
		List<Das2VersionI> versions = source.getVersions();
		version_count += versions.size();
	    }
	    System.out.println("version count: " + version_count);

	    int current_version = 0;
	    for (Das2SourceI source : sources)  {
		System.out.println("source: " + source.getLocalURIString());
		for (Das2VersionI version : source.getVersions())  {
		    try  {
			current_version++;
			String version_name = version.getLocalURIString();
			if ((! RESTRICT_VERSIONS)  || (version_test_set.contains(version_name))) {
			    System.out.println("======================================");
			    System.out.println("SUMMARY INFO FOR: " + version.getLocalURIString());
			    Das2TypesCapabilityI types_cap = (Das2TypesCapabilityI)version.getCapability("types");
			    Das2FeaturesCapabilityI feats_cap = (Das2FeaturesCapabilityI)version.getCapability("features");
			    Das2TypesResponseI types_response = types_cap.getTypes();
			    List<Das2TypeI> types = types_response.getTypes();
			    type_count += types.size();
			    System.out.println("     types: " + types.size());
			    for (Das2TypeI type : types)  {
				String tname = type.getLocalURIString();
				if (REPORT_LAST_MODIFIED)  {
				    Das2FeaturesQuery fquery = new Das2FeaturesQuery();
				    // fquery.addType(type.getLocalURI());
				    fquery.addType(type.getAbsoluteURI());
				    long last_modified = feats_cap.getLastModified(fquery);
				    if (last_modified >= 0)  {
					System.out.println(tname + " last modified: " + (new Date(last_modified)));
				    }
				    else  {
					System.out.println("LAST MODIFIED TIME UNKNOWN for type: " + tname);
				    }
				}
				if (REPORT_ANNOT_COUNT)  {
				    Das2FeaturesQuery fquery = new Das2FeaturesQuery();
				    fquery.addType(type.getAbsoluteURI());
				    int annot_count = feats_cap.getFeaturesCount(fquery);
				    System.out.println(tname + " annot count: " + annot_count);
				}
			    }
			    /*
			      Das2SegmentsCapabilityI segments_cap = 
			      (Das2SegmentsCapabilityI)version.getCapability("segments");
			      Das2SegmentsResponseI segments_response = segments_cap.getSegments();
			      List<Das2SegmentI> segments = segments_response.getSegments();
			      segment_count += segments.size();
			    */
			    // System.out.println("     segments: " + segments.size() + ", total: " + segment_count);
			    // memer.report();
			    System.out.println("======================================");
			}
		    }
		    catch (Exception ex)  {
			ex.printStackTrace();
		    }
		}
	    }
	    memer.report();
	    System.out.println();
	    System.out.println("total type count: " + type_count);

	    //   } catch (URISyntaxException ex) {
	} catch (Exception ex) {
            Logger.getLogger(DbTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void testCds()  {
	Das2FeaturesCapabilityI feats_cap;
	// Das2TypesCapabilityI types_cap = initialize();
	initialize();
	// types_cap.getType(new URI("hinv70Coding"));
    }

} 
