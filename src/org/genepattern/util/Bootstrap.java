package org.genepattern.util;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

/**
 * Class for booting GenePattern through the Tomcat catalina.sh script on Mac
 *
 * @author Thorin Tabor
 */
public class Bootstrap {

    public static void main(String[] args) {
        File catalinaPath = new File( "bin/catalina.sh" );

        List<String> processList = new ArrayList<String>();
        processList.add(catalinaPath.getPath());
        processList.add("run");

        ProcessBuilder builder = new ProcessBuilder(processList);

        // This is where you set the root folder for the executable to run in
        File fakeDir = new File( "Bootstrap" );
        File fakeDirAbsolute = new File(fakeDir.getAbsolutePath());
        File workingDir = fakeDirAbsolute.getParentFile();
        File parentDir = workingDir.getParentFile();

        builder.directory(parentDir);

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