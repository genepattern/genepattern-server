/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.database;

import org.hibernate.dialect.Oracle9Dialect;


public class PlatformOracle9Dialect extends Oracle9Dialect {
    public Class getNativeIdentifierGeneratorClass() {
        return TableNameSequenceGenerator.class;
      }
    }
