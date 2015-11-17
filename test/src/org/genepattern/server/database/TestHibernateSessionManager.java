/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.database;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.junit.Test;

public class TestHibernateSessionManager {

    @Test
    public void initAnnotatedClasses_hardCoded() {
        Collection<Class<?>> annotatedClasses=HibernateSessionManager.hardCodedAnnotatedClasses();
        assertEquals("annotatedClasses.size()", 18, annotatedClasses.size());
    }

    @Test
    public void initAnnotatedClasses_Reflections() throws IOException, ClassNotFoundException {
        Collection<Class<?>> annotatedClasses=HibernateSessionManager.initAnnotatedClasses_Reflections();
        assertEquals("annotatedClasses.size()", 18, annotatedClasses.size());
    }
    
    @Test
    public void scanForAnnotationsFlag_null_prop() {
        assertEquals(true, HibernateSessionManager.getScanForAnnotationsFlag(null));
    }
    
    @Test
    public void scanForAnnotationsFlag_notSet() {
        Properties props=new Properties();
        assertEquals(true, HibernateSessionManager.getScanForAnnotationsFlag(props));
    }

    @Test
    public void scanForAnnotationsFlag_true() {
        Properties props=new Properties();
        props.setProperty("database.scanForAnnotations", "true");
        assertEquals(true, HibernateSessionManager.getScanForAnnotationsFlag(props));
    }

    @Test
    public void scanForAnnotationsFlag_false() {
        Properties props=new Properties();
        props.setProperty("database.scanForAnnotations", "false");
        assertEquals(false, HibernateSessionManager.getScanForAnnotationsFlag(props));
    }

}
