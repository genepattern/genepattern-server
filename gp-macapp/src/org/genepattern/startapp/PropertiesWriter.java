package org.genepattern.startapp;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tabor on 1/30/15.
 */
public class PropertiesWriter {
    private String webserverPort = "8080";               // Assumed
    private String shutdownPort = "8005";                // Assumed
    private String hsqlPort = "9001";                    // Assumed
    private String hostAddress = "";                     // Assumed
    private String installDir = "..";                    // Assumed

    private String email = "";                           // Prompt user
    private String daysPurge = "7";                      // Prompt user
    private String timePurge = "23:00";                  // Prompt user
    private String perl = "/usr/bin/perl";               // Prompt user
    private String java = "/usr";                        // Prompt user
    private String r = "/usr/bin/r";                     // Prompt user
    private String r25 = "/usr/bin/r_2.5";               // Prompt user
    private String requirePassword = "false";            // Prompt user

    private String lsid = "";                            // Generate

    private String gpVersion = "";                       // Build parameter
    private String buildTag = "";                        // Build parameter

    public static void main(String[] args) {
        PropertiesWriter pw = new PropertiesWriter();

        // Get the version and build from args
        if (args.length >= 2) {
            pw.setGpVersion(args[0]);
            pw.setBuildTag(args[1]);
        }

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
            pw.writeBuildTime(propertiesFile);
            pw.writeBuildTime(serverXmlFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeUserTime(File propFile) throws IOException {
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
            else if (line.contains("$PERL$")) {
                line = line.replaceAll("\\$PERL\\$", perl);
            }
            else if (line.contains("$require.password$")) {
                line = line.replaceAll("\\$require.password\\$", requirePassword);
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

    public void writeInstallTime(File propFile) throws IOException {
        List<String> lines = new ArrayList<String>();

        // Read the file and store changes
        BufferedReader in = new BufferedReader(new FileReader(propFile));
        String line = in.readLine();
        while (line != null) {
            if (line.contains("$LSID_AUTHORITY$")) {
                line = line.replaceAll("\\$LSID_AUTHORITY\\$", lsid);
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

    public void writeBuildTime(File propFile) throws IOException {
        List<String> lines = new ArrayList<String>();

        // Read the file and store changes
        BufferedReader in = new BufferedReader(new FileReader(propFile));
        String line = in.readLine();
        while (line != null) {
            if (line.contains("$GENEPATTERN_PORT$")) {
                line = line.replaceAll("\\$GENEPATTERN_PORT\\$", webserverPort);
            }
            else if (line.contains("$GENEPATTERN_SHUTDOWN_PORT$")) {
                line = line.replaceAll("\\$GENEPATTERN_SHUTDOWN_PORT\\$", shutdownPort);
            }
            else if (line.contains("$HSQL_port$")) {
                line = line.replaceAll("\\$HSQL_port\\$", hsqlPort);
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
            else if (line.contains("$USER_INSTALL_DIR$")) {
                line = line.replaceAll("\\$USER_INSTALL_DIR\\$", installDir);
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

    public String getWebserverPort() {
        return webserverPort;
    }

    public void setWebserverPort(String webserverPort) {
        this.webserverPort = webserverPort;
    }

    public String getShutdownPort() {
        return shutdownPort;
    }

    public void setShutdownPort(String shutdownPort) {
        this.shutdownPort = shutdownPort;
    }

    public String getHsqlPort() {
        return hsqlPort;
    }

    public void setHsqlPort(String hsqlPort) {
        this.hsqlPort = hsqlPort;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public String getInstallDir() {
        return installDir;
    }

    public void setInstallDir(String installDir) {
        this.installDir = installDir;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDaysPurge() {
        return daysPurge;
    }

    public void setDaysPurge(String daysPurge) {
        this.daysPurge = daysPurge;
    }

    public String getTimePurge() {
        return timePurge;
    }

    public void setTimePurge(String timePurge) {
        this.timePurge = timePurge;
    }

    public String getPerl() {
        return perl;
    }

    public void setPerl(String perl) {
        this.perl = perl;
    }

    public String getJava() {
        return java;
    }

    public void setJava(String java) {
        this.java = java;
    }

    public String getR() {
        return r;
    }

    public void setR(String r) {
        this.r = r;
    }

    public String getR25() {
        return r25;
    }

    public void setR25(String r25) {
        this.r25 = r25;
    }

    public String getRequirePassword() {
        return requirePassword;
    }

    public void setRequirePassword(String requirePassword) {
        this.requirePassword = requirePassword;
    }

    public String getLsid() {
        return lsid;
    }

    public void setLsid(String lsid) {
        this.lsid = lsid;
    }

    public String getGpVersion() {
        return gpVersion;
    }

    public void setGpVersion(String gpVersion) {
        this.gpVersion = gpVersion;
    }

    public String getBuildTag() {
        return buildTag;
    }

    public void setBuildTag(String buildTag) {
        this.buildTag = buildTag;
    }
}
