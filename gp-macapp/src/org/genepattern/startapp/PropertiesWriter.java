package org.genepattern.startapp;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tabor on 1/30/15.
 */
public class PropertiesWriter {
    private static String webserverPort = "8080";               // Assumed
    private static String shutdownPort = "8005";                // Assumed
    private static String hsqlPort = "9001";                    // Assumed
    private static String hostAddress = "";                     // Assumed
    private static String installDir = "..";                    // Assumed

    private static String email = "";                           // Prompt user
    private static String daysPurge = "7";                      // Prompt user
    private static String timePurge = "23:00";                  // Prompt user
    private static String perl = "/usr/bin/perl";               // Prompt user
    private static String java = "/usr";                        // Prompt user
    private static String r = "/usr/bin/r";                     // Prompt user
    private static String r25 = "/usr/bin/r_2.5";               // Prompt user
    private static String requirePassword = "false";            // Prompt user

    private static String lsid = "8080.tabor.null.null";        // Generate

    private static String gpVersion = "";                       // Build parameter
    private static String buildTag = "";                        // Build parameter

    public static void main(String[] args) {
        // Get the version and build from args
        if (args.length >= 2) {
            gpVersion = args[0];
            buildTag = args[1];
        }

        // Generate the LSID Authority
        lsid = GenerateLsid.lsid();

        // Get the working directory hack
        File fakeDir = new File("Bootstrap");
        File fakeDirAbsolute = new File(fakeDir.getAbsolutePath());
        File workingDir = fakeDirAbsolute.getParentFile();

        // Get the genepattern.properties file
        File resourcesDir = new File(workingDir, "dist/GenePattern.app/Contents/Resources/GenePatternServer/resources");
        File propertiesFile = new File(resourcesDir, "genepattern.properties");

        // Get the Tomcat server.xml file
        File tomcatConf = new File(workingDir, "dist/GenePattern.app/Contents/Resources/GenePatternServer/Tomcat/conf");
        File serverXmlFile = new File(tomcatConf, "server.xml");

        try {
            writeProperties(propertiesFile);
            writeProperties(serverXmlFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeProperties(File propFile) throws IOException {
        List<String> lines = new ArrayList<String>();

        // Read the file and store changes
        BufferedReader in = new BufferedReader(new FileReader(propFile));
        String line = in.readLine();
        while (line != null) {
            if (line.contains("$webmaster$")) {
                line = line.replaceAll("\\$webmaster\\$", email);
            }
            else if (line.contains("$purgeJobsAfter$")) {
                line = line.replaceAll("\\$purgeJobsAfter\\$", daysPurge);
            }
            else if (line.contains("$purgeTime$")) {
                line = line.replaceAll("\\$purgeTime\\$", timePurge);
            }
            else if (line.contains("$GENEPATTERN_PORT$")) {
                line = line.replaceAll("\\$GENEPATTERN_PORT\\$", webserverPort);
            }
            else if (line.contains("$GENEPATTERN_SHUTDOWN_PORT$")) {
                line = line.replaceAll("\\$GENEPATTERN_SHUTDOWN_PORT\\$", shutdownPort);
            }
            else if (line.contains("$HSQL_port$")) {
                line = line.replaceAll("\\$HSQL_port\\$", hsqlPort);
            }
            else if (line.contains("$PERL$")) {
                line = line.replaceAll("\\$PERL\\$", perl);
            }
            else if (line.contains("$LSID_AUTHORITY$")) {
                line = line.replaceAll("\\$LSID_AUTHORITY\\$", lsid);
            }
            else if (line.contains("$HOST_ADDRESS$")) {
                line = line.replaceAll("\\$HOST_ADDRESS\\$", hostAddress);
            }
            else if (line.contains("$GENEPATTERN_VERSION$")) {
                line = line.replaceAll("\\$GENEPATTERN_VERSION\\$", gpVersion);
            }
            else if (line.contains("$buildtag$")) {
                line = line.replaceAll("\\$buildtag\\$", buildTag);
            }
            else if (line.contains("$require.password$")) {
                line = line.replaceAll("\\$require.password\\$", requirePassword);
            }
            else if (line.contains("$USER_INSTALL_DIR$")) {
                line = line.replaceAll("\\$USER_INSTALL_DIR\\$", installDir);
            }
            else if (line.contains("$JAVA$")) {
                line = line.replaceAll("\\$JAVA\\$", java);
            }
            else if (line.contains("$R_HOME$")) {
                line = line.replaceAll("\\$R_HOME\\$", r);
            }
            else if (line.contains("$R25bin$")) {
                line = line.replaceAll("\\$R25bin\\$", r25);
            }

            lines.add(line);
            line = in.readLine();
        }
        in.close();

        // Write changes back to file
        PrintWriter out = new PrintWriter(propFile);
        for (String l : lines)
            out.println(l);
        out.close();
    }
}
