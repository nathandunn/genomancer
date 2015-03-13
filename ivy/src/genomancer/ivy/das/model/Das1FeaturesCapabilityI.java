
package genomancer.ivy.das.model;

import genomancer.trellis.das2.model.Das2CapabilityI;

/**
 * 
 * 
 */
public interface Das1FeaturesCapabilityI extends Das2CapabilityI {

    public Das1FeaturesResponseI getFeatures();
    public Das1FeaturesResponseI getFeatures(Das1FeaturesQueryI query);


}


