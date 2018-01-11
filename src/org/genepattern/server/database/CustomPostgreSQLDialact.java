/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
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
