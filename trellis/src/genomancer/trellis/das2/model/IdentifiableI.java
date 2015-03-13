package genomancer.trellis.das2.model;

import java.net.URI;

public interface IdentifiableI  {
    public URI getAbsoluteURI();
    public URI getLocalURI();
    public String getLocalURIString();
    public String getAbsoluteURIString(); 
    public URI getBaseURI();
}