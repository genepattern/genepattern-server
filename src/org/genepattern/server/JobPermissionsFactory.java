/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import org.genepattern.server.database.HibernateUtil;

/**
 * Factory method(s) for initializing JobPermissions flags for a given user for a given job.
 */
public class JobPermissionsFactory {
    public static final JobPermissions createJobPermissionsFromDb(final boolean isAdmin, final String userId, final int jobNumber) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            PermissionsHelper perm = new PermissionsHelper(isAdmin, userId, jobNumber);
            return toJobPermissions(perm);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    /**
     * Convert PermissionsHelper instance into JobPermissions instance.
     * @param ph
     * @return
     */
    private static final JobPermissions toJobPermissions(final PermissionsHelper ph) {
        return new JobPermissions.Builder()
            .canRead(ph.canReadJob())
            .canWrite(ph.canWriteJob())
            .canSetPermissions(ph.canSetJobPermissions())
            .isPublic(ph.isPublic())
            .isShared(ph.isShared())
            .build();
    }
}
