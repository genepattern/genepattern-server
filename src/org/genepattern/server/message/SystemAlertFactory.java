package org.genepattern.server.message;

/**
 * Get singleton instance which implements ISystemAlert.
 * Hides implementation details (e.g. Hibernate classes) from the rest of the application.
 * @author pcarr
 */
public class SystemAlertFactory {
    private static ISystemAlert systemAlertHibernate = new SystemAlertHibernate();
    
    public static ISystemAlert getSystemAlert() {
        return systemAlertHibernate;
    }
}
