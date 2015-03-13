package genomancer.trellis.das2.model;

import java.io.InputStream;


public interface Das2FeatureReaderI  {
    public Das2FeaturesResponseI readFeatures(InputStream istr);
    public boolean acceptsType(Das2TypeI type);
}