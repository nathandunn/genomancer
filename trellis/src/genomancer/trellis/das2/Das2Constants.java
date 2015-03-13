package genomancer.trellis.das2;

import java.net.URI;

public class Das2Constants  {
    public static String DAS2_NAMESPACE = "http://biodas.org/documents/das2";

//    public static String SOURCES_CONTENT_TYPE = "application/x-das-sources+xml; charset=utf-8";
    // public static String SOURCES_CONTENT_TYPE = "application/x-das-sources+xml";
    public static String SOURCES_CONTENT_TYPE = "application/xml; charset=utf-8";
    // public static String SOURCES_CONTENT_TYPE = "text/xml; charset=utf-8";
    //    public static String SOURCES_CONTENT_TYPE = "application/xml";
    public static String SEGMENTS_CONTENT_TYPE = "application/xml; charset=utf-8";
    // public static String SEGMENTS_CONTENT_TYPE = "application/x-das-segments+xml; charset=utf-8";
//    public static String TYPES_CONTENT_TYPE = "application/x-das-types+xml; charset=utf-8";
        public static String TYPES_CONTENT_TYPE = "application/xml; charset=utf-8";
//    public static String FEATURES_CONTENT_TYPE = "application/x-das-features+xml; charset=utf-8";
	    public static String FEATURES_CONTENT_TYPE = "application/xml; charset=utf-8";

    public static String DAS2_SOURCES_CAPABILITY = "sources";
    public static String DAS2_FEATURES_CAPABILITY = "features";
    public static String DAS2_TYPES_CAPABILITY = "types";
    public static String DAS2_SEGMENTS_CAPABILITY = "segments";
    public static String ROOT_TYPE_ONTOLOGY_TERM = "SO:0000110";   // sequence_feature SO term
    public static String LOCATED_TYPE_ONTOLOGY_TERM = "SO:0000110";  // sequence_feature SO term
    public static URI ROOT_TYPE_ONTOLOGY_URI; 
    public static URI LOCATED_TYPE_ONTOLOGY_URI; 

    static {
	try  {
	    ROOT_TYPE_ONTOLOGY_URI =  new URI(ROOT_TYPE_ONTOLOGY_TERM);
	    LOCATED_TYPE_ONTOLOGY_URI = new URI(LOCATED_TYPE_ONTOLOGY_TERM);
	}
	catch (Exception ex)  { ex.printStackTrace(); }
    
    }

}