package genomancer.ucsc.das2;

import java.sql.ResultSet;


public class DbQueryResult  {
    public static int OK = 0;
    public static int RESPONSE_TOO_LARGE = 1;
    public static int UNKNOWN_ERROR = 2;

    protected ResultSet rs;
    protected int status;
    protected String message;
        
    public DbQueryResult(ResultSet rs)  {
	this(rs, OK, null);
	if (rs == null)  { status = UNKNOWN_ERROR; }
    }
    
    public DbQueryResult(ResultSet rs, int status, String message)  {
	this.rs = rs;
	this.status = status;
	this.message = message;
    }

    public ResultSet getResultSet()  { 
	return rs;
    }

    public int getStatus()  { return status; }

    /** 
     *   mostly intended for informative error messages
     */
    public String getMessage()  { return message; }

}

