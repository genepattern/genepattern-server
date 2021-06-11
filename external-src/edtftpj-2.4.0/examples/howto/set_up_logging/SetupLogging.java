/*
 * 
 * Copyright (C) 2006 Enterprise Distributed Technologies Ltd
 * 
 * www.enterprisedt.com
 */

import com.enterprisedt.util.debug.FileAppender;
import com.enterprisedt.util.debug.Level;
import com.enterprisedt.util.debug.Logger;
import com.enterprisedt.util.debug.StandardOutputAppender;

public class SetupLogging {

    public static void main(String[] args) {

        try {
            // set up logger so that we get some output
            Logger log = Logger.getLogger(SetupLogging.class);
            Logger.setLevel(Level.INFO);

            // log to a file and also to standard output
            Logger.addAppender(new FileAppender("output.log"));
            Logger.addAppender(new StandardOutputAppender());

            // this will not appear unless the level is set to DEBUG
            log.debug("This is a debug message");

            // this will appear with the level set to INFO
            log.info("This is an info message");

            // this will appear with the level set to INFO
            log.error("This is an error message");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
