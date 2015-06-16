/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.database;

import org.hibernate.dialect.Oracle9Dialect;


public class PlatformOracle9Dialect extends Oracle9Dialect {
    public Class getNativeIdentifierGeneratorClass() {
        return TableNameSequenceGenerator.class;
      }
    }
