package org.genepattern.startapp;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tabor on 1/30/15.
 */
public class PropertiesWriter {
    private static String email = "test@test.com";
    private static String daysPurge = "7";
    private static String timePurge = "23:00";
    private static String webserverPort = "8080";
    private static String hsqlPort = "9001";
    private static String perl = "/usr/bin/perl";
    private static String lsid = "8080.tabor.null.null";

    private static String gpVersion = "3.9.2";
    private static String buildTag = "11";
    private static String requirePassword = "false";
    private static String installDir = "..";
    private static String java = "/usr/bin/java";
    private static String r = "/usr/bin/r";
    private static String r25 = "/usr/bin/r_2.5";

    public static void main(String[] args) {
        File fakeDir = new File("Bootstrap");
        File fakeDirAbsolute = new File(fakeDir.getAbsolutePath());
        File workingDir = fakeDirAbsolute.getParentFile();

        File resourcesDir = new File(workingDir, "dist/GenePattern.app/Contents/Resources/GenePatternServer/resources");

        File propertiesFile = new File(resourcesDir, "genepattern.properties");
        try {
            writeProperties(propertiesFile);
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
            else if (line.contains("$HSQL_port$")) {
                line = line.replaceAll("\\$HSQL_port\\$", hsqlPort);
            }
            else if (line.contains("$PERL$")) {
                line = line.replaceAll("\\$PERL\\$", perl);
            }
            else if (line.contains("$LSID_AUTHORITY$")) {
                line = line.replaceAll("\\$LSID_AUTHORITY\\$", lsid);
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
