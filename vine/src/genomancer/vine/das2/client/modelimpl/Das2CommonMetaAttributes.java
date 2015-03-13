package genomancer.vine.das2.client.modelimpl;

import java.net.URI;
import genomancer.trellis.das2.model.Das2CommonMetaAttributesI;

public class Das2CommonMetaAttributes implements Das2CommonMetaAttributesI {
    URI xml_base;
    URI xml_space;
    String xml_id;
    String xml_lang;

    public URI getXmlBase() { return xml_base; }
    public URI getXmlSpace() { return xml_space; }
    public String getXmlID() { return xml_id; }
    public String getXmlLang() { return xml_lang; }

}