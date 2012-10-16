package org.genepattern.server.eula;

import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;

public class EulaManager {
    static public IEulaManager instance(Context context) {
        if (!isEulaEnabled(context)) {
            return Singleton.NO_OP;
        }
        return Singleton.INSTANCE;
    }
    
    private static boolean isEulaEnabled(Context context) {
        if (context==null) {
            return true;
        }
        boolean isEnabled=ServerConfiguration.instance().getGPBooleanProperty(context, EulaManager.class.getName()+".enabled", true);
        return isEnabled;
    }

    static private class Singleton {
        private static final IEulaManager INSTANCE = new EulaManagerImpl();
        private static final IEulaManager NO_OP = new EulaManagerNoOp();
    }
}
