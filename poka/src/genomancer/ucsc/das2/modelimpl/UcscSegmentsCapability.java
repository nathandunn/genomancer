package genomancer.ucsc.das2.modelimpl;

import genomancer.trellis.das2.model.Das2CoordinatesI;
import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2SegmentsCapabilityI;
import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import genomancer.vine.das2.client.modelimpl.Das2Coordinates;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2SegmentsResponse;
import genomancer.vine.das2.client.modelimpl.Das2Version;
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


public class UcscSegmentsCapability extends Das2GenericCapability implements Das2SegmentsCapabilityI  {
    static protected int SEQ_COUNT_THRESHOLD = UcscSourcesCapability.max_seqs_allowed;
    Das2SegmentsResponseI segments_response = null;
    UcscVersion ucsc_version = null;
    //  shouldn't need uri2segment loop anymore -- same thing is done in Das2SegmentsResponse...
    Map<URI,Das2SegmentI> uri2segment = null;
    boolean initialized = false;
    int seq_count = 0;

    public long getLastModified()  {
        try {
            System.out.println("in UcscSegmentsCapability.getLastModified()");
            if (!initialized) {
                initSegments();
            }
            String genome_name = ucsc_version.getName();
            String seq_table = "chromInfo";
            String mod_time_query = "SELECT UPDATE_TIME FROM INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA = '" + genome_name + "' AND TABLE_NAME = '" + seq_table + "'";

	    String alt_mod_time_query = "SHOW TABLE STATUS LIKE '" + seq_table + "'";
	    Connection conn = ucsc_version.getDbConnection();
            Statement stmt = conn.createStatement();
            ResultSet mod_time = stmt.executeQuery(mod_time_query);
            mod_time.next();
            Timestamp tstamp = mod_time.getTimestamp(1); // Timestamp is JDBC subclass of java.util.Date
	    long time = tstamp.getTime();
            System.out.println("SEQ TABLE MOD TIME for " + genome_name + ":  " + tstamp);
	    mod_time.close();
	    stmt.close();
	    conn.close();
            return time;
        } catch (SQLException ex) {
            Logger.getLogger(UcscSegmentsCapability.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
    }

    public UcscSegmentsCapability(UcscVersion version, Das2CoordinatesI coords, ResultSet rs) {
        super(version.getBaseURI(), (version.getLocalURIString()+"/segments"), "segments", version, coords);
	ucsc_version = version;
    }
    
    public Das2SegmentsResponseI getSegments() {
	if (! initialized)  { initSegments(); }
	return segments_response;
    }

    public Das2SegmentI getSegment(URI segment_uri)   {
	if (! initialized)  { initSegments(); }
	return uri2segment.get(segment_uri);
	//  shouldn't need uri2segment loop anymore -- same thing is done in Das2SegmentsResponse...
	//  return segments_response.getSegment(segment_uri);

    }

    public String getResidues(Das2LocationI location) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getResidues(List<Das2LocationI> locations) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

   protected synchronized boolean initSegments()  {
       // only initialized once -- if want effect of re-initializing, must create new SegmentsCapability instead
       if (initialized)  { return true; }  
       boolean success = false;
       uri2segment = new HashMap<URI,Das2SegmentI>();
       List<Das2SegmentI> segments = new ArrayList<Das2SegmentI>();
       try  {
	   //Connection conn = Das2Servlet.getDbConnection(name);
	   URI seq_base_uri = this.getAbsoluteURI().resolve("./");
	   Connection conn = ucsc_version.getDbConnection();
	   Statement stmt = conn.createStatement();
	   ResultSet status = stmt.executeQuery("show table status like 'chromInfo'");
	   status.next();
	   seq_count = status.getInt("Rows");
	   status.close();
	   System.out.println("chromInfo row count: " + seq_count);
	   if (seq_count > SEQ_COUNT_THRESHOLD)  {
	       System.out.println("WARNING: large number of sequences for " + version.getLocalURIString()
				  + ", count = " + seq_count);
	       // alternative to loading all seqs -- 
	       //   maybe set to respond with TOO_LARGE status, and rather than loading all seqs 
	       //   do conditional in getSegment() to just retrieve desired??
	       // more drastic alternative -- move seq_count check to UcscSourcesCapability and 
	       //    if too large then don't list a "segments" capability?
	   }

	   ResultSet rs = stmt.executeQuery("select * from chromInfo");
	   while (rs.next())  {
	       UcscSequence seq = new UcscSequence(seq_base_uri, ucsc_version, rs);
	       segments.add(seq);
	   }
	   rs.close();
	   stmt.close();
	   conn.close();
	   segments_response = new Das2SegmentsResponse(seq_base_uri, segments, null, null);
	   //  shouldn't need uri2segment loop anymore -- same thing is done in Das2SegmentsResponse...
	   for (Das2SegmentI seg : segments)  {
	       // for partial genomes with lots of contigs (>10K, sometimes >200K) 
	       //    creating this hash requires _lots_ of memory overhead
	       //    (actually its the call to seg.getAbsoluteURI(), which triggers 
	       //    unique URI creation (plus several strings?))
	       uri2segment.put(seg.getAbsoluteURI(), seg);
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