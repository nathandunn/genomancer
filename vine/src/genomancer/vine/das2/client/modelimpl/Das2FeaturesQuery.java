package genomancer.vine.das2.client.modelimpl;

import genomancer.trellis.das2.model.Das2LocationRefI;
import java.net.URI;
import java.util.ArrayList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import genomancer.trellis.das2.model.Das2FeaturesQueryI;


public class Das2FeaturesQuery implements Das2FeaturesQueryI  {
    // Das2VersionI version;
    String format = "das2xml";  // default format is "das2xml"

    List<Das2LocationRefI> overlaps = new ArrayList<Das2LocationRefI>();
    List<Das2LocationRefI> insides = new ArrayList<Das2LocationRefI>();
    List<Das2LocationRefI> excludes = new ArrayList<Das2LocationRefI>();
    List<URI> types = new ArrayList<URI>();
    List<URI> coordinates = new ArrayList<URI>();
    List<URI> links = new ArrayList<URI>();
    List<String> names = new ArrayList<String>();
    List<String> notes = new ArrayList<String>();

    // leaving out "prop-*" queries for now (other than pass-through as non_standard_params)
    //    List<Das2PropertyI> properties = new ArrayList<Das2PropertyI>();
    Map<String, List<String>> non_standard_params = new LinkedHashMap<String, List<String>>();
    
    //  public Das2FeaturesQuery(Das2VersionI version)  { 
    //    	this.version = vers ion;  
    //    }

    //    public Das2VersionI getVersion()  { return version; }

    public String getFormat() { return format; }
    public List<Das2LocationRefI> getOverlaps() { return overlaps; }
    public List<Das2LocationRefI> getInsides() { return insides; }
    public List<Das2LocationRefI> getExcludes() { return excludes; }
    public List<URI> getTypes() { return types; }
    public List<URI> getCoordinates() { return coordinates; }
    public List<URI> getLinks() { return links; }
    public List<String> getNames() { return names; }
    public List<String> getNotes() { return notes; }
    //    public List<Das2PropertyI> getProperties() { return properties; }
    public Map<String, List<String>> getNonStandardParams()  { return non_standard_params; }

    public void setFormat(String format)  { this.format = format; }
    public void addOverlap(Das2LocationRefI overlap)  { overlaps.add(overlap); }
    public void addInside(Das2LocationRefI inside)  { insides.add(inside); }
    public void addExclude(Das2LocationRefI exclude)  { excludes.add(exclude); }
    public void addType(URI type)  { types.add(type); }
    public void addCoordinate(URI coordinate)  { coordinates.add(coordinate); }
    public void addLink(URI link)  { links.add(link); }
    public void addName(String name)  { names.add(name); }
    public void addNote(String note)  { notes.add(note); }

    public void addNonStandardParam(String param, String value)  {
	System.out.println("    adding nonstandard param: name = " + param + ", value = " + value);
	List<String> values = non_standard_params.get(param);
	if (values == null)  {
	    values = new ArrayList<String>();
	    non_standard_params.put(param, values);
	}
	values.add(value);
    }

}
	
