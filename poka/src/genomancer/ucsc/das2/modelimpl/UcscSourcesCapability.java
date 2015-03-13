package genomancer.ucsc.das2.modelimpl;

import genomancer.trellis.das2.model.Das2CoordinatesI;
import genomancer.trellis.das2.model.Das2SourceI;
import genomancer.trellis.das2.model.Das2SourcesCapabilityI;
import genomancer.trellis.das2.model.Das2SourcesResponseI;
import genomancer.trellis.das2.model.Das2VersionI;
import genomancer.util.TimeCheck;
import genomancer.vine.das2.client.modelimpl.Das2Coordinates;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2SourcesResponse;
import java.sql.Statement;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class UcscSourcesCapability extends Das2GenericCapability 
    implements Das2SourcesCapabilityI  {

    static String DB_URI_ROOT = "jdbc:mysql://";
    static String JDBC_MULTIQUERY_PARAM = "?allowMuLtiqueries=true";
    static boolean USE_MULTIQUERY = false;
    static boolean FILTER_TOO_MANY_SEQS = false;
    static boolean DEBUGGING_HG_DM_ONLY = false;
    static public int max_seqs_allowed = 100;
    static String HGCENTRAL_DB = "hgcentral";

    /*
     *   default parameters for using public UCSC MySQL server
     */
    static String DEFAULT_DB_HOST = "genome-mysql.cse.ucsc.edu";
    static String DEFAULT_DB_USER = "genomep";
    static String DEFAULT_DB_PASSWORD = "password";
    // static String db_host = "localhost:3306";

    String db_host;
    String db_user;
    String db_password;

    Das2SourcesResponseI sources = null;
    Map<URI, Das2SourceI> uri2source = null;
    Map<URI, Das2VersionI> uri2version = null;
    Map<String, UcscSource> name2source = new HashMap<String, UcscSource>();
    Map<String, UcscVersion> name2version = new HashMap<String, UcscVersion>();

    static  {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        // persist_conn = getDbConnection(HGCENTRAL_DB);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
	//	addFormatHandler(new Das2FeatureXmlHandler());
	//	addFormatHandler(new Das2ProtoBufHandler());
    }


    public UcscSourcesCapability(URI base_uri, String query_uri, Map<String, String> params)  {
	super(base_uri, query_uri, "sources", null, null);
	if (params != null)  {
	    db_host = params.get("dbhost");
	    db_user = params.get("dbuser");
	    db_password = params.get("dbpassword");
	}
	if ( db_host == null )  { db_host = DEFAULT_DB_HOST; }
	if ( db_user == null )  { db_user = DEFAULT_DB_USER; }
	if ( db_password == null)  { db_password = DEFAULT_DB_PASSWORD; }    }

    public Das2SourcesResponseI getSources() {
	if (sources == null)  { 
	    initSources();
	}
	return sources;
    }

    public Das2SourceI getSource(String name)  { 
	if (sources == null)  { initSources(); }
	return (Das2SourceI)name2source.get(name);
    }

    public Das2VersionI getVersion(String name)  { 
	if (sources == null)  { initSources(); }
	return (Das2VersionI)name2version.get(name);
    }

    public Das2SourceI getSource(URI source_uri)  {
	if (sources == null)  { initSources(); }
	return uri2source.get(source_uri);
    }

    public Das2VersionI getVersion (URI version_uri)  {
	if (sources == null)  { initSources(); }
	return uri2version.get(version_uri);
    }

    protected synchronized boolean initSources()  {
	// only initialized once -- if want effect of re-initializing, should create new SourcesCapability instead
	if (sources != null)  { return true; }
        System.out.println("called UcscSourceCapability.initSources()");
                System.out.println("----- CALLING INITSOURCES() ------");
	boolean success = false;
	List<Das2SourceI> slist = new ArrayList<Das2SourceI>();
	//	Map<String, UcscSource> sourcemap = new HashMap<String, UcscSource>();
	TimeCheck tcheck = new TimeCheck("Das2ServerInfo.initialize()");
	tcheck.start();
	try {
            // Connection conn = persist_conn;
            //  com.mysql.jdbc.Connection con = (com.mysql.jdbc.Connection)getDbConnection();
	    //            Connection conn = Das2Servlet.getHgCentralConnection();
            Connection conn = getHgCentralConnection();
            Statement stmt = conn.createStatement();
            // want to eventually check on last modification timestamp for "dbDb" table in hgcentral, 
            //    and if hasn't been modified since last time info was needed, use existing data model instead
            //    for mysql, can get modificatin time for a table via:
            //         SHOW table STATUS FROM hgcentral LIKE 'dbDb'
            //    then returned ResultSet should have single row for 'dbDb' table, and "Update_time" 
	    //         field will have timestamp of last time table was modified in any way
            //    WARNING:  "Update_time" may be null depending on database engine used for MySQL, 
	    //         in which case should treat as unknown and go back to table query to recreate data model...
            //            String hgcentral_query = "select * from dbDb";
	    //
	    //   doing join query rather than separate query for dbDb and genomeClade because 
	    //      dbDb orderKey does sorting both by genome and by genome version, 
	    //      so both Das2Sources and Das2Versions are added based on preferred ordering

	    String db_query = "show databases";

            String hgcentral_query =
                "select * from dbDb, genomeClade where " +
                " dbDb.genome = genomeClade.genome " +
                " and dbDb.active = 1 " +
                " order by dbDb.orderKey";

	    ResultSet dbs = stmt.executeQuery(db_query);
	    Set<String> db_names = new HashSet<String>();
	    while (dbs.next())  {
		String db_name = dbs.getString("Database");
		db_names.add(db_name);
	    }

            ResultSet rs = stmt.executeQuery(hgcentral_query);
            tcheck.report();
            tcheck.start();
	    int filtered_by_seq_count = 0;
            while (rs.next()) {
		String source_name = rs.getString("genome");
		String version_name = rs.getString("name");
		// checking to make sure a database for the versioned genome actually exists:
		//    sometimes a genome will be listed as active in hgcentral dbDb but not actually exist 
		//    (at least in public MySQL server)

		if (db_names.contains(version_name))  {
		    if (DEBUGGING_HG_DM_ONLY &&
                    (! ( version_name.startsWith("hg") || version_name.startsWith("dm")))) {
			System.out.println("DEBUGGING, ONLY LOADING hg*, SKIPPPING: " + version_name);
		    }
		    else if ((FILTER_TOO_MANY_SEQS) && (! passesSeqCountFilter(version_name)))  {
			filtered_by_seq_count++;
			System.out.println("WARNING: too many seqs in " + version_name + ", skipping");
		    }

		    else  {
			UcscSource source = name2source.get(source_name);
			if (source == null)  {
			    source = new UcscSource(this.getBaseURI() , rs, this);
			    name2source.put(source_name, source);
			    slist.add(source);
			}
			UcscVersion new_version = new UcscVersion(source, rs); 
			name2version.put(new_version.getName(), new_version);
			// add coordinates
			Das2CoordinatesI coords = new Das2Coordinates(this.getBaseURI(),  
								      version_name + "/coords", 
								      null,  // taxonomy_id
								      "chromosome", 
								      "unknown", // authority
								      "unknown", // build version
								      null,      // created
								      null);     // test_range
							     
			new_version.addCoordinates(coords);
			// add segments, types, features capabilities
			//  MUST add in this order
			UcscSegmentsCapability segcap = new UcscSegmentsCapability(new_version, coords, rs);
			new_version.addCapability(segcap);
			UcscTypesCapability typecap = new UcscTypesCapability(new_version, coords, rs);
			new_version.addCapability(typecap);
			UcscFeaturesCapability featcap = new UcscFeaturesCapability(new_version, coords, rs);
			new_version.addCapability(featcap);
			source.addVersion(new_version);
		    }
		}
		else  {
		    System.out.println("WARNING: couldn't find database for: " + version_name + ", skipping");
		}
	    }
	    sources = new Das2SourcesResponse(this.getBaseURI(), slist, null, null);
	    uri2source = new HashMap<URI, Das2SourceI>();
	    uri2version = new HashMap<URI, Das2VersionI>();
	    for (Das2SourceI source : sources.getSources()) {
		uri2source.put(source.getAbsoluteURI(), source);
		uri2source.put(source.getLocalURI(), source);
		for (Das2VersionI version : source.getVersions()) {
		    uri2version.put(version.getAbsoluteURI(), version);
		    uri2version.put(version.getLocalURI(), version);
		}
	    }
            rs.close();
            stmt.close();
            conn.close();
	    success = true;
	    System.out.println("genome versions filtered due to high seq count: " + filtered_by_seq_count);
	}
        catch (SQLException ex) {
            ex.printStackTrace();
            while (ex != null) {
                System.out.println("SQL Exception:  " + ex.getMessage());
                ex = ex.getNextException();
            }  // stop while
        } // stop catch SQLException
        catch (java.lang.Exception ex) {
            System.out.println("Exception:  " + ex.getMessage());
            ex.printStackTrace();
        }
        tcheck.report();
	return success;
    }


    protected boolean passesSeqCountFilter(String db_name) throws SQLException  {
	Connection seq_conn = getDbConnection(db_name);
	Statement seq_stmt = seq_conn.createStatement();
	ResultSet status = seq_stmt.executeQuery("show table status like 'chromInfo'");
	status.next();
	int seq_count = status.getInt("Rows");
	boolean passed = (seq_count <= max_seqs_allowed);
	if (! passed)  {
	    System.out.println("WARNING: " + db_name + " seq count: " + seq_count);
	}
	return passed;
    }


   /** replace DriverManager with DataSource for
       connection pooling via JNDI or pooling libs here... */
    public Connection getDbConnection(String name) throws SQLException {
        String db_uri = DB_URI_ROOT + db_host + "/" + name;
	if (USE_MULTIQUERY)  { db_uri += JDBC_MULTIQUERY_PARAM; }
        Connection conn = DriverManager.getConnection(db_uri, db_user, db_password);
        return conn;
    }

    /** replace DriverManager with DataSource for 
        connection pooling via JNDI or pooling libs here...     */
    public Connection getHgCentralConnection() throws SQLException {
        String db_uri = DB_URI_ROOT + db_host + "/" + HGCENTRAL_DB;
	if (USE_MULTIQUERY)  { db_uri += JDBC_MULTIQUERY_PARAM; }
        Connection conn = DriverManager.getConnection(db_uri, db_user, db_password);
        return conn;
    }


}