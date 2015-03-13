package genomancer.vine.das2.proxy;

import genomancer.trellis.das2.Das2Constants;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import genomancer.trellis.das2.model.Das2CapabilityI;
import genomancer.trellis.das2.model.Das2SourcesCapabilityI;
import genomancer.trellis.das2.model.Das2SegmentsCapabilityI;
import genomancer.trellis.das2.model.Das2TypesCapabilityI;
import genomancer.trellis.das2.model.Das2FeaturesCapabilityI;
import genomancer.trellis.das2.server.GenericProxyCapability;

import genomancer.trellis.das2.model.Das2SourceI;
import genomancer.trellis.das2.model.Das2VersionI;
import genomancer.trellis.das2.model.Das2CoordinatesI;
import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import genomancer.trellis.das2.model.Das2SourcesResponseI;

import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2TypesResponseI;
import genomancer.trellis.das2.xml.SegmentsXmlWriter;
import genomancer.trellis.das2.xml.SourcesXmlWriter;
import genomancer.trellis.das2.xml.TypesXmlWriter;
import genomancer.vine.das2.client.modelimpl.Das2Coordinates;
import genomancer.vine.das2.client.modelimpl.Das2Source;
import genomancer.vine.das2.client.modelimpl.Das2SourcesCapability;
import genomancer.vine.das2.client.modelimpl.Das2SourcesResponse;
import genomancer.vine.das2.client.modelimpl.Das2TypesCapability;
import genomancer.vine.das2.client.modelimpl.Das2Version;
import java.util.LinkedHashMap;
import java.util.HashMap;
import javax.xml.stream.XMLStreamException;
import org.jdom.JDOMException;

public class Das2SourcesProxyCapability extends GenericProxyCapability implements Das2SourcesCapabilityI {
    Das2SourcesCapabilityI remote_sources_cap;
    Das2SourcesResponseI sources = null;
    Map<URI, Das2SourceI> uri2source = null;
    Map<URI, Das2VersionI> uri2version = null;

    boolean proxy_segments = true;
    boolean proxy_types = true;
    boolean proxy_features = true;
    boolean proxy_sources = true;
    boolean proxy_unknowns = true;
    // if ((! proxy_segments) &&
    //     (! proxy_types) &&
    //     (! proxy_feaures) && 
    //     (! proxy_unknown) )
    // then Das2CapabilityI should be an identity proxy (passthrough with no modifications) ?
    boolean default_to_passthrough = true;

    public Das2SourcesProxyCapability()  {
	super(null, null, "sources", null, null, null);
	
    }

    public void init(Map<String, String> params)  {
	super.init(params);  // sets init_params   
	if ((remote_sources_cap == null) && 
	    (init_params != null))  {
            try {
                String remote_sources_cap_name = init_params.get("remote_sources_capability_class");
                String remote_sources_query_uri = init_params.get("remote_sources_query_uri");
                if (remote_sources_cap_name == null) {
                    throw new UnsupportedOperationException("To use SourcesProxyCapability, servlet configuration " + "must include a remote_sources_capability_class init param");
                }
                if (remote_sources_query_uri == null) {
                    throw new UnsupportedOperationException("To use SourcesProxyCapability, servlet configuration " + "must include a remote_sources_query_uri init param");
                }
                Class remote_sources_cap_class = Class.forName(remote_sources_cap_name);
                remote_sources_cap = (Das2SourcesCapabilityI) remote_sources_cap_class.newInstance();
                Map<String, String> mod_params = new LinkedHashMap<String, String>(init_params);
                // overwrite sources_query_uri to point to remote_sources_query_uri
                mod_params.put("sources_query_uri", remote_sources_query_uri);
                remote_sources_cap.init(init_params);
            } catch (InstantiationException ex) {
                Logger.getLogger(Das2SourcesProxyCapability.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(Das2SourcesProxyCapability.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Das2SourcesProxyCapability.class.getName()).log(Level.SEVERE, null, ex);
            }
	}
    }

    public Das2SourcesProxyCapability(URI base_uri, String query_uri, Das2SourcesCapabilityI remote_sources_cap) {
	super(base_uri, query_uri, "sources", null, null, remote_sources_cap);
	// remote_sources_cap.init() will get called in superclass constructor...
	this.remote_sources_cap = remote_sources_cap;

	// make a merged params that includes params from both remote_sources_cap.init_params and this.init_params
	//	Map<String, List<String>> merged_params = null;

    }


    public Das2SourcesResponseI getSources() {
	if (sources == null)  {
	    initSources();
	}
	return sources;
    }

    public Das2SourceI getSource(URI source_uri)  {
	if (sources == null)  { initSources(); }
	return uri2source.get(source_uri);
    }

    public Das2VersionI getVersion (URI version_uri)  {
	if (sources == null)  { initSources(); }
	return uri2version.get(version_uri);
    }


    /**
     *  get Das2SourcesResponseI from remote sources capability
     *  then copy structure, redirecting URIs where needed:
     *     redirect all capabilities through proxy capabilities
     *        (and make sure proxy capabilities reroute capability LINKs in responses)
     */
    protected boolean initSources()  {
	boolean success = false;
	Das2SourcesResponseI remote_sources_holder = remote_sources_cap.getSources();
	URI remote_base_uri = remote_sources_holder.getBaseURI();
	List<Das2LinkI> remote_links = remote_sources_holder.getLinks();
	String remote_maintainer_email = remote_sources_holder.getMaintainerEmail();
	List<Das2SourceI> remote_sources = remote_sources_holder.getSources();
	List<Das2SourceI> sources_list = new ArrayList<Das2SourceI>(remote_sources.size());


	try {
	    
	    for (Das2SourceI remote_source : remote_sources)  {
		Das2Source source = new Das2Source(remote_source.getAbsoluteURI(), 
						    remote_source.getTitle(), 
						    remote_source.getDescription(), 
						    remote_source.getInfoURL());
		// set proxy maintainer at top level SOURCES element (and top-level remote maintainer if present) 
		//   but passthrough remote source maintainer for each source here
		source.setMaintainerEmail(remote_source.getMaintainerEmail());
						    
		for (Das2VersionI remote_version : remote_source.getVersions())  {
		    Das2Version version = new Das2Version(source, 
							   remote_version.getAbsoluteURI().toString(), 
							   remote_version.getTitle(), 
							   remote_version.getDescription(), 
							   remote_version.getInfoURL(), 
							   remote_version.getCreationDate(), 
							   remote_version.getLastModifiedDate() );
							   
		    for (Das2CoordinatesI remote_coord : remote_version.getCoordinates())  {
			// nearly complete clone of remote_coord?  
			// but needed in case remote_coord is relying on base uri of original sources response
			Das2Coordinates coord = new Das2Coordinates(remote_coord.getAbsoluteURI(), 
								    remote_coord.getAbsoluteURI().toString(),
								    remote_coord.getTaxonomyID(), 
								    remote_coord.getCoordinateType(), 
								    remote_coord.getAuthority(), 
								    remote_coord.getBuildVersion(), 
								    remote_coord.getCreated(), 
								    remote_coord.getTestRange() );
			version.addCoordinates(coord);
		    }
		    for (Das2CapabilityI remote_cap : remote_version.getCapabilities())  {
			URI absolute_remote_uri = remote_cap.getAbsoluteURI();
			String query_base_uri = getBaseURI().toString();
			if (! query_base_uri.endsWith("/"))  {
			    query_base_uri += "/";
			}
			String query_id = query_base_uri + absolute_remote_uri.toString();
			System.out.println("Remote Capability new query URI: " + query_id);
			URI absolute_query_uri = new URI(query_id);
			Das2CapabilityI proxy_cap = null;
			boolean passthrough_if_no_proxy = default_to_passthrough;

			if (remote_cap instanceof Das2SegmentsCapabilityI)  {
			    if (proxy_segments)  {  // make a local proxy for segment cap
			     // construct URI for segments proxy, based on remote seg cap query URI and proxy base uri
				// take proxy base URI and just append remote seq cap's absolute query URI? 
				// I think that should be okay, result is a legal URI according to 
				//    URI syntax spec (RFC 3986)
                                // make a local proxy for segment cap 
				Das2SegmentsCapabilityI segments_remote_cap = (Das2SegmentsCapabilityI)remote_cap;
                                Das2SegmentsProxyCapability segments_proxy_cap =
				    new Das2SegmentsProxyCapability(getBaseURI(), 
								absolute_query_uri.toString(), 
								segments_remote_cap);
                                proxy_cap = segments_proxy_cap;
			    }
			}
			else if (remote_cap instanceof Das2TypesCapabilityI)  { 
			    if (proxy_types)  {
				Das2TypesCapabilityI types_remote_cap = (Das2TypesCapabilityI)remote_cap;
				Das2TypesProxyCapability types_proxy_cap = 
				    new Das2TypesProxyCapability(getBaseURI(), 
							     absolute_query_uri.toString(), 
							     types_remote_cap);
				proxy_cap = types_proxy_cap;
			    }
			}
			else if (remote_cap instanceof Das2FeaturesCapabilityI)  {
			    if (proxy_features)  {
				Das2FeaturesCapabilityI features_remote_cap = (Das2FeaturesCapabilityI)remote_cap;
				Das2FeaturesProxyCapability features_proxy_cap = 
				    new Das2FeaturesProxyCapability(getBaseURI(), 
								absolute_query_uri.toString(), 
								features_remote_cap);
				proxy_cap = features_proxy_cap;
			    }
			}
			else if (remote_cap instanceof Das2SourcesCapabilityI)  {
			    // shouldn't encounter a sources capability here, but just in case...
			    if (proxy_sources)  {
				Das2SourcesCapabilityI sources_remote_cap = (Das2SourcesCapabilityI)remote_cap;
				Das2SourcesProxyCapability sources_proxy_cap = 
				    new Das2SourcesProxyCapability(getBaseURI(), 
							       absolute_query_uri.toString(), 
							       sources_remote_cap);
				proxy_cap = sources_proxy_cap;
			    }
			}
			else  {
			    // unknown capability, but redirect to local proxy caps anyway?
			    //    needed in case remote_cap is relying on base uri of original sources response?
			    if (proxy_unknowns)  {
				GenericProxyCapability generic_proxy_cap = 
				    new GenericProxyCapability(getBaseURI(), 
							       absolute_query_uri.toString(),
							       remote_cap.getType(), 
							       version, null, remote_cap ); 
				proxy_cap = generic_proxy_cap;
			    }
			}

			// add formats, extensions, additional_data ?
			// since all proxies used here are extending GenericCapabilityProxy, if these fields 
			//    should mirror those in remote_cap then don't need to add, 
			//    since gcp.getFormats(), etc. have passthrough to remote_cap.getFormats(), etc.
			//  if formats etc. _do_ need to change relative to what's in remote_cap, 
			//      then should probably happen within FooProxyCapability class rather than here?
			if (proxy_cap != null)  {  // if proxy created, then use it instead of remote capability
			    version.addCapability(proxy_cap);
			}
			else if (passthrough_if_no_proxy)  {  // if no proxy created, use remote capability?
			    version.addCapability(remote_cap);
			}
		    }
		    source.addVersion(version);
		}
		sources_list.add(source);
	    }
	    
	} catch (URISyntaxException ex) {
	    Logger.getLogger(Das2SourcesProxyCapability.class.getName()).log(Level.SEVERE, null, ex);
	}
	
	// do a merged email??
	//	String merged_email;
	// for now just using remote sources capability email
	//
	// also for now just using remote sources links
	sources = new Das2SourcesResponse(getBaseURI(), 
					  sources_list, 
					  remote_maintainer_email, 
					  remote_links );
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
      return success;
    }


    public static void main(String[] args) throws URISyntaxException, XMLStreamException, JDOMException  {
	// String sources_url = "file:./data/das2_registry_sources.mod.xml";
	String sources_url = "file:./data/netaffx_das2_sources.mod.xml";
	Das2SourcesCapability remote_sources_cap = new Das2SourcesCapability(sources_url);
	URI base_uri = new URI("http://localhost/das2/proxy/test/");
	String query_uri = "sources";
	Das2SourcesProxyCapability sources_cap = new Das2SourcesProxyCapability(base_uri, 
									query_uri, 
									remote_sources_cap);
	//	List<Das2SourceI> sources = cap.getSources();
	Das2SourcesResponseI sources_response = sources_cap.getSources();
	SourcesXmlWriter sources_writer = new SourcesXmlWriter(sources_response, System.out);
        sources_writer.writeSourcesDocument();

	Das2SourceI source = sources_response.getSources().get(0);
	Das2VersionI version = source.getVersions().get(0);
	System.out.println();
	System.out.println();

	System.out.println("getting types for assembly version: " + version.getAbsoluteURI());
	Das2TypesCapabilityI types_cap = 
            (Das2TypesCapabilityI) version.getCapability(Das2Constants.DAS2_TYPES_CAPABILITY);
	Das2TypesResponseI types_response = types_cap.getTypes();
	Das2TypeI type = types_response.getTypes().get(0);
	TypesXmlWriter types_writer = new TypesXmlWriter(types_response, System.out);
	types_writer.writeTypesDocument();
	System.out.println();
	System.out.println();

	System.out.println("getting segments for version: " + version.getAbsoluteURI());
	Das2SegmentsCapabilityI segments_cap = 
            (Das2SegmentsCapabilityI) version.getCapability(Das2Constants.DAS2_SEGMENTS_CAPABILITY);
	Das2SegmentsResponseI segments_response = segments_cap.getSegments();
	Das2SegmentI segment = segments_response.getSegments().get(0);
	SegmentsXmlWriter segments_writer = new SegmentsXmlWriter(segments_response, System.out);
	segments_writer.writeSegmentsDocument();
	System.out.println();
	System.out.println();
	
    }

}