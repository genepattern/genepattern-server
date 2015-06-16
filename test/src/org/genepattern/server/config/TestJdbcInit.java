/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import junit.framework.Assert;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test cases for connecting to a database.
 * @author pcarr
 *
 */
public class TestJdbcInit {
    String db_username="genepattern";
    String db_password="genepattern";
    String db_name="genepattern";
    String db_host="127.0.0.1";
    String db_port="3306";
    String jdbcUrl = "jdbc:mysql://" + db_host + ":" + db_port + "/" + db_name;

    String hibernateConfigurationFile;
    //String connectionUrl;
    SessionFactory sessionFactory;
 
    @Ignore @Test
    public void jdbcConnection() throws SQLException {
        Connection conn = null;
        Properties connectionProps = new Properties();
        connectionProps.put("user", db_username);
        connectionProps.put("password", db_password);
        try {
            conn = DriverManager.getConnection( jdbcUrl, connectionProps);
        }
        finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    @Ignore @Test
    public void buildSessionFactory() {
        hibernateConfigurationFile="hibernate.mysql.cfg.xml";
        //connectionUrl="jdbc:mysql://192.168.59.103:3306/genepattern";
        AnnotationConfiguration config = new AnnotationConfiguration();
        config.configure(hibernateConfigurationFile);
        config.setProperty("hibernate.connection.url", jdbcUrl);
        sessionFactory=config.buildSessionFactory();
        sessionFactory.getCurrentSession().beginTransaction();
        boolean isConnected=sessionFactory.getCurrentSession().isConnected();
        Assert.assertTrue("isConnected", isConnected);
    }
    
    @Ignore @Test
    public void mysqlInit() {

    }

}
