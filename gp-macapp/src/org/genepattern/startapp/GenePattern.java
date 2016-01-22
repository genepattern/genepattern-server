/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.startapp;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Class charged with bootstrapping GenePattern in the Mac app
 *
 * @author Thorin Tabor
 */
public class GenePattern {

    /**
     * Main method for the executable bootstrap.jar
     *
     * @param args
     */
    public static void main(String[] args) {
        // Double check args
        if (args.length < 1) {
            // fail
            System.err.println("Error: expecting first arg to be a path to the working directory");
            System.exit(1);
        }

        // Pass in the working directory
        final File workingDir = getWorkingDirectory(args[0]);

        // Generate LSID
        String lsid = GenerateLsid.lsid();

        PropertiesWriter pw = new PropertiesWriter();
        pw.setLsid(lsid);

        // Lazily create the GP Home directory
        try {
            lazilyCreateGPHome(workingDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final String user = System.getProperty("user.name");
        final File resourcesDir = new File("/Users/" + user + "/.genepattern/resources");
        final File propFile = new File(resourcesDir, "genepattern.properties");
        final File gpHome = new File(resourcesDir.getParent());
        try {
            pw.writeInstallTime(propFile, args[0], gpHome.getAbsolutePath());
        } 
        catch (IOException e) {
            e.printStackTrace();
        }

        // Prompt the user for config
        ConfigApp.main(new String[] {args[0]});
    }

    /**
     * Copy directory with 'cp -Rp' command in a new OS Process, using the ProcessBuilder class.
     * @param from, the source directory
     * @param to, the destination directory
     * @return true on success
     */
    protected static boolean copyDirectory(final File from, final File to) { 
        final List<String> cmd=Arrays.asList("cp", "-Rp", from.getAbsolutePath(), to.getAbsolutePath());
        final ProcessBuilder pb=new ProcessBuilder()
            .command( cmd );
        pb.inheritIO();
        Process p=null;
        try {
            p=pb.start();
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try {
            int exitCode=p.waitFor();
            if (exitCode==0) {
                return true;
            }
            System.err.println("cmd="+cmd);
            System.err.println("failed with exitCode="+exitCode);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Lazily sets up the GP Home directory if needed
     *
     * @return - If it had to be lazily set up
     */
    public static boolean lazilyCreateGPHome(final File workingDir) throws IOException {
        // Get reference to internal GPServer
        File gpServer = new File(workingDir.getParent(), "Resources/GenePatternServer");

        // Get user and GP Home
        String user = System.getProperty("user.name");
        File gpHome = new File("/Users/" + user + "/.genepattern");

        boolean createdDirectory = false;

        // Lazily check for GP Home
        if (!gpHome.exists()) {
            gpHome.mkdir();
            createdDirectory = true;
        }

        // Lazily create subdirectories
        File jobResults = new File(gpHome, "jobResults");
        if (!jobResults.exists()) {
            File iJobResults = new File(gpServer, "jobResults");
            copyDirectory(iJobResults, jobResults);
            createdDirectory = true;
        }

        File logs = new File(gpHome, "logs");
        if (!logs.exists()) {
            File iLogs = new File(gpServer, "logs");
            copyDirectory(iLogs, logs);
            createdDirectory = true;
        }

        File patches = new File(gpHome, "patches");
        if (!patches.exists()) {
            File iPatches = new File(gpServer, "patches");
            copyDirectory(iPatches, patches);
            createdDirectory = true;
        }

        File resources = new File(gpHome, "resources");
        if (!resources.exists()) {
            File iResources = new File(gpServer, "resources");
            copyDirectory(iResources, resources);
            createdDirectory = true;
        }

        File taskLib = new File(gpHome, "taskLib");
        if (!taskLib.exists()) {
            File iTaskLib = new File(gpServer, "taskLib");
            copyDirectory(iTaskLib, taskLib);
            createdDirectory = true;
        }

        File temp = new File(gpHome, "temp");
        if (!temp.exists()) {
            File iTemp = new File(gpServer, "temp");
            copyDirectory(iTemp, temp);
            createdDirectory = true;
        }

        File users = new File(gpHome, "users");
        if (!users.exists()) {
            File iUsers = new File(gpServer, "users");
            copyDirectory(iUsers, users);
            createdDirectory = true;
        }

        return createdDirectory;
    }

    public static File getWorkingDirectory(String workingDirString) {
        return new File(workingDirString);
    }

}
