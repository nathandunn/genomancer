package genomancer.trellis.das2.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.xml.stream.XMLStreamException;
import org.jdom.JDOMException;

import genomancer.trellis.das2.model.*;
import genomancer.trellis.das2.xml.*;
import genomancer.trellis.das2.Das2Constants;
import genomancer.trellis.das2.server.ServerUtils;
import genomancer.trellis.das2.server.Das2ExtensionCapabilityI;
import genomancer.vine.das2.client.modelimpl.Das2FeaturesQuery;
import genomancer.vine.das2.client.modelimpl.Das2LocationRef;
 
public class TrellisDas2Servlet extends HttpServlet  {
    static protected String VERSION = "1.00";  // current version of TrellisDas2Servlet
    static protected String DAS_FEATURES_XML_FORMAT = "das2xml";
    static protected boolean SET_EXPIRES_HEADER = true;

    static protected final Pattern url_query_splitter = Pattern.compile("[;\\&]");
    static protected final Pattern equals_splitter = Pattern.compile("=");
    static protected final Pattern range_splitter = Pattern.compile(":");

    static protected Set<String> known_feature_query_params = new HashSet();
    static  {
	known_feature_query_params.add("segment");
	known_feature_query_params.add("overlaps");
	known_feature_query_params.add("inside");
	known_feature_query_params.add("excludes");
	known_feature_query_params.add("type");
	known_feature_query_params.add("format");
	known_feature_query_params.add("name");
	known_feature_query_params.add("coordinates");
	known_feature_query_params.add("note");
	known_feature_query_params.add("link");
    }
    
    protected Map<String, Das2VersionI> id2version = new LinkedHashMap<String, Das2VersionI>();
    protected Map<URI, Das2CapabilityI> uri2capability = new LinkedHashMap<URI, Das2CapabilityI>();
    protected Das2SourcesCapabilityI sources_cap;
    protected SimpleDateFormat date_formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    protected Map<String, String> init_params = null;
    protected TrellisSourcesPluginI sources_plugin;
    protected List<Das2FeatureWriterI> feature_writer_plugins = new ArrayList<Das2FeatureWriterI>();
    protected long next_renewal_time = -1;
    protected long renewal_rate_ms = -1;
    protected boolean DEBUG = false;

    /** 
     *   Support for handling of client-side caching via Last-Modified and If-Modified-Since headers
     *   Basic Trellis framework doesn't know how to figure out if data sources have changed between 
     *   requests, but getLastModified() allows Trellis plugins to implement via 
     *   capability.getLastModified() 
     *
     */
    public long getLastModified(HttpServletRequest request)  { 
	// add setting of Expires header here?

	Das2CapabilityI cap = getCapability(request);
	long last_modified = -1L;
	if (cap != null)  {
	    if (cap instanceof Das2FeaturesCapabilityI)  {
		Das2FeaturesQueryI feature_query = parseFeatureQueryFilter(request);
		last_modified = ((Das2FeaturesCapabilityI)cap).getLastModified(feature_query);
	    }
	    else  {
		last_modified = cap.getLastModified();
	    }
	}
	/** capabilities return -1 if unable to determine last modified 
	    in which case let generic servlet mechanism handle 
	 */
	if (last_modified < 0) {
	    last_modified = super.getLastModified(request);
	}
	return last_modified;
    }

    /**
     *  should get parameters from ServletConfig 
     *    specifying what Das2SourcesCapabilityI implementation(s) to load, 
     *    and what params to pass to initialize those implementations
     *    also need to pass on port number, root URL, etc?
     *
     *   as part of initialization should call 
     *       Das2SourcesCapabilityI().getSources()
     *       for each Source  
     *            source.getVersions()
     *            for each Version  
     *                 version.getCapabilities()
     *                 for each Cabability
     *                     capability_map.put(capability_query_uri, Capability)
     *
     *  since all this is constructed up front, 
     *     need to occasionally poll Das2SourcesCapabilityI 
     *           for updates to sources / versions / coordinates / capabilities  / etc.
     *   should set polling interval in init based on param in servlet config
     *   spawn polling thread
     *       this implies synchronization on calls above version capability level -- 
     *             sources_cap.getSources(), source.getVersion(), version.getCoordinates(), 
     *             version.getCapabilities(), etc.
     *       I'm pretty sure that's okay, all that stuff is reasonably quick, the stuff 
     *             where we want multithreaded servlet access to work well are on the 
     *             queries that take longer, which are in the version capabilities: 
     *                 large segment residues requests, large feature requests, etc.
     */
    public void init(ServletConfig config) throws ServletException {
	super.init(config);
	init_params = ServerUtils.getInitParams(config);
	reportServletConfig(config);
	// check for renewal_rate param, if found then set up thread for repeated renewal_rate
	String renewal_rate = init_params.get("renewal_rate_minutes");
	if (renewal_rate != null)  {
	    try  {
		long renewal_rate_minutes = Long.parseLong(renewal_rate);
		renewal_rate_ms = renewal_rate_minutes * 60 * 1000;
		System.out.println("renewal rate (minutes): " + renewal_rate_minutes);
	    }
	    catch (Exception ex)  {
		System.out.println("renewal_rate_minutes param wouldn't parse: " + renewal_rate);
		renewal_rate_ms = -1;
	    }

	    if (renewal_rate_ms < 0)  {
		// parsing didn't work or negative renewal_rate time -- either way don't do renewal_rate
	    }
	    else if (renewal_rate_ms < 60000)  {  // don't bother if renewal_rate time is under a minute
		System.out.println("renewal time less than a minute, not doing: " + renewal_rate_ms);
		renewal_rate_ms = -1;
	    }
	    /*  removed option for renewal_delay_ms != renewal_rate, conflicts with current "Expires" handling
	    else  {
		long renewal_delay_ms = -1;
		String renewal_delay = init_params.get("renewal_delay_minutes");
		if (renewal_delay != null)  {
		    try  {
			long renewal_delay_minutes = Long.parseLong(renewal_delay);
			renewal_delay_ms = renewal_delay_minutes * 60 * 1000;
			System.out.println("initial renewal delay (minutes): " + renewal_delay_minutes);
		    }
		    catch (Exception ex)  {
			System.out.println("renewal_delay_minutes param wouldn't parse: " + renewal_delay);
		    }
		}
		if (renewal_delay_ms <= 0)  {
		    renewal_delay_ms = renewal_rate_ms;
		}
	    }
	    */
	}
	renew();
	if (renewal_rate_ms > 0)  {
	    TimerTask renewal_task = new TimerTask()  {
		    public void run()  {
			renew();
		    }
		};
	    Timer renewal_rater = new Timer();
	    long renewal_delay_ms = renewal_rate_ms;  
	    System.out.println("scheduling renewals -- initial delay (m) = " + (renewal_delay_ms/60000) + 
			       ", renewal rate (m) = " + (renewal_rate_ms/60000));
	    renewal_rater.scheduleAtFixedRate(renewal_task, renewal_delay_ms, renewal_rate_ms);
	}
    }

    public void renew()  {
	// last_renewal_time = System.currentTimeMillis();
	if (renewal_rate_ms > 0)  { 
	    next_renewal_time = System.currentTimeMillis() + renewal_rate_ms;
	}
	System.out.println("@@@@@@@@ next renewal time: " + next_renewal_time);
	
        try { 
	    // synchronized to ensure that no other thread can access uri2capability while it is being repopulated
	    synchronized(uri2capability)  {
		System.out.println(new Date(System.currentTimeMillis()) + ":  clearing and renewing all capabilities");
		/*  Was using reflection to get constructor that took Map as parameter,
		    but switched to using a TrellisSourcesPluginI that is a holder for a Das2SourcesCapablityI, 
		    and the TrellisSourcesPluginI has an init method
		    Constructor sources_cap_constructor =
		    sources_cap_class.getConstructor(new Class[]{Map.class});
		    sources_cap = (Das2SourcesCapabilityI)sources_cap_constructor.
		    newInstance(new Object[] {sources_cap_param_map});
		*/
		uri2capability.clear();
		id2version.clear();
		//type_filters.clear();
            //		type_filters.add(new BedFormatHandler());
		setupOutputPlugins();
		String sources_plugin_name = init_params.get("sources_plugin_class");	    
		if (sources_plugin_name == null)  {
		    throw new UnsupportedOperationException("servlet configuration must include a " + 
							    "sources_plugin_class init param");
		}
		Class sources_plugin_class = Class.forName(sources_plugin_name);
		sources_plugin = (TrellisSourcesPluginI)sources_plugin_class.newInstance();
		sources_plugin.init(this.getServletConfig());
		sources_cap = sources_plugin.getSourcesCapability();
		URI sources_query_uri = sources_cap.getAbsoluteURI();

		uri2capability.put(sources_query_uri, sources_cap);

		System.out.println("added sources capability: " + sources_query_uri.toString());

		Das2SourcesResponseI sources_holder = sources_cap.getSources();
		List<Das2SourceI> sources = sources_holder.getSources();
		for (Das2SourceI source : sources) {
		    Collection<Das2VersionI> versions = source.getVersions();
		    for (Das2VersionI version : versions) {
			// id2version.put(version.getTitle(), version);
                        id2version.put(version.getLocalURIString(), version);
			List<Das2CapabilityI> caps = version.getCapabilities();
			for (Das2CapabilityI cap : caps) {
			    URI query_uri;
			    query_uri = cap.getAbsoluteURI();
			    uri2capability.put(query_uri, cap);
			}
		    }
		}
	    }
	}
        catch (InstantiationException ex) {
            Logger.getLogger(TrellisDas2Servlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(TrellisDas2Servlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(TrellisDas2Servlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setupOutputPlugins()  { 
	feature_writer_plugins.clear();
	// these should probably be Classes added instead of objects,
	//   (or at least should create new *Writer object for each write, or might
	//     end up with weird thread issues if multiple threads are using same Writer object ??
	//
	// also, should allow scripted addition of feature writer plugins as parameters
	//   to servlet, so can add them without needing to modify servlet code
	feature_writer_plugins.add(new BedFeatureWriter());
	// feature_writer_plugins.add(new JBrowseNCListFullFeatureWriter());
	feature_writer_plugins.add(new JBrowseFeatureWriter());  // latest feature writer
    }

    /**
     *  for most situations returns same as request.getRequestURL().toString(), but 
     *     attempts to correct for problems with request URLs introduced by 
     *     Amazon Elastic Beanstalk infrastructure, particulary proxying by the automated Load Balancer
     *     see http://jira.codehaus.org/browse/GRAILSPLUGINS-2774 for a good explanation of problem and solution used here
     */
    public static String getFixedRequestURL(HttpServletRequest request)  {
	String fport = request.getHeader("x-forwarded-port");
	String fhost = request.getHeader("x-forwarded-host");
	String result;
	if (fhost == null)  {
	    result = request.getRequestURL().toString();
	}
	else  {
	    if (fport == null)   {
		fport = Integer.toString(request.getServerPort());
	    }
	    if (fport.equals("80"))  {  // by convention, leave out port if 80
		result = ("http://" + fhost + request.getRequestURI().toString());
	    }
	    else  {
		result = ("http://" + fhost + ":" + fport + request.getRequestURI().toString());
	    }
	}
	return result;
    }

    public Das2CapabilityI getCapability(HttpServletRequest request)  {
	URI request_uri_no_query = null;
	Das2CapabilityI cap = null;

	// Can't just do uri = request.getRequestURI(), that only gives relative URI
	//     getRequestURL().toString() provides what is needed -- 
	//     full URL before "?" marking start of query params
	String request_no_query = getFixedRequestURL(request);
        try {
	    //	    request_uri_no_query = new URI(request.getRequestURL().toString());
	    request_uri_no_query = new URI(getFixedRequestURL(request));
        } catch (URISyntaxException ex) {
            Logger.getLogger(TrellisDas2Servlet.class.getName()).log(Level.SEVERE, null, ex);
        }
	if (request_uri_no_query == null)  { 
	    System.out.println("badly formed URI: " + request.getRequestURI()); 
	}
	cap = uri2capability.get(request_uri_no_query);	
	if (cap == null)  {
	    // for sources, may be based on request uri (relative URI) rather than full URI
	    //    getRequestURI() returns relative URI, so should be un-affected by Elastic Beanstalk issue...
	    cap = uri2capability.get(request.getRequestURI());
	}
	if (DEBUG)  { System.out.println("request: " + this.getFullRequestString(request) + ", capability: " + cap); }
      return cap;
    }

    /**
     *   given an HTTP request, map the pre-query parameter part of the URI to 
     *       a Das2Capabiility object
     *   if no mapping then send an error back
     *   if mapping then inspect the Das2Capability to decide what to do
     *   if get a Das2Capability but this servlet doesn't recognize the capability, 
     *       do a pass-through of raw request into Das2Capability, send back 
     *       raw response from Das2Capability
     *   if it's a known capability (currently Das2SourcesCapabilityI, Das2SegmentsCapabilityI, 
     *       Das2TypesCapabilityI, Das2FeaturesCapabilityI), then parse query params 
     *       into DAS2 data models for querying, but also pass through any unknown params
     *       (for exmple for a Das2FeaturesCapabilityI unknown params are passed via 
     *             Das2FeatureQuerI.getNonStandardParams() )
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException  {
        System.out.println("***** TrellisDas2Servlet received GET request: *****");
	String request_url = request.getRequestURL().toString();
	String full_original_url = getFullRequestString(request);
	System.out.println("Request URL: " + full_original_url);
	// System.out.println("Request URL (no params): " + request_url);

	if (DEBUG)  { reportRequestInfo(request, new PrintWriter(System.out)); }
	// if (DEBUG)  { reportServletConfig(this.getServletConfig()); }
	response.addHeader("DAS2-Server", "TrellisDas2Servlet/" + VERSION);
	response.addHeader("x-das-request-url", request_url);
	response.addHeader("x-das-fixed-url", full_original_url);
	response.addHeader("x-das-request-uri", request.getRequestURI().toString());
	if (SET_EXPIRES_HEADER && (next_renewal_time > 0))  {
	    if (DEBUG)  { System.out.println("###### adding Expires: " + next_renewal_time); }
	    response.addDateHeader("Expires", next_renewal_time);
	}
	sources_plugin.addHeaders(request, response);
	
	// WARNING: DO NOT call response.getWriter() until AFTER the response.setContentType() has 
	//   been called -- otherwise "charset" part of content-type might be ignored/overwritten!!
        //   see ServletResponse documentation at 
	//       java.sun.com/products/servlet/2.2/javadoc/javax/servlet/ServletResponse.html#setContentType(java.lang.String)
	//	PrintWriter pw = response.getWriter();


	Das2CapabilityI cap = getCapability(request);

	// if it's a known capability, do param parsing and call known capability 
	//    methods directly
	//
	// if not known, then do pass-through
	//
	// if no capability found, then throw request-not-recognized error

	// for now set HTTP status code to OK
	response.setStatus(HttpServletResponse.SC_OK);

	if (cap == null)  {
	    // should probably be SC_NOT_FOUND (404), but I think the based on the current spec it's 
	    //   SC_BAD_REQUEST
	    //	response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	    StringBuffer msg = new StringBuffer();
	    msg.append("DAS2 server could not find capability for request: " + full_original_url + "\n");
	    msg.append("   request url: " + request_url + "\n");
	    response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg.toString());
	}
	else if (cap instanceof Das2SourcesCapabilityI)  {
	    try {
		response.setStatus(HttpServletResponse.SC_OK);
		handleSourcesQuery(request, response, (Das2SourcesCapabilityI) cap);
	    } catch (Exception ex) {
		Logger.getLogger(TrellisDas2Servlet.class.getName()).log(Level.SEVERE, null, ex);
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
				   "DAS2 server could not respond to request: " + full_original_url);
            }
	}
	else if (cap instanceof Das2TypesCapabilityI)  {
            try {
		response.setStatus(HttpServletResponse.SC_OK);
                handleTypesQuery(request, response, (Das2TypesCapabilityI) cap);
            } catch (Exception ex) {
                Logger.getLogger(TrellisDas2Servlet.class.getName()).log(Level.SEVERE, null, ex);
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
				   "DAS2 server could not respond to request: " + full_original_url);
            }
	}
	else if (cap instanceof Das2SegmentsCapabilityI)  {
            try {
		response.setStatus(HttpServletResponse.SC_OK);
                handleSegmentsQuery(request, response, (Das2SegmentsCapabilityI) cap);
            } catch (Exception ex) {
                Logger.getLogger(TrellisDas2Servlet.class.getName()).log(Level.SEVERE, null, ex);
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
				   "DAS2 server could not respond to request: " + full_original_url);
            }
	}
	else if (cap instanceof Das2FeaturesCapabilityI)  {
            try {
		response.setStatus(HttpServletResponse.SC_OK);
                handleFeaturesQuery(request, response, (Das2FeaturesCapabilityI) cap);
            } catch (Exception ex) {
                Logger.getLogger(TrellisDas2Servlet.class.getName()).log(Level.SEVERE, null, ex);
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
				   "DAS2 server could not respond to request: " + full_original_url);
            }
	}
	else if (cap instanceof Das2ExtensionCapabilityI)  {  // pass through
	    response.setStatus(HttpServletResponse.SC_OK);
	    handleExtensionQuery(request, response, (Das2ExtensionCapabilityI)cap);
	}
	else  {
	    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
			       "DAS2 server did not recognize request: " + full_original_url);
	}
	    
    }


    /**
     *  In addition to standard ~/sources request, also supports sources requests with query parameters 
     *    that the Sanger DAS1 registry supports:
     *   ~/sources?[label=]*[organism=]*[authority=]*[capability=]*[type=]*
     *
     *  For details see http://www.dasregistry.org/help_scripting.jsp  
     *
     *  assumes [SOURCES|SOURCE|VERSION]/MAINTAINER/@name & @href are not used 
     *     (9-15-2008 these may be removed from the spec soon)
     */
    public void handleSourcesQuery(HttpServletRequest request, HttpServletResponse response, 
				    Das2SourcesCapabilityI sources_cap) 
	throws IOException, XMLStreamException, JDOMException {
        System.out.println("called TrellisDas2Servlet.handleSourcesQuery()");

       //  Map<String, List<String>> params = ServerUtils.getQueryParams(request);
        Das2SourcesResponseI sources_holder = sources_cap.getSources();
        response.setStatus(HttpServletResponse.SC_OK);

        System.out.println("handling sources response");
        handleSourcesResponse(request, response, sources_holder);

	
        System.out.println("finished TrellisDas2Servlet.handleSourcesQuery()");
	// dw.close();
    }

    public void handleSourcesResponse(HttpServletRequest request, HttpServletResponse response,
        Das2SourcesResponseI sources_holder) throws XMLStreamException, IOException, JDOMException  {
        Map<String, List<String>> params = ServerUtils.getQueryParams(request);
        // WARNING: DO NOT call response.getWriter() until AFTER the response.setContentType() has
	//   been called -- otherwise "charset" part of content-type might be ignored/overwritten!!
	response.setContentType(Das2Constants.SOURCES_CONTENT_TYPE);

	SourcesXmlWriter dw = new SourcesXmlWriter(sources_holder, response.getWriter());
	/**
	 *  handling of parameters is different for sources than for other capabilities
	 *  always getting the full sources response from the sources capability,
	 *     then filtering out sources/versions only when writing out sources doc...
	 */
	dw.writeSourcesDocument(params);
    }
    

   /**
     *  no support for serving residues yet
     *
     *       public String getResidues(Das2LocationI location);
     *       public String getResidues(List<Das2LocationI> locations);
     */
   public void handleSegmentsQuery(HttpServletRequest request, 
				   HttpServletResponse response, 
				   Das2SegmentsCapabilityI segments_cap) throws XMLStreamException, IOException {
       System.out.println("called handleSegmentsQuery()");
       Das2SegmentsResponseI segments_holder = segments_cap.getSegments();
       response.setStatus(HttpServletResponse.SC_OK);
       handleSegmentsResponse(request, response, segments_holder);
    }

    public void handleSegmentsResponse(HttpServletRequest request, 
				       HttpServletResponse response, 
				       Das2SegmentsResponseI segments_holder) throws XMLStreamException, IOException {
        // WARNING: DO NOT call response.getWriter() until AFTER the response.setContentType() has
        //   been called -- otherwise "charset" part of content-type might be ignored/overwritten!!
       response.setContentType(Das2Constants.SEGMENTS_CONTENT_TYPE);
       SegmentsXmlWriter dw = new SegmentsXmlWriter(segments_holder, response.getWriter());
       dw.writeSegmentsDocument();
       // dw.close();
    }

   public void handleTypesQuery(HttpServletRequest request, HttpServletResponse response, 
				Das2TypesCapabilityI types_cap) 
       throws XMLStreamException, IOException, JDOMException  {

       System.out.println("called handleTypesQuery()");
       Das2TypesResponseI types_holder = types_cap.getTypes();       
       // possibly set up types_cap ==> wrapped_types_cap hash for wrapping here...
       response.setStatus(HttpServletResponse.SC_OK);
       handleTypesResponse(request, response, types_holder);
   }

   public void handleTypesResponse(HttpServletRequest request,
       HttpServletResponse response,
       Das2TypesResponseI types_holder) throws IOException, XMLStreamException, JDOMException  {

       if (feature_writer_plugins.size() > 0)   {
	   System.out.println("applying feature writer plugins to types request");
	   // TypesResponseWrapper wrapped_types_holder = new TypesResponseWrapper(types_holder);
	   TypesResponseWrapper wrapped_types_holder = new TypesResponseWrapper(types_holder);
	   for (Das2TypeI type : wrapped_types_holder.getTypes())  {
	       // wrapped types are created in TypesResponseWrapper constructor
	       TypeWrapper wrapped_type = (TypeWrapper)type; 
	       for (Das2FeatureWriterI fwriter : feature_writer_plugins)  {
		   if (fwriter.acceptsType(wrapped_type))  {
		       wrapped_type.addFormat(fwriter.getFormat());
		   }
	       }
	   }
	   types_holder = wrapped_types_holder;
       }

	// WARNING: DO NOT call response.getWriter() until AFTER the response.setContentType() has 
	//   been called -- otherwise "charset" part of content-type might be ignored/overwritten!!
       response.setContentType(Das2Constants.TYPES_CONTENT_TYPE);

       List<Das2TypeI>types = types_holder.getTypes();
       TypesXmlWriter dw = new TypesXmlWriter(types_holder, response.getWriter());
       dw.writeTypesDocument();
       // dw.close();
    }
    
    
   public void handleFeaturesQuery(HttpServletRequest request, HttpServletResponse response, 
				   Das2FeaturesCapabilityI featurescap) throws XMLStreamException, IOException {

       if (DEBUG)  { System.out.println("called handleFeaturesQuery()"); }
       Das2FeaturesQueryI feature_query = parseFeatureQueryFilter(request);
       String format_name = feature_query.getFormat();
       if (DEBUG)  { System.out.println("format name: " + format_name); }
       int feature_count = -1;
       /*
        FeatureResponseFormatter formatter = feature_response_formatter.get(format_name);
       if (formatter != null)  {
	   formatter.outputFeatures(response, features);
       }
       else  
        */

       if (format_name == null || 
	   format_name.equals("das2xml") || 
	   format_name.equals("das2xml-minimal") || 
	   format_name.equals("das2xml-verbose"))  {
	   Das2FeaturesResponseI features_holder = featurescap.getFeatures(feature_query);
	   Status status = features_holder.getStatus();
	   if (status == Status.OK)  {
	       List<Das2FeatureI> features = features_holder.getFeatures();
	       feature_count = features.size();
	       response.setStatus(HttpServletResponse.SC_OK);
	       // WARNING: DO NOT call response.getWriter() until AFTER the response.setContentType() has 
	       //   been called -- otherwise "charset" part of content-type might be ignored/overwritten!!
	       response.setContentType(Das2Constants.FEATURES_CONTENT_TYPE);
	       FeaturesXmlWriter dw = new FeaturesXmlWriter(features_holder, response.getWriter());
	       dw.writeFeaturesDocument();
	   }

	   else if (status == Status.RESPONSE_TOO_LARGE)  {
	       response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, 
				  "Query would return larger response than maximum allowed by server");
	   }
	   else if (status == Status.SERVER_ERROR)  {
	       response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
				  "Internal server error");
	   }
	   else {
	       response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
				  "Unknown internal server error");
	   }
	   // dw.close();
       }
       else if (format_name.equals("count"))  {
	   // just output count as plain text?
	   feature_count = featurescap.getFeaturesCount(feature_query);
	   PrintWriter pw = response.getWriter();
	   String type_name = request.getParameter("type");
	   pw.print("<b>annot count for " + type_name + ": " + feature_count + "</b>");
	   // throw new UnsupportedOperationException("COUNT FORMAT NOT YET SUPPORTED");
       }
       else if (format_name.equals("uri"))  {
	   // just output whitespace separated list of URIs?
	   List<String> uris = featurescap.getFeaturesURI(feature_query);
	   feature_count = uris.size();
	   throw new UnsupportedOperationException("URI FORMAT NOT YET SUPPORTED");
       }
       else  {
	   // Specified format currently not recognized by core Trellis framework
	   //
	   // Try two other options:
	   // 1. see if the versioned source can handle returning that format on it's own 
	   //    if format is specified in versioned source's TypeCapabilityI for the given types, 
	   //        then pass through 
	   // 2. Otherwise check Trellis format plugins, see if one of them can handle it
	   //    if so, pass off results to format plugin
	   //
	   // Giving priority to (1) -- if both format plugin and feature capabilities plugin 
	   //     can handle output for given type(s), priority goes to feature capabilities
	   // For 1), 
	   //    would like to do checking here to make sure capability supports 
	   //      the data type, but will need to reach across capabilities for the 
	   //      given Das2VersionI to get it's Das2TypeI capability and see if 
	   //      the format is supported
	   //    therefore can only complete (1) if versioned source also has Das2TypesCapabilityI
	   
	   // assuming that formats are uniquely named, or in other words if two Das2FormatI  
	   //   share the same name then they also share the same mimetype and actual data format
	   // 1. see if the versioned source can handle returning that format on it's own 
	   Das2FormatI format = null;
	   Das2VersionI version = featurescap.getVersion();
	   boolean do_pass_through = true;
	   if (version == null)  { do_pass_through = false; }
	   else  {
	       Das2TypesCapabilityI types_cap = 
		   (Das2TypesCapabilityI)version.getCapability(Das2Constants.DAS2_TYPES_CAPABILITY);
	       if (types_cap == null)   {
		   do_pass_through = false;
	       }
	       else  {
             Das2TypesResponseI types_response = types_cap.getTypes();
		   // check to make sure that every type in the query can be represented in the 
		   //    requested format, based on format elements in types response 
		   //    (and maybe also format elements in types capability element in sources doc?)
		   for (URI type_uri : feature_query.getTypes())  {
		       Das2TypeI type = types_response.getType(type_uri);
		       if (type == null)  {
			   do_pass_through = false;
			   break;
		       }
		       else {
			   format = type.getFormat(format_name);
			   if (format == null)  {
			       do_pass_through = false;
			       break;
			   }
		       }

		   }
		   //  format = types_cap.getFormat(format_name);
	       }
	   }
	   // GAH hacking to bypass pass-through???  9/7/2011
	   do_pass_through = false;
	   if (do_pass_through)  {
	       InputStream plugin_reply = featurescap.getFeaturesAlternateFormat(feature_query);
	       // Pull from plugin_reply InputStream, push to servlet response's OutputStream
	       response.setStatus(HttpServletResponse.SC_OK);
	       response.setContentType(format.getMimeType());
	       throw new UnsupportedOperationException("PASSTHROUGH OF ALTERNATIVE FORMAT NOT YET SUPPORTED: " + 
						       response.getContentType());
	   }

	   else  {
	       // NEED TO REFACTOR THIS!!!
	       // 2. Otherwise check Trellis format plugins, see if one of them can handle it
	       if (DEBUG)  { System.out.println("looking for format in Trellis output plugins: " + format_name); }
	       for (Das2FeatureWriterI fwriter : feature_writer_plugins)  {
		   Das2FormatI writer_format = fwriter.getFormat();
		   String writer_format_name = writer_format.getName();
		   if (format_name.equals(writer_format_name))  {
		       if (DEBUG)  { System.out.println("returning in requested format: " + writer_format_name); }
		       // assume for now any type is acceptable
		       // for (type : types)  {
		       //      if (! writer_format.acceptsType(type))  {  // can't do it
		       Das2FeaturesResponseI features_holder = featurescap.getFeatures(feature_query);
		       Status status = features_holder.getStatus();
		       if (status == Status.OK)  {
			   List<Das2FeatureI> features = features_holder.getFeatures();
			   feature_count = features.size();
			   response.setStatus(HttpServletResponse.SC_OK);
			   // WARNING: DO NOT call response.getWriter() until AFTER the response.setContentType() has 
			   //   been called -- otherwise "charset" part of content-type might be ignored/overwritten!!
			   response.setContentType(writer_format.getMimeType());
			   fwriter.writeFeatures(features_holder, response.getOutputStream());
		       }

		       else if (status == Status.RESPONSE_TOO_LARGE)  {
			   response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, 
					      "Query would return larger response than maximum allowed by server");
		       }
		       else if (status == Status.SERVER_ERROR)  {
			   response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
					      "Internal server error");
		       }
		       else {
			   response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
					      "Unknown internal server error");
		       }
		       //		       fwriter.writeFeatures(feature_respo
		       System.out.println("returned features in requested format: " + writer_format_name);
		       break;
		   }
	       }
	   }
       }  // END FORMAT NOT RECOGNIZED BY TRELLIS CONDITIONAL(

       /**
	*  if query is empty or null then return all features
	*/
       
   }


    protected void handleExtensionQuery(HttpServletRequest request, HttpServletResponse response, 
				      Das2ExtensionCapabilityI cap)  {
	boolean success = cap.handleQuery(request, response);
    }


    /**
     *  for any URI params, resolve against the base URI of the request in case they are relative
     *     (if URI param is absolute, resolving against base URI will return the unchanged URI param)
     *   
     */
    protected Das2FeaturesQueryI parseFeatureQueryFilter(HttpServletRequest request) {

	Das2FeaturesQuery query = new Das2FeaturesQuery();
	try  {
	    
	    //	    String request_url = request.getRequestURL().toString();
	    String request_url = getFixedRequestURL(request);
	    URI base_uri = new URI(request_url);
	    Map<String, List<String>> params = ServerUtils.getQueryParams(request);
	    List<String> format_params = params.get("format");
	    List<String> segment_params = params.get("segment");
	    List<String> overlap_params = params.get("overlaps");
	    List<String> inside_params = params.get("inside");
	    List<String> exclude_params = params.get("excludes");		
	    List<String> type_params = params.get("type");
	    List<String> coord_params = params.get("coordinates");
	    List<String> link_params = params.get("link");
	    List<String> name_params = params.get("name");
	    List<String> note_params= params.get("note");

	    // leaving out "prop-*" queries for now (other than pass-through as non_standard_params)
	    if (format_params != null)  {
		if (format_params.size() > 1)  { // problem  
		    throw new UnsupportedOperationException("Can't have more than one format param in feature query");
		}
		query.setFormat(format_params.get(0));
	    }
	    if (segment_params != null)  { 
		for (String segid : segment_params)  {
		    if (DEBUG)  {
			System.out.println("segid: "+ segid);
			System.out.println("base_uri: " + base_uri);
		    }
		    URI seguri = base_uri.resolve(segid);
		    if (overlap_params == null && inside_params == null && exclude_params == null)  {
			// no overlap/inside/exclude qualifiers, so must be overlap of entire segment -- 
			//    add overlap to whole segment
			if (DEBUG)  { System.out.println("Das2LocationRef covers whole segment: " + seguri); }
			Das2LocationRefI locref = new Das2LocationRef(seguri); 
			query.addOverlap(locref);
		    }
		    else  {
			if (overlap_params != null)  {
			    for (String overlap : overlap_params)  {
				Das2LocationRefI locref = ServerUtils.getLocationRef(seguri, overlap);
				if (DEBUG)  { 
				    System.out.println("parsing query, made locref: " + 
					locref.getSegmentURI().toString() + 
					"  min=" + locref.getMin() + "  max=" + locref.getMax());
				}
				query.addOverlap(locref);
			    }
			}
			if (inside_params != null)   {
			    for (String inside : inside_params)  {
				Das2LocationRefI locref = ServerUtils.getLocationRef(seguri, inside);
				query.addInside(locref);
			    }
			}
			if (exclude_params != null)  {
			    for (String exclude : exclude_params)  {
				Das2LocationRefI locref = ServerUtils.getLocationRef(seguri, exclude);
				query.addExclude(locref);
			    }
			}
		    }
		}
	    }
	    if (type_params != null)  {
		for (String typeid : type_params)  {
		    query.addType(base_uri.resolve(typeid));
                } 
	    }

	    if (coord_params != null)  {
		for (String coordid : coord_params)  { query.addCoordinate(base_uri.resolve(coordid)); }
	    }
	    if (link_params != null)  {
		for (String linkid : link_params)  { query.addLink(base_uri.resolve(linkid)); }
	    }

	    if (name_params != null)  {
		for (String name : name_params)  { query.addName(name); }
	    }
	    if (note_params != null)  {
		for (String note : note_params)  { query.addNote(note); }
	    }
	    for (String param : params.keySet())  {
		if (! known_feature_query_params.contains(param)) {
		    List<String> values = params.get(param);
		    for (String value : values)  {
			query.addNonStandardParam(param, value);
		    }
		}
	    }
	}
	catch (URISyntaxException ex) {
	    Logger.getLogger(TrellisDas2Servlet.class.getName()).log(Level.SEVERE, null, ex);
	}
	return query;
    }

    public void reportRequestInfo(HttpServletRequest request, PrintWriter pw)  {
        pw.println("url: " + request.getRequestURL().toString());
        pw.println("fixed url: " + getFixedRequestURL(request));
	pw.println("uri: " + request.getRequestURI());
        pw.println("path info: " + request.getPathInfo());
        pw.println("query string: " + request.getQueryString());
        pw.println("path translated = " + request.getPathTranslated());
        pw.println("context path = " + request.getContextPath());
        pw.println("servlet path = " + request.getServletPath());
	pw.println("local name: " + request.getLocalName());
	pw.println("local addr: " + request.getLocalAddr());
	pw.println("server name: " + request.getServerName());
	pw.flush();
    }

    public void reportServletConfig()  { reportServletConfig(this.getServletConfig()); }
    public void reportServletConfig(ServletConfig config)  {
	System.out.println("********** ServletConfig info ***********");
	System.out.println("servlet name: " + config.getServletName());
	Enumeration init_param_names = config.getInitParameterNames();
	while (init_param_names.hasMoreElements())  {
	    String param_name = (String)init_param_names.nextElement();
	    String param_val = config.getInitParameter(param_name);
	    System.out.println("config init param, name = " + param_name + ", value = " + param_val);
	}
	ServletContext scontext = config.getServletContext();
	reportServletContext(scontext);
	System.out.println("****************************************");
    }

    public void reportServletContext(ServletContext context)  {
	if (context == null)  { System.out.println("ServletContext is null"); }
	else  {
	    System.out.println("ServletContext info  *******");
	    System.out.println("SC.getServerInfo(): " + context.getServerInfo());
	    System.out.println("SC.getServletContextName(): " + context.getServletContextName());
	    Enumeration att_names = context.getAttributeNames();
            while (att_names.hasMoreElements())  {
		String att_name = (String) att_names.nextElement();
		Object att_val = context.getAttribute(att_name);
		System.out.println("SC att, name = " + att_name + ", value = " + att_val);
            }
	    Enumeration init_param_names = context.getInitParameterNames();
	    while (init_param_names.hasMoreElements())  {
		String param_name = (String)init_param_names.nextElement();
		String param_val = context.getInitParameter(param_name);
		System.out.println("SC init param, name = " + param_name + ", value = " + param_val);
	    }
	}
    }

    
    public Das2VersionI getVersionFromId(String title)  {
	return id2version.get(title);
    }

    public static String getFullRequestString(HttpServletRequest request) {
	// String request_url = request.getRequestURL().toString();
	String request_url = getFixedRequestURL(request);
	String full_original_url;
	String query_string = request.getQueryString();
	if (query_string == null || query_string.length() == 0)  {
	    full_original_url = request_url;
	}
	else  {
	    full_original_url = request_url + "?" + query_string;
	}
	return full_original_url;
    }

}