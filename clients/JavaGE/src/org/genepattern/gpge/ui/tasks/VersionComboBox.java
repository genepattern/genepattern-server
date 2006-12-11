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

package org.genepattern.gpge.ui.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.maindisplay.GPGE;
import org.genepattern.util.LSID;
import org.genepattern.webservice.AdminProxy;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.WebServiceException;

/**
 * A combo box that allows the user to select a version for a task
 * 
 * @author jgould
 * 
 */
public class VersionComboBox extends JComboBox {

    public VersionComboBox(String lsidString, final int type) {
        this(lsidString, type, false);

    }

    /**
     * @param lsidString
     *            the lsid
     * @param the
     *            type of event to fire
     * @param isSuite
     *            whether the lsid refers to a suite
     */
    public VersionComboBox(String lsidString, final int type,
            final boolean isSuite) {
        try {
            final LSID lsid = new LSID(lsidString);

            final String lsidNoVersion = lsid.toStringNoVersion();
            List versions = null;
            if (isSuite) {
                AnalysisServiceManager asm = AnalysisServiceManager
                        .getInstance();

                try {
                    Map suiteLsidToVersions = new AdminProxy(asm.getServer(),
                            asm.getUsername(), asm.getPassword(), false)
                            .getSuiteLsidToVersionsMap();
                    versions = (List) suiteLsidToVersions.get(lsidNoVersion);
                } catch (WebServiceException e) {
                    e.printStackTrace();
                }
            } else {
                versions = (List) AnalysisServiceManager.getInstance()
                        .getLSIDToVersionsMap().get(lsidNoVersion);
            }

            if (versions != null) {
                String version = lsid.getVersion();
                for (int i = 0; i < versions.size(); i++) {
                    String s = (String) versions.get(i);
                    addItem(s);
                }
                setSelectedItem(version);
            }

            if (getItemCount() > 1) {

                if (!GPGE.RUNNING_ON_MAC) {
                    this.setBackground(java.awt.Color.white);
                }
                this.setSelectedItem(lsid.getVersion());

                this.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        String selectedItem = (String) ((JComboBox) e
                                .getSource()).getSelectedItem();
                        if (selectedItem.equals("")) {
                            return;
                        }

                        String selectedLSID = lsidNoVersion + ":"
                                + selectedItem;
                        Object objectToDisplay = null;
                        if (isSuite) {
                            try {
                                AnalysisServiceManager asm = AnalysisServiceManager
                                        .getInstance();
                                objectToDisplay = new AdminProxy(asm
                                        .getServer(), asm.getUsername(), asm
                                        .getPassword(), false)
                                        .getSuite(selectedLSID);
                                if (objectToDisplay == null) {
                                    GenePattern
                                            .showMessageDialog("The suite was not found.");
                                    return;
                                }
                            } catch (WebServiceException e2) {
                                e2.printStackTrace();
                            }

                        } else {
                            objectToDisplay = AnalysisServiceManager
                                    .getInstance().getAnalysisService(
                                            selectedLSID);
                            if (objectToDisplay == null) {
                                GenePattern
                                        .showMessageDialog("The task was not found.");
                                return;
                            }
                        }

                        MessageManager
                                .notifyListeners(new ChangeViewMessageRequest(
                                        this, type, objectToDisplay));

                    }
                });
            }
        } catch (MalformedURLException mfe) {
            mfe.printStackTrace();
        }
    }
}
