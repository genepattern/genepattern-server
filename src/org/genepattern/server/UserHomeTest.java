package org.genepattern.server;

import java.util.Date;

import org.genepattern.server.webservice.server.dao.HibernateUtil;

import junit.framework.TestCase;

public class UserHomeTest extends TestCase {
    
    UserHome home = new UserHome();

    @Override
    protected void setUp() throws Exception {
        org.hsqldb.Server.main(new String[] {});
        HibernateUtil.getSession().beginTransaction();
    }

    @Override
    protected void tearDown() throws Exception {
        HibernateUtil.getSession().getTransaction().rollback();
    }

    public void testCRUD() {
  
        Integer id = 9999999;
        
        User user = new User();
        user.setId(id);
        user.setUsername("jim");
        user.setPassword("rob");
        user.setEmail("jrob@dot.com");
        user.setLastLoginDate(new Date());
        user.setLastLoginIP("127.0.0.1");
        user.setTotalLoginCount(10);
        
        home.persist(user);
        HibernateUtil.getSession().flush();
        
        User user2 = home.findById(id);
        assertNotNull(user2);
        assertEquals(user2.getEmail(), user.getEmail());
        
        home.delete(user2);
        HibernateUtil.getSession().flush();
        
        User user3 = home.findById(id);
        assertNull(user3);
        
    }

}
