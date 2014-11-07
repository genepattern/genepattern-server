package org.genepattern.server.webservice;

import java.io.File;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.axis.server.AxisServer;
import org.apache.axis.transport.http.AxisServlet;
import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

public class CustomAxisServlet extends AxisServlet {
    private static final long serialVersionUID = 2536640532496660156L;

    protected void initAttachmentsDirectory() {
        Logger log = Logger.getLogger(CustomAxisServlet.class);
        GpContext serverContext=GpContext.getServerContext();
        File soapAttDir=ServerConfigurationFactory.instance().getSoapAttDir(serverContext);
        if (soapAttDir != null && this.axisServer != null) {
            this.axisServer.setOption(AxisServer.PROP_ATTACHMENT_DIR, soapAttDir.toString());
            log.info("Setting "+AxisServer.PROP_ATTACHMENT_DIR+"="+soapAttDir.toString());
        }
        else {
            log.error("Initialization error, "+AxisServer.PROP_ATTACHMENT_DIR+" is not set");
        }
    }

    public void init() throws ServletException {
        super.init(); 
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initAttachmentsDirectory();
    }
}
