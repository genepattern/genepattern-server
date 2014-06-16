package org.genepattern.server.webapp.rest.api.v1.job.search;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.webapp.rest.api.v1.Rel;
import org.genepattern.server.webservice.server.Analysis.JobSortOrder;

import com.google.common.base.Strings;

/**
 * Representation of the search query.
 * Also use this to generate page links.
 * 
 * @author pcarr
 */
public class SearchQuery {
    private static final Logger log = Logger.getLogger(SearchQuery.class);

    public static final int DEFAULT_PAGE_SIZE=20;

    public static final String Q_USER_ID="userId";
    public static final String Q_GROUP_ID="groupId";
    public static final String Q_BATCH_ID="batchId";
    public static final String Q_PAGE="page";
    public static final String Q_PAGE_SIZE="pageSize";
    public static final String Q_ORDER_BY="orderBy";
    public static final String Q_ORDER_FILES_BY="orderFilesBy";

    /**
     * the full URI to the jobs resource, for constructing links to related resources
     */
    final String jobsResourcePath;
    /**
     * the GP User who is making the query
     */
    final String currentUser;
    /**
     * is the current user an admin
     */
    final boolean currentUserIsAdmin;
    /**
     * search for jobs owned by this user, can be '*' to indicate all jobs.
     */
    final String userId;

    final String groupId;
    final String batchId;
    
    final int page;
    final int pageSize;
    
    final String orderBy;
    final String orderFilesBy;
    final JobSortOrder jobSortOrder;
    final boolean ascending;

    private SearchQuery(final Builder in) {
        this.jobsResourcePath=in.jobsResourcePath;
        this.currentUser=in.userContext.getUserId();
        this.currentUserIsAdmin=in.userContext.isAdmin();
        // filters
        this.userId=in.userId;
        this.groupId=in.selectedGroup;
        this.batchId=in.selectedBatchId;
        // pages
        this.page=in.pageNum;
        this.pageSize=in.pageSize;
        // order
        this.orderBy=in.orderBy;
        //Builder parses the 'orderBy' param
        this.jobSortOrder=in.jobSortOrder;
        this.ascending=in.ascending;
        this.orderFilesBy=in.orderFilesBy;
    }
    
    public String getCurrentUser() {
        return currentUser;
    }
    public boolean isCurrentUserAdmin() {
        return currentUserIsAdmin;
    }
    /**
     * For the 'All job results' drop-down, an admin user can see all jobs.
     * A non-admin user can see all of their jobs plus all public jobs.
     * @return
     */
    public boolean isShowAll() {
        return "*".equals(userId);
    }
    public String getGroupId() {
        return groupId;
    }
    public String getBatchId() {
        return batchId;
    }
    public String getUserId() {
        return userId;
    }
    public boolean isBatchFilter() {
        return !Strings.isNullOrEmpty(batchId);
    }
    public boolean isGroupFilter() {
        return !Strings.isNullOrEmpty(groupId);
    }
    public boolean isUserFilter() {
        return !Strings.isNullOrEmpty(userId);
    }

    public JobSortOrder getJobSortOrder() {
        return this.jobSortOrder;
    }
    public boolean isAscending() {
        return ascending;
    }

    public int getPageNum() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    // for the BATCH JOB QUERY, compute maxResults and firstResult from the pageSize and pageNum attributes
    public int getFirstResult() {
        return pageSize * (page-1);
    }

    /**
     * Creates a link to a new page, using all other search criteria.
     * @param page
     * @return
     */
    public PageLink makePageLink(final String name, final int toPage) {
        return makePageLink(null, name, toPage);
    }
    public PageLink makePageLink(final Rel rel, final String name, final int toPage) {
        PageLink pageLink=new PageLink(toPage);
        pageLink.rel=rel;
        pageLink.name=name;
        try {
            final String queryString=getQueryString(toPage);
            if (queryString!=null) {
                pageLink.href=jobsResourcePath+"?"+queryString;
            }
            else {
                pageLink.href=jobsResourcePath;
            }
        }
        catch (UnsupportedEncodingException e) {
            log.error("Error creating link toPage="+toPage, e);
            pageLink.href=jobsResourcePath;
        }
        return pageLink;
    }

    /**
     * Example, <pre>
     *     userId=&groupId=&batchId=&page=&pageSize=
     * </pre>
     * @return the http query string for the search.
     */
    private String getQueryString(int toPage)  throws UnsupportedEncodingException {
        QueryStringBuilder b=new QueryStringBuilder();
        b.param(Q_USER_ID, userId);
        b.param(Q_GROUP_ID, groupId);
        b.param(Q_BATCH_ID, batchId);
        b.param(Q_PAGE, ""+toPage);
        if (orderBy != null) {
            b.param(Q_ORDER_BY, orderBy);
        }
        if (orderFilesBy != null) {
            b.param(Q_ORDER_FILES_BY, orderFilesBy);
        }
        return b.build();
    }

    public static class QueryStringBuilder {

        private List<GpQueryParam> params;

        public QueryStringBuilder param(final String name) throws UnsupportedEncodingException  {
            return param(name, null);
        }
        public QueryStringBuilder param(final String name, final String value) throws UnsupportedEncodingException {
            //skip null values
            if (value==null) {
                return this;
            }
            if (params==null) {
                params=new ArrayList<GpQueryParam>();
            }
            
            params.add(new GpQueryParam(name, value));
            return this;
        }

        public String build() {
            //null means, no query string
            if (params==null || params.size()==0) {
                return null;
            }
            boolean first=true;
            final StringBuffer sb=new StringBuffer();
            for(final GpQueryParam param : params) {
                if (first) {
                    first=false;
                }
                else {
                    sb.append("&");
                }
                sb.append(param.toString());
            }
            return sb.toString();
        }
    }

    public static class Builder {
        private final GpConfig gpConfig;
        private final GpContext userContext;
        private final String jobsResourcePath;
        private String userId=null; // null or not-set means, currentUser
        private String selectedGroup=null;
        private String selectedBatchId=null;
        private String orderBy=null;
        private String orderFilesBy=null;

        private JobSortOrder jobSortOrder=JobSortOrder.JOB_NUMBER;
        private boolean ascending=false;

        private int pageNum=1;
        private int pageSize=DEFAULT_PAGE_SIZE;

        public Builder(final GpConfig gpConfig, final GpContext userContext, final String jobsResourcePath) {
            this.gpConfig=gpConfig;
            this.userContext=userContext;
            this.jobsResourcePath=jobsResourcePath;
        }

        public Builder userId(final String userId) {
            this.userId=userId;
            return this;
        }

        public Builder groupId(final String groupId) {
            this.selectedGroup=groupId;
            return this;
        }

        public Builder batchId(final String batchId) {
            this.selectedBatchId=batchId;
            return this;
        }

        public Builder pageNum(final int pageNum) {
            this.pageNum=pageNum;
            return this;
        }

        public Builder pageSize(final int pageSize) {
            this.pageSize=pageSize;
            return this;
        }
        
        public Builder orderBy(final String orderBy) {
            this.orderBy=orderBy;
            return this;
        }
        public Builder orderFilesBy(final String orderFilesBy) {
            this.orderFilesBy=orderFilesBy;
            return this;
        }

        public SearchQuery build() {
            initSortOrder();
            initPageSize();
            return new SearchQuery(this);
        }

        /**
         * If necessary, initialize the pageSize.
         * @param gpConfig
         * @param userContext
         */
        private void initPageSize() {
            if (this.pageSize>0) {
                return;
            }
            if (this.pageSize==0) {
                log.error("Undefined behavior, pageSize="+pageSize);
                //change to default value
            }
            this.pageSize=this.gpConfig.getGPIntegerProperty(this.userContext, "job.results.per.page", DEFAULT_PAGE_SIZE);
        }
        
        /**
         * Parse the 'orderBy' query parameter, to set the jobSortOrder and ascending flags.
         *     orderBy=jobId | taskName | dateSubmitted | dateCompleted | status
         * With an optional '+' or '-' prefix.
         * @param orderBy
         * @return
         */
        private void initSortOrder() {
            if (orderBy==null) {
                //use default values
                return;
            }

            final String jobSortColumn;
            if (orderBy.startsWith("+") || orderBy.startsWith("-")) {
                jobSortColumn=orderBy.substring(1);
            }
            else {
                jobSortColumn=orderBy;
            }
            if ("jobId".equals(jobSortColumn)) {
                this.jobSortOrder=JobSortOrder.JOB_NUMBER;
            }
            else if ("taskName".equals(jobSortColumn)) {
                this.jobSortOrder=JobSortOrder.MODULE_NAME;
            }
            else if ("dateSubmitted".equals(jobSortColumn)) {
                this.jobSortOrder=JobSortOrder.SUBMITTED_DATE;
            }
            else if ("dateCompleted".equals(jobSortColumn)) {
                this.jobSortOrder=JobSortOrder.COMPLETED_DATE;
            }
            else if ("status".equals(jobSortColumn)) { 
                this.jobSortOrder=JobSortOrder.JOB_STATUS; 
            }
            else {
                log.error("Invalid orderBy="+orderBy);
                //break out of this method, use default values
                return;
            }

            // set the ascending flag here, after we initialize the jobSortOrder
            if (orderBy.startsWith("-")) {
                this.ascending=false;
            }
            else if (orderBy.startsWith("+")) {
                this.ascending=true;
            }
            else {
                this.ascending=true;
            }
        }
    }
}
