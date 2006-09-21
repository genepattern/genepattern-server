package org.genepattern.server.util;

import org.hibernate.dialect.Oracle9Dialect;


public class PlatformOracle9Dialect extends Oracle9Dialect {
    public Class getNativeIdentifierGeneratorClass() {
        return TableNameSequenceGenerator.class;
      }
    }
