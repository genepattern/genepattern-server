/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
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
