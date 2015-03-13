/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package genomancer.tengcha;

import genomancer.vine.das2.client.modelimpl.Das2Source;
import genomancer.vine.das2.client.modelimpl.Das2Version;
import java.util.Date;
import org.gmod.gbol.simpleObject.io.impl.HibernateHandler;


/**
 *
 * @author gregg
 */
public class TengchaDas2Version extends Das2Version {
    HibernateHandler handler;
    public TengchaDas2Version(Das2Source source,
		       String local_uri_string, 
		       String title, 
		       String description, 
		       String info_url,
		       Date creation_date, 
		       Date last_modified_date, 
		       HibernateHandler handler)  {
	super(source, local_uri_string, title, description, info_url, creation_date, last_modified_date);
	this.handler = handler;
    }
	
    public HibernateHandler getHibernateHandler()  { return handler; }
}
