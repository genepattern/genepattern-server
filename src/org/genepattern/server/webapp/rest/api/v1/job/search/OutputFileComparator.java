/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job.search;
import java.util.Comparator;

import org.genepattern.server.dm.GpFilePath;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * Helper class for sorting job result files as a List of GpFilePath.
 * This class does not deal with special-case files such as 'stdout', 'stderr', 
 * 'gp_execution_log.txt' and the pipeline log file.
 * 
 * @author pcarr
 *
 */
public class OutputFileComparator implements Comparator<GpFilePath> {
    public enum OrderFilesBy {
        name,
        date,
        size
    }
    
    private final String orderFilesByParam;
    private final boolean ascending;
    private final OrderFilesBy orderFilesBy;
    
    private OutputFileComparator(final Builder in) {
        this.orderFilesByParam=in.orderFilesByParam;
        this.orderFilesBy=in.orderFilesBy;
        this.ascending=in.ascending;
    }

    @Override
    public int compare(final GpFilePath o1, final GpFilePath o2) {
        final GpFilePath left;
        final GpFilePath right;
        if (ascending) {
            left=o1;
            right=o2;
        }
        else {
            right=o1;
            left=o2;
        }

        switch (orderFilesBy) {
        case date:
            return ComparisonChain.start()
                    .compare(left.getLastModified(), right.getLastModified(), Ordering.natural().nullsLast())
                    .result();
        case name:
            return ComparisonChain.start()
                    .compare(left.getName(), right.getName(), Ordering.from(String.CASE_INSENSITIVE_ORDER).nullsLast())
                    .result();
        }

        return Double.compare(left.getFileLength(),right.getFileLength()); 
    }

    public static class Builder {
        private String orderFilesByParam=null;
        private boolean ascending=true;
        private OrderFilesBy orderFilesBy=OrderFilesBy.date;

        /**
         * Initialize from the 'orderFilesBy' query parameters, can be null.
         * @param orderFilesBy
         */
        public Builder(final String orderFilesByParam) {
            this.orderFilesByParam=orderFilesByParam;
            if (orderFilesByParam==null) {
                //default value
            }
            else if (orderFilesByParam.startsWith("-")) {
                ascending=false;
                orderFilesBy=OrderFilesBy.valueOf(orderFilesByParam.substring(1));
            }
            else if (orderFilesByParam.startsWith("+")) {
                ascending=true;
                orderFilesBy=OrderFilesBy.valueOf(orderFilesByParam.substring(1));
            }
            else {
                orderFilesBy=OrderFilesBy.valueOf(orderFilesByParam);
            }
        }
        
        public Comparator<GpFilePath> build()  {
            return new OutputFileComparator(this);
        }

    }
}
