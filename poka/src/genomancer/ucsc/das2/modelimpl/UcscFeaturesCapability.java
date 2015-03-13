package genomancer.ucsc.das2.modelimpl;

import genomancer.trellis.das2.model.Das2CoordinatesI;
import genomancer.trellis.das2.model.Das2FeatureI;
import genomancer.trellis.das2.model.Das2FeaturesCapabilityI;
import genomancer.trellis.das2.model.Das2FeaturesQueryI;
import genomancer.trellis.das2.model.Das2FeaturesResponseI;
import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.trellis.das2.model.Das2LocationRefI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Strand;
import genomancer.trellis.das2.model.Status;
import genomancer.ucsc.das2.DbQueryResult;
import genomancer.ucsc.das2.DbUtils;
import genomancer.ucsc.das2.TrackType;
import genomancer.vine.das2.client.modelimpl.Das2Coordinates;
import genomancer.vine.das2.client.modelimpl.Das2Feature;
import genomancer.vine.das2.client.modelimpl.Das2FeaturesResponse;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2Location;
import genomancer.vine.das2.client.modelimpl.Das2Segment;
import genomancer.vine.das2.client.modelimpl.Das2Version;
import java.io.InputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


public class UcscFeaturesCapability extends Das2GenericCapability implements Das2FeaturesCapabilityI  {
    static boolean DEBUG_ANNOT_COUNT = false;
    static final Pattern block_splitter = Pattern.compile(",");
    static boolean SORT_BY_MIN = true;
    // if SPLIT_CDS, then use combination of "UTR" and "CDS-segments" as children of transcripts
    // if !SPLIT_CDS, then use combination of exons and whole-CDS (translation start to stop span)
    static boolean SPLIT_CDS = false;

    UcscVersion ucsc_version = null;
    UcscSegmentsCapability segcap = null;  // never use directly, always call this.getSegmentsCapability()
    UcscTypesCapability typecap = null;   // never use directly, always call this.getTypesCapability()
    URI response_base_uri;

    // Throttling: if more than max_rows_threshold top-level features
    //     would be returned, then return a RESPONSE_TOO_LARGE error message instead
    // Integer max_rows_threshold = new Integer(5000);
    // Integer max_rows_threshold = new Integer(1000);
    Integer max_rows_threshold = null;
    
    public UcscFeaturesCapability(UcscVersion version, Das2CoordinatesI coords, ResultSet rs) {
        super(version.getBaseURI(), (version.getLocalURIString()+"/features"), "features", version, coords);
	ucsc_version = version;
	response_base_uri = this.getAbsoluteURI().resolve("./");
    }
    
    protected synchronized UcscSegmentsCapability getSegmentsCapability()  {
        if (segcap == null)  { 
            segcap = (UcscSegmentsCapability) version.getCapability("segments");
        }
        return segcap;
    }
    
    protected synchronized UcscTypesCapability getTypesCapability()  {
	if (typecap == null)  {
	    typecap = (UcscTypesCapability)version.getCapability("types");
	}
	return typecap;
    }
      

    public boolean supportsFullQueryFilters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    public boolean supportsCountFormat() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean supportsUriFormat() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** 
     *  for now not trying to narrow down to particular sequences
     *   so if query contains overlaps/insides/excludes, just ignore them 
     *   and if type uses split table, be conservative and report the most recent modification time of the set of tables
     */
    public long getLastModified(Das2FeaturesQueryI query)  {
	List<URI> types = query.getTypes();

	if (types == null || types.size() == 0 || types.size() > 1)  {
	    // return REQUEST_TOO_BIG error status code if no types specified or multiple types specified
	    //   currently only implementing for a single captype
	    //
	    // for now just returning -1 to signify last modified time is unknown
	    return -1;
	}
	URI typeuri = types.get(0);
	System.out.println("called getLastModified() for feature type: " + typeuri);
	UcscType utype = (UcscType) getTypesCapability().getType(typeuri);
	boolean is_split = utype.isSplit();
	// TrackType track_type = captype.getTrackType();
	String genome_name = ucsc_version.getName();
	long time = -1;
	    /*  
		String feat_table;
		preliminary work on restricting to sequence in query filter
	    if (is_split)  {
	    List<Das2LocationRefI> overlaps = query.getOverlaps();
	    URI sequri = overlaps.get(0).getSegmentURI();
	    UcscSequence seq = (UcscSequence) getSegmentsCapability().getSegment(sequri);	    
	    String seq_name = seq.getName();
	    feat_table = seq_name + "_" + utype.getName();
	    }
	    */
	String table_name_clause;
	if (is_split)  {
	    table_name_clause = "TABLE_NAME like '%_" + utype.getName() + "'";
	}
	else {
	    table_name_clause = "TABLE_NAME = '" + utype.getTableName() + "'";
	}
	try {
	    String mod_time_query = "SELECT TABLE_NAME, UPDATE_TIME FROM INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA = '" + 
		genome_name + "' AND " + table_name_clause;
	    Connection conn = ucsc_version.getDbConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(mod_time_query);
	    while (rs.next())  {
		Timestamp tstamp = rs.getTimestamp("UPDATE_TIME"); // Timestamp is JDBC subclass of java.util.Date
		long row_time = tstamp.getTime();
		if (row_time > time)  { time = row_time; }
	    }
            // System.out.println("lastModified time for " + utype.getName() + ": " + tstamp);
	    rs.close();
	    stmt.close();
	    conn.close();
	} catch (SQLException ex) {
	    Logger.getLogger(UcscFeaturesCapability.class.getName()).log(Level.SEVERE, null, ex);
	}
	return time;
    }


    public Das2FeaturesResponseI getFeatures(Das2FeaturesQueryI query) {
	System.out.println("called UcscFeaturesCapability.getFeatures()");
	List<Das2FeatureI> feats = new ArrayList<Das2FeatureI>();
	Das2FeaturesResponseI response = null;
	Connection conn = null;
	ResultSet rs = null;
	try {
            List<URI> types = query.getTypes();
            if (types == null || types.size() == 0) {
                // return REQUEST_TOO_BIG error status code --
                //  UCSC DAS2 server requires specification of at least one captype filter
            }
            // check for links, names, notes, coordinates, non-standard params
            //   -- none are currently supported
            //   -- if present, return what???
            //
            if (query.getLinks().size() > 0) {
            } 
	    else if (query.getNames().size() > 0) {
            }
	    else if (query.getNotes().size() > 0) {
            }
	    else if (query.getCoordinates().size() > 0) {
            }
	    else if (query.getNonStandardParams().size() > 0) {
            }

            List<Das2LocationRefI> overlaps = query.getOverlaps();
            List<Das2LocationRefI> insides = query.getInsides();
            List<Das2LocationRefI> excludes = query.getExcludes();
	    System.out.println("excludes count: " + excludes.size());
            if (overlaps.size() == 0 && insides.size() == 0) {
                // return REQUEST_TOO_BIG error status code --
                //   UCSC DAS2 server requires specification of at least one overlap or inside filter,
                //    to limit response size
            }
            // make sure there is only one seq in overlaps/insides/excludes
            //   (currently done in DbUtils.rangeQuery())
            URI sequri = null;
            if (overlaps.size() > 0) {
                sequri = overlaps.get(0).getSegmentURI();
            } else {
                sequri = insides.get(0).getSegmentURI();
            }
            UcscSequence seq = (UcscSequence) getSegmentsCapability().getSegment(sequri);

            if (seq == null) {
                // could not find seq
                // return 404 not found??
                // or client request error??
            }

            // loop through types
            conn = ucsc_version.getDbConnection();
            for (URI typeuri : types) {
                UcscType type = (UcscType) getTypesCapability().getType(typeuri);
                if (type == null) {
                    // could not find captype
                    // return 404 not found??
                    // or client request error??
                }
		try  {
		    DbQueryResult query_result = DbUtils.rangeQuery(conn, 
								    type, 
								    seq, 
								    overlaps, 
								    insides, 
								    excludes, 
								    null, 
								    max_rows_threshold,  // Integer, max_rows_threshold
								    SORT_BY_MIN,
								    null);
		    rs = query_result.getResultSet();
		    int query_status = query_result.getStatus();
		    if (query_status == DbQueryResult.OK)  {
			List<Das2FeatureI> new_feats = convertAnnots(type, seq, rs);
			System.out.println("features: " + new_feats.size() + ",  type: " + type.getLocalURIString());
			feats.addAll(new_feats);		    
		    }
		    else if (query_status == DbQueryResult.RESPONSE_TOO_LARGE)  {
			response = new Das2FeaturesResponse(Status.RESPONSE_TOO_LARGE);
			break;
		    }
		    else  {
			response = new Das2FeaturesResponse(Status.UNKNOWN);
			break;
		    }
		}
		finally {
		    if (rs != null)  {
			Statement stmt = rs.getStatement();
			rs.close();
			if (stmt != null)  { stmt.close(); }
		    }
		}
            }
        } catch (SQLException ex) {
	    // return SERVER_ERRROR status code
            Logger.getLogger(UcscFeaturesCapability.class.getName()).log(Level.SEVERE, null, ex);
        }
	finally  {
	    if (conn != null)  {try {
                    conn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(UcscFeaturesCapability.class.getName()).log(Level.SEVERE, null, ex);
                }
	    }
	}
        if (response == null)  {  // no errors encountered that trigger shortened response
	    response = new Das2FeaturesResponse(response_base_uri, feats, null, true, true);
	}
        return response;
    }

    // does union of locations require overlap??
    //    protected Das2LocationI union(Das2LocationI locA, Das2LocationI locB, Strand strand_overide)  {
    //	
    //    }

    protected Das2LocationI intersection(Das2LocationI locA, Das2LocationI locB, Strand strand_overide)  {
	Das2LocationI result = null;
	if (locA.getSegment() == locB.getSegment()) {
	    int minR = Math.max(locA.getMin(), locB.getMin());
	    int maxR = Math.min(locA.getMax(), locB.getMax());
	    if (minR < maxR)  {  // if (max of mins) < (min of maxs), then there is intersection
		Strand result_strand = strand_overide;
		if (result_strand == null)  {
		    if (locA.getStrand() == locB.getStrand())  {
			result_strand = locA.getStrand();
		    }
		    else  {
			result_strand = Strand.UNKNOWN;
		    }
		    
		}
		result = new Das2Location(locA.getSegment(), minR, maxR, result_strand);
	    }
	}
	return result;
    }

    protected List<Das2LocationI> aNotB(Das2LocationI locA, Das2LocationI locB, Strand strand_overide)  {
	List<Das2LocationI> result = new ArrayList();
	Das2LocationI overlap = intersection(locA, locB, strand_overide);
	Strand result_strand = strand_overide;
	if (result_strand == null)  {
	    if (locA.getStrand() == locB.getStrand())  {
		result_strand = locA.getStrand();
	    }
	    else  {
		result_strand = Strand.UNKNOWN;
	    }
	}
	if (overlap == null)  { // no overlap, so return entirety of locA
	    result.add(new Das2Location(locA.getSegment(), locA.getMin(), locA.getMax(), result_strand));
	}  
	else  {
	    if (locA.getMin() < overlap.getMin())  { // part of A < B
		result.add(new Das2Location(locA.getSegment(), locA.getMin(), overlap.getMin(), result_strand));
	    }
	    if (locA.getMax() > overlap.getMax())  { // part of A > B
		result.add(new Das2Location(locA.getSegment(), overlap.getMax(), locA.getMax(), result_strand));
	    }
	}
	return result;
    }

    protected void handleChildren(UcscCdsFeature annot, List<Das2LocationI> child_locs)  { 
	String annotid = annot.getLocalURIString();
	int i = 0;
	Das2LocationI cds_loc = annot.getCds();
	boolean has_cds = (cds_loc != null);
	UcscTypesCapability tcap = this.getTypesCapability();
	if (has_cds) {
	    int cds_min = cds_loc.getMin();
	    int cds_max = cds_loc.getMax();
	    if (SPLIT_CDS)  {  // if SPLIT_CDS, then use combination of "UTR" and "CDS-segments" as children of transcripts
		for (Das2LocationI child_loc : child_locs)  {
		    int min = child_loc.getMin();
		    int max = child_loc.getMax();
		    if (cds_min <= min && cds_max >= max)  {  // child is entirely within CDS bounds
			String child_annotid = annotid + "." + Integer.toString(i++);
			Das2Feature child_annot = new Das2Feature(response_base_uri, child_annotid, tcap.type_CDS); 
			child_annot.addLocation(child_loc);
			annot.addPart(child_annot);
			child_annot.addParent(annot);
		    }
		    else if (cds_min >= max || cds_max <= min)  { // child is entirely outside CDS bounds
			// split this conditional if want to distinguish 5' UTR from 3' UTR...
			String child_annotid = annotid + "." + Integer.toString(i++);
			Das2Feature child_annot = new Das2Feature(response_base_uri, child_annotid, tcap.type_UTR); 
			child_annot.addLocation(child_loc);
			annot.addPart(child_annot);
			child_annot.addParent(annot);
		    }
		    else  {  // CDS partially overlaps child
			// get intersection ==> CDS
			Das2LocationI overlap = intersection(child_loc, cds_loc, child_loc.getStrand());
			// get a_not_b(child, cds) ==> UTR(s)
			List<Das2LocationI> non_overlaps = aNotB(child_loc, cds_loc, child_loc.getStrand());
			Das2LocationI lowloc = null;
			Das2LocationI hiloc = null;
			if (non_overlaps != null && non_overlaps.size() > 0)  {
			    if (non_overlaps.size() == 1)  {
				Das2LocationI nloc = non_overlaps.get(0);
				if (nloc.getMin() < overlap.getMin())  { lowloc = nloc; }
				else  { hiloc = nloc; }
			    }
			    else if (non_overlaps.size() == 2)  {
				lowloc = non_overlaps.get(0);
				hiloc = non_overlaps.get(1);
			    }
			    if (lowloc != null)  {
				String child_annotid = annotid + "." + Integer.toString(i++);
				Das2Feature child_annot = new Das2Feature(response_base_uri, child_annotid, tcap.type_UTR); 
				child_annot.addLocation(lowloc);
				annot.addPart(child_annot);
				child_annot.addParent(annot);
			    }
			    if (overlap != null)  {
				String child_annotid = annotid + "." + Integer.toString(i++);
				Das2Feature child_annot = new Das2Feature(response_base_uri, child_annotid, tcap.type_CDS); 
				child_annot.addLocation(overlap);
				annot.addPart(child_annot);
				child_annot.addParent(annot);
			    }
			    if (hiloc != null)  {
				String child_annotid = annotid + "." + Integer.toString(i++);
				Das2Feature child_annot = new Das2Feature(response_base_uri, child_annotid, tcap.type_UTR); 
				child_annot.addLocation(hiloc);
				annot.addPart(child_annot);
				child_annot.addParent(annot);
			    }
			}
		    }
		}  // end child_loc loop
	    }  // end (SPLIT_CDS) conditional
	    else  { // if !SPLIT_CDS, then use combination of exons and whole-CDS (translation start to stop span)
		for (Das2LocationI child_loc : child_locs)  {
		    String child_annotid = annotid + "." + Integer.toString(i++);
		    Das2Feature child_annot = new Das2Feature(response_base_uri, child_annotid, tcap.type_EXON);
		    child_annot.addLocation(child_loc);
		    annot.addPart(child_annot);
		    child_annot.addParent(annot);
		}
		String cds_id = annotid + "." + Integer.toString(i++);
		Das2Feature cds_annot = new Das2Feature(response_base_uri, cds_id, tcap.type_WHOLECDS);
		cds_annot.addLocation(cds_loc);
		annot.addPart(cds_annot);
		cds_annot.addParent(annot);
	    }
	}
	else  {
	    for (Das2LocationI child_loc : child_locs)  {
		String child_annotid = annotid + "." + Integer.toString(i++);
		Das2Feature child_annot = new Das2Feature(response_base_uri, child_annotid, tcap.type_UNKNOWN);
		child_annot.addLocation(child_loc);
		annot.addPart(child_annot);
		child_annot.addParent(annot);
	    }
	}
    }

    /**
     *  children:
     *     BED: blockStartsField/blockSizesField, blockStarts coords relative to feature start (feature start = 0)
     *     GENEPRED: blockStartsField/blockEndsField, blockStarts/blockEnds coords absolute
     *     PSL: blockStartsField/blockSizesField, blockStarts coords absolute
     *          PSL also has rev-comp weirdness, but only for "other" seq (q***) coords (NOT YET IMPLEMENTED)
     */
    protected List<Das2FeatureI> convertAnnots(UcscType type, UcscSequence seq, ResultSet rs) throws SQLException  {
	// to cut down on copy ops, starting with space for 10K annots
	List<Das2FeatureI> annots = new ArrayList<Das2FeatureI>(10000); 
	TrackType track_type = type.getTrackType();
	String seqid = seq.getName();

	if (track_type == TrackType.BED3 || 
	    track_type == TrackType.BED4 || 
	    track_type == TrackType.BED5 || 
	    track_type == TrackType.BED6 || 
	    track_type == TrackType.BED8 || 
	    track_type == TrackType.BED9 || 
	    track_type == TrackType.BED12 || 
	    track_type == TrackType.BED15 )  {
	    while (rs.next())  {
		int min = rs.getInt(type.start_col);
		int max = rs.getInt(type.end_col);
		Strand strand = Strand.FORWARD;     
		if (type.strand_col >= 0)  {
		    if (rs.getString(type.strand_col).equals("-"))  {
			strand = Strand.REVERSE;
		    }
		}
		String annotid;
		if (type.name_col  >= 0)  {
		    annotid = rs.getString(type.name_col);
		}
		else  {
		    if (strand == Strand.REVERSE)  {
			annotid = seqid + "/" + min + "-" + max + "-";
		    } else  {
			annotid = seqid + "/" + min + "-" + max;
		    }
		}
		String title = annotid;
		if (type.hasOtherName())  { title = rs.getString(type.other_name_col); }

		Das2LocationI loc = new Das2Location(seq, min, max, strand);
		UcscCdsFeature annot = new UcscCdsFeature(response_base_uri,
						    annotid,    // local_uri_string for feature 
						    title, 
						    null, 
						    null, 
						    type, 
						    null, 
						    null);
		annot.addLocation(loc);
		if (type.hasCDS())  {
		    int cds_min = rs.getInt(type.cds_start_col);
		    int cds_max = rs.getInt(type.cds_end_col);
		    // check for no CDS: convention is if table has CDS column but no CDS, then cds_min == cds_max
		    if (cds_min != cds_max)  {  
			Das2LocationI cds_loc = new Das2Location(seq, cds_min, cds_max, strand);
			annot.setCds(cds_loc);
		    }
		}
		if (type.hasBlocks())   {
		    int child_count = rs.getInt(type.block_count_col);
		    // List child_list = new ArrayList(child_count);
		    String startsblock = rs.getString(type.block_starts_col);
		    String sizesblock = rs.getString(type.block_sizes_col);
		    String[] cstarts = block_splitter.split(startsblock);
		    String[] csizes = block_splitter.split(sizesblock);  // BED uses blockSizes

		    List<Das2LocationI> child_locs = new ArrayList();
		    for (int i=0; i<child_count; i++)  {
			int cmin = min + Integer.parseInt(cstarts[i]);   // BED startBlocks relative to parent start
			int cmax = cmin + Integer.parseInt(csizes[i]);
			Das2LocationI child_loc = new Das2Location(seq, cmin, cmax, strand);
			child_locs.add(child_loc);
		    }
		    handleChildren(annot, child_locs);
		    
		    /*
		    for (int i=0; i<child_count; i++)  {
			String child_annotid = annotid + "." + Integer.toString(i);
			int cmin = min + Integer.parseInt(cstarts[i]);   // BED startBlocks relative to parent start
			int cmax = cmin + Integer.parseInt(csizes[i]);
			// strand of child is always same as strand of parent
			Das2LocationI child_loc = new Das2Location(seq, cmin, cmax, strand);
			Das2Feature child_annot = new Das2Feature(response_base_uri, 
								  child_annotid,   // local_uri_string for child
								  null, 
								  null, 
								  null, 
								  type, 
								  null, 
								  null);
			child_annot.addLocation(child_loc);
			//			child_list.add(child_annot);
			annot.addPart(child_annot);
			child_annot.addParent(annot);
		    }
		    */
		}
		annots.add(annot);
	    }
	}   // end BED branch

	else if (track_type == TrackType.GENEPRED)  {
	    while (rs.next()) {
		String annotid = rs.getString(type.name_col);
		String title = annotid;
		if (type.hasOtherName())  { title = rs.getString(type.other_name_col); }
		int min = rs.getInt(type.start_col);
		int max = rs.getInt(type.end_col);
		Strand strand = Strand.FORWARD;
		if (type.strand_col >= 0)  {
		    if (rs.getString(type.strand_col).equals("-"))  {
			strand = Strand.REVERSE;
		    }
		}
		Das2LocationI loc = new Das2Location(seq, min, max, strand);
		UcscCdsFeature annot = new UcscCdsFeature(response_base_uri, 
							  annotid,    // local_uri_string for feature 
							  title, 
							  null, 
							  null, 
							  type, 
							  null, 
							  null);
		annot.addLocation(loc);
		if (type.hasCDS())  {
		    int cds_min = rs.getInt(type.cds_start_col);
		    int cds_max = rs.getInt(type.cds_end_col);
		    // check for no CDS: convention is if table has CDS column but no CDS, then cds_min == cds_max
		    if (cds_min != cds_max)  {  
			Das2LocationI cds_loc = new Das2Location(seq, cds_min, cds_max, strand);
			annot.setCds(cds_loc);
		    }
		}
		if (type.hasBlocks())   {
		    int child_count = rs.getInt(type.block_count_col);
		    String startsblock = rs.getString(type.block_starts_col);
		    String endsblock = rs.getString(type.block_ends_col);   // GENEPRED uses exonEnds/blockEnds
		    String[] cstarts = block_splitter.split(startsblock);
		    String[] cends = block_splitter.split(endsblock);
		    List<Das2LocationI> child_locs = new ArrayList();
		    for (int i=0; i<child_count; i++)  {
			int cmin = Integer.parseInt(cstarts[i]);  // GENEPRED blockStarts coords are absolute
			int cmax = Integer.parseInt(cends[i]);    // GENEPRED blockEnds coords are absolute
			// strand of child is always same as strand of parent
			Das2LocationI child_loc = new Das2Location(seq, cmin, cmax, strand);
			child_locs.add(child_loc);
		    }
		    handleChildren(annot, child_locs);
		    /*
		    for (int i=0; i<child_count; i++)  {
			String child_annotid = annotid + "." + Integer.toString(i);
			int cmin = Integer.parseInt(cstarts[i]);  // GENEPRED blockStarts coords are absolute
			int cmax = Integer.parseInt(cends[i]);    // GENEPRED blockEnds coords are absolute
			// strand of child is always same as strand of parent
			Das2LocationI child_loc = new Das2Location(seq, cmin, cmax, strand);
			Das2Feature child_annot = new Das2Feature(response_base_uri, 
								  child_annotid,   // local_uri_string for child
								  null, 
								  null, 
								  null, 
								  type, 
								  null, 
								  null);
			child_annot.addLocation(child_loc);
			annot.addPart(child_annot);
			child_annot.addParent(annot);
		    }
		    */
		}
		annots.add(annot);
	    }
	}   // end GENEPRED branch

	else if (track_type == TrackType.PSL)  {
	    Map<String, Das2SegmentI> qseqs = new HashMap<String, Das2SegmentI>();
	    while (rs.next())  {
		int qlength = rs.getInt("qSize");
		// int tlength = rs.getInt("tSize");
		int tlength = seq.getLength();
		
		String annotid = rs.getString(type.name_col);
		String title = annotid;
		if (type.hasOtherName())  { title = rs.getString(type.other_name_col); }
		int tmin = rs.getInt(type.start_col);
		int tmax = rs.getInt(type.end_col);
		int qmin = rs.getInt("qStart");
		int qmax = rs.getInt("qEnd");
		Strand tstrand = Strand.FORWARD;
		Strand qstrand = Strand.FORWARD;
		if (type.strand_col >= 0)  {
		    // current implentation assumes the only possibilities for strand are: 
		    //    "+", "-", "++", "--", "+-", "-+"
		    String strands = rs.getString(type.strand_col);
		    if (strands.length() == 1)  {
			qstrand = ((strands.charAt(0) == '-') ? Strand.REVERSE : Strand.FORWARD);
			tstrand = Strand.FORWARD;
		    }
		    else if (strands.length() >= 2)  {
			qstrand = ((strands.charAt(0) == '-') ? Strand.REVERSE : Strand.FORWARD);
			tstrand = ((strands.charAt(1) == '-') ? Strand.REVERSE : Strand.FORWARD);
		    }

		}
		boolean same_orientation = (qstrand == tstrand);

		Das2Feature annot = new Das2Feature(response_base_uri, 
						    annotid,    // local_uri_string for feature 
						    title, 
						    null, 
						    null, 
						    type, 
						    null, 
						    null);
		
		Das2SegmentI qseq = qseqs.get(annotid);
		if (qseq == null)  { 
		    qseq = new Das2Segment(response_base_uri, annotid, null, null, qlength, null);
		    qseqs.put(annotid, qseq);
		}

		// Das2LocationI tloc = new Das2Location(seq, tmin, tmax, tstrand);
		// Das2LocationI qloc = new Das2Location(qseq, qmin, qmax, qstrand);
		Das2LocationI tloc;
		if (same_orientation)  {
		    tloc = new Das2Location(seq, tmin, tmax, Strand.FORWARD);
		}
		else  {
		    tloc = new Das2Location(seq, tmin, tmax, Strand.REVERSE);
		}
		Das2LocationI qloc = new Das2Location(qseq, qmin, qmax, Strand.FORWARD);
		annot.addLocation(tloc);
		annot.addLocation(qloc);

		if (type.hasBlocks())   {
		    int child_count = rs.getInt(type.block_count_col);
		    String tstarts_block = rs.getString(type.block_starts_col);
		    String qstarts_block = rs.getString("qStarts");
		    String sizes_block = rs.getString(type.block_sizes_col);  // PSL uses blockSizes
		    String[] tstarts = block_splitter.split(tstarts_block);
		    String[] qstarts = block_splitter.split(qstarts_block);
		    String[] sizes = block_splitter.split(sizes_block);
		    for (int i=0; i<child_count; i++) {
			// query = forward, target = forward
			int block_index;
			if (qstrand == Strand.REVERSE)  {
			    block_index = child_count - i - 1;
			}
			else {
			    block_index = i;
			}
			int tstart = Integer.parseInt(tstarts[block_index]);
			int qstart = Integer.parseInt(qstarts[block_index]);
			int match_length = Integer.parseInt(sizes[block_index]);
			int child_qmin;
			int child_tmin;
			if (qstrand == Strand.FORWARD && tstrand == Strand.FORWARD)  {
			    //			    match_length = Integer.parseInt(sizes[i]);
			    child_qmin = qstart;
			    child_tmin = tstart;
			}
			// query = reverse, target = forward
			else if (qstrand == Strand.REVERSE && tstrand == Strand.FORWARD)  { 
			    //			    int block_index = child_count-i-1;
			    //			    match_length = Integer.parseInt(sizes[block_index]);
			    child_qmin = qlength - qstart - match_length;
			    child_tmin = tstart;
			}
			// query = forward, target = reverse
			else if (qstrand == Strand.FORWARD && tstrand == Strand.REVERSE)  {
			    //			    match_length = Integer.parseInt(sizes[i]);
			    child_qmin = qstart;
			    child_tmin = tlength - tstart - match_length;
			}
			else { // query = reverse, target = reverse
			    //			    int block_index = child_count-i-1;
			    //			    match_length = Integer.parseInt(sizes[block_index]);
			    child_qmin = qlength - qstart - match_length;
			    child_tmin = tlength - tstart - match_length;
			}
			//			for (int i=0; i<child_count; i++)  {
			String child_annotid = annotid + "." + Integer.toString(i);

			Das2Feature child_annot = new Das2Feature(response_base_uri, 
								  child_annotid,   // local_uri_string for child
								  null, 
								  null, 
								  null, 
								  type, 
								  null, 
								  null);
			
			// Das2LocationI child_tloc = 
			//   new Das2Location(seq, child_tmin, child_tmin + match_length, tstrand);
			// Das2LocationI child_qloc = 
			//   new Das2Location(qseq, child_qmin, child_qmin + match_length, qstrand);

			Das2LocationI child_tloc;
			if (same_orientation)  {
			    child_tloc = new Das2Location(seq, child_tmin, child_tmin + match_length, Strand.FORWARD);
			}
			else  {
			    child_tloc = new Das2Location(seq, child_tmin, child_tmin + match_length, Strand.REVERSE);
			}
			Das2LocationI child_qloc = 
			    new Das2Location(qseq, child_qmin, child_qmin + match_length, Strand.FORWARD);
			child_annot.addLocation(child_tloc);
			child_annot.addLocation(child_qloc);
			annot.addPart(child_annot);
			child_annot.addParent(annot);
			//		    }
		    }
		}
		annots.add(annot);
	    }
	}  // end PSL branch

	return annots;
    }

    public int getFeaturesCount(Das2FeaturesQueryI query) {
	// want to support fast way of determining feature count across entire genome assembly
	// query requirements to support fast method:
	//      no overlaps, insides, excludes, coordinates, links, names, notes parameters
	//      single captype parameter
	// note that this will be less efficient for "split" data types, where each sequence in the 
	//      assembly has its own table, thus requiring either querying each table or querying 
	//      mysql INFORMATION_SCHEMA.TABLES and summing across table counts.  Fortunately there 
	//      appears to be a practical limit on the the number of tables one would need to query, 
	//      since for partial assemblies where the number of sequences in the assembly is likely 
	//      to be large (example, bosTau4, 12,000 sequences) UCSC doesn't use split tables 
	//      (I'm assuming to avoid for example 12,000 tables needed for a bosTau4 split data captype)

	//  example of alternative with querying INFORMATION_SCHEMA.TABLES:  
	//      SELECT TABLE_NAME, TABLE_ROWS, UPDATE_TIME FROM INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA = 'apiMel2' AND TABLE_NAME like '%_chainDm2';
	//
	// see http://stackoverflow.com/questions/1018336/getting-row-count-for-a-table-in-mysql
	//   for argument for using INFORMATION_SCHEMA instead of "select count(*)", if mysql is 
	//   using InnoDB engine or if engine is unknown.  If engine is known to be MyISAM then 
	//   "select count(*)" is fine.  Based on current hgcentral.sql DB initialization script, 
	//   UCSC appears to only use MyISAM, but not sure if this will always be the case
	//
	// would also like to support fast method of feature count for a single sequence
	//      but for many UCSC data types, this would require a much less efficient 
	//      select statement with a "where" clause, so not supporting this at least for no
	//
	int feat_count = 0;
	List<URI> types = query.getTypes();
	if (types.size() == 1 && 
	    query.getOverlaps().size() == 0 &&
	    query.getInsides().size() == 0 &&
	    query.getExcludes().size() == 0 && 
	    query.getCoordinates().size() == 0 &&
	    query.getLinks().size() == 0 && 
	    query.getNames().size() == 0 &&
	    query.getNotes().size() == 0  )   {

	    URI typeuri = types.get(0);
	    UcscType utype = (UcscType) getTypesCapability().getType(typeuri);
	    boolean is_split = utype.isSplit();
	    TrackType track_type = utype.getTrackType();
	    String table_name_clause;
	    if (is_split)  {
		table_name_clause = "TABLE_NAME like '%_" + utype.getName() + "'";
	    }
	    else  {
		table_name_clause = "TABLE_NAME = '" + utype.getTableName() + "'";
	    }
	    Connection conn = null;
	    Statement stmt = null;
	    ResultSet rs = null;
	    try {
		conn = ucsc_version.getDbConnection();
		String genome_name = ucsc_version.getName();
		//                    String count_query = "SELECT COUNT(*) FROM " + utype.getName();
		String count_query =  "SELECT TABLE_NAME, TABLE_ROWS FROM INFORMATION_SCHEMA.TABLES " + 
		    "where TABLE_SCHEMA = '" + genome_name + "' AND " + table_name_clause;
		stmt = conn.createStatement();
		rs = stmt.executeQuery(count_query);
		// looping will cover both non-split cases (single entry) and 
		//    split cases (entry per sequence)
		while (rs.next())  {
		    if (is_split && rs.getString("TABLE_NAME").startsWith("all_"))  {
			// skip "all_***" tables for split table types, since counts should be reflected 
			//  in individaul sequence tables
			continue;
		    }
		    else  {
			feat_count += rs.getInt("TABLE_ROWS");
		    }
		}
		if (DEBUG_ANNOT_COUNT)  { System.out.println(utype.getName() + " total annot count: " + feat_count); }
	    } catch (Exception ex) {
		Logger.getLogger(UcscFeaturesCapability.class.getName()).log(Level.SEVERE, null, ex);
		feat_count = -1;  // return feat_count of -1 ==> means couldn't be determined
	    }
	    finally {
		try  {
		    if (rs != null)  { rs.close(); }
		    if (stmt != null)  { stmt.close(); }
		    if (conn != null)  { conn.close(); }
		} catch (SQLException ex)  { ex.printStackTrace(); }
	    }
	}
	    
	else  {
	    // can also easily add support for inefficient but more general query mechanism by 
	    //    calling getFeatues(query), then doing response.getFeatures().size()
	    //	String count_query = "SELECT COUNT(*) FROM " + table_name;

	    // return feat_count of -1 ==> means couldn't be determined
	    feat_count = -1;
	}
	return feat_count;
    }

    public List<String> getFeaturesURI(Das2FeaturesQueryI query) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public InputStream getFeaturesAlternateFormat(Das2FeaturesQueryI query) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     *  should add code here to return different Das2FeatureI implementations
     *      depending on captype --
     *      PSLFeature, BEDFeature, GraphFeature, etc...
     */
    public Class getFeatureClassForType(Das2TypeI type) {
        return Das2FeatureI.class;
    }

    /**
     *  depth of UCSC annotations is usually (always?) known based on captype,
     *  so return appropriate response
     * ( 2 for bed12+, psl, genepred, etc. types)
     * ( 1 for bed3-9, graphs, scored intervals?)
     */
    public int getMaxHierarchyDepth(Das2TypeI type) {
	int depth = Das2FeaturesCapabilityI.UNKNOWN;
        if (type instanceof UcscType)  {
	    UcscType utype = (UcscType)type;
	    TrackType track_type = utype.getTrackType();

	    if (track_type == TrackType.GENEPRED || 
		track_type == TrackType.PSL || 
		track_type == TrackType.BED12 || 
		track_type == TrackType.BED15)  {
		depth = 2;
	    }
	    else if (track_type == TrackType.BED3 ||
		     track_type == TrackType.BED4 ||
		     track_type == TrackType.BED5 ||
		     track_type == TrackType.BED6 ||
		     track_type == TrackType.BED8 ||
		     track_type == TrackType.BED9 )  {
		depth = 1;
	    }
	}
	return depth;
    }

}