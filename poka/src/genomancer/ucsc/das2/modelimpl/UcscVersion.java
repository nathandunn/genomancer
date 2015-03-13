package genomancer.ucsc.das2.modelimpl;

import genomancer.trellis.das2.model.Das2SourceI;
import genomancer.trellis.das2.model.Das2VersionI;
import genomancer.vine.das2.client.modelimpl.Das2Version;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


public class UcscVersion extends Das2Version implements Das2VersionI  {

   static boolean DEBUG = false;
    static boolean TESTING = false;
    static Pattern split_table_pattern = Pattern.compile("^chr.+_(random_)?");
    static Set tables_to_test = new HashSet();
    
    static {
	tables_to_test = new HashSet();
	tables_to_test.add("refGene");  // GENEPRED
	tables_to_test.add("chr10_mrna");  // PSL & split
	tables_to_test.add("firstEF");  // BED6
    }

    String name;
    /*
     *  id is a relative URI based on name, just with spaces substituted with underscores
     *  Currently in the UCSC database all names should just be ASCII a-zA-Z0-9 chars, but 
     *      doing space substitution just in case
     */
    String id;  
    String ucsc_description;
    String nibPath;
    String organism;
    String defaultPos;
    int active;
    int orderKey;
    String genome;
    String scientificName;
    String htmlPath;
    int hgNearOk;
    int hgPbOk;
    String sourceName;
    String clade;
    int priority;
    UcscSource ucsc_source;  // inheriting Das2Source source from Das2Version
    boolean types_initialized = false;
    boolean segs_initialized = false;

/**
 * 
 * @param rs ResultSet from hgcentral dbDb and genomeClade table join
 *  
 * select * from dbDb, genomeClade where 
 *          dbDb.genome = genomeClade.genome 
 *     and  dbDb.active = 1 
 *     order by dbDb.orderKey";

 * 
 */
    public UcscVersion(UcscSource ucsc_source, ResultSet rs) throws SQLException { 
	//    public Das2Version(ResultSet rs, Das2Source dsource) throws SQLException {
	super(ucsc_source,                                  // source
	      rs.getString("name").replace(' ', '_'),  // local_uri_string
	      rs.getString("name"),                    // title
	      (rs.getString("genome") + ", " + rs.getString("description")),   // description
	      ("http://genome.ucsc.edu/cgi-bin/hgGateway?org=" + 
	       rs.getString("organism") + "&db=" + rs.getString("name")), // info_url
	      null,        // creation_date
	      null         // last_modified_date
	      );       

        name = rs.getString("name");  
	id = name.replace(' ', '_');
        ucsc_description = rs.getString("description");  // used to construct DAS2 description
        nibPath = rs.getString("nibPath");
        organism = rs.getString("organism");  
        defaultPos = rs.getString("defaultPos");
        active = rs.getInt("active");
        orderKey = rs.getInt("orderKey");
        genome = rs.getString("genome");  // used to construct DAS2 description
        scientificName = rs.getString("scientificName");  // used to construct DAS2 description
        htmlPath = rs.getString("htmlPath");  // can be used to construct info_url?
        hgNearOk = rs.getInt("hgNearOk");
        hgPbOk = rs.getInt("hgPbOk");
        sourceName = rs.getString("sourceName");  // used to construct DAS2 description
	clade = rs.getString("clade");
	priority = rs.getInt("priority");
	this.ucsc_source = ucsc_source; 
	//	source.addVersion(this);
    }

    public UcscSource getSource()  { return ucsc_source; }

    public String getName() { return name; }
    public String getID()  { return id; }
    public int getActive() { return active; }
    public String getDefaultPos() { return defaultPos; }
    public String getDescription() { return description; }  // currently holds date
    public String getGenome() { return genome; }
    public int getHgNearOk() { return hgNearOk; }
    public int getHgPbOk() { return hgPbOk; }
    public String getHtmlPath() { return htmlPath; }
    public String getNibPath() { return nibPath; }
    public int getOrderKey() { return orderKey; }
    public String getOrganism() { return organism; }
    public String getScientificName() { return scientificName; }
    public String getSourceName() { return sourceName; }
    public String getClade()  { return clade; }
    public int getPriority()  { return priority; }

    public Connection getDbConnection() throws SQLException  {
	return getSource().getSourcesCapability().getDbConnection(getName());
    }
}