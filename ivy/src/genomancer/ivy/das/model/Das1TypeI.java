
package genomancer.ivy.das.model;
/**
 * 
 * 
 */
public interface Das1TypeI {
    int NO_TYPE_COUNT = -1;

    public String getID();  // FEATURE/TYPE/@id in features response, TYPE/@id in types response
    public String getCategory();  //FEATURE/TYPE/@category in features response, TYPE/@category in types responses
    public String getLabel();  // FEATURE/TYPE/text:label in features response
    public boolean isReference(); // FEATURE/TYPE/@reference in features response
    public String getMethod();  // TYPE/@method in types response
    public boolean hasFeatureCount();
    public int getFeatureCount();  // TYPE/text:count in types response
    // what about FEATURE/METHOD/@id and FEATURE/METHOD/text:label in features response ??
    
}


