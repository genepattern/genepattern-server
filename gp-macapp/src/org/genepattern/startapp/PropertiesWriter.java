package org.genepattern.startapp;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Write the necessary config to the correct files
 * Used to write to genepattern.properties and server.xml
 *
 * @author Thorin Tabor
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
    private String r25 = "/Library/Frameworks/R.framework/Versions/2.5/Resources";               // Prompt user
    private String requirePassword = "false";            // Prompt user

    private String lsid = "";                            // Generate

    private String gpVersion = "";                       // Build parameter
    private String buildTag = "";                        // Build parameter

    /**
     * This method is called by the ant task at build time
     *
     * @param args
     */
    public static void main(String[] args) {
        PropertiesWriter pw = new PropertiesWriter();

        // Get the working directory hack
        File fakeDir = new File("Bootstrap");
        File fakeDirAbsolute = new File(fakeDir.getAbsolutePath());
        File workingDir = fakeDirAbsolute.getParentFile();

        // Get the version and build from args
        if (args.length >= 1) {
            String version = null;
            try {
                version = pw.readGpVersion(workingDir);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            pw.setGpVersion(version);
            pw.setBuildTag(args[0]);
        }
        else {
            // There is an error, return
            return;
        }

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

    /**
     * Reads build.versionNumbers to get the version number
     *
     * @return - the version number
     */
    public String readGpVersion(File workingDir) throws IOException {
        File prop = new File(workingDir.getParent(), "build.versionNumbers");
        if (!prop.exists()) {
            throw new IOException("build.versionNumbers does not exist");
        }
        Properties versionProps = new Properties();
        FileInputStream in = new FileInputStream(prop);
        versionProps.load(in);
        in.close();

        return versionProps.getProperty("genepattern.version");
    }

    /**
     * Used to write necessary options selected by the user
     *
     * @param propFile
     * @throws IOException
     */
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

    /**
     * Used to write the LSID authority at runtime
     *
     * @param propFile
     * @throws IOException
     */
    public void writeInstallTime(File propFile, String workingDirString, String gpHomeDirString) throws IOException {
        List<String> lines = new ArrayList<String>();

        // Read the file and store changes
        BufferedReader in = new BufferedReader(new FileReader(propFile));
        String line = in.readLine();
        while (line != null) {
            if (line.contains("$LSID_AUTHORITY$")) {
                line = line.replaceAll("\\$LSID_AUTHORITY\\$", lsid);
            }
            else if (line.contains("run_r_path=")) {
                line = line.replaceAll("\\.\\.", workingDirString + "/GenePatternServer");
            }
            else if (line.contains("R.suppress.messages.file=")) {
                line = line.replaceAll("\\.\\.", gpHomeDirString);
            }
            else if (line.startsWith("resources=")) {
                line = "resources=" + gpHomeDirString + "/resources";
            }
            else if (line.startsWith("tomcatCommonLib=")) {
                line = "tomcatCommonLib=" + workingDirString + "/GenePatternServer/Tomcat/common/lib";
            }
            else if (line.startsWith("webappDir=")) {
                line = "webappDir=" + workingDirString + "/GenePatternServer/Tomcat/webapps/gp";
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

    /**
     * Used to write correct properties at build time
     *
     * @param propFile
     * @throws IOException
     */
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

            // Comment out problematic lines entirely
            if (line.contains("java.io.tmpdir=")) {
                line = "# " + line;
            }
            else if (line.contains("soap.attachment.dir=")) {
                line = "# " + line;
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
