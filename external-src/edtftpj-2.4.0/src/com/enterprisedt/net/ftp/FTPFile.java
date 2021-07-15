/**
 *
 *  edtFTPj
 * 
 *  Copyright (C) 2000-2004 Enterprise Distributed Technologies Ltd
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
 *    $Log: FTPFile.java,v $
 *    Revision 1.20  2010-03-25 04:03:32  bruceb
 *    make toString() clearer
 *
 *    Revision 1.19  2008-07-15 05:41:07  bruceb
 *    isFile() added
 *
 *    Revision 1.18  2008-05-14 05:51:30  bruceb
 *    fix getLinkedName()
 *
 *    Revision 1.17  2007-10-12 05:21:29  bruceb
 *    print out null if lastmodified is null
 *
 *    Revision 1.16  2007-08-09 00:10:53  hans
 *    Removed unused imports.
 *
 *    Revision 1.15  2007/04/21 04:25:14  bruceb
 *    added listFiles() and children
 *
 *    Revision 1.14  2007/01/15 23:03:48  bruceb
 *    added some setters, new constructor (for MLST)
 *
 *    Revision 1.13  2006/10/11 08:53:43  hans
 *    made cvsId final
 *
 *    Revision 1.12  2006/02/09 09:01:52  bruceb
 *    made setters public
 *
 *    Revision 1.11  2005/06/03 11:26:05  bruceb
 *    VMS stuff
 *
 *    Revision 1.10  2005/03/03 21:06:46  bruceb
 *    removed type
 *
 *    Revision 1.9  2005/02/04 12:21:30  bruceb
 *    made FTPFile constructor public
 *
 *    Revision 1.8  2004/09/17 14:13:00  bruceb
 *    added link count
 *
 *    Revision 1.7  2004/09/02 11:02:31  bruceb
 *    rolled back
 *
 *
 */

package com.enterprisedt.net.ftp;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *  Represents a remote file (implementation)
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.20 $
 */
public class FTPFile {
    
    /**
     *  Revision control id
     */
    protected static final String cvsId = "@(#)$Id: FTPFile.java,v 1.20 2010-03-25 04:03:32 bruceb Exp $";
    
    /**
     * Unknown remote server type
     */
    public final static int UNKNOWN = -1;  
    
    /**
     * Windows type
     */
    public final static int WINDOWS = 0;
    
    /**
     * UNIX type
     */
    public final static int UNIX = 1;
    
    /**
     * VMS type
     */
    public final static int VMS = 2;
        
    /**
     * Date formatter type 1
     */
    private final static SimpleDateFormat formatter =
        new SimpleDateFormat("dd-MM-yyyy HH:mm");
    
    /**
     * Type of file
     */
    private int type;
    
    /**
     * Is this file a symbolic link?
     */
    protected boolean isLink = false;
    
    /**
     * Number of links to file
     */
    protected int linkCount = 1;
    
    /**
     * Permission bits string
     */
    protected String permissions;
       
    /**
     * Is this a directory?
     */
    protected boolean isDir = false;
    
    /**
     * Size of file
     */
    protected long size = 0L;
    
    /**
     * File/dir name
     */
    protected String name;
    
    /**
     * Name of file this is linked to
     */
    protected String linkedname;
    
    /**
     * Owner if known
     */
    protected String owner;
    
    /**
     * Group if known
     */
    protected String group;
    
    /**
     * Last modified
     */
    protected Date lastModified;
    
    /**
     * Created time
     */
    protected Date created;

    /**
     * Raw string
     */
    protected String raw;
    
    /**
     * Directory if known
     */
    protected String path;
    
    /**
     * Children if a directory
     */
    private FTPFile[] children;
    
    /**
     * Constructor
     * 
     * @param type          type of file
     * @param raw           raw string returned from server
     * @param name          name of file
     * @param size          size of file
     * @param isDir         true if a directory
     * @param lastModified  last modified timestamp
     * @deprecated 'type' no longer used.
     */
    public FTPFile(int type, String raw, String name, long size, boolean isDir, Date lastModified) {
        this(raw);
        this.type = type;
        this.name = name;
        this.size = size;
        this.isDir = isDir;
        this.lastModified = lastModified;
    }
    
    /**
     * Constructor
     * 
     * @param raw           raw string returned from server
     * @param name          name of file
     * @param size          size of file
     * @param isDir         true if a directory
     * @param lastModified  last modified timestamp
     */
    public FTPFile(String raw, String name, long size, boolean isDir, Date lastModified) {
        this(raw);
        this.type = UNKNOWN;
        this.name = name;
        this.size = size;
        this.isDir = isDir;
        this.lastModified = lastModified;
    }
    
    /**
     * Constructor
     * 
     * @param raw   raw string returned from server
     */
    public FTPFile(String raw) {
        this.raw = raw;
    }
    
    /**
     * Returns an array of FTPFile objects denoting the files and directories in this
     * directory
     * 
     * @return FTPFile array
     */
    public FTPFile[] listFiles() {
        return children;
    }
    
    /**
     * 
     * @param children
     */
    void setChildren(FTPFile[] children) {
        this.children = children;
    }
    
    /**
     * Get the type of file, i.e UNIX
     * 
     * @return the integer type of the file
     * @deprecated No longer necessary.
     */
    public int getType() {
        return type;
    }
    
    /**
     * @return Returns the group.
     */
    public String getGroup() {
        return group;
    }

    /**
     * @return Returns the isDir.
     */
    public boolean isDir() {
        return isDir;
    }
    
    /**
     * Is this a file (and not a directory
     * or a link).
     * 
     * @return true if a file, false if link or directory
     */
    public boolean isFile() {
        return !isDir() && !isLink();
    }

    /**
     * @return Returns the lastModified date.
     */
    public Date lastModified() {
        return lastModified;
    }
    
    /**
     * Set the last modified date
     * 
     * @param date  last modified date
     */
    public void setLastModified(Date date) {
        lastModified = date;
    }
    
    /**
     * Get the created date for the file. This is not
     * supported by many servers, e.g. Unix does not record
     * the created date of a file.
     * 
     * @return Returns the created date.
     */
    public Date created() {
        return created;
    }
    
    /**
     * Set the created date
     * 
     * @param date
     */
    public void setCreated(Date date) {
        created = date;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the name of the file
     * 
     * @param name  name of file
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the owner.
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @return Returns the raw server string.
     */
    public String getRaw() {
        return raw;
    }

    /**
     * @return Returns the size.
     */
    public long size() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    /**
     * @return Returns the permissions.
     */
    public String getPermissions() {
        return permissions;
    }
    
    /**
     * @return Returns true if file is a symlink
     */
    public boolean isLink() {
        return isLink;
    }
    
    /**
     * @return Returns the number of links to the file
     */
    public int getLinkCount() {
        return linkCount;
    }
    
    /**
     * @return Returns the linked name.
     * @deprecated
     */
    public String getLinkedname() {
        return linkedname;
    }
    
    /**
     * @return Returns the linked name.
     */
    public String getLinkedName() {
        return linkedname;
    }

    /**
     * @param group The group to set.
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * @param isDir The isDir to set.
     */
    public void setDir(boolean isDir) {
        this.isDir = isDir;
    }

    /**
     * @param isLink The isLink to set.
     */
    public void setLink(boolean isLink) {
        this.isLink = isLink;
    }
    
    /**
     * @param linkedname The linked name to set.
     */
    public void setLinkedName(String linkedname) {
        this.linkedname = linkedname;
    }

    /**
     * @param owner The owner to set.
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @param permissions The permissions to set.
     */
    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    /**
     * @param linkCount   new link count
     */
    public void setLinkCount(int linkCount) {
        this.linkCount = linkCount;
    }
    
    /**
     * @return string representation
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(raw).append("\n");
        buf.append("Name=").append(name).append(",").
            append("Size=").append(size).append(",").
            append("Permissions=").append(permissions).append(",").
            append("Owner=").append(owner).append(",").
            append("Group=").append(group).append(",").
            append("Is link=").append(isLink).append(",").
            append("Link count=").append(linkCount).append(".").
            append("Is dir=").append(isDir).append(",").
            append("Linked name=").append(linkedname).
            append(",").append("Last modified=").
            append(lastModified != null ? formatter.format(lastModified) : "null");
        if (created != null)
            buf.append(",").append("Created=").append(
                    formatter.format(created));
        return buf.toString();
    }

    public String getPath() {
		return path;
	}

    public void setPath(String path) {
		this.path = path;
	}
}
