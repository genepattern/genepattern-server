/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import static org.genepattern.server.webapp.jsf.UIBeanHelper.getUserId;
import static org.genepattern.util.GPConstants.SUITE_NAMESPACE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.LSIDManager;
import org.genepattern.server.util.AuthorizationManagerFactoryImpl;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.server.webservice.server.AdminService;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * @author jrobinso
 * 
 */
public class CreateSuiteBean implements java.io.Serializable {

	private static Logger log = Logger.getLogger(CreateSuiteBean.class);

	private String name;

	private String description;

	private String author;

	private String contact;

	private int accessId = 1; // Public

	private UploadedFile supportFile1;

	private UploadedFile supportFile2;

	private UploadedFile supportFile3;

	private List<ModuleCategory> categories;

	private boolean success = false; // Default value

	private SuiteInfo currentSuite = null;

	public CreateSuiteBean() {
		if (currentSuite == null) {
			String lsid = UIBeanHelper.getRequest().getParameter("lsid");
			if (lsid != null) {
				try {
					currentSuite = (new AdminService()).getSuite(lsid);
				} catch (WebServiceException e) {
					log.error(e);
				}
			}
		}
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public int getAccessId() {
		return (currentSuite == null) ? accessId : currentSuite.getAccessId();
	}

	public void setAccessId(int accessId) {
		this.accessId = accessId;
	}

	public String getAuthor() {
		return (currentSuite == null) ? author : currentSuite.getAuthor();
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getDescription() {
		return (currentSuite == null) ? description : currentSuite.getDescription();
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return (currentSuite == null) ? name : currentSuite.getName();
	}

	public void setName(String name) {
		this.name = name;
	}

	public List getCategoryColumns() {

		List<List> cols = new ArrayList<List>();
		if (categories == null) {
			if (currentSuite != null) {
				categories = (new ModuleHelper()).getSelectedTasksByType(currentSuite.getModuleLsids());
			} else {
				categories = (new ModuleHelper()).getTasksByType();
			}
		}

		// Find the midpoint in the category list.
		int totalCount = 0;
		for (ModuleCategory cat : categories) {
			totalCount += cat.getModuleCount();
		}
		int midpoint = totalCount / 2;

		cols.add(new ArrayList());
		cols.add(new ArrayList());
		int cumulativeCount = 0;
		for (ModuleCategory cat : categories) {
			if (cumulativeCount < midpoint) {
				cols.get(0).add(cat);
			} else {
				cols.get(1).add(cat);
			}
			cumulativeCount += cat.getModuleCount();
		}
		return cols;
	}

	public UploadedFile getSupportFile1() {
		return supportFile1;
	}

	public void setSupportFile1(UploadedFile supportFile1) {
		this.supportFile1 = supportFile1;
	}

	public UploadedFile getSupportFile2() {
		return supportFile2;
	}

	public void setSupportFile2(UploadedFile supportFile2) {
		this.supportFile2 = supportFile2;
	}

	public UploadedFile getSupportFile3() {
		return supportFile3;
	}

	public void setSupportFile3(UploadedFile supportFile3) {
		this.supportFile3 = supportFile3;
	}


	public String save() {
		IAuthorizationManager authManager = new AuthorizationManagerFactoryImpl().getAuthorizationManager();
		if (!authManager.checkPermission("createSuite", UIBeanHelper.getUserId())) {
			UIBeanHelper.setInfoMessage("You don't have the required permissions to perform the requested operation.");
		}
		try {

			if (currentSuite == null) {
				currentSuite = new SuiteInfo();
				currentSuite.setLsid(LSIDManager.getInstance().createNewID(SUITE_NAMESPACE).toString());
			}

			// Save or update database record

			currentSuite.setOwner(getUserId());
			currentSuite.setName(name);
			currentSuite.setDescription(description);
			currentSuite.setAccessId(new Integer(accessId));
			currentSuite.setAuthor(author);
			currentSuite.setContact(contact);

			List<String> selectedLSIDs = new ArrayList<String>();
			for (ModuleCategory cat : categories) {
				for (Module mod : cat.getModules()) {
					if (mod.isSelected()) {
						selectedLSIDs.add(mod.getLsid());
					}
				}
			}
			if (!selectedLSIDs.isEmpty()) {
				currentSuite.setModuleLsids(selectedLSIDs);
			}

			LocalTaskIntegratorClient ti = new LocalTaskIntegratorClient(UIBeanHelper.getUserId());
			ti.saveOrUpdateSuite(currentSuite);

			// Save uploaded files, if any
			String suiteDir = DirectoryManager.getSuiteLibDir(currentSuite.getName(), currentSuite.getLsid(),
					currentSuite.getOwner());
			if (supportFile1 != null) {
				saveUploadedFile(supportFile1, suiteDir);
			}
			if (supportFile2 != null) {
				saveUploadedFile(supportFile2, suiteDir);
			}
			if (supportFile3 != null) {
				saveUploadedFile(supportFile3, suiteDir);
			}

			RunTaskBean homePageBean = (RunTaskBean) UIBeanHelper.getManagedBean("#{runTaskBean}");
			homePageBean.setSplashMessage("Suite " + currentSuite.getName() + " was successfully created.");

			return "home";
		} catch (Exception e) {
			HibernateUtil.rollbackTransaction(); // This shouldn't be
			// neccessary, but just in
			// case
			throw new RuntimeException(e); // @todo -- replace with appropriate
			// GP exception
		}

	}

	private void saveUploadedFile(UploadedFile uploadedFile, String suiteDir) throws FileNotFoundException, IOException {
		String fileName = uploadedFile.getName();
		if (fileName != null) {
			fileName = FilenameUtils.getName(fileName);

		}
		FileOutputStream out = new FileOutputStream(new File(suiteDir, fileName));
		InputStream in = uploadedFile.getInputStream();
		int c;
		while ((c = in.read()) != -1) {
			out.write(c);
		}
		in.close();
		out.close();
	}

	public String clear() {
		return null;
	}

	public List<ModuleCategory> getCategories() {
		return categories;
	}

	public void setCategories(List<ModuleCategory> categories) {
		this.categories = categories;
	}

	public String getContact() {
		return (currentSuite == null) ? contact : currentSuite.getContact();
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

}
