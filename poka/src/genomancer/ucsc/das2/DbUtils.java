package genomancer.ucsc.das2;

//import java.io.*;
import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.trellis.das2.model.Das2LocationRefI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.ucsc.das2.modelimpl.UcscSequence;
import genomancer.ucsc.das2.modelimpl.UcscType;
import java.net.URI;
import java.util.*;
import java.sql.*;


public class DbUtils  {

    /** NOT YET IMPLEMENTED */
    /*
      public static ResultSet nonRangeQuery(
      Connection conn,
      Das2Type type, 
      List nameglobs, 
      List segments, 
      String extraWhere, 
      boolean sort,
      String fields )  throws SQLException  {
      return null;
      }
    */

    /*
      public static ResultSet rangeQuery(
      Connection conn, 
      Das2Type type,  
      int start, 
      int end, 
      String extraWhere,
      boolean sort,
      String fields)  throws SQLException {
    */
					  

    /** 
     *   Based on /kent/src/hg/lib/hdb.c hExtendedRangeQuery()
     *
     *   If there are any range filters (overlaps, inside, excludes), then must be one and only one segment, 
     *      shared by all the overlaps/insides/excludes filters
     *
     *   Only optimizing with bin index for the overlaps filters (not insides or excludes)
     *
     *   Adding restrictions on number of rows returned
     *   Option A:
     *     Use MySQL found_rows(), no limits:
     *     1) select * from ......
     *     2) select found_rows() ==> count of rows returned in previous select
     *   Option B:  (might be faster?)
     *     Use MySQL found_rows() with sql_calc_found_rows and limits
     *     1) select SQL_CALC_FOUND_ROWS * from ... LIMIT x
     *     2) select found_rows() ==> count of total rows (1) would have returned without limit
     *   Option C:
     *     same as above but without SQL_CALC_FOUND_ROWS, (2) will return x if limit is reached
     *   Option D: (more standard SQL, but slowest due to doubling main query
     *     1) select count(1) from .... ==> count of rows would have returnedd
     *     2) select * from ...
     *     could add LIMIT to (1) for another option
     *
     */
    public static DbQueryResult rangeQuery(
				       Connection conn, 
				       //				       String root_table_name, 
				       UcscType type, 
				       UcscSequence segment, 
				       List<Das2LocationRefI> overlaps, // Das2Locations that features must overlap (OR'd)
				       List<Das2LocationRefI> insides,  //  Das2Locations that features must be inside (OR'd)
				       List<Das2LocationRefI> excludes, // Das2Locations that feature must _not_ overlap (AND'd)
				       String extraWhere,
				       // max_rows_threshold ==> if rows returned >= max_rows_threshold, then 
				       //      return a "result too large" error??
				       Integer max_rows_threshold, 
				       boolean sort,
				       String fields)  throws SQLException { 
	//  left out int *retRowOffset) [ Returns offset past bin field ] -- doesn't appear necessary
	StringBuffer buf = new StringBuffer(1000);
	if (fields == null)  { fields = "*"; }
	if (conn == null || type == null || segment == null)  {
	    System.out.println("ERROR: rangeQuery() received a null argument: conn = " + conn + 
			       ", type = " + type + ", segment = " + segment);
	    return null;
	}
	System.out.println("in DbUtils.rangeQuery(), excludes count: " + excludes.size());
	System.out.println("segment: " + segment.getLocalURIString());
    
	String segname = segment.getName();
	URI seguri = segment.getAbsoluteURI();
	boolean prefix_with_and = false;
	
	// select $fields from 
	buf.append("select ");   
	buf.append(fields);
	buf.append(" from ");
	// select $fields from $chrom_$table where 
	if (type.isSplit())  { 
	    buf.append( segname + "_" + type.getName());
	    buf.append(" where ");
	}
	// select $fields from $table where $chromField='$chrom' and 
	else  { 
	    buf.append( type.getTableName());
	    buf.append(" where ");
	    buf.append( type.getChromField());
	    buf.append("='");
	    buf.append(segname);
	    buf.append("' ");
	    prefix_with_and = true;
	}
	
	// add binning clause(s) if there's a bin index for the table and overlap segment(s)
	if ((type.hasBin()) && (overlaps != null) && (overlaps.size() > 0))  {
	    if (prefix_with_and)  { buf.append(" and "); } 
	    for (int i=0; i<overlaps.size(); i++)  {
		if (i != 0)  { buf.append(" or "); }
		Das2LocationRefI loc = overlaps.get(i);
		String bin_clause = BinRange.getBinIndexingClause(loc.getMin(), loc.getMax());
		buf.append( bin_clause );
	    }
	    prefix_with_and = true;
	}

	// add overlaps constraints: annot_min < query_max AND annot_max > query_min
	if ((overlaps != null) && (overlaps.size() > 0))  { 
	    if (prefix_with_and)  { buf.append(" and "); }
	    if (overlaps.size() > 1)  { buf.append(" ( "); }
	    for (int i=0; i<overlaps.size(); i++)  {
		// checking to make sure all range locs have same segment as input segment arg
		// Das2SegmentI locseg = loc.getSegment();
		//    if (segment != locseg)  { System.out.println("ERROR in rangeQuery"); return null; } 
		Das2LocationRefI loc = overlaps.get(i);		
		if (! seguri.equals(loc.getSegmentURI())) {
		    System.out.println("ERROR in rangeQuery overlaps filter, segment URIs disagree: ");
		    System.out.println("     " + seguri);
		    System.out.println("     " + loc.getSegmentURI());
		    return null; 
		}
		if (i != 0)  { buf.append(" or "); }
		buf.append("(");
		buf.append( type.getStartField() );
		buf.append("<");
		buf.append( loc.getMax() );
		buf.append(" and ");
		buf.append( type.getEndField() );
		buf.append(">");
		buf.append( loc.getMin() );
		buf.append(")");
	    }
	    if (overlaps.size() > 1)  { buf.append(" ) "); }
	    prefix_with_and = true;
	}

	// add insides constraints: annot_min >= query_min AND annot_max <= query_max
	if ((insides != null)  && (insides.size() > 0))  {
	    if (prefix_with_and)  { buf.append(" and "); }
	    if (insides.size() > 1)  { buf.append(" ( "); }
	    for (int i=0; i<insides.size(); i++)  {
		// checking to make sure all range locs have same segment as input segment arg
		//Das2SegmentI locseg = loc.getSegment();
		//  if (segment != locseg)  { System.out.println("ERROR in rangeQuery"); return null; } 
		Das2LocationRefI loc = insides.get(i);		
		if (! seguri.equals(loc.getSegmentURI())) {
		    System.out.println("ERROR in rangeQuery insides filter, segment URIs disagree: ");
		    System.out.println("     " + seguri);
		    System.out.println("     " + loc.getSegmentURI());
		    return null; 
		}
		if (i != 0)  { buf.append(" or "); }
		buf.append("(");
		buf.append( type.getStartField() );
		buf.append(">=");
		buf.append( loc.getMin() );
		buf.append(" and ");
		buf.append( type.getEndField() );
		buf.append("<=");
		buf.append( loc.getMax() );
		buf.append(")");
	    }
	    if (insides.size() > 1)  { buf.append(" ) "); }
	    prefix_with_and = true;
	}

	// add excludes constraints:
	//      (annot_min >= query_max OR annot_max <= query_min) 
	if ((excludes != null)  && (excludes.size() > 0))  {
	    if (prefix_with_and)  { buf.append(" and "); }
	    if (excludes.size() > 1)  { buf.append(" ( "); }
	    for (int i=0; i<excludes.size(); i++)  {
		// checking to make sure all range locs have same segment as input segment arg
		// Das2SegmentI locseg = loc.getSegment();
		//    if (segment != locseg)  { System.out.println("ERROR in rangeQuery"); return null; } 
		Das2LocationRefI loc = excludes.get(i);		
		if (! seguri.equals(loc.getSegmentURI())) {
		    System.out.println("ERROR in rangeQuery excludes filter, segment URIs disagree: ");
		    System.out.println("     " + seguri);
		    System.out.println("     " + loc.getSegmentURI());
		    return null; 
		}
		System.out.println("excludes loc: " + loc);
		if (i != 0)  { buf.append(" or "); }
		buf.append("(");
		buf.append( type.getStartField() );
		buf.append(">=");
		buf.append( loc.getMax() );
		buf.append(" or ");
		buf.append( type.getEndField() );
		buf.append("<=");
		buf.append( loc.getMin() );
		buf.append(")");
	    }
	    if (excludes.size() > 1)  { buf.append(" ) "); }
	    prefix_with_and = true;
	}
	
	// add extraWhere clause, unless it starts with an sort or limit modifier
	if (extraWhere != null && 
	    (! extraWhere.startsWith("order")) &&    
	    (! extraWhere.startsWith("limit")) )  {
	    if (prefix_with_and)  { buf.append(" and "); }
	    buf.append(extraWhere);
	    prefix_with_and = true;
	}

	// if sort, then sort by start coord (this will likely degrade performance)
	if (sort)  {
	    buf.append(" order by ");
	    buf.append(type.getStartField());
	}
	if (max_rows_threshold != null)  {
	    buf.append(" limit ");
	    buf.append(max_rows_threshold.toString());
	}
				       
	String dbquery = buf.toString();
	System.out.println("RANGE QUERY: ");
	System.out.println(dbquery);
	Statement stmt = conn.createStatement();
	ResultSet rs = stmt.executeQuery(dbquery);
	DbQueryResult result = null;
	if (max_rows_threshold != null)  {
	    ResultSet row_count_rs = conn.createStatement().executeQuery("select found_rows()");
	    row_count_rs.next();
	    int row_count = row_count_rs.getInt(1);
	    System.out.println("feature retrieval row count: " + row_count);
	    if (row_count >= max_rows_threshold)  {
		System.out.println("  limit reached or exceeded: " + max_rows_threshold);
		result = new DbQueryResult(null, DbQueryResult.RESPONSE_TOO_LARGE, 
					   "Too many rows retrieved from database: " + row_count + 
					   ", must be < " + max_rows_threshold);
	    }
	}
	if (result == null)  {
	    result = new DbQueryResult(rs);
	}
	return result;
    }


    /*
      public static void main(String[] args)  {
      String table_name = "refGene";
      String chrom_name = "chr21";
      Das2ServerInfo server = new Das2ServerInfo(null, "http://test.server/das2/genome/");
      Das2Version version = server.getVersion("hg18");
      Das2Segment chrom = version.getSegment(chrom_name);
      Das2Location overlap = new Das2Location(chrom, 26137850, 26522069);
      List overlaps = new ArrayList();
      overlaps.add(overlap);
      //	    Das2Type type = version.getType("refGene");
      try  {
      Connection conn = Das2Servlet.getDbConnection("hg18");
      Statement stmt = conn.createStatement();
      ResultSet col_query = stmt.executeQuery("show columns from " + table_name);
      Das2Type type = new Das2Type(col_query, table_name, table_name, version);
        
      System.out.println("*******");
      type.print();
      System.out.println("*******"); 

      ResultSet rs = rangeQuery(conn, type, chrom, overlaps, null, null, null, false, null);
      FormatHandler formatter = new Das2FeatureXmlHandler();
      formatter.output(rs, type, chrom, "das2xml", System.out);
      System.out.println("hello!");
      System.out.flush();
      //	      while (rs.next())  {
      //	      String name = rs.getString("name");
      //	      System.out.println("NAME: " + name);
      //	      }

      }
      catch (Exception ex)  {
      ex.printStackTrace();
      }
      }
    */
    
}

