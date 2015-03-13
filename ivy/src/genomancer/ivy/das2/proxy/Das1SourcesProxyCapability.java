package genomancer.ivy.das2.proxy;

import genomancer.ivy.das.client.modelimpl.Das1SourcesCapability;
import genomancer.ivy.das.model.Das1EntryPointsCapabilityI;
import genomancer.ivy.das.model.Das1FeaturesCapabilityI;
import genomancer.ivy.das.model.Das1TypesCapabilityI;
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
import genomancer.trellis.das2.model.Das2PropertyI;
import genomancer.trellis.das2.model.Das2SegmentI;
import genomancer.trellis.das2.model.Das2SegmentsResponseI;
import genomancer.trellis.das2.model.Das2SourcesResponseI;

import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2TypesResponseI;
import genomancer.trellis.das2.xml.SegmentsXmlWriter;
import genomancer.trellis.das2.xml.SourcesXmlWriter;
import genomancer.trellis.das2.xml.TypesXmlWriter;
import genomancer.vine.das2.client.modelimpl.Das2Coordinates;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2Source;
import genomancer.vine.das2.client.modelimpl.Das2SourcesCapability;
import genomancer.vine.das2.client.modelimpl.Das2SourcesResponse;
import genomancer.vine.das2.client.modelimpl.Das2TypesCapability;
import genomancer.vine.das2.client.modelimpl.Das2Version;
import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.xml.stream.XMLStreamException;
import org.jdom.JDOMException;

public class Das1SourcesProxyCapability extends GenericProxyCapability implements Das2SourcesCapabilityI {
    public boolean DEBUG = false;

    static public String DAS1_CAPABILITY_PREFIX = "das1:";

    Das2SourcesCapabilityI remote_sources_cap;
    Das2SourcesResponseI sources = null;
    Map<URI, Das2SourceI> uri2source = null;
    Map<URI, Das2VersionI> uri2version = null;


    boolean proxy_das1_sequence = true;
    boolean proxy_das1_types = true;
    boolean proxy_das1_features = true;
    boolean proxy_das1_entry_points = true;
    boolean proxy_unknowns = false;
    // if ((! proxy_segments) &&
    //     (! proxy_types) &&
    //     (! proxy_feaures) && 
    //     (! proxy_unknown) )
    // then Das2CapabilityI should be an identity proxy (passthrough with no modifications) ?
    boolean default_to_passthrough = true;
    boolean inject_segments_cap = true;

    public Das1SourcesProxyCapability()  {
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
                    throw new UnsupportedOperationException("To use Das1SourcesProxyCapability, servlet configuration "
							+ "must include a remote_sources_capability_class init param");
                }
                if (remote_sources_query_uri == null) {
                    throw new UnsupportedOperationException("To use Das1SourcesProxyCapability, servlet configuration "
							    + "must include a remote_sources_query_uri init param");
                }
                Class remote_sources_cap_class = Class.forName(remote_sources_cap_name);
                remote_sources_cap = (Das2SourcesCapabilityI) remote_sources_cap_class.newInstance();
                Map<String, String> mod_params = new LinkedHashMap<String, String>(init_params);
                // overwrite sources_query_uri to point to remote_sources_query_uri
                mod_params.put("sources_query_uri", remote_sources_query_uri);
                remote_sources_cap.init(init_params);
            } catch (InstantiationException ex) {
                Logger.getLogger(Das1SourcesProxyCapability.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(Das1SourcesProxyCapability.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Das1SourcesProxyCapability.class.getName()).log(Level.SEVERE, null, ex);
            }
	}
    }

    public Das1SourcesProxyCapability(URI base_uri, String query_uri, Das2SourcesCapabilityI remote_sources_cap) {
	super(base_uri, query_uri, "sources", null, null, remote_sources_cap);
	// remote_sources_cap.init() will get called in superclass constructor...
	this.remote_sources_cap = remote_sources_cap;

	// make a merged params that includes params from both remote_sources_cap.init_params and this.init_params
	//	Map<String, List<String>> merged_params = null;

    }


    //    public void init(Map<String, List<String>> params)  {
    //	remote_sources_cap.init(params);
    //    }

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
	int segcap_injections = 0;
	Das2SourcesResponseI remote_sources_holder = remote_sources_cap.getSources();
	List<Das2LinkI> remote_links = remote_sources_holder.getLinks();
	String remote_maintainer_email = remote_sources_holder.getMaintainerEmail();
	List<Das2SourceI> remote_sources = remote_sources_holder.getSources();
	List<Das2SourceI> sources_list = new ArrayList<Das2SourceI>(remote_sources.size());

	Map<URI, Das2CoordinatesI> uri2coords = new HashMap<URI, Das2CoordinatesI>();
	Map<URI, Das1EntryPointsCapabilityI> coord_uri_to_entry_cap = new HashMap<URI, Das1EntryPointsCapabilityI>();
	List<Das2Version> versions_without_segments = new ArrayList<Das2Version>();
	Map<URI, Das2SegmentsCapabilityI> coord_uri_to_segments_cap = new HashMap<URI, Das2SegmentsCapabilityI>();

	try {
	    
	    for (Das2SourceI remote_source : remote_sources)  {
              URI source_uri = remote_source.getAbsoluteURI();
              if (source_uri == null)  {
                  System.out.println("couldn't determine legitimate URI for " + remote_source.getTitle() +
                      ", therefore skipping");
                continue;
              }
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
							   

		    Das2SegmentsCapabilityI das2_segments_cap = null;

		    for (Das2CapabilityI das1_cap : remote_version.getCapabilities())  {
			Das2CapabilityI proxy_cap = null;
			if (das1_cap.getType().startsWith(DAS1_CAPABILITY_PREFIX)) {
			    // construct URIs for proxy base URI and proxy query URI 
			    //   based on sources base URI and remote capability query URI
			    //   using remote capability query URIs authority and path parts
			    //   (don't want to just append cap query URI to sources base URI 
			    //       because resulting URI would have "http://" in the middle of it, 
			    //       which might be legal but looks odd)
			    URI absolute_remote_uri = das1_cap.getAbsoluteURI();
			    String remote_authority = absolute_remote_uri.getAuthority();
			    String remote_path = absolute_remote_uri.getPath();
			    // need to append "./" to deal with cases where otherwise remote_path wouldn't
			    //    parse as a relative URI (and thereofre URI.resolve(remote_path) would 
			    //    throw an IllegalArgumentException
			    String remote_section = "./" + remote_authority + remote_path;
			    URI proxy_query_uri = null;
			    try  {
				proxy_query_uri = getBaseURI().resolve(remote_section);
			    }
			    catch (Exception ex)  {
				System.out.println("problem creating URI: " + remote_section);
				// couldn't make a proxy capability URI, so skip this capability
				continue;
			    }
			    // for proxy_base_uri use proxy_query_uri but get rid of anything after last slash
			    URI proxy_base_uri = proxy_query_uri.resolve(".");  
			    String proxy_query_string = proxy_query_uri.toString();

			    if (DEBUG) { System.out.println("resolved proxy query: " + proxy_query_uri); }
			    if (DEBUG) { System.out.println("resolved proxy base:  " +  proxy_base_uri); }

			    boolean passthrough_if_no_proxy = default_to_passthrough;

			    if (das1_cap instanceof Das1FeaturesCapabilityI)  {
				if (proxy_das1_features)  {
				    if (DEBUG)  { System.out.println("DAS1 features cap: " + das1_cap); }
				    // add a Das2FeaturesCababilityI implemented by Das1FeaturesProxyCapability
				    Das1FeaturesCapabilityI das1_feat_cap = (Das1FeaturesCapabilityI)das1_cap;
				    Das2FeaturesCapabilityI das2_feat_cap = 
					new Das1FeaturesProxyCapability(proxy_base_uri, 
									proxy_query_string, 
									das1_feat_cap);
				    proxy_cap = das2_feat_cap;
				}
			    }
			    else if (das1_cap instanceof Das1EntryPointsCapabilityI)  {
				if (proxy_das1_entry_points)  {
				    Das1EntryPointsCapabilityI das1_entry_points_cap = 
					(Das1EntryPointsCapabilityI)das1_cap;
				    das2_segments_cap =
					new Das1SegmentsProxyCapability(proxy_base_uri,  
									proxy_query_string, 
									das1_entry_points_cap);
				    proxy_cap = das2_segments_cap;
				}
			    }
			    else if (das1_cap instanceof Das1TypesCapabilityI)  { 
				if (proxy_das1_types)  {
				    Das1TypesCapabilityI das1_types_cap = (Das1TypesCapabilityI)das1_cap;
				    Das2TypesCapabilityI das2_types_cap = 
					new Das1TypesProxyCapability(proxy_base_uri, 
								     proxy_query_string, 
								     das1_types_cap);
				    proxy_cap = das2_types_cap;
				}
			    }
			    /*
			      else if (das1_cap instanceof Das1SourcesCapabilityI)  { 
			      // shouldn't encounter a sources capability here, but just in case...
                              } 
			      else  {  
			      // unknown capability, but may need to proxy 
			      //   in case capability is relying on base uri of original sources response?
			      if (proxy_unknowns)  {
			      GenericProxyCapability generic_proxy_cap = 
			      new GenericProxyCapability(getBaseURI(), absolute_query_uri.toString(),
                           			      das1_cap.getType(), version, null, das1_cap  ); 
			      proxy_cap = generic_proxy_cap;
			      }
			      }
			    */
			}
			// add formats, extensions, additional_data ?
			// since all proxies used here are extending GenericCapabilityProxy, if these fields 
			//    should mirror those in das1_cap then don't need to add, 
			//    since gcp.getFormats(), etc. have passthrough to das1_cap.getFormats(), etc.
			//  if formats etc. _do_ need to change relative to what's in das1_cap, 
			//      then should probably happen within FooProxyCapability class rather than here?
		        if (proxy_cap != null)  {  // if proxy created, then use it instead of remote capability
			    version.addCapability(proxy_cap); // add new DAS/2 capability
			    version.addCapability(das1_cap);  // keep old DAS1 capability too
			}
			else if (default_to_passthrough)  {  // if no proxy created, use remote capability?
			    version.addCapability(das1_cap);
			}
		    }  // END capabilities handling

		    for (Das2CoordinatesI remote_coord : remote_version.getCoordinates())  {
			// nearly complete clone of remote_coord?  
			// but needed in case remote_coord is relying on base uri of original sources response
			URI remote_coord_uri = remote_coord.getAbsoluteURI();
			String remote_coord_str = remote_coord_uri.toString();
			if (remote_coord_str.endsWith("CS_DS40")) {
			    remote_coord_uri = new URI("http://www.ncbi.nlm.nih.gov/genome/H_sapiens/B36.1");
			}
			Das2Coordinates coord = new Das2Coordinates(remote_coord_uri, 
								    remote_coord_uri.toString(), 
								    remote_coord.getTaxonomyID(), 
								    remote_coord.getCoordinateType(), 
								    remote_coord.getAuthority(), 
								    remote_coord.getBuildVersion(), 
								    remote_coord.getCreated(), 
								    remote_coord.getTestRange() );
			if (das2_segments_cap != null) {
			    coord_uri_to_segments_cap.put(coord.getAbsoluteURI(), das2_segments_cap);
			}
			version.addCoordinates(coord);
		    }
		    for (Das2PropertyI prop : remote_version.getProperties())  {
			version.addProperty(prop);
		    }

		    source.addVersion(version);
		    if (das2_segments_cap == null)  { versions_without_segments.add(version); }
		}
		sources_list.add(source);
	    }

	    /**
	     *  attempting to assign segments capability for versions whose corresponding 
	     *    DAS1 version had no entry_points capability
	     *  Basing this on assumption that any DAS1 entry_points capability whose version 
	     *      has coordinates XYZ can also be used to find segments for any other version that 
	     *      also has coordinates XYZ
	     */
	    if (inject_segments_cap)  {
		System.out.println("versions without segments: " + versions_without_segments.size());
		for (Das2Version version : versions_without_segments)  {
		    for (Das2CoordinatesI coord : version.getCoordinates())  {
			URI coord_uri = coord.getAbsoluteURI();
			Das2SegmentsCapabilityI segments_cap = coord_uri_to_segments_cap.get(coord_uri);
			if (segments_cap != null)  {
			    segcap_injections++;
			    if (DEBUG)  {
				System.out.println("segcap inject: v= " + version.getLocalURIString() + 
						   ", coord= " + 
				    coord.getLocalURIString().substring(coord.getLocalURIString().lastIndexOf("/")) +
						   ", cap = " + segments_cap.getLocalURIString().substring(40));
			    }
			    version.addCapability(segments_cap);
			    break;
			}
		    }
		}
	    }
	} //catch (URISyntaxException ex) {
	catch (Exception ex) {
	    Logger.getLogger(Das1SourcesProxyCapability.class.getName()).log(Level.SEVERE, null, ex);
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

	if (inject_segments_cap)  {
	    System.out.println("number of segments capability injections: " + segcap_injections);
	}
					  
      return success;
    }


    public static void main(String[] args) throws URISyntaxException, XMLStreamException, JDOMException  {
	// String sources_url = "file:./data/das1_registry_sources.slice.xml";
	String sources_url = "file:./data/das1_registry_sources.xml";
	Das1SourcesCapability remote_sources_cap = new Das1SourcesCapability(sources_url);
	URI base_uri = new URI("http://localhost/das2/das1_proxy/genome/");
	String query_uri = "sources";
	Das1SourcesProxyCapability sources_cap = new Das1SourcesProxyCapability(base_uri, 
									query_uri, 
									remote_sources_cap);
	//	List<Das2SourceI> sources = cap.getSources();
	Das2SourcesResponseI sources_response = sources_cap.getSources();
	// SourcesXmlWriter sources_writer = new SourcesXmlWriter(sources_response, System.out);
	//   sources_writer.writeSourcesDocument();

	Das2SourceI source = sources_response.getSources().get(0);
	Das2VersionI version = source.getVersions().get(0);
	System.out.println();
	System.out.println();

	System.out.println("getting types for assembly version: " + version.getAbsoluteURI());
	Das2TypesCapabilityI types_cap = (Das2TypesCapabilityI) version.getCapability(Das2Constants.DAS2_TYPES_CAPABILITY);
	Das2TypesResponseI types_response = types_cap.getTypes();
	Das2TypeI type = types_response.getTypes().get(0);
	//	TypesXmlWriter types_writer = new TypesXmlWriter(types_response, System.out);
	//	types_writer.writeTypesDocument();
	System.out.println();
	System.out.println();

	System.out.println("getting segments for version: " + version.getAbsoluteURI());
        Das2SegmentsCapabilityI segments_cap = (Das2SegmentsCapabilityI) version.getCapability(Das2Constants.DAS2_SEGMENTS_CAPABILITY);
	Das2SegmentsResponseI segments_response = segments_cap.getSegments();
	Das2SegmentI segment = segments_response.getSegments().get(0);
	//	SegmentsXmlWriter segments_writer = new SegmentsXmlWriter(segments_response, System.out);
	//	segments_writer.writeSegmentsDocument();
	System.out.println();
	System.out.println();
	
    }

}