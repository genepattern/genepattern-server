/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.myfaces.custom.navmenu.NavigationMenuItem;
import org.genepattern.codegenerator.CodeGeneratorUtil;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class RecentJobsBean extends JobBean {

	private List<MyJobInfo> jobs;

	private static Logger log = Logger.getLogger(RecentJobsBean.class);


	protected JobInfo[] getJobInfos() {
		String userId = UIBeanHelper.getUserId();
		assert userId != null;
		int recentJobsToShow = Integer.parseInt(UserPrefsBean.getProp(
				UserPropKey.RECENT_JOBS_TO_SHOW, "4").getValue());
		LocalAnalysisClient analysisClient = new LocalAnalysisClient(userId);
		try {
			// JobInfo[] temp = analysisClient.getJobs(userId, -1,
			// recentJobsToShow,
			// false); // FIXME uncomment when bug recording jobs is fixed
			return analysisClient.getJobs(null, -1, recentJobsToShow, false);
		} catch (WebServiceException wse) {
			log.error(wse);
			return new JobInfo[0];
		}
	}

}
