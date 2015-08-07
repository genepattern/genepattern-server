/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.database;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SequenceGenerator;
import org.hibernate.type.Type;

public class TableNameSequenceGenerator extends SequenceGenerator {
    
    private static Logger log = Logger.getLogger(TableNameSequenceGenerator.class);

    /**
     * If the parameters do not contain a {@link SequenceGenerator#SEQUENCE}
     * name, we assign one based on the table name.
     */
    public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
        if (params.getProperty(SEQUENCE) == null || params.getProperty(SEQUENCE).length() == 0) {
            String tableName = params.getProperty(PersistentIdentifierGenerator.TABLE);
            if (tableName != null) {
                String seqName = tableName + "_SEQ";
                params.setProperty(SEQUENCE, seqName);
                log.info("Registering seqeunce " + tableName + " -> " + seqName);
            }
        }
        super.configure(type, params, dialect);
    }
}
