/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.startapp;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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

    protected static void copyDirectory(final File from, final File to) throws IOException {
        //FileUtils.copyDirectory(from, to);
        copyDir_sh(from,to);
    }

    protected static void copyDirectory_apache_commons_io(final File from, final File to) throws IOException {
        FileUtils.copyDirectory(from, to);
    }

    // copy directory as shell exec
    protected static void copyDir_sh(final File from, final File to) throws IOException {
        final String[] cmd={"cp", "-Rp", from.getAbsolutePath(), to.getAbsolutePath()};
        Runtime.getRuntime().exec(cmd);
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

    /**
     * Run the shell script to launch GenePattern
     * Used after the config app has saved its data
     *
     * @param workingDirectory
     */
    private static void runShellScript(String workingDirectory) {
        File workingDir = new File(workingDirectory);

        File catalinaPath = new File(workingDir.getParent(), "MacOS/GenePattern");

        List<String> processList = new ArrayList<String>();
        processList.add(catalinaPath.getPath());

        ProcessBuilder builder = new ProcessBuilder(processList);

        // This is where you set the root folder for the executable to run in
        builder.directory(workingDir);

        builder.redirectErrorStream(true);
        Process process = null;
        try {
            process = builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Scanner s = new Scanner(process.getInputStream());
        StringBuilder text = new StringBuilder();
        while (s.hasNextLine()) {
            text.append(s.nextLine());
            text.append("\n");
        }
        s.close();

        int result = 0;
        try {
            result = process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.print( "Process exited with result " + result + " and output " + text );
    }
}
