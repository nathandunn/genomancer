package genomancer.trellis.das2.model;

import java.io.OutputStream;

public interface Das2FeatureWriterI  {
    public boolean writeFeatures(Das2FeaturesResponseI feature_response, OutputStream ostr);
    public boolean acceptsType(Das2TypeI type);
    public Das2FormatI getFormat();
}