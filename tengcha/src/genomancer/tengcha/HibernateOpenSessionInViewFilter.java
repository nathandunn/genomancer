package genomancer.tengcha;

import java.io.IOException;
import javax.servlet.*;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import genomancer.tengcha.Config;
import org.gmod.gbol.util.HibernateUtil;

/**
 * 
 * based on "Open Session in View" pattern: 
 *    started with code at   https://community.jboss.org/wiki/OpenSessionInView 
 *    and modified as needed
 * 
 * Don't need to explicitly call session.flush() before the commit, or session.close() after the commit
 * in fact if tried to close would get a "Session already closed" error, 
 *    because the managed current session is automatically flushed (before the actual commit) 
 *    and closed (after the actual commit) ,  
 * From Hibernate documentation: https://community.jboss.org/wiki/SessionsAndTransactions :
 *   Because Hibernate can't bind the "current session" to a transaction, as it does in a JTA environment, 
 *   it binds it to the current Java thread. It is opened when getCurrentSession() is called for the first time, 
 *   but in a "proxied" state that doesn't allow you to do anything except start a transaction. 
 *   When the transaction ends, either through commit or roll back, the "current" Session is closed automatically. 
 *   The next call to getCurrentSession() starts a new proxied Session, and so on...
 *   This does not mean that all Hibernate Sessions are closed when a transaction is committed! 
 *   Only the Session that you obtained with sf.getCurrentSession() is flushed and closed automatically
*/
public class HibernateOpenSessionInViewFilter implements Filter {
 
    // Private static Log log = LogFactory.getLog(HibernateSessionRequestFilter.class);
 
    private SessionFactory sf;
 
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

	try  {
	System.out.println("*** start OpenSessionInViewFilter.doFilter() ***");
            System.out.println("Starting a database transaction");
            sf.getCurrentSession().beginTransaction();
 
            // Call the next filter (continue request processing)
            chain.doFilter(request, response);
 
            // Commit and cleanup
            System.out.println("Committing the database transaction");
	    // sf.getCurrentSession.flush()  // do not call flush, see comment above 
	    sf.getCurrentSession().getTransaction().commit();	    
	  //sf.getCurrentSession().close(); // do not call close, see comment above

    
 
        } catch (StaleObjectStateException staleEx) {
            System.err.println("This interceptor does not implement optimistic concurrency control!");
            System.err.println("Your application will not work until you add compensation actions!");
            // Rollback, close everything, possibly compensate for any permanent changes
            // during the conversation, and finally restart business conversation. Maybe
            // give the user of the application a chance to merge some of his work with
            // fresh data... what you do here depends on your applications design.
            throw staleEx;
        } catch (Throwable ex) {
            // Rollback only
            ex.printStackTrace();
            try {
                if (sf.getCurrentSession().getTransaction().isActive()) {
                    System.out.println("Trying to rollback database transaction after exception");
                    sf.getCurrentSession().getTransaction().rollback();
                }
            } catch (Throwable rbEx) {
                System.err.println("Could not rollback transaction after exception!");
                rbEx.printStackTrace();
            }
 
            // Let others handle it... maybe another interceptor for exceptions?
            throw new ServletException(ex);
        }
	System.out.println("*** end OpenSessionInViewFilter.doFilter() ***");
    }
 
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("*** Initializing HibernateOpenSessionInvViewFilter...");
	//        System.out.println("Obtaining SessionFactory from static HibernateUtil singleton");
	try  {
	    sf = HibernateUtil.getSessionFactory(Config.HIBERNATE_CFG_XML);
	    System.out.println("in HibernateOpenSessionInViewFilter.init(), SessionFactory: " + sf);
	}
	catch (Exception ex)  {
	    System.err.println("could not create SessionFactory");
	    ex.printStackTrace(); 
	}
    }
 
    public void destroy() {}
 
}