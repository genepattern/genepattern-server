package org.genepattern.server.eula;

import java.io.File;

import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.eula.InitException;
import org.genepattern.webservice.TaskInfo;

public class EulaManager {
    static public IEulaManager instance(final GpContext context) {
        if (!isEulaEnabled(context)) {
            return Singleton.NO_OP;
        }
        return Singleton.INSTANCE;
    }
    
    private static boolean isEulaEnabled(final GpContext context) {
        if (context==null) {
            return true;
        }
        boolean isEnabled=ServerConfigurationFactory.instance().getGPBooleanProperty(context, EulaManager.class.getName()+".enabled", true);
        return isEnabled;
    }

    static private class Singleton {
        private static final IEulaManager INSTANCE = new EulaManagerImpl();
        private static final IEulaManager NO_OP = new EulaManagerNoOp();
    }
    
    /**
     * Helper method for initializing a EulaInfo for the given taskInfo and licenseFile.
     * 
     * @param taskInfo, must be non-null and have a valid lsid
     * @param licenseFile, must be readable from the current working directory. This same exact path is used when reading the content.
     * @return
     * @throws EulaInitException if the taskInfo is null, or has an invalid LSID
     */
    final static public EulaInfo initEulaInfo(final TaskInfo taskInfo, final File licenseFile) throws InitException {
        if (taskInfo==null) {
            throw new InitException("taskInfo==null");
        }
        if (licenseFile==null) {
            throw new InitException("licenseFile==null");
        }
        EulaInfo eula = new EulaInfo();
        eula.setModuleLsid(taskInfo.getLsid());
        eula.setModuleName(taskInfo.getName());
        eula.setLicenseFile(licenseFile);
        return eula;
    }
}
