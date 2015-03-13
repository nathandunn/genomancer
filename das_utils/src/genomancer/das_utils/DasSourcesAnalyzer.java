package genomancer.das_utils;

import genomancer.ivy.das.client.modelimpl.Das1SourcesCapability;
import genomancer.ivy.das.model.Das1EntryPointsCapabilityI;
import genomancer.ivy.das.model.Das1FeaturesCapabilityI;
import genomancer.ivy.das.model.Das1SequenceCapabilityI;
import genomancer.ivy.das.model.Das1StylesheetCapabilityI;
import genomancer.ivy.das.model.Das1TypesCapabilityI;
import genomancer.trellis.das2.model.Das2CapabilityI;
import genomancer.trellis.das2.model.Das2CoordinatesI;
import genomancer.trellis.das2.model.Das2SourceI;
import genomancer.trellis.das2.model.Das2SourcesResponseI;
import genomancer.trellis.das2.model.Das2VersionI;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DasSourcesAnalyzer  {
    static String default_das1_registry_sources = "http://www.dasregistry.org/das1/sources";
    // static String default_das1_registry_sources = "file:./data/das1_registry_sources.xml";

    String das1_sources_query;
    Das1SourcesCapability sources_cap;

    public static void main(String[] args) throws URISyntaxException  {
	DasSourcesAnalyzer analyzer = new DasSourcesAnalyzer(default_das1_registry_sources);
	analyzer.analyze();
    }

    public DasSourcesAnalyzer(String das1_sources_query) throws URISyntaxException  {
	this.das1_sources_query = das1_sources_query;
      sources_cap = new Das1SourcesCapability(das1_sources_query);
    }

    public void analyze()  {
	System.out.println("querying for DAS1 sources: " + sources_cap.getAbsoluteURI());
	Das2SourcesResponseI sources_response = sources_cap.getSources();
	List<Das2SourceI> sources = sources_response.getSources();
	System.out.println("total sources: " + sources.size());
	int version_count = 0;
	int coord_count = 0;
	int type_cap_count = 0;
	int versions_missing_coords = 0;
	int versions_multiple_coords = 0;
	int entry_point_no_coords = 0;
	int coord_uri_diff_entry_points = 0;
	int ds40_count = 0;

	Map<URI, Das2CoordinatesI> uri2coords = new HashMap<URI, Das2CoordinatesI>();
	Map<URI, Das1EntryPointsCapabilityI> coord_uri_to_entry_cap = new HashMap<URI, Das1EntryPointsCapabilityI>();

	for (Das2SourceI source : sources)  {
	    List<Das2VersionI> versions = source.getVersions();
	    version_count += versions.size();
	    for (Das2VersionI version : versions)  {
		List<Das2CoordinatesI> coords = version.getCoordinates();
		List<Das2CapabilityI> caps = version.getCapabilities();

		Das1EntryPointsCapabilityI entry_points_cap = null;
		Das1SequenceCapabilityI sequnce_cap = null;
		Das1TypesCapabilityI types_cap = null;
		Das1FeaturesCapabilityI features_cap = null;
		Das1StylesheetCapabilityI stylesheet_cap = null;

		for (Das2CapabilityI cap : caps)  {
		    if (cap instanceof Das1EntryPointsCapabilityI)  {
			entry_points_cap = (Das1EntryPointsCapabilityI)cap;
		    }
		    else if (cap instanceof Das1TypesCapabilityI) {
			type_cap_count++;
		    }
		}

		if (coords.size() == 0)  { 
		    versions_missing_coords++; 
		    if (entry_points_cap != null)  { entry_point_no_coords++; }
		}
		if (coords.size() > 1)  { versions_multiple_coords++; }
		coord_count += coords.size();
		for (Das2CoordinatesI coord : coords)  {
		    URI coord_uri = coord.getAbsoluteURI();
		    // 
		    uri2coords.put(coord_uri, coord);
		    if (entry_points_cap != null)  {
			URI entry_points_uri = entry_points_cap.getAbsoluteURI();
			Das1EntryPointsCapabilityI old_entry_points = coord_uri_to_entry_cap.get(coord_uri);
			if (old_entry_points != null)  {
			    if (! entry_points_uri.equals(old_entry_points.getAbsoluteURI()))  {
				coord_uri_diff_entry_points++;
			    }
			}
			coord_uri_to_entry_cap.put(coord_uri, entry_points_cap);
		    }
		}
	    }
	}

	int coord_uri_no_entry_point = uri2coords.size() - coord_uri_to_entry_cap.size();

	System.out.println("total versions: " + version_count);
	System.out.println("type cap count: " + type_cap_count);
	System.out.println("total coords: " + coord_count);
	System.out.println("unique coords: " + uri2coords.size());
	System.out.println("coord URI with no coord having entry_points: " + coord_uri_no_entry_point);
	System.out.println("same coord URI, different entry_points: " + coord_uri_diff_entry_points);
	System.out.println("versions with entry_points, no coords: " + entry_point_no_coords);
	System.out.println("versions with no coords: " + versions_missing_coords);
	System.out.println("versions with multiple coords: " + versions_multiple_coords);
	System.out.println("CS_DS40 coords count: " + ds40_count);

	List<Das2CoordinatesI> coords_no_entry_points = new ArrayList<Das2CoordinatesI>();
	for (URI coord_uri : uri2coords.keySet())  {
	    if (coord_uri_to_entry_cap.get(coord_uri) == null)  {
		coords_no_entry_points.add( uri2coords.get(coord_uri) );
	    }
	}	
	System.out.println();
	System.out.println("coords with no matching entry points");
	printCoords(coords_no_entry_points);
    }

    public void printCoords(List<Das2CoordinatesI> coords)  {
	for (Das2CoordinatesI coord : coords)  { printCoords(coord);  }
    }

	    
    public void printCoords(Das2CoordinatesI coord)  {
	// abbreviating coord uri for now
	String local_name = coord.getLocalURIString();
	int last_slash = local_name.lastIndexOf("/");
	if ((last_slash >= 0) && (last_slash != local_name.length()-1))  {
	    local_name = local_name.substring(last_slash+1); 
	}
	System.out.println("coords: name = " + local_name + 
			   ", authority = " + coord.getAuthority() + 
			   ", build_version = " + coord.getBuildVersion() + 
			   ", type = " + coord.getCoordinateType() + 
			   ", taxid = " + coord.getTaxonomyID());
    }

    /*
		    if (coord_uri.toString().endsWith("CS_DS85"))  {
			//			System.out.println("CS_DS85 coord");
			printCoords(coord);
			for (Das2CapabilityI cap : caps)  { System.out.println("    " + cap); }
			//			System.out.println("CS_DS85, entry points cap: " + entry_points_cap);
		    }
		    else if (coord_uri.toString().endsWith("CS_DS40"))  {
			//			System.out.println("CS_DS40 coord");
			// printCoords(coord);
			ds40_count++;
			if (entry_points_cap != null)   { 
			    printCoords(coord);
			    System.out.println("   " + entry_points_cap.getAbsoluteURI());
			}
		    }
    */

				// System.out.println(version.getLocalURIString());
				// printCoords(coord);
				// System.out.println("prior entry_points: " + old_entry_points.getAbsoluteURI());
				// System.out.println("new   entry_points: " + entry_points_uri);
				// System.out.println();


}