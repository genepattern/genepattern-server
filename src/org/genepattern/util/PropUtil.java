/*
 *                Omnigene Development Code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for the code is held jointly by Whitehead Institute
 * and the the individual authors.  These should be listed in
 * @author doc comments.
 *
 * For more information on the Omnigene project and its aims
 * visit the www.sourceforge.net site.
 *
 */

package org.genepattern.util;

//import javax.ejb.*;
import java.rmi.RemoteException;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;

import org.genepattern.analysis.OmnigeneException;



/**
 * This utility class is used for save and retrive 
 * properties from JNDI context.
 *
 * @author Rajesh Kuttan
 * @version 1.0
 */

public class PropUtil {

    protected PropUtil(){}
    /**
     * This method used to load property to JNDI context.
     * @throws RemoteException RemoteException
     * @throws OmnigeneException OmnigeneException
     */
    
    public static synchronized void loadOmnigeneProperties() throws RemoteException, OmnigeneException {
        
        try {
            PropertyFactory factory = PropertyFactory.getInstance();
            Properties props = factory.getProperties("omnigene.properties");
            
            
            String initialFactory = props.getProperty("java.naming.factory.initial");
            String providerUrl = props.getProperty("java.naming.provider.url");
            String urlPkg = props.getProperty("java.naming.factory.url.pkgs");
            Properties h = new Properties();
            
            h.put("java.naming.factory.initial", initialFactory);
            h.put("java.naming.provider.url", providerUrl);
            h.put("java.naming.factory.url.pkgs",urlPkg);
            
            // Get a naming context
            InitialContext jndiContext = new InitialContext(h);
            
            try { 
                jndiContext.bind("omnigene.properties",props);
		    System.out.println("Bind...");
            } catch (NameAlreadyBoundException  nameBoundException) { 
		    System.out.println("Rebinding...");
                    jndiContext.rebind("omnigene.properties",props);
            }    
            
            
        } catch (RemoteException remoteEx) {
            throw remoteEx;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new OmnigeneException(ex.toString());
        }
        
    }

    public static synchronized Properties getContextProperty() throws  OmnigeneException {
        
	Properties props = null;
        try {
            PropertyFactory factory = PropertyFactory.getInstance();
            props = factory.getProperties("omnigene.properties");
            
            
            String initialFactory = props.getProperty("java.naming.factory.initial");
            String providerUrl = props.getProperty("java.naming.provider.url");
            String urlPkg = props.getProperty("java.naming.factory.url.pkgs");
            Properties h = new Properties();
            
            h.put("java.naming.factory.initial", initialFactory);
            h.put("java.naming.provider.url", providerUrl);
            h.put("java.naming.factory.url.pkgs",urlPkg);

            // Get a naming context
            InitialContext jndiContext = new InitialContext(h);
            
            props =  (Properties)jndiContext.lookup("omnigene.properties");

       } catch (Exception ex) {
		System.out.println("Error in retriving Property from contaxt");
		ex.printStackTrace();
		throw new OmnigeneException(ex.toString());
       }
       return props;
   }

    public static void main(String args[]) {
        try {
	    PropUtil pu = new PropUtil();
	    pu.loadOmnigeneProperties();
	    Properties props = null;
            props = pu.getContextProperty();	
	    if (props != null){
            	System.out.println("Loaded....");
	    }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        
    }
}
