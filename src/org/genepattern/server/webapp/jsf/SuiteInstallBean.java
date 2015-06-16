/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A session scoped bean to keep track of suites installed during the user
 * session.
 * 
 * 
 */
public class SuiteInstallBean {

    /**
     * A map of LSID -> intall status (true for installed, false for not).
     */
    Map<String, SuiteInstallStatus> suitesMap;

    public SuiteInstallBean() {

    }

    /**
     * Add the list of lsids to the suite installation map
     * 
     * @param lsids
     */
    public void setSuites(String[] lsids, String[] names) {
        suitesMap = new HashMap<String, SuiteInstallStatus>();
        for (int i = 0; i < lsids.length; i++) {
            suitesMap.put(lsids[i], new SuiteInstallStatus(lsids[i], names[i]));
        }
    }

    public void setStatus(String lsid, String status) {
        setStatus(lsid, status, null);
    }

    public void setStatus(String lsid, String status, String message) {
        SuiteInstallStatus bean = suitesMap.get(lsid);
        if (bean != null) {
            bean.setStatus(status);
            bean.setMessage(message);
        }
    }

    public List<SuiteInstallStatus> getSuites() {
        if (suitesMap == null) {
            return Collections.EMPTY_LIST;
        }
        List<SuiteInstallStatus> suites = new ArrayList<SuiteInstallStatus>(
                suitesMap.values());
        Collections.sort(suites);
        return suites;
    }

    public String getInstalledSuiteString() {

        JSONArray jsonArray = new JSONArray();
        if (suitesMap != null) {
            for (SuiteInstallStatus suite : suitesMap.values()) {
                try {
                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("lsid", suite.getLsid());
                    jsonObj.put("status", suite.getStatus());
                    jsonObj.put("message", suite.getMessage());
                    jsonArray.put(jsonObj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }

        return jsonArray.toString();
    }

    public static class SuiteInstallStatus implements
            Comparable<SuiteInstallStatus> {
        String lsid;

        String name;

        String status;

        String message;

        public SuiteInstallStatus(String lsid, String name) {
            this.lsid = lsid;
            this.name = name;
        }

        public String getLsid() {
            return lsid;
        }

        public void setLsid(String lsid) {
            this.lsid = lsid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int compareTo(SuiteInstallStatus o) {
            return name.compareToIgnoreCase(o.getName());
        }

    }

}
