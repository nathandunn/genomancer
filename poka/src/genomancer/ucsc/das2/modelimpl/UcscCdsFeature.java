package genomancer.ucsc.das2.modelimpl;

import java.net.URI;
import java.util.Date;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.vine.das2.client.modelimpl.Das2Feature;

public class UcscCdsFeature extends Das2Feature  {
    Das2LocationI cds_loc = null;

     public UcscCdsFeature(URI base_uri,
	String local_uri_string,
	String title,
	String description,
	String info_url,
	Das2TypeI type,
	Date creation_date,
	Date last_modified_date) {
	 super(base_uri, local_uri_string, title, description, info_url, type, creation_date, last_modified_date);
     }

    public void setCds(Das2LocationI loc)  {
	cds_loc = loc;
    }
    public Das2LocationI getCds()  {
	return cds_loc; 
    }

}