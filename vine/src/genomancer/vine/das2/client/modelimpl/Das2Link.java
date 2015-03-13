package genomancer.vine.das2.client.modelimpl;

import java.net.URI;
import genomancer.trellis.das2.model.Das2LinkI;

public class Das2Link extends Das2CommonMetaAttributes implements Das2LinkI {
    String href;
    String title;
    String mimetype;
    String relationship;
    String reverse_relationship;

    public Das2Link(String href, String title, String mimetype, 
		    String relationship, String reverse_relationship)  {
	this.href = href;
	this.title = title;
	this.mimetype = this.mimetype;
	this.relationship = relationship;
	this.reverse_relationship = reverse_relationship;
    }

    public String getHref() { return href; }
    public String getTitle() { return title; }
    public String getMimeType() { return mimetype; }
    public String getRelationship() { return relationship; }
    public String getReverseRelationship() { return reverse_relationship; }

}