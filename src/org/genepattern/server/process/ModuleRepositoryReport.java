/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.process;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;

import org.genepattern.util.LSID;

import com.google.common.collect.TreeMultimap;

/**
 * For debugging the module repository ... connects to the repository and generates a report.
 * @author pcarr
 *
 */
public class ModuleRepositoryReport {
    public static void main(final String[] args) throws Exception {
        final String repoUrlStr;
        if (args.length>0) {
            repoUrlStr=args[0];
        }
        else {
            repoUrlStr="http://software.broadinstitute.org/webservices/gpModuleRepository/";
        }
        
        final URL repoUrl=new URL(repoUrlStr);
        final ModuleRepository mr = new ModuleRepository(repoUrl);
        
        System.out.println("Parsing entries from repoUrl="+repoUrlStr);
        final InstallTask[] installTasks=mr.parse(repoUrlStr);
        System.out.println("Found "+installTasks.length+" entries");
        
        //build up a report
        final Comparator<String> keyComparator=new Comparator<String>() {

            @Override
            public int compare(String arg0, String arg1) {
                if (arg0==null) {
                    if (arg1==null) {
                        return 0;
                    }
                    return -1;
                }
                return arg0.compareTo(arg1);
            }
        };
        final Comparator<InstallTask> valueComparator=new Comparator<InstallTask>() {

            @Override
            public int compare(InstallTask arg0, InstallTask arg1) {
                if (arg0==null) {
                    if (arg1==null) {
                        return 0;
                    }
                    return -1;
                }
                try {
                    LSID lsid0=new LSID( arg0.getLsid() );
                    LSID lsid1=new LSID( arg1.getLsid() );
                    return lsid0.compareTo(lsid1);
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
            
        };
        
        final TreeMultimap<String,InstallTask> map=TreeMultimap.create(keyComparator, valueComparator); 
        for(final InstallTask installTask : installTasks) {
            final String lsidStr=installTask.getLsid();
            try {
                final LSID lsid=new LSID(lsidStr);
                final String baseLsid=lsid.toStringNoVersion();
                map.put(baseLsid, installTask);
            }
            catch (MalformedURLException e) {
                System.err.println("Error initializing lsid for lsidStr="+lsidStr);
            }
        }
        
        //print the report
        System.out.println("num modules: "+map.size());
        System.out.println("num distinct modules: "+map.keySet().size());
        
        final String DELIM="\t";
        for(final String baseLsid : map.keySet()) {
            NavigableSet<InstallTask> items=map.get(baseLsid);
            InstallTask top=items.first();
            List<String> versions=new ArrayList<String>();
            for(final InstallTask item : items) {
                versions.add( item.getLsidVersion() );
            }
            if (top != null) {
                System.out.print(""+top.getName()+DELIM+top.getLsid()+DELIM);
                System.out.print("[ ");
                boolean first=true; 
                for(final String version : versions) {
                    if (first) {
                        first=!first;
                    }
                    else {
                        System.out.print(", ");
                    }
                    System.out.print(version);
                }
                System.out.println(" ]");
            }
        }
    }

}
