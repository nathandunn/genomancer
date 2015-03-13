package genomancer.vine.das2.client.modelimpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Identifiable  {
    // extends Das2CommonMetaAttributes

    protected URI base_uri;
    protected URI absolute_uri;
    protected URI local_uri;
    protected String local_uri_string;
    protected boolean has_local_uri = true;

    public Identifiable(URI base_uri, String local_uri_string)  {
	this.base_uri = base_uri;
	this.local_uri_string = local_uri_string;
      has_local_uri = (local_uri_string != null); 
    }

    /**
     *  Das2SourceI.getAbsoluteURI() implementation 
     *  
     *   return absolute URI  (resolves local URI against a base URI if needed)
     *   Uses lazy instantiation of absolute_uri (and potentially of local_uri as well)
     *   Creating a URI from a String (and URI resolution) can be a relatively slow parsing operation, 
     *          and URIs generally have a bigger memory footprint than the equivalent String
     *      Furthermore some DAS2 clients may never call getLocalURI() or getAbsoluteURI(), for 
     *          example clients that are just collecting location/score statistics over a set of features 
     *      Therefore avoiding URI creation till it is first needed
     */
    public URI getAbsoluteURI() { 
	if (absolute_uri == null)  {
          if (has_local_uri)  {
              absolute_uri = getBaseURI().resolve(getLocalURI());
          }
          else  { absolute_uri = getBaseURI(); }
	}
	return absolute_uri;
    }

    /**
     *  Das2SourceI.getLocalURI() implementation 
     *
     *   return local URI 
     *   using term "local URI" rather than "relative URI" because this URI can be either 
     *      absolute or relative (if it's absolute then getLocalURI() and getAbsoluteURI() will return same URI)
     *
     *   Uses lazy instantiation of local_uri based on local_uri_string
     *   Creating a URI from a String can be a relatively slow parsing operation, 
     *          and URIs generally have a bigger memory footprint than the equivalent String
     *      Furthermore some DAS2 clients may never call getLocalURI() or getAbsoluteURI(), for 
     *          example clients that are just collecting location/score statistics over a set of features 
     *      Therefore avoiding URI creation till it is first needed
     *      Once local_uri is populated, also null out local_uri_string to minimize memory footprint?
     *         (local_uri_string is likely cached inside URI object anyway, depending on implementation)
     *             [in Java 5.0 standard URI implementation it's cached in "string" field if 
     *                 URI was created via URI(String uri) constructor ]
     */
    public URI getLocalURI()  {
	if (local_uri == null && has_local_uri)  {
            try {
                local_uri = new URI(local_uri_string);
		//  null out local_uri_string to minimize memory footprint
		local_uri_string = null;
            } catch (URISyntaxException ex) {
                Logger.getLogger(Das2CommonData.class.getName()).log(Level.SEVERE, null, ex);
            }
	}
	return local_uri;
    }

    /**
     *   if only making calls to getLocalURIString(), then local_uri_string should be populated, 
     *      so return local_uri_string rather than triggering URI creation
     *   otherwise local_uri should be populated, just call URI.toString()
     */
    public String getLocalURIString()  {
	if (local_uri_string == null)  {
	    return getLocalURI().toString();
	}
	else  { return local_uri_string; }
    }

    public String getAbsoluteURIString()  {
	return getAbsoluteURI().toString();
    }

    public URI getBaseURI()  { return base_uri; }
 
}