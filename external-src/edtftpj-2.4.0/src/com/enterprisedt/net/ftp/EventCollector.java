/**
 * 
 *  Copyright (C) 2010 Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  Bug fixes, suggestions and comments should be should posted on 
 *  http://www.enterprisedt.com/forums/index.php
 *
 *  Change Log:
 *
 *    $Log: EventCollector.java,v $
 *    Revision 1.1  2010-10-05 01:32:59  hans
 *    Logs EventListener events to an internal buffer.
 *
 */
package com.enterprisedt.net.ftp;

/**
 *  Logs events in an internal buffer.
 *
 *  @author      Hans Andersen
 *  @version     $Revision: 1.1 $
 */
public class EventCollector implements EventListener {
	
	/**
	 * Should connection identifiers be logged?
	 */
	private boolean logConnectionIdentifiers = false;

	/**
	 * Should commands and replies be logged?
	 */
	private boolean logCommands = true;
	
	/**
	 * Should transfer start and complete events be logged?
	 */
	private boolean logTransferStartComplete = true;
	
	/**
	 * Should transfer progress be logged?
	 */
	private boolean logTransferProgress = false;
	
	/**
     * Log of messages
     */
    private StringBuffer log = new StringBuffer();
    
	
    /**
     * Are connection identifiers being logged?
     * Default is false.
     * @return true if connection identifiers are being logged.
     */
	public boolean isLogConnectionIdentifiers() {
		return logConnectionIdentifiers;
	}

	/**
	 * Should connection identifiers be logged?
     * Default is false.
	 * @param logCommands true to enable logging of connection identifiers.
	 */
	public void setLogConnectionIdentifiers(boolean logConnectionIdentifiers) {
		this.logConnectionIdentifiers = logConnectionIdentifiers;
	}
 
    /**
     * Are commands and replies being logged?
     * Default is true.
     * @return true if commands and replies are being logged.
     */
	public boolean isLogCommands() {
		return logCommands;
	}

	/**
	 * Should commands and replies be logged?
     * Default is true.
	 * @param logCommands true to enable logging of commands and replies.
	 */
	public void setLogCommands(boolean logCommands) {
		this.logCommands = logCommands;
	}

	/**
	 * Are transfer start and complete events being logged?
     * Default is true.
	 * @return true if transfer start and complete events are being logged.
	 */
	public boolean isLogTransferStartComplete() {
		return logTransferStartComplete;
	}

	/**
	 * Should transfer start and complete events be logged?
     * Default is true.
	 * @param logTransferStartComplete true if transfer start and complete events are to be logged.
	 */
	public void setLogTransferStartComplete(boolean logTransferStartComplete) {
		this.logTransferStartComplete = logTransferStartComplete;
	}

	/**
	 * Is transfer progress being logged?
     * Default is false.
	 * @return true if transfer progress being logged.
	 */
	public boolean isLogTransferProgress() {
		return logTransferProgress;
	}

	/**
	 * Should transfer progress be logged?
     * Default is false.
	 * @param logTransferProgress true if transfer progress is to be logged.
	 */
	public void setLogTransferProgress(boolean logTransferProgress) {
		this.logTransferProgress = logTransferProgress;
	}

    /**
     * Get the log of messages
     * 
     * @return  message log as a string
     */
    public String getLog() {
        return log.toString();
    }
    
    /**
     * Clear the log of all messages
     */
    public void clearLog() {
        log = new StringBuffer();
    }

    /**
     * Log an FTP command being sent to the server. Not used for SFTP.
     * 
     * @param connID Identifier of FTP connection
     * @param cmd   command string
     */
	public void commandSent(String connId, String cmd) {
		if (logCommands) {
			if (logConnectionIdentifiers)
				log.append('[').append(connId).append("] ");
			log.append(cmd).append("\n");		
		}
	}

	/**
     * Log an FTP reply being sent back to the client. Not used for
     * SFTP.
     * 
     * @param connID Identifier of FTP connection
     * @param reply   reply string
     */
	public void replyReceived(String connId, String reply) {
		if (logCommands) {
			if (logConnectionIdentifiers)
				log.append('[').append(connId).append("] ");
			log.append(reply).append("\n");
		}
	}

	/**
     * Notifies that a download has started
     * 
     * @param connID Identifier of FTP connection
     * @param remoteFilename   remote file name
     */
	public void downloadStarted(String connId, String remoteFilename) {
		if (logTransferStartComplete) {
			if (logConnectionIdentifiers)
				log.append('[').append(connId).append("] ");
			log.append("Started download: ").append(remoteFilename).append("\n");
		}
	}

	/**
     * Notifies that a download has completed
     * 
     * @param connID Identifier of FTP connection
     * @param remoteFilename   remote file name
     */
	public void downloadCompleted(String connId, String remoteFilename) {
		if (logTransferStartComplete) {
			if (logConnectionIdentifiers)
				log.append('[').append(connId).append("] ");
			log.append("Completed download: ").append(remoteFilename).append("\n");
		}
	}

	/**
     * Notifies that an upload has started
     * 
     * @param connID Identifier of FTP connection
     * @param remoteFilename   remote file name
     */
	public void uploadStarted(String connId, String remoteFilename) {
		if (logTransferStartComplete) {
			if (logConnectionIdentifiers)
				log.append('[').append(connId).append("] ");
			log.append("Started upload: ").append(remoteFilename).append("\n");
		}
	}

	/**
     * Notifies that an upload has completed
     * 
     * @param connID Identifier of FTP connection
     * @param remoteFilename   remote file name
     */
	public void uploadCompleted(String connId, String remoteFilename) {
		if (logTransferStartComplete) {
			if (logConnectionIdentifiers)
				log.append('[').append(connId).append("] ");
			log.append("Completed upload: ").append(remoteFilename).append("\n");
		}
	}

	/**
     * Report the number of bytes transferred so far. This may
     * not be entirely accurate for transferring text files in ASCII
     * mode, as new line representations can be represented differently
     * on different platforms.
     * 
     * @param connID Identifier of FTP connection
     * @param remoteFilename Name of remote file
     * @param count  count of bytes transferred
     */
    public void bytesTransferred(String connId, String remoteFilename,
			long count) {
    	if (logTransferProgress) {
			if (logConnectionIdentifiers)
				log.append('[').append(connId).append("] ");
    		log.append(remoteFilename).append(" - ").append(count).append("\n");	
    	}
	}
}
