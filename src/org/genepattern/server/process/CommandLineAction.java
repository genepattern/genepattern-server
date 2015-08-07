/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;


/**
 * install a set of zipped GenePattern tasks into the local GenePattern server.
 * create a new task database if necessary
 * 
 * @author Jim Lerner
 */

public class CommandLineAction {
    boolean hadToStartDB = false;
    Connection conn = null;
    protected boolean DEBUG = false;
    Properties props = null;

    public void preRun(String[] args) {
        DEBUG = (System.getProperty("DEBUG") != null);
        try {
            String resourcesDir = new File(System.getProperty("resources")).getCanonicalPath();
            if (DEBUG) {
                System.out.println("resourcesDir=" + new File(resourcesDir).getCanonicalPath());
            }
            Properties props = loadProps(resourcesDir);
            if (DEBUG) {
                System.out.println(props);
            }
            System.setProperty("genepattern.properties", resourcesDir);
        }
        catch (Throwable e) {
            System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void postRun(String[] args) throws Exception {
		if (hadToStartDB) {
			System.out.println("stopping database because I started it");
			Connection conn = getConnection(props);
			Statement stmt = conn.createStatement();
			stmt.executeUpdate("SHUTDOWN COMPACT");
			stmt.close();
			//conn.close();
		}
	}

	//Returns Connection from connection pool based on DAO settings
	private Connection getConnection(Properties props)
			throws ClassNotFoundException, SQLException {
		String driver = props.getProperty("DB.driver");
		String url = props.getProperty("DB.url");
		String username = props.getProperty("DB.username");
		String password = props.getProperty("DB.password");
		Class.forName(driver);
		Connection conn = DriverManager.getConnection(url, username, password);
		return conn;
	}

	private Properties loadProps(String propsDir) throws IOException {
		File propFile = new File(propsDir, "genepattern.properties");
		FileInputStream fis = null;
		Properties props = new Properties();
		try {
			fis = new FileInputStream(propFile);
			props.load(fis);
		} catch (IOException ioe) {
			throw new IOException("InstallTasks.loadProps: "
					+ propFile.getCanonicalPath() + " cannot be loaded.  "
					+ ioe.getMessage());
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (IOException ioe) {
			}
		}
		return props;
	}
}

class CLAInstallThread extends Thread {
	Method mainMethod;

	String[] args;

	protected boolean DEBUG = false;

	public CLAInstallThread(String taskName, Method mainMethod, String[] args) {
		super(taskName);
		this.mainMethod = mainMethod;
		this.args = args;

		this.DEBUG = (System.getProperty("DEBUG") != null);
	}

	public void run() {
		try {
			if (DEBUG)
				System.out.println("invoking "
						+ mainMethod.getDeclaringClass().getName() + "."
						+ mainMethod.getName());
			mainMethod.invoke(null, new Object[] { args });
			if (DEBUG)
				System.out.println(getName() + " "
						+ mainMethod.getDeclaringClass().getName() + "."
						+ mainMethod.getName() + " returned from execution");
		} catch (IllegalAccessException iae) {
			System.err
					.println("Can't invoke main in " + getName() + ": " + iae);
		} catch (IllegalArgumentException iae2) {
			System.err.println("Bad args for " + getName() + ": " + iae2);
		} catch (InvocationTargetException ite) {
			System.err.println("InvocationTargetException for " + getName()
					+ ": " + ite.getTargetException());
		} catch (Exception e) {
			System.err.println("Exception for " + getName() + ": " + e);
		}
	}
}

