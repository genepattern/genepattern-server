/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.database;

import org.hibernate.dialect.PostgreSQLDialect;

/**
 * Customize the PostgreSQLDialect by using the same exact TableNameSequenceGenerator
 * that we use for Oracle.
 * 
 * @author pcarr
 *
 */
public class CustomPostgreSQLDialact extends PostgreSQLDialect {
    public Class<TableNameSequenceGenerator> getNativeIdentifierGeneratorClass() {
        return TableNameSequenceGenerator.class;
      }

}
