package org.genepattern.server.webservice.server.dao;

import org.hibernate.dialect.Oracle9Dialect;


public class PlatformOracle9Dialect extends Oracle9Dialect {
    public Class getNativeIdentifierGeneratorClass() {
        return TableNameSequenceGenerator.class;
      }
    }
