package genomancer.vine.das2.client.modelimpl;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom.Element;
import genomancer.trellis.das2.model.Das2CommonDataI;
import genomancer.trellis.das2.model.Das2PropertyI;

public abstract class Das2CommonData extends Identifiable implements Das2CommonDataI  {
    public boolean DEBUG_UNKNOWN_XML = true;
    protected String info_url;
    protected String title;
    protected String description;
    protected List<Das2PropertyI> properties = new ArrayList<Das2PropertyI>();
    protected List<org.jdom.Element> additonal_data = new ArrayList<org.jdom.Element>();

    public Das2CommonData(URI base_uri, 
			  String local_uri_string,
			  String title, 
			  String description, 
			  String info_url)  {
	super(base_uri, local_uri_string);
	this.title = title;
	this.description = description;
	this.info_url = info_url;
    }


    /** Das2CommonDataI implementation */
    public String getInfoURL() { return info_url; }

    /** Das2CommonDataI implementation */
    public String getTitle() { return title; }

    /** Das2CommonDataI implementation */
    public String getDescription() { return description; }

    /** Das2CommonDataI implementation */
    public List<Das2PropertyI> getProperties() { return properties; }

    /** Das2CommonDataI implementation */
    public List<org.jdom.Element> getAdditionalData() { return additonal_data; }

    public void addProperty(Das2PropertyI property)  { properties.add(property); }

    public void addAdditionalData(org.jdom.Element data)  {
	additonal_data.add(data);
	if (DEBUG_UNKNOWN_XML)  {
	    org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
	    java.io.PrintWriter pw = new java.io.PrintWriter(System.out);
	    System.out.println("***************");
            try {
                outputter.output(data, pw);
            } catch (IOException ex) {
                Logger.getLogger(Das2CommonData.class.getName()).log(Level.SEVERE, null, ex);
            }
	    pw.println();
	    pw.flush();	
	    System.out.println("***************");
	}
    }

}