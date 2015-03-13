package genomancer.tengcha;

import genomancer.trellis.das2.Das2Constants;
import genomancer.trellis.das2.model.Das2FormatI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;

import genomancer.trellis.das2.model.Das2CapabilityI;
import genomancer.trellis.das2.model.Das2SourcesCapabilityI;
import genomancer.trellis.das2.model.Das2SegmentsCapabilityI;
import genomancer.trellis.das2.model.Das2TypesCapabilityI;
import genomancer.trellis.das2.model.Das2FeaturesCapabilityI;

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
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2Source;
import genomancer.vine.das2.client.modelimpl.Das2SourcesCapability;
import genomancer.vine.das2.client.modelimpl.Das2SourcesResponse;
import genomancer.vine.das2.client.modelimpl.Das2TypesCapability;
import genomancer.vine.das2.client.modelimpl.Das2Version;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.HashMap;
import javax.xml.stream.XMLStreamException;
import org.jdom.Element;
import org.jdom.JDOMException;

// stuff we need to connect to Chado using GBOL
import org.postgresql.Driver;
import org.hibernate.SessionFactory;
import org.gmod.gbol.util.HibernateUtil;
import org.gmod.gbol.simpleObject.Organism;
import org.gmod.gbol.simpleObject.CVTermRelationship;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.io.impl.HibernateHandler;

public class SourcesCapability extends Das2GenericCapability implements Das2SourcesCapabilityI {
    Das2SourcesResponseI sources = null;
    Map<URI, Das2SourceI> uri2source = null;
    Map<URI, Das2VersionI> uri2version = null;

    public SourcesCapability(URI base_uri, String query_uri, Map<String,String> params) throws URISyntaxException, Exception  {
        super(base_uri, query_uri, "sources", 
	      null, // Das2SourceCapabilities are not assigned a version (they contain version(s)
	      null  // Das2SourceCapabilities are not assigned coordinates (they contain coordinate(s)
	      );
            initSources();
    }

    public Das2SourcesResponseI getSources() {
	return sources;
    }

    public Das2SourceI getSource(URI source_uri)  {
	return uri2source.get(source_uri);
    }

    public Das2VersionI getVersion (URI version_uri)  {
	return uri2version.get(version_uri);
    }

    protected synchronized void initSources() throws Exception {
	HibernateHandler handler;
        
	try {
	    handler = new HibernateHandler( genomancer.tengcha.Config.HIBERNATE_CFG_XML);
	} catch (Exception e) {
	    System.err.println("Unable configure session factory while connecting to Chado: erm" + e.getMessage());
	    e.printStackTrace();
	    throw (e);
	}

        List<Das2SourceI> sources_list = new ArrayList<Das2SourceI>();

	// since initSources() is called within servlet init() method, need to call beginTransaction() 
	//    (during normal servlet HTTP request calls, this is handled by a servlet filter instead)
	// TODO:  would be better to make this more robust so will detect if need to begin transaction
	//      maybe something like:
	//         Transaction trans = handler.getCurrentSession().getTransaction();
	//         if (! trans.isActive())  { trans.begin(); }
	handler.beginTransaction();
        for (Iterator<? extends Organism> organisms = handler.getOrganismsWithFeatures(); organisms.hasNext();) {
            Organism thisOrg = organisms.next();
            
            String abbrev = thisOrg.getAbbreviation(); // we assume assembly number is in abbrev, e.g. amel45
            String genus = thisOrg.getGenus();
            String species = thisOrg.getSpecies();
            String commonName = thisOrg.getCommonName();
            String comment = thisOrg.getComment();
                        
            Das2Source usource = new Das2Source(base_uri, 
                    URLEncoder.encode(genus + "_" + species, "UTF-8"),   
                    genus + "_" + species, 
                    null, null);
            sources_list.add(usource);

            Das2Version uversion = new TengchaDas2Version(usource, 
                    URLEncoder.encode( abbrev, "UTF-8" ),
                    genus + "_" + species + " (" + commonName + ") " + comment,
                    null, null, null, null, handler);

            Das2CoordinatesI ucoords = new Das2Coordinates(base_uri, 
                    URLEncoder.encode( abbrev, "UTF-8") + "/coords",
                    genus + "_" + species,
                    genomancer.tengcha.Config.REFERENCE_SEQUENCE_SO_TERM, "authority_placeholder", 
                    URLEncoder.encode( abbrev, "UTF-8" ),
                    null, null);
            usource.addVersion(uversion);
            uversion.addCoordinates(ucoords);

            uversion.addCapability(new SegmentsCapability(uversion, ucoords));
            uversion.addCapability(new TypesCapability(uversion, ucoords));
            uversion.addCapability(new FeaturesCapability(uversion, ucoords));

            sources = new Das2SourcesResponse(getBaseURI(), sources_list, "maintainer_email@wherever", null);
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
        }
	// since initSources() is called within servlet init() method, need to call beginTransaction() 
	//    (during normal servlet HTTP request calls, this is handled by a servlet filter instead)
	// TODO:  would be better to make this more robust so will detect if need to commit transaction
	//     ( can assume that if (need to begin transaction) ==> (need to commit transaction) )
	handler.commitTransaction();
    }   

}
