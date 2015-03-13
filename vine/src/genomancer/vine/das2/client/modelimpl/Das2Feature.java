package genomancer.vine.das2.client.modelimpl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import genomancer.trellis.das2.model.Das2FeatureI;
import genomancer.trellis.das2.model.Das2LinkI;
import genomancer.trellis.das2.model.Das2LocationI;
import genomancer.trellis.das2.model.Das2TypeI;

public class Das2Feature extends Das2CommonData implements Das2FeatureI {
    Das2TypeI type;
    Date creation_date;
    Date last_modified_date;
    List<Das2LocationI> locations;
    List<Das2FeatureI> parts;
    List<Das2FeatureI> parents;
    List<String> aliases;
    List<Das2LinkI> links;
    List<String> notes;
    
    public Das2Feature(URI base_uri, 
		       String local_uri_string, 
		       String title, 
		       String description, 
		       String info_url, 
		       Das2TypeI type, 
		       Date creation_date, 
		       Date last_modified_date)  {
	super(base_uri, local_uri_string, title, description, info_url);
	this.type = type;
	this.creation_date = creation_date;
	this.last_modified_date = last_modified_date;
    }

    public Das2Feature(URI base_uri, 
		       String local_uri_string, 
		       Das2TypeI type)  {
	this(base_uri, local_uri_string, null, null, null, type, null, null);
    }
	
    /**  Das2FeatureI implementation */	   
    public Das2TypeI getType() { return type; }

    /**  Das2FeatureI implementation */	   
    public Date getCreationDate() { return creation_date; }

    /**  Das2FeatureI implementation */	   
    public Date getLastModifiedDate() { return last_modified_date; }

    /**  Das2FeatureI implementation */	   
    public List<Das2LocationI> getLocations() { return locations; }  // may be null

    /**  Das2FeatureI implementation */	   
    public List<Das2FeatureI> getParts() { return parts; }  // null for leafs

    /**  Das2FeatureI implementation */	   
    public List<Das2FeatureI> getParents() { return parents; }   // null for roots

    /**  Das2FeatureI implementation */	   
    public List<String> getAliases() { return aliases; }  // may be null

    /**  Das2FeatureI implementation */	   
    public List<Das2LinkI> getLinks() { return links; }  // may be null

    /**  Das2FeatureI implementation */	   
    public List<String> getNotes() { return notes; }  // may be null


    public void addLocation(Das2LocationI location)  { 
	if (locations == null)  { locations = new ArrayList<Das2LocationI>(); }
	locations.add(location);
    }

    /**
     *   any code constructing a parent/part hierarchy is 
     *   responsible for explicitly calling both addPart() on all parents 
     *   and addParent() on all parts -- there is no auto-reciprocation 
     *   of parent/part relationships (but they are required for it to be a valid DAS2 model)
     */
    public void addPart(Das2FeatureI part)  {
	if (parts == null)  { parts = new ArrayList<Das2FeatureI>(); }
	parts.add(part);
    }

    /**
     *   any code constructing a parent/part hierarchy is 
     *   responsible for explicitly calling both addPart() on all parents 
     *   and addParent() on all parts -- there is no auto-reciprocation 
     *   of parent/part relationships (but they are required for it to be a valid DAS2 model)
     */
    public void addParent(Das2FeatureI parent)  {
	if (parents == null)  { parents = new ArrayList<Das2FeatureI>();  }
	parents.add(parent);
    }

    public void addAlias(String alias)  {
	if (aliases == null)  { aliases = new ArrayList<String>(); }
	aliases.add(alias);
    }

    public void addLink(Das2LinkI link)  {
	if (links == null)  { links = new ArrayList<Das2LinkI>(); }
	links.add(link);
    }

    public void addNote(String note)  {
	if (notes == null)  { notes = new ArrayList<String>(); }
	notes.add(note);
    }

}