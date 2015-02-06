package org.genepattern.startapp;

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
public class Bootstrap {

    /**
     * Main method for the executable bootstrap.jar
     *
     * @param args
     */
    public static void main(String[] args) {
        // Pass in the working directory
        File workingDir = getWorkingDirectory(args[0]);

        // Generate LSID
        String lsid = GenerateLsid.lsid();

        PropertiesWriter pw = new PropertiesWriter();
        pw.setLsid(lsid);

        File resourcesDir = new File(workingDir.getParent(), "Resources/GenePatternServer/resources");
        File propFile = new File(resourcesDir, "genepattern.properties");
        try {
            pw.writeInstallTime(propFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Prompt the user for config
        ConfigApp.main(new String[] {args[0]});

        // Create the setup flag
        File resources = new File(workingDir.getParent(), "Resources");
        File readyFlag = new File(resources, "ready");
        try {
            readyFlag.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    public static void runShellScript(String workingDirectory) {
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
