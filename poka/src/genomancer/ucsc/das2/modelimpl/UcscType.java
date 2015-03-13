package genomancer.ucsc.das2.modelimpl;

import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.ucsc.das2.TrackType;
import genomancer.vine.das2.client.modelimpl.Das2Type;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

  /** Possible mappings of UCSC annotation types to genome database tables:
   *     annotation type name is always same as trackdb.tableName field, but tableName does not necessarily
   *         map directly to a database table name
   *
   *     default -- for each annotation type, a single table with same name as type
   *     "split" -- for each annotation type, each annotated sequence has its own table with name "[seqid]_[type_name]"
   *     "merged" -- for each annotaiton type, a single table with name = "all_[type_name]"
   *         other mappings may be possble, based on trackDb.settings==>table tagval property -- not yet accounting for this

   *     observed mappings:
     *     only split
     *     only merged,
     *     both (in which case there is data duplication)
     *     neither (default)
     */

public class UcscType extends Das2Type implements Das2TypeI  {

    /*  
     // using column index rather than column name when retrieving annotation data, 
     //  since using column name can incur significant overhead, for instance see:
     //    http://www.jroller.com/sjivan/entry/hibernate_overlooks_fundamental_jdbc_performance
     //
     */
    boolean initialized = false;
    int chrom_col = -1;
    int start_col = -1;
    int end_col = -1;
    int name_col = -1;
    int other_name_col = -1;
    int score_col = -1;
    int strand_col = -1;
    int cds_start_col = -1;
    int cds_end_col = -1;
    int block_count_col = -1;
    int block_starts_col = -1;
    //  part intervals are usually specified as starts/ends OR starts/sizes, 
    //    but never both, so only one of ends_col and sizes_col will be set
    int block_ends_col = -1;  
    int block_sizes_col = -1;
    int span_col = -1;
    int bin_col = -1;
    
    /** for split types, table_name should be considered the root of actual database table names, 
     * and actual table name for annotations on a seq is "[seqid]_[type.getTableName()]"
     */
    String table_name;

    /*    if isSplit, then annotations of this type are represented in the UCSC genome 
     *    database in split tables, with one table of annotations for each chromosome
     *    tablne names are: "chr" + chromnum + "_" + typename
     *                     (or for random parts of chromsomes: "chr" + chromnum + "_random_" + typename)
     */
    boolean isSplit;
    boolean isPos;		/* True if table is positional. */
    boolean hasBin;		/* True if table has bin field (name is always "bin"). */
    boolean hasCDS;		/* True if it has cdsStart,cdsEnd fields. */
    boolean hasBlocks;		/* True if it has count,starts,ends/sizes. */


    //    String name;	        /* Name without chrN_. */
    String other_name;          /* Another name -- for example "geneName" field for refFlat */
    String chromField;	        /* Name of chromosome field. */
    String startField;      	/* Name of chromosome start field. */
    String endField;		/* Name of chromosome end field. */
    String nameField;	 	/* Name of item name field. */
    String otherNameField;      /* Name of "other name" field. */
    String scoreField;    	/* Name of score field. */
    String strandField;  	/* Name of strand field. */
    String cdsStartField;	/* Name of cds(thick)Start field. */
    String cdsEndField;	        /* Name of cds(thick)End field. */
    String blockCountField;  	/* Name of exon(block)Count field. */
    String blockStartsField; 	/* Name of exon(block)Starts field. */
    /* blockEndsSizesField currently does double duty, in some cases indicating field name for block lengths 
       and in other cases for block end coords 
       which it is depends on the field name...  maybe should split into endsField and lengthsField to disambiguate??
     */
    //    String blockEndsSizesField;	/* Name of exon(block)Ends(Sizes) field. */
    String blockEndsField;	/* Name of exon(block)Ends field. */
    String blockSizesField;	/* Name of exon(block)Sizes) field. */
    String spanField;		/* Name of span field. (wiggle) */

    protected boolean has_extra_fields = false;
    protected String track_table_type = null;
    protected int track_table_subtype = -1;  // -1 for no or unkwown subtype

    protected String split_table_exemplar = null;
    protected List<String> linked_tables = null;

    TrackType track_type = TrackType.UNKNOWN;  /* A guess at the trackDb type for this (BED3, BED12, PSL, etc.) */
    //    Das2Version version;
    UcscVersion ucsc_version;
    Map<String, Integer> fields;

    public UcscType(URI base_uri, UcscVersion version, ResultSet rs, String group, 
	String table_name,
	String split_table_exemplar)
	throws SQLException  {
	this(base_uri, version, rs, group, table_name);
	if (split_table_exemplar != null)  {
	    isSplit = true;
	    this.split_table_exemplar = split_table_exemplar;
	    //	    System.out.println("type uses split_table: " + this.getLocalURIString());
	}
	else  {

	}
    }

    public UcscType(URI base_uri, UcscVersion version, ResultSet rs, String group, String table_name) throws SQLException  {
	super(base_uri,                    // base_uri
	      rs.getString("tableName"),  // local_uri_string
	      null, // rs.getString("shortLabel"),  // title
	      rs.getString("longLabel"),   // description
	      null,           // info_url
	      null,                        // ontology_term_name (will get assigned ROOT_TYPE if null)
	      null,                        // method
	      true);                       // is_searchable
	ucsc_version = version;
	this.table_name = table_name;
	
	// do substitutions to get proper title
	// handling title here, to use grouping info
	String short_title = rs.getString("shortLabel");

	short_title = short_title.replace('/', ':');
	String fixed_title = group + "/" + short_title;
	fixed_title = doVarSubstitutions(fixed_title, rs);
	this.setTitle(fixed_title);

	// handling description here
	String fixed_description = rs.getString("longLabel");
	fixed_description = doVarSubstitutions(fixed_description, rs);
	this.setDescription(fixed_description);

	String track_type_field = rs.getString("type");	
	setTrackTableType(track_type_field);
    }

    protected String doVarSubstitutions(String input, ResultSet rs) throws SQLException  {
	String output = input;
	if (output.contains("$Organism"))  {
	    output = output.replace("$Organism", ucsc_version.getOrganism());
	}
	if (output.contains("$o_"))  {
	    String[] track_type_fields = rs.getString("type").split(" ");	
	    if (track_type_fields != null && track_type_fields.length >= 2)  {
		String other_db = track_type_fields[1];
UcscSourcesCapability sources_cap = ucsc_version.ucsc_source.getSourcesCapability();
		UcscVersion other_version = (UcscVersion)sources_cap.getVersion(other_db);
		if (output.contains("$o_db"))  {
		    output = output.replace("$o_db", other_db);
		}
		if ((output.contains("$o_Organism")) && (other_version != null))  {
		    output = output.replace("$o_Organism", other_version.getOrganism());
		}
		if ((output.contains("$o_date")) && (other_version != null))  {
		    // for UcscVersion description field holds date
		    output = output.replace("$o_date", other_version.getDescription());
		}		
	    }
	}
	return output;
    }

    public final String getChromField()  { 
	if (! initialized)  { initialize(); }
	return chromField; 
    }
    
    public final String getStartField()  { 
	if (! initialized)  { initialize(); }
	return startField; 
    }

    public final String getEndField()  { 
	if (! initialized)  { initialize(); }
	return endField; 
    }

    public final boolean hasBin()  { 
	if (! initialized)  { initialize(); }
	return hasBin; 
    }
    
    public String getName() { return this.getLocalURIString(); }

    //    public Das2Version getVersion()  { return version; }
    //    public final boolean hasPosition()  { return isPos; }
    public String getTableName()  { return table_name; }
    public boolean isSplit()  { return isSplit; }
    public boolean hasCDS()  { return hasCDS; }
    public boolean hasBlocks()  { return hasBlocks; }
    public boolean hasBlockEnds()  { return (blockEndsField != null); }
    public boolean hasBlockSizes()  { return (blockSizesField != null); }
    public boolean hasOtherName()  { return (otherNameField != null); }
    public TrackType getTrackType()  { return track_type; }


    protected void setTitle(String str)  {
	// setting title field inherited from genomancer.vine.das2.client.modelimpl.Das2CommonData
	//    (usually set in constructor, but needs modification for UcscType)
	this.title = str;
    }

    protected void setDescription(String str)  {
	// setting description field inherited from genomancer.vine.das2.client.modelimpl.Das2CommonData
	//   (usually set in constructor, but needs modification for UcscType)
	this.description = str;
    }

    public synchronized boolean initialize()  {
	boolean success = false;
	fields = new LinkedHashMap<String, Integer>();
	String table_name;
	if (isSplit)  { table_name = split_table_exemplar; }
	else  { table_name = this.getName(); }
	try  {
	    Connection conn = ucsc_version.getDbConnection();
	    Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery("show columns from " + table_name);
            int index = 1;

            while (rs.next()) {
                String field = rs.getString("Field");
		fields.put(field, new Integer(index));
		index++;
            }
	}
	catch (SQLException ex)  {
	    ex.printStackTrace();
	}

	/* Look for bed-style or linkedFeatures names. */
	if (fields.containsKey("chrom") && fields.containsKey("chromStart") && fields.containsKey("chromEnd"))  {
	    chromField = "chrom";
	    startField = "chromStart";
	    endField = "chromEnd";
	    chrom_col = ((Integer)fields.get("chrom")).intValue(); 
	    start_col = ((Integer)fields.get("chromStart")).intValue(); 
	    end_col = ((Integer)fields.get("chromEnd")).intValue(); 
	    if (fields.containsKey("name"))  { 
		nameField = "name"; 
		name_col = ((Integer)fields.get("name")).intValue(); }
	    else if (fields.containsKey("acc"))  { 
		nameField = "acc"; 
		name_col = ((Integer)fields.get("acc")).intValue(); }
	    else if (fields.containsKey("frag"))  { 
		nameField = "frag"; 
		name_col = ((Integer)fields.get("frag")).intValue(); }
	    else if (fields.containsKey("contig"))  { 
		nameField = "contig"; 
		name_col = ((Integer)fields.get("contig")).intValue(); }
	    if (fields.containsKey("score"))  { 
		scoreField = "score"; 
		score_col = ((Integer)fields.get("score")).intValue(); }
	    if (fields.containsKey("strand"))  { 
		strandField = "strand"; 
		strand_col = ((Integer)fields.get("strand")).intValue(); }
	    if (fields.containsKey("thickStart"))  { 
		cdsStartField = "thickStart"; 
		cds_start_col = ((Integer)fields.get("thickStart")).intValue(); }
	    if (fields.containsKey("thickEnd"))  { 
		cdsEndField = "thickEnd"; 
		cds_end_col = ((Integer)fields.get("thickEnd")).intValue(); }
	    if (fields.containsKey("blockCount"))  { 
		blockCountField = "blockCount"; 
		block_count_col = ((Integer)fields.get("blockCount")).intValue(); }
	    else if (fields.containsKey("lfCount"))  { 
		blockCountField = "lfCount"; 
		block_count_col = ((Integer)fields.get("lfCount")).intValue(); } 
	    if (fields.containsKey("chromStarts"))  {
		blockStartsField = "chromStarts"; 
		block_starts_col = ((Integer)fields.get("chromStarts")).intValue(); }
	    else if (fields.containsKey("blockStarts"))  { 
		blockStartsField = "blockStarts"; 
		block_starts_col = ((Integer)fields.get("blockStarts")).intValue(); }
	    else if (fields.containsKey("lfStarts"))  { 
		blockStartsField = "lfStarts"; 
		block_starts_col = ((Integer)fields.get("lfStarts")).intValue(); }
	    if (fields.containsKey("blockSizes"))  { 
		blockSizesField = "blockSizes"; 
		block_sizes_col = ((Integer)fields.get("blockSizes")).intValue(); }
	    else if (fields.containsKey("lfSizes"))  {
		blockSizesField = "lfSizes"; 
		block_sizes_col = ((Integer)fields.get("lfSizes")).intValue(); }
	    if (fields.containsKey("span"))  { 
		spanField = "span"; 
		span_col = ((Integer)fields.get("span")).intValue(); }
	}
	/* Look for names of psl and psl-like (chain, chainLink, net, altGraphX,
	   some older types). */
	else if (fields.containsKey("tName") && fields.containsKey("tStart") && fields.containsKey("tEnd"))  {
	    chromField = "tName";
	    startField = "tStart";
	    endField = "tEnd";
	    chrom_col = ((Integer)fields.get("tName")).intValue(); 
	    start_col = ((Integer)fields.get("tStart")).intValue(); 
	    end_col = ((Integer)fields.get("tEnd")).intValue(); 
	    if (fields.containsKey("qName"))  { 
		nameField = "qName"; 
		name_col = ((Integer)fields.get("qName")).intValue(); }
	    else if (fields.containsKey("name"))  { 
		nameField = "name"; 
		name_col = ((Integer)fields.get("name")).intValue(); }
	    else if (fields.containsKey("chainId"))  { 
		nameField = "chainId"; 
		name_col = ((Integer)fields.get("chainId")).intValue(); }
	    if (fields.containsKey("strand"))  { 
		strandField = "strand"; 
		strand_col = ((Integer)fields.get("strand")).intValue(); }
	    if (fields.containsKey("blockCount"))  { 
		blockCountField = "blockCount";
		block_count_col = ((Integer)fields.get("blockCount")).intValue(); }
	    if (fields.containsKey("tStarts"))  { 
		blockStartsField = "tStarts"; 
		block_starts_col = ((Integer)fields.get("tStarts")).intValue(); }
	    if (fields.containsKey("blockSizes"))  { 
		blockSizesField = "blockSizes"; 
		block_sizes_col = ((Integer)fields.get("blockSizes")).intValue(); }
	}
	/* Look for gene prediction names. */
	else if (fields.containsKey("chrom") && fields.containsKey("txStart") && fields.containsKey("txEnd"))  {
	    chromField = "chrom";
	    startField = "txStart";
	    endField = "txEnd";
	    chrom_col = ((Integer)fields.get("chrom")).intValue(); 
	    start_col = ((Integer)fields.get("txStart")).intValue(); 
	    end_col = ((Integer)fields.get("txEnd")).intValue(); 
	    if (fields.containsKey("name"))  {
		nameField = "name";
		name_col = ((Integer)fields.get("name")).intValue(); 
		if (fields.containsKey("geneName"))  { // tweak for refFlat type
		    otherNameField = "geneName";
		    other_name_col = ((Integer)fields.get("geneName")).intValue(); 
		}
	    }
	    else if (fields.containsKey("geneName"))  { // tweak for refFlat type
		nameField = "geneName";
		name_col = ((Integer)fields.get("geneName")).intValue(); }
	    if (fields.containsKey("score"))  {
		scoreField = "score";
		score_col = ((Integer)fields.get("score")).intValue(); }
	    if (fields.containsKey("strand"))  { 
		strandField = "strand"; 
		strand_col = ((Integer)fields.get("strand")).intValue(); }
	    if (fields.containsKey("cdsStart"))  { 
		cdsStartField = "cdsStart"; 
		cds_start_col = ((Integer)fields.get("cdsStart")).intValue(); }
	    if (fields.containsKey("cdsEnd"))  { 
		cdsEndField = "cdsEnd"; 
		cds_end_col = ((Integer)fields.get("cdsEnd")).intValue(); } 
	    if (fields.containsKey("exonCount"))  { 
		blockCountField = "exonCount"; 
		block_count_col = ((Integer)fields.get("exonCount")).intValue(); }
	    if (fields.containsKey("exonStarts"))  { 
		blockStartsField = "exonStarts"; 
		block_starts_col = ((Integer)fields.get("exonStarts")).intValue(); }
	    if (fields.containsKey("exonEnds"))  { 
		blockEndsField = "exonEnds"; 
		block_ends_col = ((Integer)fields.get("exonEnds")).intValue(); }
    }
	/* Look for repeatMasker names. */
	else if (fields.containsKey("genoName") && fields.containsKey("genoStart") && fields.containsKey("genoEnd"))  {
	    chromField = "genoName";
	    startField = "genoStart";
	    endField = "genoEnd";
	    chrom_col = ((Integer)fields.get("genoName")).intValue(); 
	    start_col = ((Integer)fields.get("genoStart")).intValue(); 
	    end_col = ((Integer)fields.get("genoEnd")).intValue(); 
	    if (fields.containsKey("repName"))  { 
		nameField = "repName"; 
		name_col = ((Integer)fields.get("repName")).intValue(); }
	    if (fields.containsKey("swScore"))  { 
		scoreField = "swScore"; 
		score_col = ((Integer)fields.get("swScore")).intValue(); }
	    if (fields.containsKey("strand"))  { 
		strandField = "strand"; 
 		strand_col = ((Integer)fields.get("strand")).intValue(); }
	}
	/*  catchall for other data types that have a "chrom" and "chromStart" field but not "chromEnd"
	    (could have merged this into bed-style conditional, but trying to emulate overall structure 
	    of hdb.c-->hFindBed12FieldsAndDb() for maintenance purposes
	*/
	else if (fields.containsKey("chrom") && fields.containsKey("chromStart"))  {
	    chromField = "chrom";
	    startField = "chromStart";
	    chrom_col = ((Integer)fields.get("chrom")).intValue(); 
	    start_col = ((Integer)fields.get("chromStart")).intValue(); 
	}
	/* covering some edge case with per-chromosome tables and starts/ends but no chromField ?? */
	else if (isSplit &&
		 table_name.endsWith("_gl") &&
		 fields.containsKey("start") && fields.containsKey("end") )  {
	    // leaving chromField = null
	    startField = "start";
	    endField = "end";
	    start_col = ((Integer)fields.get("start")).intValue(); 
	    end_col = ((Integer)fields.get("end")).intValue(); 
	    if (fields.containsKey("frag"))  { 
		nameField = "frag"; 
		name_col = ((Integer)fields.get("frag")).intValue(); }
	    if (fields.containsKey("strand"))  { 
		strandField = "strand"; 
		strand_col = ((Integer)fields.get("strand")).intValue(); }
	}

	if (nameField == null)  {
	    if (fields.containsKey("acc"))  { 
		nameField = "acc"; 
		name_col = ((Integer)fields.get("acc")).intValue(); }
	    else if (fields.containsKey("id"))  { 
		nameField = "id"; 
		name_col = ((Integer)fields.get("id")).intValue(); }
	    else if (fields.containsKey("name"))  { 
		nameField = "name"; 
		name_col = ((Integer)fields.get("name")).intValue(); }
	}

	/* bin indexing field is always called bin 
	 *   (and always the first field/column, but setting bin_col here just as a precaution)
	 */ 
	hasBin = (fields.containsKey("bin"));
	if (hasBin)  { bin_col = ((Integer)fields.get("bin")).intValue(); }

	// isSplit = (! root_table_name.equals(actual_table_name));
	//	isPos = (startField != null);
	hasCDS = (cdsStartField != null);
	hasBlocks = (blockStartsField != null);

	// need to replace setting of TrackType here with call to setTrackTableType(), and rely on 
	//   info in trackDb
	if (hasBlocks)  {
	    if (blockStartsField.equals("exonStarts")) { track_type = TrackType.GENEPRED; }
	    else if (blockStartsField.equals("chromStarts") || blockStartsField.equals("blockStarts")) {
		track_type = TrackType.BED12; } 
	    else if (blockStartsField.equals("lfStarts")) { track_type = TrackType.LINKEDFEATURES; }
	    else if (blockStartsField.equals("tStarts")) { track_type = TrackType.PSL; }
	}
	if (track_type == TrackType.UNKNOWN)  {
	    if (cdsStartField != null)  { track_type = TrackType.BED8; }
	    else if (chromField == null && strandField != null)  { track_type = TrackType.GL; }
	    else if (strandField != null)  { track_type = TrackType.BED6; }
	    else if (spanField != null)  { track_type = TrackType.WIGGLE; }
	    else if (nameField != null)  { track_type = TrackType.BED4; }
	    else if (endField != null)  { track_type = TrackType.BED3; }
	    else {
		track_type = TrackType.CHROMGRAPH;
		// endField is set like this in hdb.c hFindTableInfoDb() but I don't think it's needed
		//  endField = startfield + "+1";   
	    }
	}
	System.out.println(this.getName() + ": " + track_type);
	/*
	// only using types that have a handler for basic das2xml format
	//	if (track_type.getHandler("das2xml") != null)  {
        FormatHandler handler = (FormatHandler)track_type.getHandler("das2xml");
        if (handler != null)  {
            version.addType(this);
        }
	*/
	initialized = true;
	return success;
    }

    /**
     *  returns null if str can't be parsed as an integer
     */
    public static Integer getInteger(String str)  {
	Integer result = null;
	try  {
	    result = new Integer(str);
	}
	catch (NumberFormatException ex)  {
	    result = null;
	}
	return result;
    }


    protected void setTrackTableType(String track_type_field)  {
	String[] type_fields = track_type_field.split(" ");
	if (type_fields != null && type_fields.length >= 1)  {
	    track_table_type = type_fields[0];
	    if (track_table_type.equals("bed") || 
		track_table_type.equals("bedGraph"))  {
		if (type_fields.length >= 2)  {
		    String second_field = type_fields[1];
		    if (second_field.equals("."))  {
			has_extra_fields = false;
		    }
		    else if (second_field.equals("+"))  {
			has_extra_fields = true;
		    }
		    else  {
			Integer second_field_int = getInteger(second_field);
			if (second_field_int != null)  { 
			    track_table_subtype = second_field_int.intValue();
			}
			else  {
			    track_table_subtype = 3;
			}
		    }
		}
		if (type_fields.length >= 3)  {
		    String third_field = type_fields[2];
		    if (third_field.equals("."))  { has_extra_fields = false; }
		    else if (third_field.equals("+"))  { has_extra_fields = true; }
		}
	    }
	}
	/**
	 *  handling of trackDb "type" field
	 *  
	 *  type field has multiple entries separated by whitespace (single spaces?)
	 *  handling of all but first entry is determined by first entry
	 *  first entry = 
	 *
	 *    NULL/BLANK ==> ???  these appear to not actually have tables 
	 *         BUT, each one's tableName (minus optional suffix "Super") is prefix for a group of other tables
	 *         so maybe these are only here as a grouping mechanism  (check UCSC genome browser for grouping)
	 *         if so then could use for grouping via appending as path to DAS2 type "title" attribute 
	 *       looks like some trackDb rows that _do_ have entry in "type" field also have no tables:
	 *             example: encodeUtexChip (type="bedGraph 4")
	 *             but have 8 tables that start with "encodeUtexChip", for example: encodeUtexChipHeLaMycPeaks
	 *             and none of these tables starting "encodeUtexChip" are listed in trackDb
	 *           some, like "encodeUtexChip" have a similar entry *Super ("encodeUtexChipSuper") in trackDb 
	 *                   with no "type" field
	 *           but others, like "encodeUWRegulomeBase", have no *Super counterpart
	 *   
	 *    altGraphX    
	 *    bed [int] [+|.]
	 *    bed5FloatScore
	 *    bed5FloatScoreWithFdr
	 *    bedGraph [int]
	 *    chain {genome_id}
	 *    clonePos
	 *    coloredExon
	 *    ctgPos
	 *    expRatio
	 *    genePred [linked_table]*
	 *    Id2
	 *    netAlign {genome_id} {linked_chain_table}
	 *    psl [.] [align_lib??]
	 *    rmsk
	 *    wig {min_score} {max_score}
	 *    wigMaf {min_score} {max_score}
	 *    
	 *         
	 *    bed ==> BED3, BED4, BED5, BED6, BED8, BED9, BED12, BED15
	 *            2nd entry is int indicating how many fields of track table follow bed format
	 *                 (note that this doesn't include possible "bin" field inserted as first field)
	 *            3rd entry [optional [+|.]] ==> if not present, then assume "."
	 *                                     if ".", then assume no extra fields beyond those indicated in 2nd field
	     *                                     if "+", then extra fields that don't follow bed spec
	 *    bedGraph ==> 2nd entry is int indicating how many fields of track table follow bed format 

	 *    Examples from hg18:
	 *    altGraphX ==> sibTxGraph (only instance of "altGraphX" type)  
	 *          seqid="tName", min="tStart", max="tEnd", annotid="name"
	 *    bed . ==> enocodeYaleChIPSTAT1Sites
	 *              (all "bed ." in hg18 currently sem to be bed3 (w/bin), but not sure if this holds)

	 *    bed 12 ==> kiddEichlerDiscAbc13
	 *    bed 12 + ==> encodeBuFirstExonCerebrum (has one extra column, scoreValue (a 2nd score))
	 *    bed 12 . ==> rnaCluster
	      bed 3
	      bed 3 .
	      bed 3 +
	      bed 4
	      bed 4 .
	      bed 4 + 
	      bed 5
	      bed 5 . 
	      bed 5 + 
	      bed 6 . 
	      bed 6 +
	      bed 8 . 
	      bed 8 + 
	      bed 9 .
	      bed 9 +
	      bed5FloatScore ==> encodeYaleChipSitesFos (bed5 + extra "floatScore" column for 2nd score)
	      bed5FloatScoreWithFdr ==> encodeYaleChipSitesPol2Hela (bed5FloatScore + extra "fdr" column for 3rd score)
	      bedGraph 4 ==> encodeRikenCagePlus (bed3 + extra "dataValue" column for 1rst score)
	                                         (can also think of as bed5 - "name", w/ "score"-->"dataValue")
	      bedGraph 5 ==> encodeUWRegulomeBaseCD4 (bed4 + extra "dataValue" column for 1rst score
	                                             (can also htink of as bed5 w/ "score" --> "dataValue")
	      chain XYZ ==> chr21_chainAnoCar1, etc.
	           tName/tStart/tEnd,     qName/qStart/qEnd/qStrand
	      clonePos ==> clonePos (only instance of "clonePos" type)
	           chrom/chromStart/chromEnd/name
	      coloredExon ==> contrastGene (only instance of "coloredExon" type)
	            (bed12 + expCount, expIds columns)
	      ctgPos ==> ctgPos (only instance of "ctgPos" type)
	            chrom/chromStart/chromEnd, id="contig"
	      expRatio ==> affyHumanExon 
	            (bed12 + expCount, expIds, expScores columns for individual experiments)
	      genePred ==> vegaGene
	            remember, genePred uses exonStarts/exonEnds instead of blockStart/blockSize for children
	      Id2 ==> hapmapLdPhYri 
                    (bed4 + idCount, dprime, rsquared, lod, avgDprime, avgRsquared, avgLod, tInt)
	      netAlign ==> 
	           tName/tStart/tEnd/strand,   qName/qStart/qEnd
	      psl ==> mgcFullMrna
	          need to check psl flip thing...
	      psl . ==> HInvGeneMrna
	      rmsk ==> rmsk (only instance of "rmsk" type)
	           genoName/genoStart/genoEnd/strand
		   id derived from genoName/genoStart/genoEnd?
		   title derived from repName/repClass/repFamily?
	      wig ==> encodeBUORChID
	      wigMaf ==> multiz28way

	      
	 *    
	 *    bed [int] [+|.]
	 *    bed5FloatScore
	 *    bed5FloatScoreWithFdr
	 *    bedGraph [int]
	 *    chain {genome_id}
	 *    clonePos
	 *    coloredExon
	 *    ctgPos
	 *    expRatio
	 *    genePred [linked_table]*
	 *    Id2
	 *    netAlign {genome_id} {linked_chain_table}
	 *    psl [.] [align_lib??]
	 *    rmsk
	 *    wig {min_score} {max_score}
	 *    wigMaf {min_score} {max_score}
       
	 *
	 */

      /*
       String table_type_name = type_fields[0];
	//	String table_type_extras = type_fields(1);
 String track_type = TrackType.getTrackType(table_type_name);
	if (track_type == TrackType.GENEPRED)  {

	}
	else if (track_type == BED3 || 
		 track_type == BED4 || 
		 track_type == BED5 || 
		 track_type == BED6 || 
		 track_type == BED8 || 
		 track_type == BED9 || 
		 track_type == BED12 || 
		 track_type == BED15 )  {

	}
 */
	/*
	has_extra_fields = (type_fields[1].equals("+"));
	int table_link_count = type_fields.length - 2;
	if (table_link_count > 0)  {
	    linked_tables = new ArrayList<String>();
	    for (int i=2; i<type_fields.length; i++)  {
              linked_tables.add(type_fields[i]);
	    }
	}
	System.out.println("type: " + getName() + 
			   ", has_extra_fields: " + has_extra_fields + 
			   ", linked_tables: " + linked_tables);
	*/
    }
}