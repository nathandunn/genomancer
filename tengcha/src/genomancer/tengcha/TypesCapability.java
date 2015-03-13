package genomancer.tengcha;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import genomancer.trellis.das2.model.Das2CoordinatesI;
import genomancer.trellis.das2.model.Das2FormatI;
import genomancer.trellis.das2.model.Das2TypeI;
import genomancer.trellis.das2.model.Das2TypesCapabilityI;
import genomancer.trellis.das2.model.Das2TypesResponseI;
import genomancer.trellis.das2.model.Das2VersionI;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2Type;
import genomancer.vine.das2.client.modelimpl.Das2TypesResponse;

// stuff we need to connect to Chado using GBOL
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gmod.gbol.simpleObject.io.impl.HibernateHandler;
import org.gmod.gbol.simpleObject.Analysis;
import org.gmod.gbol.simpleObject.Organism;

public class TypesCapability extends Das2GenericCapability implements Das2TypesCapabilityI  {
    Das2TypesResponse response = null;
    boolean initialized = false;
    
    // child types 
    // protected Das2Type type_UTR;
    // protected Das2Type type_CDS;  // actually more appropriate term is CDS-segment
    // protected Das2Type type_EXON;
    // protected Das2Type type_UNKNOWN;
    // protected Das2Type type_WHOLECDS;  // actually more appropriate term is CDS, but need to distinguish from CDS-segment
    
    public TypesCapability(Das2VersionI version, Das2CoordinatesI coords)  {
        super(version.getBaseURI(), (version.getLocalURIString()+"/types"), "types", version, coords);
    }

    public Das2TypesResponseI getTypes() {
	if (!initialized)  { initTypes(); }
        return response;
    }

    public Das2TypeI getType(URI type_uri) {
	if (!initialized)  { initTypes(); }
        return response.getType(type_uri);
    }

    public Das2FormatI getFormat(String format_name)  {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected synchronized void initTypes()  {
	// only initialized once -- if want effect of re-initializing, must create new TypesCapability instead
	if (initialized)  { return; }
	System.out.println("called TypesCapability.initTypes(): " + version.getLocalURIString());
	HibernateHandler handler = ((TengchaDas2Version)this.getVersion()).getHibernateHandler();

        List<Das2TypeI> types_list = new ArrayList<Das2TypeI>();
        URI type_base_uri = this.getAbsoluteURI().resolve("./");
        
	// type_UTR = new Das2Type(type_base_uri, "UTR", "UTR", null, null, "UTR", null, false);
	// type_CDS = new Das2Type(type_base_uri, "CDS", "CDS", null, null, "CDS", null, false);
	// type_EXON = new Das2Type(type_base_uri, "exon", "exon", null, null, "exon", null, false);
	// type_UNKNOWN = new Das2Type(type_base_uri, "unknown", "unknown", null, null, "unknown", null, false);
	// type_WHOLECDS = new Das2Type(type_base_uri, "wholeCDS", "wholeCDS", null, null, "wholeCDS", null, false);        

//	Organism org = handler.getOrganismsWithFeatures().next();  // GAH added as stopgap
//	for (Iterator<? extends Analysis> analyses = handler.getAnalysesForOrganism(org); analyses.hasNext();) {   // GAH added as stopgap

        String orgName = getOrganismFromURI(type_base_uri); // assuming http://foo/A.mellifera/, return A.mellifera
        Organism thisOrg = handler.getOrganismByAbbreviation(orgName);

        for (Iterator<? extends Analysis> analyses = handler.getAnalysesForOrganism(thisOrg); analyses.hasNext();) {
            Analysis thisAnalysis = analyses.next();
            List<Das2TypeI> types = new ArrayList<Das2TypeI>();
            System.out.println("analysis: " + thisAnalysis.getName());

            // use [program]_[version]_[source] for the uri - the following is guaranteed to be 1) non-null and 2) unique in the analysis table
            String thisLocalUri = null;
            try {
                thisLocalUri = URLEncoder.encode(
                        thisAnalysis.getProgram() + "_" + thisAnalysis.getProgramVersion() + "_" + thisAnalysis.getSourceName(),
                        "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(TypesCapability.class.getName()).log(Level.SEVERE, null, ex);
            }

            String thisTitle =
                    thisOrg.getGenus() + " " + thisOrg.getSpecies() + " "
                    + thisAnalysis.getName() + " (" + thisAnalysis.getProgram() + ")";

            Das2Type thisType = new Das2Type(type_base_uri, // base_uri
                    thisLocalUri, // local_uri
                    thisTitle, // title
                    thisAnalysis.getDescription(), // description
                    null, // info_url
                    "unknown", // ontology_term_name
                    null, // method
                    true);          // is_searchable
            types_list.add(thisType);

        }
        response = new Das2TypesResponse(type_base_uri, types_list, null);
        initialized = true;
    }

    protected String getOrganismFromURI(URI thisUri) {
        String[] items = thisUri.toString().split("/");
        String lastItem = items[items.length - 1];
        return lastItem;
    }
}
