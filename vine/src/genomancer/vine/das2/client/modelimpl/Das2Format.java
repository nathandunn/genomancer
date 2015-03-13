package genomancer.vine.das2.client.modelimpl;

import genomancer.trellis.das2.model.Das2FormatI;

/**
 *
 */
public class Das2Format implements Das2FormatI {
    String name;
    String mime_type;

    
    public Das2Format(String name, String mime_type)  {
	this.name = name;
	this.mime_type = mime_type;
    }

    public String getName() { return name; }
    public String getMimeType() { return mime_type; }

}