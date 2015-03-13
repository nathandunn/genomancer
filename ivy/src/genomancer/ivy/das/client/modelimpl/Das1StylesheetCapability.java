/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package genomancer.ivy.das.client.modelimpl;

import genomancer.ivy.das.model.Das1StylesheetI;
import genomancer.ivy.das.model.Das1StylesheetCapabilityI;
import genomancer.vine.das2.client.modelimpl.Das2Coordinates;
import genomancer.vine.das2.client.modelimpl.Das2GenericCapability;
import genomancer.vine.das2.client.modelimpl.Das2Version;
import java.net.URI;

/**
 *
 * @author gregg
 */
public class Das1StylesheetCapability extends Das2GenericCapability implements Das1StylesheetCapabilityI {

    public Das1StylesheetCapability(URI xml_base, 
				    String local_query_uri, 
				    Das2Version version, 
				    Das2Coordinates coords) {
	super(xml_base, local_query_uri, "das1:stylesheet", version, coords);
    }

    public Das1StylesheetI getStylesheet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }


}
