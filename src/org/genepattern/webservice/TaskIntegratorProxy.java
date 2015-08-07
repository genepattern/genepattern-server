/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.webservice;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.axis.client.Service;
import org.apache.axis.configuration.BasicClientConfig;

/**
 * @author Joshua Gould
 */
public class TaskIntegratorProxy {
    String endpoint = null;

    org.apache.axis.client.Service service = null;

    TaskIntegratorSoapBindingStub stub;

    public TaskIntegratorProxy(String url, String userName, String password) throws WebServiceException {
        this(url, userName, password, true);
    }

    public TaskIntegratorSoapBindingStub getStub() {
        return stub;
    }

    public TaskIntegratorProxy(String url, String userName, String password, boolean maintainSession)
            throws WebServiceException {
        try {
            this.endpoint = ProxyUtil.createEndpoint(url, "/services/TaskIntegrator");
            this.service = new Service(new BasicClientConfig());
            stub = new TaskIntegratorSoapBindingStub(new URL(endpoint), service);
            stub.setUsername(userName);
            stub.setPassword(password);
            stub.setMaintainSession(maintainSession);
        } catch (java.net.MalformedURLException mue) {
            throw new WebServiceException(mue);
        } catch (org.apache.axis.AxisFault af) {
            throw new WebServiceException(af);
        }
    }

    public void installTask(String lsid) throws WebServiceException {
        try {
            stub.installTask(lsid);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public String importZipFromURL(String url, int privacy) throws WebServiceException {
        try {
            return stub.importZipFromURL(url, privacy);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public String installSuite(SuiteInfo suiteInfo) throws org.genepattern.webservice.WebServiceException {
        try {
            return stub.installSuite(suiteInfo);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }

    }

    public String modifySuite(SuiteInfo suiteInfo, File[] files, String[] existingFileNames) throws WebServiceException {
        try {
            DataHandler[] handlers = null;
            String[] uploadedFileNames = null;
            if (files != null) {
                handlers = new DataHandler[files.length];
                uploadedFileNames = new String[files.length];
                for (int i = 0; i < handlers.length; i++) {
                    handlers[i] = new DataHandler(new FileDataSource(files[i]));
                    uploadedFileNames[i] = files[i].getName();
                }

            }
            List fileNames = new ArrayList();
            if (uploadedFileNames != null) {
                fileNames.addAll(Arrays.asList(uploadedFileNames));
            }
            if (existingFileNames != null) {
                fileNames.addAll(Arrays.asList(existingFileNames));
            }

            return stub.modifySuite(suiteInfo.getAccessId(), suiteInfo.getLsid(), suiteInfo.getName(), suiteInfo
                    .getDescription(), suiteInfo.getAuthor(), suiteInfo.getOwner(), suiteInfo.getModuleLsids(),
                    handlers, (String[]) fileNames.toArray(new String[0]));
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public String importZip(File zipFile, int privacy) throws WebServiceException {
        try {
            return stub.importZip(new DataHandler(new FileDataSource(zipFile)), privacy);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public void exportToZip(String taskName, File destinationFile) throws WebServiceException {
        try {
            DataHandler dh = stub.exportToZip(taskName);
            copy(dh, destinationFile);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public void exportToZip(String taskName, boolean recursive, File destinationFile) throws WebServiceException {
        try {
            DataHandler dh = stub.exportToZip(taskName, recursive);
            copy(dh, destinationFile);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    /**
     * 
     * @param accessId
     *            one of GPConstants.ACCESS_PUBLIC or GPConstants.ACCESS_PRIVATE
     * @param taskName
     *            The task name
     * @param description
     *            The task description
     * @param parameterInfoArray
     *            The parameter info array
     * @param taskAttributes
     *            Task info attributes
     * @param files
     *            array of files to upload
     * @param existingFileNames
     *            array of file names to copy from existing task or
     *            <tt>null</tt>
     * @return The new LSID
     * @throws WebServiceException
     */
    public String modifyTask(int accessId, String taskName, String description, ParameterInfo[] parameterInfoArray,
            java.util.HashMap taskAttributes, File[] files, String[] existingFileNames) throws WebServiceException {
        try {
            String[] uploadedFileNames = null;
            DataHandler[] dataHandlers = null;
            if (files != null) {
                dataHandlers = new DataHandler[files.length];
                uploadedFileNames = new String[files.length];
                for (int i = 0; i < files.length; i++) {
                    dataHandlers[i] = new DataHandler(new FileDataSource(files[i]));
                    uploadedFileNames[i] = files[i].getName();
                }
            }
            List fileNames = new ArrayList();
            if (uploadedFileNames != null) {
                fileNames.addAll(Arrays.asList(uploadedFileNames));
            }
            if (existingFileNames != null) {
                fileNames.addAll(Arrays.asList(existingFileNames));
            }
            return stub.modifyTask(accessId, taskName, description, parameterInfoArray, taskAttributes, dataHandlers,
                    (String[]) fileNames.toArray(new String[0]));
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public String deleteFiles(String lsid, String[] fileNames) throws WebServiceException {
        try {
            return stub.deleteFiles(lsid, fileNames);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public void deleteTask(String lsid) throws WebServiceException {
        try {
            stub.deleteTask(lsid);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public void deleteSuite(String lsid) throws WebServiceException {
        try {
            stub.delete(lsid);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public void getSupportFiles(String lsid, String[] fileNames, File destinationDirectory) throws WebServiceException {
        try {
            DataHandler[] dataHandlers = stub.getSupportFiles(lsid, fileNames);
            if (!destinationDirectory.exists()) {
                destinationDirectory.mkdirs();
            }
            for (int i = 0, length = dataHandlers.length; i < length; i++) {
                File destinationFile = new File(destinationDirectory, fileNames[i]);
                copy(dataHandlers[i], destinationFile);
            }
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    private void copy(DataHandler dh, File destinationFile) throws WebServiceException {
        File axisFile = new File(dh.getName());
        axisFile.renameTo(destinationFile);
        /*
         * java.io.FileOutputStream fos = null; try { fos = new
         * java.io.FileOutputStream(destinationFile); dh.writeTo(fos); }
         * catch(java.io.IOException e) { throw new WebServiceException(e); }
         * finally { try { if(fos != null) { fos.close(); } }
         * catch(java.io.IOException x) { } }
         */
    }

    public String cloneTask(String lsid, String clonedTaskName) throws WebServiceException {
        try {
            return stub.cloneTask(lsid, clonedTaskName);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public long[] getLastModificationTimes(String lsid, String[] fileNames) throws WebServiceException {
        try {
            return stub.getLastModificationTimes(lsid, fileNames);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public String[] getSupportFileNames(String lsid) throws WebServiceException {
        try {
            return stub.getSupportFileNames(lsid);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public String[] getDocFileNames(String lsid) throws WebServiceException {
        try {
            return stub.getDocFileNames(lsid);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

    public void exportSuiteToZip(String lsid, File destination) throws WebServiceException {
        try {
            DataHandler dh = stub.exportSuiteToZip(lsid);
            copy(dh, destination);
        } catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }
    
    public int getPermittedAccessId(int access_id) throws WebServiceException {
        try {
            return stub.getPermittedAccessId(access_id);
        }
        catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }
    
    public boolean checkPermission(String permission) throws WebServiceException {
        try {
            return stub.checkPermission(permission);
        }
        catch (RemoteException re) {
            throw new WebServiceException(re);
        }
    }

}
