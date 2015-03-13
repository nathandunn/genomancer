package genomancer.ucsc.das2.modelimpl;

import genomancer.trellis.das2.model.Das2CoordinatesI;
import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2TypesCapabilityI;
import genomancer.trellis.das2.model.Das2TypesResponseI;
import genomancer.vine.das2.client.modelimpl.Das2Coordinates;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2Type;
import genomancer.vine.das2.client.modelimpl.Das2TypesResponse;
import genomancer.vine.das2.client.modelimpl.Das2Version;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class UcscTypesCapability extends Das2GenericCapability implements Das2TypesCapabilityI  {
    boolean DEBUG = true;
    Das2TypesResponseI types_response = null;
    UcscVersion ucsc_version = null;
    /** for convenience, uri2type maps both absolute URI and local URI to type */
    Map<URI,Das2TypeI> uri2type = null;
    
    // child types 
    protected Das2Type type_UTR;
    protected Das2Type type_CDS;  // actually more appropriate term is CDS-segment
    protected Das2Type type_EXON;
    protected Das2Type type_WHOLECDS;  // actually more appropriate term is CDS, but need to distinguish from CDS-segment
    protected Das2Type type_UNKNOWN;

    boolean initialized = false;

    public UcscTypesCapability(UcscVersion version, Das2CoordinatesI coords, ResultSet rs) {
        super(version.getBaseURI(), (version.getLocalURIString()+"/types"), "types", version, coords);
	ucsc_version = version;
    }

    public Das2TypeI getType(URI type_uri)  {
	if (! initialized)  {
	    initTypes();
	}
	return uri2type.get(type_uri);
    }
    
    public Das2TypesResponseI getTypes() {
	if (! initialized)  {
	    initTypes();
	}
	return types_response;
    }

    public Das2FormatI getFormat(String format_name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected synchronized boolean initTypes()  {
	// only initialized once -- if want effect of re-initializing, must create new TypesCapability instead
	if (initialized)  { return true; }
	boolean success = false;
	uri2type = new HashMap<URI,Das2TypeI>();
	List<Das2TypeI> types = new ArrayList<Das2TypeI>();
	//	Set<String> table_names = new HashSet<String>();
	//	Set<String> merged_split_types = new HashSet<String>(); // types with ("all_" + type_name) table
	// map of track type names that have split tables to exemplar of table name
	Map<String, String> possible_split_types = new HashMap<String, String>();
	Map<String, String> group_name2label = new HashMap<String, String>();

	/** mapping of tableName field in trackDb (which is also used for type name)
	 *     to actual table name in database (or null for split fields...) 
	*/
	Map<String, String> type2table = new HashMap<String, String>();
	List<Das2TypeI> extra_types = new ArrayList();

	URI type_base_uri = this.getAbsoluteURI().resolve("./");
	type_UTR = new Das2Type(type_base_uri, "UTR", "UTR", null, null, "UTR", null, false);
	type_CDS = new Das2Type(type_base_uri, "CDS", "CDS", null, null, "CDS", null, false);
	type_EXON = new Das2Type(type_base_uri, "exon", "exon", null, null, "exon", null, false);
	type_WHOLECDS = new Das2Type(type_base_uri, "wholeCDS", "wholeCDS", null, null, "wholeCDS", null, false);
	type_UNKNOWN = new Das2Type(type_base_uri, "unknown", "unknown", null, null, "unknown", null, false);
	extra_types.add(type_UTR); extra_types.add(type_CDS); extra_types.add(type_UNKNOWN);
	for (Das2TypeI utype : extra_types)  {
	    uri2type.put(utype.getAbsoluteURI(), utype);
	    uri2type.put(utype.getLocalURI(), utype);
	}

	//	UcscSegmentsCapability seg_cap = (UcscSegmentsCapability)ucsc_version.getCapability("segments");
	//	Das2SegmentsResponseI seg_response = seg_cap.getSegments();
	//	List<Das2SegmentI> segments = seg_response.getSegments();
	//	Das2SegmentI first_seg = segments.get(0);
	//	String split_tester = first_seg.getLocalURIString() + "_";
	String merge_tester = "all_";
	//	System.out.println("split_tester: " + split_tester);

	String trackdb_query =  "select " + 
	    "tableName, " +   // local_uri_string (and table name)
	    "shortlabel, " +  // title
	    "type, " +        // UCSC table type
	    "longLabel, " +   // description
	    "grp "        +   // UCSC group (prepend to title for grouping?)
	    "from trackDb";
	String tables_query = "show tables";
	String groups_query = "select * from grp";

	try  {

	    Connection conn = ucsc_version.getDbConnection();
	    Statement stmt = conn.createStatement();
	    ResultSet tables = stmt.executeQuery(tables_query);
	    while (tables.next())  {
		String table_name = tables.getString(1);
		//		table_names.add(table_name);
		if (table_name.startsWith(merge_tester))  {   
		    String type_name = table_name.substring(merge_tester.length());
		    // it's possbile for there to be only an "all_*" table, a set of "seqid_*" tables, both, or neither
		    System.out.println("FOUND MERGED TABLE: " + type_name);
		    type2table.put(type_name, table_name);
		}
		//		else if (table_name.startsWith(split_tester))  { 
		else if (table_name.contains("_"))  {
		    int _index = table_name.lastIndexOf("_");
		    String type_name = table_name.substring(_index+1);
		    //		    String new_name = table_name.substring(split_tester.length());
		    if (! possible_split_types.containsKey(type_name))  {
			possible_split_types.put(type_name, table_name);
		    }
		}
		else  {
		    type2table.put(table_name, table_name);
		}
	    }
	    tables.close();
	    ResultSet groups = stmt.executeQuery(groups_query);
	    while (groups.next())  {
		String name = groups.getString("name");
		String label = groups.getString("label");
		group_name2label.put(name, label);
	    }
	    groups.close();
	    ResultSet rs = stmt.executeQuery(trackdb_query);
	    int net_or_chain = 0;
	    while (rs.next())  {
		String type_name = rs.getString("tableName");
		String track_type = rs.getString("type");
		String group = rs.getString("grp");
		if (track_type.startsWith("wig") || track_type.startsWith("wigMaf")) {
		    System.out.println("WARNING: skipping wig/wigMaf track: " + type_name);
		    continue;
		}
		/*
		  if (track_type.startsWith("bed ") ||  
		  // need space at end of bed to distinguish form begGraph, bed5FloatScore, Bed5FloatScoreFdr
		  track_type.startsWith("genePred") || 
		  track_type.startsWith("psl") )  {
		*/
		if (true)  {
		    if (track_type.startsWith("net") || 
			track_type.startsWith("chain") )  {
			net_or_chain++;
		    }
		    UcscType utype;
		    boolean is_split = possible_split_types.containsKey(type_name);
		    /** 
		     * for most non-split tables, table_name will be same as type_name 
		     *  but need mapping for cases like "all_*" tables
		     */
		    String table_name;
		    if (is_split)  {
			table_name = type_name;
		    }
		    else  {
			table_name = type2table.get(type_name);
		    }
		    if (table_name != null)  {
			if (is_split) {
			    String split_table_exemplar = possible_split_types.get(type_name);
			    // System.out.println("found split type: " + type_name + " , table: " + split_table_exemplar);
			    utype = new UcscType(type_base_uri, ucsc_version, rs, group, table_name, split_table_exemplar);
			}
			else  {
			    utype = new UcscType(type_base_uri, ucsc_version, rs, group, table_name);
			}
			types.add(utype);
		    }
		    else  {
			if (DEBUG)  {
			    System.out.println("WARNING: no table or split-tables found for type: " + 
					       type_name + ", skipping");
			}
		    }
		}
		else  {
		    if (DEBUG)  {
			System.out.println(" skipping track: " + type_name + 
					   ", track_type not yet handled: " + track_type);
		    }
		}
	    }
	    System.out.println("total chain+net types = " + net_or_chain);
	    rs.close();
	    stmt.close();
	    conn.close();
	    types_response = new Das2TypesResponse(type_base_uri, types, null);
	    for (Das2TypeI utype : types)  {
		uri2type.put(utype.getAbsoluteURI(), utype);
		uri2type.put(utype.getLocalURI(), utype);
	    }
	    success = true;
	    initialized = true;
	}
	catch (SQLException ex)  {
	    success = false;
	    ex.printStackTrace();
	}
	return success;
    }

}