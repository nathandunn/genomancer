
package genomancer.ivy.das.model;

import java.net.URL;
import java.util.List;

public interface Das1TypesResponseI extends Das1ResponseI  {

    public Das1TypeI getType(String id);
    public List<Das1TypeI> getTypes();

}


