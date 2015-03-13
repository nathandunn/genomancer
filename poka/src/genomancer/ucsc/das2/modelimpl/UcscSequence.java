package genomancer.ucsc.das2.modelimpl;

import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.vine.das2.client.modelimpl.Das2Segment;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;


public class UcscSequence extends Das2Segment implements Das2SegmentI  {
    String filename;
    UcscVersion ucsc_version;

    UcscSequence(URI base_uri, UcscVersion ucsc_version, ResultSet rs) throws SQLException {
	super(base_uri,                // base_uri
	      rs.getString("chrom").replace(' ', '_'),  // local_uri_string
	      rs.getString("chrom"),                    // title
	      null,               // reference
	      rs.getInt("size"),  // length
	      null );             // info_url
	this.filename = rs.getString("filename");
	this.ucsc_version = ucsc_version;
	//	System.out.println("new sequence: " + this.getAbsoluteURIString());
    }

    public String getName()  { return this.getLocalURIString(); }
    public String getFileName()  { return filename; }
    
}