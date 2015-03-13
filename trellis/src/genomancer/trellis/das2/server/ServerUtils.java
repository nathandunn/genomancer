package genomancer.trellis.das2.server;

import genomancer.trellis.das2.model.Das2LocationRefI;
import genomancer.trellis.das2.model.Strand;
import genomancer.vine.das2.client.modelimpl.Das2LocationRef;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

public class ServerUtils  {

    static final public String DAS2_RANGE_DELIMITER = ":";
    static final public Pattern url_query_splitter = Pattern.compile("[;\\&]");
    static final public Pattern equals_splitter = Pattern.compile("=");
    static final public Pattern range_splitter = Pattern.compile(DAS2_RANGE_DELIMITER);
    static boolean DEBUG = false;

/**
	   using getParams() rather than ServletRequest.getParameterValues() because that method 
	   is more strict about url-encoding of query parameters, and I want this server to be forgiving 
	   about url-encoding when possible (even though it is required in the DAS/2 spec)
*/

    /**
     *   returns a Map of query parameter name to a List of query parameter values for that name
     *   for single query paramaters (the most common case), will still return a List (with a single entry)
     */
    public static final Map<String, List<String>> getQueryParams(HttpServletRequest request)  {
	String raw_query = request.getQueryString();
	return getQueryParams(raw_query);
    }

    /**
     *   returns a Map of query parameter name to a List of query parameter values for that name
     *   for single query paramaters (the most common case), will still return a List (with a single entry)
     *   Assumes values are URL-encoded, so URL-decodes them
     */
    public static final Map<String, List<String>> getQueryParams(String query)  {
	Map name2val = new LinkedHashMap();
	if (query != null && query.length()>0)  {
	    if (DEBUG)  { 
		System.out.println("ServerUtils.getQueryParams(), query = " + query);
		System.out.println("     URL-decoded: " + URLDecoder.decode(query));
	    }
	    //	String query = URLDecoder.decode(query);
	    String[] qarray = url_query_splitter.split(query);
	    for (int i=0; i< qarray.length; i++)  {
		String name_val = qarray[i];
		String[] nvarray= equals_splitter.split(name_val);
            if (nvarray.length != 2)  {
                System.out.println("in ..trellis..ServerUtils.getQueryParams(), diagnosing ArrayOutoOfBoundsException");
                System.out.println("   nameval:   " + name_val);
                for (int pos=0; pos<nvarray.length; pos++) { 
                    System.out.println("               " + nvarray[pos]);
                }
            }

            String name = URLDecoder.decode(nvarray[0]);
            String val = URLDecoder.decode(nvarray[1]);

		List vlist = (List)name2val.get(name);
		if (vlist == null)  { 
		    vlist = new ArrayList(); 
		    name2val.put(name, vlist);
		}
		if (DEBUG)  { System.out.println("   adding query param name = " + name + ", val = " + val); }
		vlist.add(val);
	    }
	}
	return name2val;
    }

    public static Das2LocationRefI getLocationRef(URI segment_uri, String range_string)  {
	Das2LocationRef locref = null;
	try {
	    String[] minmax = range_splitter.split(range_string);
	    int min = Integer.parseInt(minmax[0]);
	    int max;
	    if (minmax.length > 1)  {
		max = Integer.parseInt(minmax[1]);
	    }
	    else  { max = min; }
	    if (minmax.length > 2)  {
		Strand strand = null;
		int sint = Integer.parseInt(minmax[2]);
		if (sint == 1)  { strand = Strand.FORWARD; }
		else if (sint == -1)  { strand = Strand.REVERSE; }
		else if (sint == 0)  { strand = Strand.BOTH; }
		else  { strand = Strand.UNKNOWN; }
		locref = new Das2LocationRef(segment_uri, min, max, strand);
	    }
	    else  {
		locref = new Das2LocationRef(segment_uri, min, max);
	    }
	}
	catch (Exception ex)  {
	    System.out.println("Error parsing range query parameter: " + range_string);
	}
	return locref;
    }

    public static Map<String, String> getInitParams(ServletConfig config)  {
	Map<String, String> params = new LinkedHashMap<String, String>();
	Enumeration init_param_names = config.getInitParameterNames();
	while (init_param_names.hasMoreElements())  {
	    String param_name = (String)init_param_names.nextElement();
	    String param_val = config.getInitParameter(param_name);
	    params.put(param_name, param_val);
	}
	return params;
    }



}
