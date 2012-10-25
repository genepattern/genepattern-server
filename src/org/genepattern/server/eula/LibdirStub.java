package org.genepattern.server.eula;

import java.io.File;

import org.genepattern.server.eula.InitException;

public class LibdirStub implements LibdirStrategy {
    private File libdir=new File("."); //default to current working directory
    public LibdirStub(final File libdir) {
        this.libdir=libdir;
    }

    //@Override
    public File getLibdir(final String moduleLsid) throws InitException {
        return libdir;
    }
}
