/**
 *
 *  edtFTPj
 *
 *  Copyright (C) 2000-2003  Enterprise Distributed Technologies Ltd
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
 *        $Log: FTPClient.java,v $
 *        Revision 1.133  2012/11/30 05:00:55  bruceb
 *        flag for filelocking enabled or not
 *
 *        Revision 1.132  2012/11/26 06:43:47  bruceb
 *        catch tryLock() exceptions
 *
 *        Revision 1.131  2012/11/06 04:11:22  bruceb
 *        increase tcp buffer size & add new readChunk method
 *
 *        Revision 1.130  2012/10/24 05:17:07  bruceb
 *        get/setNetworkBufferSize() + allow cdup to fail
 *
 *        Revision 1.129  2012/03/13 03:34:46  bruceb
 *        file locking
 *
 *        Revision 1.128  2012-02-08 06:20:10  bruceb
 *        resumeNextDownload
 *
 *        Revision 1.127  2011-08-26 03:48:46  bruceb
 *        remove import
 *
 *        Revision 1.126  2011-04-05 06:18:07  bruceb
 *        fix features()
 *
 *        Revision 1.125  2011-03-24 00:56:58  bruceb
 *        ensure data socket is closed when calling quit()
 *
 *        Revision 1.124  2011-03-18 06:28:53  bruceb
 *        MLST changes
 *
 *        Revision 1.123  2011-03-18 06:22:44  hans
 *        Add existsFile and existsDirectory methods and deprecate the existing exists method.
 *
 *        Revision 1.122  2011-01-16 22:46:33  bruceb
 *        fix resume bug
 *
 *        Revision 1.121  2010-11-04 01:08:06  bruceb
 *        allow REST to fail
 *
 *        Revision 1.120  2010-09-15 04:07:08  bruceb
 *        fix nptr in connected()
 *
 *        Revision 1.119  2010-08-20 00:25:07  bruceb
 *        IPv6 changes
 *
 *        Revision 1.118  2010-06-30 14:02:43  bruceb
 *        permit 232 to be returned from USER
 *
 *        Revision 1.117  2010-05-28 07:04:55  bruceb
 *        allow aborting the listing
 *
 *        Revision 1.116  2010-04-26 15:55:46  bruceb
 *        add new dirDetails method with callback
 *
 *        Revision 1.115  2010-04-09 21:09:31  hans
 *        Fixed setProgressMonitorEx so that it works when called multiple times.
 *
 *        Revision 1.114  2010-02-25 01:22:48  bruceb
 *        add debug
 *
 *        Revision 1.113  2010-02-17 00:37:46  bruceb
 *        initialise resumeMarker correctly
 *
 *        Revision 1.112  2009-10-18 23:59:03  bruceb
 *        move name resolution to connect()
 *
 *        Revision 1.111  2009-08-20 05:15:09  bruceb
 *        fix re FTPProgressMonitorEx - bytesTransferred not being called
 *
 *        Revision 1.110  2009-07-17 03:04:08  bruceb
 *        proxy changes + change exists to use dirDetails
 *
 *        Revision 1.109  2009-06-18 07:17:04  bruceb
 *        autoPassiveIPSubstitution set to true by default, and throw exception when we cancel transfer
 *
 *        Revision 1.108  2009-04-14 01:47:26  bruceb
 *        PASV/PORT callbacks
 *
 *        Revision 1.107  2009-03-20 04:35:10  bruceb
 *        lots of little changes to fix automode bug, post transfer check methods etc
 *
 *        Revision 1.106  2009-02-20 06:24:24  hans
 *        Removed unused local variables.
 *
 *        Revision 1.105  2009-02-20 06:21:52  hans
 *        Removed unused local variables.
 *
 *        Revision 1.104  2009-01-28 04:15:45  bruceb
 *        processControlChannelException added
 *
 *        Revision 1.103  2009-01-28 03:52:18  bruceb
 *        implement reconnect
 *
 *        Revision 1.102  2009-01-15 03:39:37  bruceb
 *        *** empty log message ***
 *
 *        Revision 1.101  2008-09-22 03:50:00  bruceb
 *        fix doco bug re timeout
 *
 *        Revision 1.100  2008-09-18 06:56:33  bruceb
 *        retry settings
 *
 *        Revision 1.99  2008-09-18 05:18:03  bruceb
 *        sendCommand now throws an FTPException too
 *
 *        Revision 1.98  2008-08-26 04:35:52  bruceb
 *        move resume/size to before data socket creation
 *
 *        Revision 1.97  2008-05-22 04:20:55  bruceb
 *        moved stuff to internal etc
 *
 *        Revision 1.96  2008-05-02 07:41:30  bruceb
 *        setModTime added
 *
 *        Revision 1.95  2008-03-13 04:23:29  bruceb
 *        changed to system()
 *
 *        Revision 1.94  2008-03-13 00:22:14  bruceb
 *        added executeCommand
 *
 *        Revision 1.93  2008-01-09 03:54:21  bruceb
 *        executeCommand() now returns reply code
 *
 *        Revision 1.92  2007-12-18 07:54:58  bruceb
 *        many small changes to prepare for FileTransferClient
 *
 *        Revision 1.91  2007-11-29 02:32:09  hans
 *        Added DEFAULT_LISTING_LOCALES and made related changes to initializers.
 *
 *        Revision 1.90  2007-11-13 07:14:04  bruceb
 *        ListenOnAllInterfaces
 *
 *        Revision 1.89  2007-11-07 23:53:50  bruceb
 *        refactoring for FXP
 *
 *        Revision 1.88  2007-10-12 05:21:13  bruceb
 *        can set multiple locales (and 2 set by default)
 *
 *        Revision 1.87  2007-08-15 03:47:39  bruceb
 *        fix separator
 *
 *        Revision 1.86  2007-08-09 00:50:20  bruceb
 *        added 250 reply to some commands
 *
 *        Revision 1.85  2007-08-07 04:46:25  bruceb
 *        added counts for transfers and deletes plus getLastReply()
 *
 *        Revision 1.84  2007-07-18 02:16:33  bruceb
 *        ignore size() exception in resume
 *
 *        Revision 1.83  2007-07-05 05:28:27  bruceb
 *        add getters for message collections
 *
 *        Revision 1.82  2007-06-14 04:12:48  bruceb
 *        added setForceUniqueNames()
 *
 *        Revision 1.81  2007-06-06 22:20:11  bruceb
 *        FTP_LINE_SEPARATOR now public
 *
 *        Revision 1.80  2007-05-29 04:17:24  bruceb
 *        modify connected() method, and null out control when quitting
 *
 *        Revision 1.79  2007-05-15 01:02:58  bruceb
 *        file factory change if syst doesn't work
 *
 *        Revision 1.78  2007/04/24 01:58:03  bruceb
 *        more debug for validateTransferOnError
 *
 *        Revision 1.77  2007/04/23 23:43:20  bruceb
 *        check on dirname in dirDetails()
 *
 *        Revision 1.76  2007/04/21 04:24:46  bruceb
 *        fix to cope with any ascii file
 *
 *        Revision 1.75  2007/03/22 04:03:47  bruceb
 *        added getOutputStream()
 *
 *        Revision 1.74  2007/03/19 22:07:36  bruceb
 *        set control to null
 *
 *        Revision 1.73  2007/03/13 02:45:08  bruceb
 *        deleteOnFailure flag added
 *
 *        Revision 1.72  2007/03/09 05:04:01  bruceb
 *        fixed bugs in ASCII put & get
 *
 *        Revision 1.71  2007/02/26 07:17:54  bruceb
 *        make various method package or protected visibility so they can be accessed by subclasses etc
 *
 *        Revision 1.70  2007/02/07 23:02:26  bruceb
 *        fixed failure to throw cancellation exception
 *
 *        Revision 1.69  2007/02/06 07:19:24  bruceb
 *        fixed autodetect bug re actual mode not being changed on the server
 *
 *        Revision 1.68  2007/02/04 23:02:40  bruceb
 *        more error logging and extra codes, set strict validation off
 *
 *        Revision 1.67  2007/02/01 06:05:18  bruceb
 *        enable cancelling of large directory listings
 *
 *        Revision 1.66  2007/01/15 23:05:14  bruceb
 *        added fileDetails()
 *
 *        Revision 1.65  2007/01/12 02:04:56  bruceb
 *        extracted string matchers & fixed exists()
 *
 *        Revision 1.64  2007/01/10 02:37:39  bruceb
 *        modify exists to use RETR if necessary
 *
 *        Revision 1.63  2006/12/12 01:04:54  hans
 *        Fixed bug in exists method and added logging of raw directory listing.
 *
 *        Revision 1.62  2006/10/27 15:44:06  bruceb
 *        added sendServerWakeup()
 *
 *        Revision 1.61  2006/10/17 11:03:41  bruceb
 *        fix setActivePortRange comment, include using single port info
 *
 *        Revision 1.60  2006/10/17 10:28:43  bruceb
 *        refactored to get setupDataSocket()
 *
 *        Revision 1.59  2006/10/11 08:38:00  bruceb
 *        controlEncoding applied to directory listings
 *
 *        Revision 1.58  2006/09/11 12:34:00  bruceb
 *        added exists() method
 *
 *        Revision 1.57  2006/08/23 08:48:51  bruceb
 *        don't null out FileFactory when quit() is called
 *
 *        Revision 1.56  2006/07/27 14:12:01  bruceb
 *        IPV6 changes and fixed bug re control channel messages after unexpected close on data connection
 *
 *        Revision 1.55  2006/05/22 01:54:42  hans
 *        Made remoteHost protected.
 *
 *        Revision 1.54  2006/02/16 19:47:09  hans
 *        Added comment
 *
 *        Revision 1.53  2005/11/15 21:02:32  bruceb
 *        more debug
 *
 *        Revision 1.52  2005/11/10 19:46:13  bruceb
 *        delegate resume comments to FTPClientInterface
 *
 *        Revision 1.51  2005/11/10 13:40:28  bruceb
 *        more elaborate versioning info to debug
 *
 *        Revision 1.50  2005/11/09 21:15:38  bruceb
 *        autodetect file types
 *
 *        Revision 1.49  2005/10/10 20:42:56  bruceb
 *        append now in FTPClientInterface
 *
 *        Revision 1.48  2005/09/29 16:03:06  bruceb
 *        permit 350 return from STOR
 *
 *        Revision 1.47  2005/09/21 10:38:06  bruceb
 *        fix for LIST error re empty dir (proFTPD/TLS)
 *
 *        Revision 1.46  2005/09/20 09:44:36  bruceb
 *        extra no files found string, SYST accepts 213
 *
 *        Revision 1.45  2005/09/02 21:03:04  bruceb
 *        no abort() with cancel
 *
 *        Revision 1.44  2005/08/26 17:48:16  bruceb
 *        passive ip address setting + ASCII optimisation
 *
 *        Revision 1.43  2005/06/17 18:25:56  bruceb
 *        fix javadoc
 *
 *        Revision 1.42  2005/06/16 21:39:49  hans
 *        deprecated ControlPort accessors and removed comments for FTPClientInterface methods
 *
 *        Revision 1.41  2005/06/10 15:44:38  bruceb
 *        added noOperation() and connected()
 *
 *        Revision 1.40  2005/06/03 11:25:17  bruceb
 *        ascii fixes, setActivePortRange
 *
 *        Revision 1.41  2005/05/24 11:32:28  bruceb
 *        version + timestamp info in static block
 *
 *        Revision 1.40  2005/05/15 19:46:28  bruceb
 *        changes for testing setActivePortRange + STOR accepting 350 nonstrict
 *
 *        Revision 1.39  2005/04/01 13:58:15  bruceb
 *        restructured dir() exception handling + quote() change
 *
 *        Revision 1.38  2005/03/18 11:04:32  bruceb
 *        deprecated constructors
 *
 *        Revision 1.37  2005/03/11 14:40:11  bruceb
 *        added cdup() and changed buffer defaults
 *
 *        Revision 1.36  2005/03/03 21:07:14  bruceb
 *        implement interface & augment login doco
 *
 *        Revision 1.35  2005/02/04 12:40:35  bruceb
 *        tidied javadoc
 *
 *        Revision 1.34  2005/02/04 12:28:51  bruceb
 *        when getting, if file exists and is readonly, exception is thrown
 *
 *        Revision 1.33  2005/01/28 13:55:39  bruceb
 *        added ACCT handling
 *
 *        Revision 1.32  2005/01/14 20:27:02  bruceb
 *        exception restructuring + ABOR
 *
 *        Revision 1.31  2004/11/19 08:28:10  bruceb
 *        added setPORTIP()
 *
 *        Revision 1.30  2004/10/18 15:54:48  bruceb
 *        clearSOCKS added, set encoding for control sock, locale for parser
 *
 *        Revision 1.29  2004/09/21 21:28:28  bruceb
 *        fixed javadoc comment
 *
 *        Revision 1.28  2004/09/18 14:27:57  bruceb
 *        features() throw exception if not supported
 *
 *        Revision 1.27  2004/09/18 09:33:47  bruceb
 *        1.1.8 tweaks
 *
 *        Revision 1.26  2004/09/17 14:12:38  bruceb
 *        fixed javadoc re filemasks
 *
 *        Revision 1.25  2004/09/14 06:24:03  bruceb
 *        fixed javadoc comment
 *
 *        Revision 1.24  2004/08/31 13:48:29  bruceb
 *        resume,features,restructure
 *
 *        Revision 1.23  2004/07/23 08:34:32  bruceb
 *        strict replies or not, better tfr monitor reporting
 *
 *        Revision 1.22  2004/06/25 11:47:46  bruceb
 *        made 1.1.x compatible
 *
 *        Revision 1.21  2004/06/11 10:20:35  bruceb
 *        permit 200 to be returned from various cmds
 *
 *        Revision 1.20  2004/05/22 16:52:57  bruceb
 *        message listener
 *
 *        Revision 1.19  2004/05/15 22:37:22  bruceb
 *        put debugResponses back in
 *
 *        Revision 1.18  2004/05/13 23:00:34  hans
 *        changed comment
 *
 *        Revision 1.17  2004/05/08 21:14:41  bruceb
 *        checkConnection stuff
 *
 *        Revision 1.14  2004/04/19 21:54:06  bruceb
 *        final tweaks to dirDetails() re caching
 *
 *        Revision 1.13  2004/04/18 11:16:44  bruceb
 *        made validateTransfer() public
 *
 *        Revision 1.12  2004/04/17 18:37:38  bruceb
 *        new parse functionality
 *
 *        Revision 1.11  2004/03/23 20:26:49  bruceb
 *        tweak to size(), catch exceptions on puts()
 *
 *        Revision 1.10  2003/11/15 11:23:55  bruceb
 *        changes required for ssl subclasses
 *
 *        Revision 1.6  2003/05/31 14:53:44  bruceb
 *        1.2.2 changes
 *
 *        Revision 1.5  2003/01/29 22:46:08  bruceb
 *        minor changes
 *
 *        Revision 1.4  2002/11/19 22:01:25  bruceb
 *        changes for 1.2
 *
 *        Revision 1.3  2001/10/09 20:53:46  bruceb
 *        Active mode changes
 *
 *        Revision 1.1  2001/10/05 14:42:03  bruceb
 *        moved from old project
 *
 */

package com.enterprisedt.net.ftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;

import com.enterprisedt.net.ftp.internal.FTPDataSocket;
import com.enterprisedt.util.debug.Level;
import com.enterprisedt.util.debug.Logger;

/**
 *  Supports client-side FTP. Most common
 *  FTP operations are present in this class.
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.133 $
 */
public class FTPClient implements FTPClientInterface {

    /**
     *  Revision control id
     */
    public static String cvsId = "@(#)$Id: FTPClient.java,v 1.133 2012/11/30 05:00:55 bruceb Exp $";
    
    /**
     * Default byte interval for transfer monitor
     */
    final public static int DEFAULT_MONITOR_INTERVAL = 65535;
    
    /**
     * Default transfer buffer size
     */
    final public static int DEFAULT_BUFFER_SIZE = 16384;
    
    /**
     * Maximum port number
     */
    final private static int MAX_PORT = 65535;
    
    /**
     * Default timeout
     */
    final public static int DEFAULT_TIMEOUT = 60*1000;
    
    /**
     * Short value for a timeout
     */
    final private static int SHORT_TIMEOUT = 500;
    
    /**
     * Default number of retries for file transfers
     */
    final public static int DEFAULT_RETRY_COUNT = 3;

    /**
     * Default retry delay in milliseconds
     */
    final public static int DEFAULT_RETRY_DELAY = 5000;
    
    /**
     * Default SO_SNDBUF and SO_RCVBUF size
     */
    final public static int DEFAULT_TCP_BUFFER_SIZE = 128*1024;
    
    /**
     * Default encoding used for control data
     */
    final public static String DEFAULT_ENCODING = "US-ASCII";
    
    /**
     * SOCKS port property name
     */
    final private static String SOCKS_PORT = "socksProxyPort";

    /**
     * SOCKS host property name
     */
    final private static String SOCKS_HOST = "socksProxyHost";
    
    /**
     * Line separator
     */
    final private static byte[] LINE_SEPARATOR = System.getProperty("line.separator").getBytes();
    
    /**
     * Used for ASCII translation
     */
    final public static byte CARRIAGE_RETURN = 13;
    
    
    /**
     * Used for ASCII translation
     */
    final public static byte LINE_FEED = 10;
    
    /**
     * Used for ASCII translation
     */
    final public static byte[] FTP_LINE_SEPARATOR = {CARRIAGE_RETURN, LINE_FEED};
                
    /**
     * Marker in reply for STOU reply with filename
     */
    final private static String STOU_FILENAME_MARKER = "FILE:";
    
    /**
     * Store command
     */
    final private static String STORE_CMD = "STOR ";
        
    /**
     * Store unique command
     */
    final private static String STORE_UNIQ_CMD = "STOU ";   
    
    /**
     * MFMT return string keyword
     */
    final private static String MODTIME_STR = "modtime";
    
    /**
     * Default locales
     */
    public static Locale[] DEFAULT_LISTING_LOCALES;    
       
    /**
     * Logging object
     */
    private static Logger log = Logger.getLogger("FTPClient");
    
    /**
     *  Format to interpret MTDM timestamp
     */
    private SimpleDateFormat tsFormat =
        new SimpleDateFormat("yyyyMMddHHmmss");
    
    /**
     *  Socket responsible for controlling
     *  the connection
     */
	protected FTPControlSocket control = null;

    /**
     *  Socket responsible for transferring
     *  the data
     */
    protected FTPDataSocket data = null;
    
    /**
     *  Socket timeout for both data and control. In
     *  milliseconds
     */
    protected int timeout = DEFAULT_TIMEOUT;
    
    /**
     * Interval in seconds in between server wakeups. O is
     * not enabled
     */
    protected int serverWakeupInterval = 0;
    
    /**
     * Address of the remote server.
     */
    protected InetAddress remoteAddr;
    
    /**
     * Name/IP of remote host
     */
    protected String remoteHost;
    
    /**
     * Id of instance
     */
    protected String id;
    
    /**
     * Master id 
     */
    private static int masterId = 0;
    
    /**
     * Control port number.
     */
    protected int controlPort = FTPControlSocket.CONTROL_PORT;
    
    /**
     * If true, uses the original host IP if an internal IP address
     * is returned by the server in PASV mode
     */
    private boolean autoPassiveIPSubstitution = true;
    
    /**
     * IP address to force to use in active mode
     */
    private String activeIP = null;
    
    /**
     * Encoding used on control socket
     */
    protected String controlEncoding = DEFAULT_ENCODING;
      
    /**
     * Use strict return codes if true
     */
    private boolean strictReturnCodes = false;
    
    /**
     * Matcher for directory empty
     */
    protected DirectoryEmptyStrings dirEmptyStrings = new DirectoryEmptyStrings();
    
    /**
     * Matcher for transfer complete
     */
    protected TransferCompleteStrings transferCompleteStrings = new TransferCompleteStrings();
    
    /**
     * Matcher for permission denied
     */
    protected FileNotFoundStrings fileNotFoundStrings = new FileNotFoundStrings();
    /**
     *  Can be used to cancel a transfer
     */
    private boolean cancelTransfer = false;
    
    /**
     * If true, a file transfer is being resumed
     */
    private boolean resume = false;
    
    /**
     * MDTM supported flag
     */
    private boolean mdtmSupported = true;
    
    /**
     * SIZE supported flag
     */
    private boolean sizeSupported = true;
    
    /**
     * CDUP supported flag
     */
    private boolean cdupSupported = true;
    
    /**
     * Resume byte marker point
     */
    private long resumeMarker = 0;
    
    /**
     * Delete partial files on transfer failure?
     */
    private boolean deleteOnFailure = true;
    
    /**
     * If true, filetypes are autodetected and transfer mode changed to binary/ASCII as 
     * required
     */
    protected boolean detectTransferMode = false;
    
    /**
     * If true, file locking is used on local downloaded files to prevent
     * other processes corrupting them
     */
    protected boolean fileLockingEnabled = true;

    /**
     * Lowest port in active mode port range
     */
    private int lowPort = -1;

    /**
     * Highest port in active mode port range
     */
    private int highPort = -1;
    
    /**
     * Command sent to server for storing a file
     */
    private String storeCommand = STORE_CMD;
    
    /**
     * Bytes transferred in between monitor callbacks
     */
    protected long monitorInterval = DEFAULT_MONITOR_INTERVAL;
    
    /**
     * Size of transfer buffers
     */
    protected int transferBufferSize = DEFAULT_BUFFER_SIZE;
    
    /**
     * Size of data socket's receive buffer
     */
    protected int dataReceiveBufferSize = DEFAULT_TCP_BUFFER_SIZE;
    
    /**
     * Size of data socket's send buffer
     */
    protected int dataSendBufferSize = DEFAULT_TCP_BUFFER_SIZE;
    
    /**
     * Count of downloaded files
     */
    private int downloadCount = 0;
    
    /**
     * Count of uploaded files
     */
    private int uploadCount = 0;
    
    /**
     * Count of deleted files
     */
    private int deleteCount = 0;
    
    /**
     * Number of times to retry a transfer operation before giving up.
     */
    private int retryCount = DEFAULT_RETRY_COUNT; 

    /**
     * Number of milliseconds to wait before retrying.
     */
    private int retryDelay = DEFAULT_RETRY_DELAY;  
    
    /**
     * Listen to all interfaces in active mode
     */
    private boolean listenOnAllInterfaces = true;
        
    /**
     * Parses LIST output
     */
    private FTPFileFactory fileFactory = null;
    
    /**
     * Locales for date parsing
     */
    private Locale[] listingLocales;
    
    /**
     * Parses the MLSD and MLST formats
     */
    private MLSXEntryParser mlsxParser = new MLSXEntryParser();
    
    /**
     *  Progress monitor
     */
    protected FTPProgressMonitor monitor = null;  
    
    /**
     * Message listener
     */
    protected FTPMessageListener messageListener = null;
    
    /**
     * File transfer listener
     */
    protected FTPProgressMonitorEx monitorEx = null;

    /**
     *  Record of the transfer type - make the default ASCII
     */
    protected FTPTransferType transferType = FTPTransferType.ASCII;

    /**
     *  Record of the connect mode - make the default PASV (as this was
     *  the original mode supported)
     */
    private FTPConnectMode connectMode = FTPConnectMode.PASV;

    /**
     *  Holds the last valid reply from the server on the control socket
     */
	protected FTPReply lastValidReply;   
    
    /**
     *  Holds the last reply from the server on the control socket
     */
    protected FTPReply lastReply;   
    
    /**
     * Username cached
     */
    protected String user;
    
    /**
     * Password cached
     */
    protected String password;
    
    /**
     * Threshold for throttling
     */
    protected BandwidthThrottler throttler = null;
    
    /**
     * Pasv callback method
     */
    protected DataChannelCallback dataChannelCallback = null;
    
    /**
     * set default listing locales.
     */
    static {
    	DEFAULT_LISTING_LOCALES = new Locale[2];
    	DEFAULT_LISTING_LOCALES[0] = Locale.ENGLISH;
    	DEFAULT_LISTING_LOCALES[1] = Locale.getDefault();
    }
    
    /**
     *  Instance initializer. Sets formatter to GMT.
     */
    {
        tsFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        listingLocales = DEFAULT_LISTING_LOCALES;
        id = Integer.toString(++masterId);
    }  
    
    
    /**
     * Get the version of edtFTPj
     * 
     * @return int array of {major,middle,minor} version numbers 
     */
    public static int[] getVersion() {
        return VersionDetails.getVersion();
    }
    
    /**
     * Get the build timestamp
     * 
     * @return d-MMM-yyyy HH:mm:ss z build timestamp 
     */
    public static String getBuildTimestamp() {
        return VersionDetails.getBuildTimestamp();
    }

    /**
     *  Constructor. Creates the control
     *  socket
     *
     *  @param   remoteHost  the remote hostname
     *  @deprecated  use setter methods to set properties
     */
    public FTPClient(String remoteHost)
        throws IOException, FTPException {

		this(remoteHost, FTPControlSocket.CONTROL_PORT, 0);
    }

    /**
     *  Constructor. Creates the control
     *  socket
     *
     *  @param   remoteHost  the remote hostname
     *  @param   controlPort  port for control stream (-1 for default port)
     *  @deprecated  use setter methods to set properties
     */
    public FTPClient(String remoteHost, int controlPort)
        throws IOException, FTPException {

		this(remoteHost, controlPort, 0);
    }
    
    
    /**
     *  Constructor. Creates the control
     *  socket
     *
     *  @param   remoteHost  the remote hostname
     *  @param   controlPort  port for control stream (use -1 for the default port)
     *  @param  timeout       the length of the timeout, in milliseconds
     *                        (pass in 0 for no timeout)
     *  @deprecated  use setter methods to set properties
     */
    public FTPClient(String remoteHost, int controlPort, int timeout)
    throws IOException, FTPException {

        this(InetAddress.getByName(remoteHost), controlPort, timeout);
    }

    /**
     *  Constructor. Creates the control
     *  socket
     *
     *  @param   remoteHost  the remote hostname
     *  @param   controlPort  port for control stream (use -1 for the default port)
     *  @param  timeout       the length of the timeout, in milliseconds
     *                        (pass in 0 for no timeout)
     *  @param   encoding         character encoding used for data
     *  @deprecated  use setter methods to set properties
     */
    public FTPClient(String remoteHost, int controlPort, int timeout, String encoding)
        throws IOException, FTPException {

        this(InetAddress.getByName(remoteHost), controlPort, timeout, encoding);
    }

    /**
     *  Constructor. Creates the control
     *  socket
     *
     *  @param   remoteAddr  the address of the
     *                       remote host
     *  @deprecated  use setter methods to set properties
     */
    public FTPClient(InetAddress remoteAddr)
        throws IOException, FTPException {

		this(remoteAddr, FTPControlSocket.CONTROL_PORT, 0);
    }
    

    /**
     *  Constructor. Creates the control
     *  socket. Allows setting of control port (normally
     *  set by default to 21).
     *
     *  @param   remoteAddr  the address of the
     *                       remote host
     *  @param   controlPort  port for control stream
     *  @deprecated  use setter methods to set properties
     */
    public FTPClient(InetAddress remoteAddr, int controlPort)
        throws IOException, FTPException {

		this(remoteAddr, controlPort, 0);
    }

    /**
     *  Constructor. Creates the control
     *  socket. Allows setting of control port (normally
     *  set by default to 21).
     *
     *  @param   remoteAddr    the address of the
     *                          remote host
     *  @param   controlPort   port for control stream (-1 for default port)
     *  @param  timeout        the length of the timeout, in milliseconds 
     *                         (pass in 0 for no timeout)
     *  @deprecated  use setter methods to set properties
     */
    public FTPClient(InetAddress remoteAddr, int controlPort, int timeout)
        throws IOException, FTPException {
        if (controlPort < 0)
            controlPort = FTPControlSocket.CONTROL_PORT;
		initialize(new FTPControlSocket(remoteAddr, controlPort, timeout, DEFAULT_ENCODING, null));
    }
    
    /**
     *  Constructor. Creates the control
     *  socket. Allows setting of control port (normally
     *  set by default to 21).
     *
     *  @param   remoteAddr    the address of the
     *                          remote host
     *  @param   controlPort   port for control stream (-1 for default port)
     *  @param   timeout        the length of the timeout, in milliseconds 
     *                         (pass in 0 for no timeout)
     *  @param   encoding         character encoding used for data
     *  @deprecated  use setter methods to set properties
     */
    public FTPClient(InetAddress remoteAddr, int controlPort, int timeout, String encoding)
        throws IOException, FTPException {
        if (controlPort < 0)
            controlPort = FTPControlSocket.CONTROL_PORT;
        initialize(new FTPControlSocket(remoteAddr, controlPort, timeout, encoding, null));
    }
    
    /**
     *  Default constructor should now always be used together with setter methods
     *  in preference to other constructors (now deprecated). The {@link #connect()}
     *  method is used to perform the actual connection to the remote host - but only
     *  for this constructor. Deprecated constructors connect in the constructor and
     *  connect() is not required (and cannot be called).
     */
    public FTPClient() {
        log.debug(VersionDetails.report(this));
    }
    
    /**
     * Connects to the server at the address and port number defined
     * in the constructor. Must be performed <b>before</b> login() or user() is
     * called.
     * 
     * @throws IOException Thrown if there is a TCP/IP-related error.
     * @throws FTPException Thrown if there is an error related to the FTP protocol. 
     */
    public void connect() throws IOException, FTPException {

        checkConnection(false);
        
        if (remoteAddr == null)
            remoteAddr = InetAddress.getByName(remoteHost);
       
        log.debug("Connecting to " + remoteAddr + ":" + controlPort);
        
        initialize(new FTPControlSocket(remoteAddr, controlPort, timeout, 
                                         controlEncoding, messageListener));
    }
    
    /**
     * Is this client connected? 
     * 
     * @return true if connected, false otherwise
     */
    public boolean connected() {
        if (control==null)
           return false;
        else {
            return control.controlSock == null ? false : control.controlSock.isConnected();
        }
    }
    
    /**
     * Checks if the client has connected to the server and throws an exception if it hasn't.
     * This is only intended to be used by subclasses
     * 
     * @throws FTPException Thrown if the client has not connected to the server.
     */
    protected void checkConnection(boolean shouldBeConnected) throws FTPException {
    	if (shouldBeConnected && !connected())
    		throw new FTPException("The FTP client has not yet connected to the server.  "
    				+ "The requested action cannot be performed until after a connection has been established.");
    	else if (!shouldBeConnected && connected())
    		throw new FTPException("The FTP client has already been connected to the server.  "
    				+"The requested action must be performed before a connection is established.");
    }
    	
    /**
     * Set the control socket explicitly
     * 
     * @param control   control socket reference
     */
	protected void initialize(FTPControlSocket control) throws IOException {
		this.control = control;
        control.setMessageListener(messageListener);
        control.setStrictReturnCodes(strictReturnCodes);
        control.setListenOnAllInterfaces(listenOnAllInterfaces);
        control.setTimeout(timeout);
        control.setAutoPassiveIPSubstitution(autoPassiveIPSubstitution);
        control.setDataChannelCallback(dataChannelCallback);
        if (activeIP != null)
            control.setActivePortIPAddress(activeIP);
        if (lowPort > 0 && highPort > 0)
            control.setActivePortRange(lowPort, highPort);
	}
    
    /**
     *  Switch debug of responses on or off
     *
     *  @param  on  true if you wish to have responses to
     *              the log stream, false otherwise
     *  @deprecated  use the Logger class to switch debugging on and off
     */
    public void debugResponses(boolean on) {
        if (on)
            Logger.setLevel(Level.DEBUG);
        else
            Logger.setLevel(Level.OFF);
    }    
    
    /**
     * Get the identifying string for this instance
     */
    public String getId() {
        return id;
    }

    /**
     * Set the identifying string for this instance
     * 
     * @param id    identifying string
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get the number of files downloaded since the count was
     * reset
     * 
     * @return  download file count
     */
    public int getDownloadCount() {
        return downloadCount;
    }
    
    /**
     * Reset the count of downloaded files to zero.
     *
     */
    public void resetDownloadCount() {
        downloadCount = 0;
    }
    
    /**
     * Get the number of files uploaded since the count was
     * reset
     * 
     * @return  upload file count
     */
    public int getUploadCount() {
        return uploadCount;
    }
    
    /**
     * Reset the count of uploaded files to zero.
     *
     */
    public void resetUploadCount() {
        uploadCount = 0;
    }
    
    /**
     * Get the number of files deleted since the count was
     * reset
     * 
     * @return  deleted file count
     */
    public int getDeleteCount() {
        return deleteCount;
    }
    
    /**
     * Reset the count of deleted files to zero.
     *
     */
    public void resetDeleteCount() {
        deleteCount = 0;
    }
    
    /**
     * Set the data channel callback, which notifies of the
     * ip and port number to be connected to, and gives an opportunity
     * to modify these values
     * 
     * @param callback  callback to set
     */
    public void setDataChannelCallback(DataChannelCallback callback) {
        this.dataChannelCallback = callback;
        if (control != null)
            control.setDataChannelCallback(callback);
    }
    

    /**
     * Set strict checking of FTP return codes. If strict 
     * checking is on (the default) code must exactly match the expected 
     * code. If strict checking is off, only the first digit must match.
     * 
     * @param strict    true for strict checking, false for loose checking
     */
    public void setStrictReturnCodes(boolean strict) {
        this.strictReturnCodes = strict;
        if (control != null)
            control.setStrictReturnCodes(strict);
    }
    
    /**
     * Determine if strict checking of return codes is switched on. If it is 
     * (the default), all return codes must exactly match the expected code.  
     * If strict checking is off, only the first digit must match.
     * 
     * @return  true if strict return code checking, false if non-strict.
     */
    public boolean isStrictReturnCodes() {
        return strictReturnCodes;
    }
    
    /**
     * Listen on all interfaces for active mode transfers (the default).
     * 
     * @param listenOnAll   true if listen on all interfaces, false to listen on the control interface
     */
    public void setListenOnAllInterfaces(boolean listenOnAll) {
        this.listenOnAllInterfaces = listenOnAll;
        if (control != null)
            control.setListenOnAllInterfaces(listenOnAll);
    }
    
    /**
     * Are we listening on all interfaces in active mode, which is the default?
     * 
     * @return true if listening on all interfaces, false if listening just on the control interface
     */
    public boolean getListenOnAllInterfaces() {
        return listenOnAllInterfaces;
    }
    
    /**
     * Get class that holds fragments of server messages that indicate a file was 
     * not found. New messages can be added.
     * <p>
     * The fragments are used when it is necessary to examine the message
     * returned by a server to see if it is saying a file was not found. 
     * If an FTP server is returning a different message that still clearly 
     * indicates a file was not found, use this property to add a new server 
     * fragment to the repository via the add method. It would be helpful to
     * email support at enterprisedt dot com to inform us of the message so
     * it can be added to the next build.
     * 
     * @return  messages class
     */
    public FileNotFoundStrings getFileNotFoundMessages() {
        return fileNotFoundStrings;
    }
    
    /**
     * Set a new instance of the strings class
     * 
     * @param fileNotFoundStrings  new instance
     */
    public void setFileNotFoundMessages(FileNotFoundStrings fileNotFoundStrings) {
        this.fileNotFoundStrings = fileNotFoundStrings;
    }
    
    /**
     * Get class that holds fragments of server messages that indicate a transfer completed. 
     * New messages can be added.
     * <p>
     * The fragments are used when it is necessary to examine the message
     * returned by a server to see if it is saying a transfer completed. 
     * If an FTP server is returning a different message that still clearly 
     * indicates a transfer failed, use this property to add a new server 
     * fragment to the repository via the add method. It would be helpful to
     * email support at enterprisedt dot com to inform us of the message so
     * it can be added to the next build.
     * 
     * @return  messages class
     */
    public TransferCompleteStrings getTransferCompleteMessages() {
        return transferCompleteStrings;
    }
    
    /**
     * Set a new instance of the strings class
     * 
     * @param transferCompleteStrings  new instance
     */
    public void setTransferCompleteMessages(TransferCompleteStrings transferCompleteStrings) {
        this.transferCompleteStrings = transferCompleteStrings;
    }
    
    /**
     * Get class that holds fragments of server messages that indicate a  
     * directory is empty. New messages can be added.
     * <p>
     * The fragments are used when it is necessary to examine the message
     * returned by a server to see if it is saying a directory is empty. 
     * If an FTP server is returning a different message that still clearly 
     * indicates a directory is empty, use this property to add a new server 
     * fragment to the repository via the add method. It would be helpful to
     * email support at enterprisedt dot com to inform us of the message so
     * it can be added to the next build.
     * 
     * @return  messages class
     */
    public DirectoryEmptyStrings getDirectoryEmptyMessages() {
        return dirEmptyStrings;
    }
    
    /**
     * Set a new instance of the strings class
     * 
     * @param dirEmptyStrings  new instance
     */
    public void setDirectoryEmptyMessages(DirectoryEmptyStrings dirEmptyStrings) {
        this.dirEmptyStrings = dirEmptyStrings;
    }
    
    
    /* (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#setDetectTransferMode(boolean)
     */
    public void setDetectTransferMode(boolean detectTransferMode) {
        this.detectTransferMode = detectTransferMode;
    }

    /* (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#getDetectTransferMode()
     */
    public boolean getDetectTransferMode() {
         return detectTransferMode;
    }
    
    /* (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#setFileLockingEnabled(boolean)
     */
     public void setFileLockingEnabled(boolean lockingEnabled) {
         this.fileLockingEnabled = lockingEnabled;
     }
    
     /* (non-Javadoc)
      * @see com.enterprisedt.net.ftp.FTPClientInterface#getFileLockingEnabled()
      */
    public boolean getFileLockingEnabled() {
        return fileLockingEnabled;
    }
    
    /**
     * Set to true if the STOU command is always to be used when
     * uploading files, even if a filename is supplied. Normally
     * STOU is only used if the supplied remote filename is null or
     * the empty string.
     * 
     * @param forceUnique   true if STOU is always to be used
     */
    public void setForceUniqueNames(boolean forceUnique) {
        if (forceUnique)
            storeCommand = STORE_UNIQ_CMD;
        else
            storeCommand = STORE_CMD;
    }

    /**
     * Switch the transfer mode if requested and if necessary
     * 
     * @param filename      filename of file to be transferred
     * @throws FTPException 
     * @throws IOException 
     */
    protected FTPTransferType chooseTransferMode(String filename) 
        throws IOException, FTPException {
        if (detectTransferMode) {
            if (filename == null) {
                log.warn("Cannot choose transfer mode as filename not supplied");
                return getType();
            }
            if (FileTypes.ASCII.matches(filename) && 
                transferType.equals(FTPTransferType.BINARY)) {
                setType(FTPTransferType.ASCII);
                log.debug("Autodetect on - changed transfer type to ASCII");
            }
            else if (FileTypes.BINARY.matches(filename) && 
                      transferType.equals(FTPTransferType.ASCII)) {
                setType(FTPTransferType.BINARY);
                log.debug("Autodetect on - changed transfer type to binary");
            }
        }
        return getType();
    }

    /**
     *   Set the SO_TIMEOUT in milliseconds on the underlying socket.
     *   If set this means the socket will block in a read operation
     *   only for this length of time - useful if the FTP sever has 
     *   hung, for example. The default is 60,000 milliseconds. 
     *
     *   Note that for JREs 1.4+, the timeout is also used when first 
     *   connecting to the remote host. 
     *
     *   @param millis The length of the timeout, in milliseconds
     */
    public void setTimeout(int millis)
        throws IOException {

        this.timeout = millis;
        if (control != null)
            control.setTimeout(millis);
    }
    
 
    /**
     *  Get the TCP timeout 
     *  
     *  @return timeout that is used, in milliseconds
     */
    public int getTimeout() {
        return timeout;
    }
    
    /**
     * Returns the control-port being connected to on the remote server. 
     * 
     * Note that this method replaces {@link #getControlPort()}.
     * 
     * @return Returns the port being connected to on the remote server. 
     */
    public int getRemotePort() {
        return controlPort;
    }
    
    /** 
     * Set the control to connect to on the remote server. Can only do this if
     * not already connected.
     * 
     * Note that this method replaces {@link #setControlPort(int)}.
     * 
     * @param remotePort The port to use. 
     * @throws FTPException Thrown if the client is already connected to the server.
     */
    public void setRemotePort(int remotePort) throws FTPException {
        checkConnection(false);
        this.controlPort = remotePort;
    }

    /**
     * Returns the control-port being connected to on the remote server. 
     * @return Returns the port being connected to on the remote server. 
     * @deprecated Use {@link com.enterprisedt.net.ftp.FTPClientInterface#getRemotePort()} instead.
     */
    public int getControlPort() {
        return controlPort;
    }
    
    /** 
     * Set the control to connect to on the remote server. Can only do this if
     * not already connected.
     * 
     * @param controlPort The port to use. 
     * @throws FTPException Thrown if the client is already connected to the server.
     * @deprecated Use {@link com.enterprisedt.net.ftp.FTPClientInterface#setRemotePort(int)} instead.
     */
    public void setControlPort(int controlPort) throws FTPException {
        checkConnection(false);
        this.controlPort = controlPort;
    }
    
    /**
     * @return Returns the remoteAddr.
     */
    public InetAddress getRemoteAddr() {
        return remoteAddr;
    }
    
    /**
     * Set the remote address
     * 
     * @param remoteAddr The remoteAddr to set.
     * @throws FTPException
     */
    public void setRemoteAddr(InetAddress remoteAddr) throws FTPException {
        checkConnection(false);
        this.remoteAddr = remoteAddr;
        this.remoteHost = remoteAddr.getHostAddress();
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#getRemoteHost()
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#setRemoteHost(java.lang.String)
     */
    public void setRemoteHost(String remoteHost) throws IOException, FTPException {
        checkConnection(false);
        this.remoteHost = remoteHost;
    }
    
    
    /**
     * Is automatic substitution of the remote host IP set to
     * be on for passive mode connections?
     * 
     * @return true if set on, false otherwise
     */
    public boolean isAutoPassiveIPSubstitution() {
        return autoPassiveIPSubstitution;
    }

    /**
     * Set automatic substitution of the remote host IP on if
     * in passive mode. If proxies are used (e.g. SOCKS) then this
     * setting is ignored.
     * 
     * @param autoPassiveIPSubstitution true if set to on, false otherwise
     */
    public void setAutoPassiveIPSubstitution(boolean autoPassiveIPSubstitution) {
        this.autoPassiveIPSubstitution = autoPassiveIPSubstitution;
        if (control != null)
            control.setAutoPassiveIPSubstitution(autoPassiveIPSubstitution);
    }
    
    /**
     * Get server wakeup interval in seconds. A value of 0
     * means it is disabled (the default).
     * 
     * @return interval in seconds
     */
    public int getServerWakeupInterval() {
        return serverWakeupInterval;
    }
    
    /**
     * Set server wakeup interval in seconds. A value of 0 
     * means it is disabled (the default). This may hang or confuse 
     * the FTP server - use with caution.
     * 
     * @param interval  interval in seconds
     */
    public void setServerWakeupInterval(int interval) {
        this.serverWakeupInterval = interval;
    }
    
    
    /**
     * Get the encoding used for the control connection
     * 
     * @return Returns the current controlEncoding.
     */
    public String getControlEncoding() {
        return controlEncoding;
    }
    
    /**
     * Get the size of the network buffers (SO_SNDBUF
     * and SO_RCVBUF).
     * 
     * @return int
     */
    public int getNetworkBufferSize() {
        return dataReceiveBufferSize;
    }


    /**
     * Set the size of the network buffers (SO_SNDBUF
     * and SO_RCVBUF).
     * 
     * @param networkBufferSize  new buffer size to set
     */
    public void setNetworkBufferSize(int networkBufferSize) {
        dataReceiveBufferSize = dataSendBufferSize = networkBufferSize;
    }
    
    /**
     * Set the size of the data socket's receive buffer.
     * 
     * @deprecated  see {@link #setNetworkBufferSize(int)}
     * 
     * @param size  must be > 0
     */
    public void setDataReceiveBufferSize(int size) {
        this.dataReceiveBufferSize = size;
    }
    
    /**
     * Get the size of the data socket's receive buffer.
     * A value of 0 means the defaults are being used.
     * 
     * @deprecated  see {@link #getNetworkBufferSize()}
     * 
     * @return  size
     */
    public int getDataReceiveBufferSize() {
        return dataReceiveBufferSize;
    }
    
    /**
     * Set the size of the data socket's send buffer.
     * 
     * @deprecated  see {@link #setNetworkBufferSize(int)}
     * 
     * @param size  must be > 0
     */
    public void setDataSendBufferSize(int size) {
        this.dataSendBufferSize = size;
    }
    
    /**
     * Get the size of the data socket's send buffer.
     * A value of 0 means the defaults are being used.
     * 
     * @deprecated  see {@link #getNetworkBufferSize()}
     * 
     * @return  size
     */
    public int getDataSendBufferSize() {
        return dataSendBufferSize;
    }
    
    
    /**
     * Set the control socket's encoding. Can only do this if
     * not connected
     * 
     * @param controlEncoding The controlEncoding to set, which is the name of a Charset
     * @see java.nio.charset.Charset
     * @throws FTPException
     */
    public void setControlEncoding(String controlEncoding) throws FTPException {
        checkConnection(false);
        this.controlEncoding = controlEncoding;
    }
    /**
     * @return Returns the messageListener.
     */
    public FTPMessageListener getMessageListener() {
        return messageListener;
    }
    
    /**
     * Set a listener that handles all FTP messages
     * 
     * @param listener  message listener
     */
    public void setMessageListener(FTPMessageListener listener) {
        this.messageListener = listener;
        if (control != null)
           control.setMessageListener(listener);
    }
    
    /**
     * Get reference to the transfer listener
     * 
     * @return FTPProgressMonitorEx
     */
    public FTPProgressMonitorEx getProgressMonitorEx() {
        return monitorEx;
    }

    /**
     * Set reference to the transfer listener
     * 
     * @param monitorEx  transfer listener
     */
    public void setProgressMonitorEx(FTPProgressMonitorEx monitorEx) {
        this.monitorEx = monitorEx;
        this.monitor = monitorEx;
    }

    /**
     *  Set the connect mode
     *
     *  @param  mode  ACTIVE or PASV mode
     */
    public void setConnectMode(FTPConnectMode mode) {
        connectMode = mode;
    }
    
    /**
     * @return Returns the connectMode.
     */
    public FTPConnectMode getConnectMode() {
        return connectMode;
    }
    

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#setProgressMonitor(com.enterprisedt.net.ftp.FTPProgressMonitor, long)
     */
    public void setProgressMonitor(FTPProgressMonitor monitor, long interval) {
        this.monitor = monitor;
        this.monitorInterval = interval;
    }    

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#setProgressMonitor(com.enterprisedt.net.ftp.FTPProgressMonitor)
     */
    public void setProgressMonitor(FTPProgressMonitor monitor) {
        this.monitor = monitor;
    }   
    
    /**
     * Get the reference to the progress monitor
     * 
     * @return  progress monitor
     */
    public FTPProgressMonitor getProgressMonitor() {
        return monitor;
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#getMonitorInterval()
     */
    public long getMonitorInterval() {
        return monitorInterval;
    }
    
    /**
     *  Set the number of bytes transferred between each callback on the
     *  progress monitor
     * 
     * param interval     bytes to be transferred before a callback
     */
    public void setMonitorInterval(long interval) {
        this.monitorInterval = interval;
    }
    
    /**
     * Set the size of the buffers used in writing to and reading from
     * the data sockets
     * 
     * @param size  new size of buffer in bytes
     */
    public void setTransferBufferSize(int size) {
        transferBufferSize = size;
    }
    
    /**
     * Get the size of the buffers used in writing to and reading from
     * the data sockets
     * 
     * @return  transfer buffer size
     */
    public int getTransferBufferSize() {
        return transferBufferSize;
    }
    
    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#cancelTransfer()
     */
    public void cancelTransfer() {
        cancelTransfer = true;
        log.warn("cancelTransfer() called");
    } 
    
    /**
     * Has the current transfer been cancelled?
     * 
     * @return true if cancel, false otherwise
     */
    public boolean isTransferCancelled() {
        return cancelTransfer;
    }
    
    /**
     * If true, delete partially written files when exceptions are thrown
     * during a download
     * 
     * @return true if delete local file on error
     */
    public boolean isDeleteOnFailure() {
        return deleteOnFailure;
    }

    /**
     * Switch on or off the automatic deletion of partially written files 
     * that are left when an exception is thrown during a download
     * 
     * @param deleteOnFailure  true if delete when a failure occurs
     */
    public void setDeleteOnFailure(boolean deleteOnFailure) {
        this.deleteOnFailure = deleteOnFailure;
    }
    
    /**
     * We can force PORT to send a fixed IP address, which can be useful with certain
     * NAT configurations. Must be connected to the remote host to call this method.
     * 
     * @param IPAddress     IP address to force, in 192.168.1.0 form
     * @deprecated
     */
    public void setPORTIP(String IPAddress) 
        throws FTPException {
        setActiveIPAddress(IPAddress);
    }
    
    /**
     * We can force PORT to send a fixed IP address, which can be useful with certain
     * NAT configurations. Must be connected to the remote host to call this method.
     * 
     * @param activeIP     IP address to force, in 192.168.1.0 form or in IPV6 form, e.g.
     *                            1080::8:800:200C:417A
     */
    public void setActiveIPAddress(String activeIP) 
        throws FTPException {
        
        this.activeIP = activeIP;
        if (control != null)
            control.setActivePortIPAddress(activeIP);
    }
    
    /**
     * Get the active IP address that is set.
     * 
     * @return  active IP address or null if not set
     */
    public String getActiveIPAddress() {
        return activeIP;
    }
    
    /**
     * Force a certain range of ports to be used in active mode. This is
     * generally so that a port range can be configured in a firewall. Note
     * that if lowest == highest, a single port will be used. This works well
     * for uploads, but downloads generally require multiple ports, as most
     * servers fail to create a connection repeatedly for the same port.
     * 
     * @param lowest     Lower limit of range.
     * @param highest    Upper limit of range.
     */
    public void setActivePortRange(int lowest, int highest) 
        throws FTPException {
                
        this.lowPort = lowest;
        this.highPort = highest;
        
        if (lowest < 0 || lowest > highest || highest > MAX_PORT)
            throw new FTPException("Invalid port range specified");
        
        if (control != null)        
            control.setActivePortRange(lowest, highest);
        
        log.debug("setActivePortRange(" + lowest + "," + highest + ")");
    }
    
    /**
     * Get the lower limit of the port range for active mode.
     * 
     * @return lower limit, or -1 if not set
     */
    public int getActiveLowPort() {
        return lowPort;
    }

    /**
     * Get the upper limit of the port range for active mode.
     * 
     * @return upper limit, or -1 if not set
     */
    public int getActiveHighPort() {
        return highPort;
    }
    
       
    /**
     *  Login into an account on the FTP server. This
     *  call completes the entire login process. Note that
     *  connect() must be called first.
     *
     *  @param   user       user name
     *  @param   password   user's password
     */
    public void login(String user, String password)
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
    	this.user = user;
    	this.password = password;
        
        user(user);

        if (lastValidReply.getReplyCode().equals("230") || lastValidReply.getReplyCode().equals("232"))
            return;
        else {
            password(password);
        }
    }
    
    /**
     *  Login into an account on the FTP server. This call completes the 
     *  entire login process. This method permits additional account information 
     *  to be supplied. FTP servers can use combinations of these parameters in 
     *  many different ways, e.g. to pass in proxy details via this method, some 
     *  servers use the "user" as 'ftpUser + "@" + ftpHost + " " + ftpProxyUser', 
     *  the "password" as the FTP user's password, and the accountInfo as the proxy 
     *  password. Note that connect() must be called first.
     *
     *  @param   user           user name
     *  @param   password       user's password
     *  @param   accountInfo    account info string
     */
    public void login(String user, String password, String accountInfo)
        throws IOException, FTPException {
        
        checkConnection(true);
        
        this.user = user;
        this.password = password;
        
        user(user);

        if (lastValidReply.getReplyCode().equals("230") || lastValidReply.getReplyCode().equals("232")) // no pwd
            return;
        else {
            password(password);
            if (lastValidReply.getReplyCode().equals("332")) // requires acct info
                account(accountInfo);
        }
    }    
    
    /**
     *  Supply the user name to log into an account
     *  on the FTP server. Must be followed by the
     *  password() method - but we allow for no password.
     *  Note that connect() must be called first.
     *
     *  @param   user       user name
     */
    public void user(String user)
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
    	this.user = user;
    	
        lastReply = control.sendCommand("USER " + user);

        // we allow for a site with no password - 230 response
        String[] validCodes = {"230", "232", "331"};
        lastValidReply = control.validateReply(lastReply, validCodes);
    }


    /**
     *  Supplies the password for a previously supplied
     *  username to log into the FTP server. Must be
     *  preceeded by the user() method
     *
     *  @param   password       The password.
     */
    public void password(String password)
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
        this.password = password;
    	
        lastReply = control.sendCommand("PASS " + password);

        // we allow for a site with no passwords (202) or requiring
        // ACCT info (332)
        String[] validCodes = {"230", "202", "332"};
        lastValidReply = control.validateReply(lastReply, validCodes);
    }
    
    
    /**
     *  Supply account information string to the server. This can be
     *  used for a variety of purposes - for example, the server could
     *  indicate that a password has expired (by sending 332 in reply to
     *  PASS) and a new password automatically supplied via ACCT. It
     *  is up to the server how it uses this string.
     *
     *  @param   accountInfo    account information string
     */
    public void account(String accountInfo)
        throws IOException, FTPException {
        
        checkConnection(true);
        
        lastReply = control.sendCommand("ACCT " + accountInfo);

        // ok or not implemented
        String[] validCodes = {"230", "202"};
        lastValidReply = control.validateReply(lastReply, validCodes);
    }


    /**
     *  Set up SOCKS v4/v5 proxy settings. This can be used if there
     *  is a SOCKS proxy server in place that must be connected thru.
     *  Note that setting these properties directs <b>all</b> TCP
     *  sockets in this JVM to the SOCKS proxy
     *
     *  @param  port  SOCKS proxy port
     *  @param  host  SOCKS proxy hostname
     */
    public static void initSOCKS(String port, String host) {
        Properties props = System.getProperties();
        props.put(SOCKS_PORT, port);
        props.put(SOCKS_HOST, host);
        System.setProperties(props);
    }

    /**
     *  Set up SOCKS username and password for SOCKS username/password
     *  authentication. Often, no authentication will be required
     *  but the SOCKS server may be configured to request these.
     *
     *  @param  username   the SOCKS username
     *  @param  password   the SOCKS password
     */
    public static void initSOCKSAuthentication(String username,
                                               String password) {
        Properties props = System.getProperties();
        props.put("java.net.socks.username", username);
        props.put("java.net.socks.password", password);
        System.setProperties(props);
    }
    
    /**
     * Clear SOCKS settings. Note that setting these properties affects 
     * <b>all</b> TCP sockets in this JVM
     */
    public static void clearSOCKS() {
        
        Properties prop = System.getProperties(); 
        prop.remove(SOCKS_HOST); 
        prop.remove(SOCKS_PORT); 
        System.setProperties(prop); 
    }

    /**
     *  Get the name of the remote host
     *
     *  @return  remote host name
     */
    String getRemoteHostName() {
        return control.getRemoteHostName();
    }

    /**
     *  Issue arbitrary ftp commands to the FTP server.
     *
     *  @param command     ftp command to be sent to server
     *  @param validCodes  valid return codes for this command. If null
     *                      is supplied no validation is performed
     * 
     *  @return  the text returned by the FTP server
     */
    public String quote(String command, String[] validCodes)
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
        lastReply = control.sendCommand(command);

        // allow for no validation to be supplied
        if (validCodes != null) {
            lastValidReply = control.validateReply(lastReply, validCodes);
        }
        else { // no validation
            lastValidReply = lastReply; // assume valid
        }
        return lastValidReply.getReplyText();       
    }
    
    /**
     *  Issue arbitrary ftp commands to the FTP server.
     *
     *  @param command     ftp command to be sent to server
     *  @return  the raw text returned by the FTP server including reply code
     */
    public String quote(String command)
        throws IOException, FTPException {
        
        checkConnection(true);
        
        lastValidReply = control.sendCommand(command);        
        return lastValidReply.getRawReply();   
    }
    
    /**
     * Request that the remote server execute the literal command supplied. 
     * This is a synonym for the quote() command.
     * 
     * @param command   command string
     * @return result string by server
     * @throws FTPException
     * @throws IOException
     */
    public String executeCommand(String command) 
        throws FTPException, IOException {
        return quote(command);
    }
    
    /**
     * Use to find out if a file exists or not.
     * Since there is no reliable standard command to check the existence of a file,
     * this method first tries to get the size of the file.  If this fails then it 
     * tries to get the modtime and finally, if that fails will get a directory listing
     * and look for the file in that listing.
     * @param remoteFile File of which to check existence.
     * @return true if directory exists.
     * @throws IOException Thrown if there is a TCP/IP-related error.
     * @throws FTPException Thrown if there is an error related to the FTP protocol. 
     */
    public boolean existsFile(String remoteFile) throws IOException, FTPException {
        checkConnection(true);
        
        // first try the SIZE command
        if (sizeSupported)
        {
            lastReply = control.sendCommand("SIZE " + remoteFile);
            char ch = lastReply.getReplyCode().charAt(0);
            if (ch == '2')
                return true;
            if (ch == '5' && fileNotFoundStrings.matches(lastReply.getReplyText()))
                return false;
                
            sizeSupported = false;
            log.debug("SIZE not supported - trying MDTM");
        }

        // then try the MDTM command
        if (mdtmSupported)
        {
            lastReply = control.sendCommand("MDTM " + remoteFile);
            char ch = lastReply.getReplyCode().charAt(0);
            if (ch == '2')
                return true;
            if (ch == '5' && fileNotFoundStrings.matches(lastReply.getReplyText()))
                return false;
             
            mdtmSupported = false;
            log.debug("MDTM not supported - trying LIST");
        }

        try {
            FTPFile[] files = dirDetails(".");
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().equals(remoteFile)) {
                    if (files[i].isFile())
                        return true;
                    else
                        return false;
                }
            }
            return false;
        }
        catch (ParseException ex) {
            log.warn(ex.getMessage());
            return false;
        }
    }
    
    /**
     * Use to find out if a directory exists or not.
     * Since there is no reliable standard command to check the existence of a directory,
     * this method tries to change into the directory and then changes back to the original 
     * directory.
     * @param remoteDirectory Directory of which to check existence.
     * @return true if directory exists.
     * @throws IOException Thrown if there is a TCP/IP-related error.
     * @throws FTPException Thrown if there is an error related to the FTP protocol. 
     */
    public boolean existsDirectory(String remoteDirectory) throws IOException, FTPException {
        String initDir = pwd();
        try
        {
            chdir(remoteDirectory);
        }
        catch (Exception ex)
        {
            return false;
        }
        chdir(initDir);
        return true;
    }
    
    /**
     * @deprecated Use existsFile(String).
     */
    public boolean exists(String remoteFile) throws IOException, FTPException {
    	return existsFile(remoteFile);
    }
    
    /**
     * Read reply from control socket
     * 
     * @return
     * @throws IOException
     */
    FTPReply readReply() throws IOException, FTPException {
        return control.readReply();
    }
    
    /**
     * Get the PASV address string (including port numbers)
     * @param pasvReply
     * @return
     */
    String getPASVAddress(String pasvReply) {
        int start = -1;
        int i = 0;
        while (i < pasvReply.length()) {
            if (Character.isDigit(pasvReply.charAt(i))) {
                start = i;
                break;
            }
            i++;
        }
        int end = -1;
        i = pasvReply.length() -1;
        while (i >= 0) {
            if (Character.isDigit(pasvReply.charAt(i))) {
                end = i;
                break;
            }
            i--;
        }
        if (start < 0 || end < 0)
            return null;
        
        return pasvReply.substring(start, end+1);
    }
    
    /**
     * Send a command to the server and get the reply
     * @param command   command
     * @return FTPReply
     * @throws IOException
     * @throws FTPException 
     */
    public FTPReply sendCommand(String command) throws IOException, FTPException  {
        return control.sendCommand(command);
    }
    
    /**
     * Validate an FTPReply 
     * 
     * @param reply         reply object
     * @param expectedReplyCode  expected code
     * @throws FTPException
     */
    public void validateReply(FTPReply reply, String expectedReplyCode) throws FTPException {
        control.validateReply(reply, expectedReplyCode);
    }
    
    /**
     * Validate an FTPReply 
     * 
     * @param reply         reply object
     * @param expectedReplyCodes  expected codes
     * @throws FTPException
     */
    public void validateReply(FTPReply reply, String[] expectedReplyCodes) throws FTPException {
        control.validateReply(reply, expectedReplyCodes);
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#size(java.lang.String)
     */
     public long size(String remoteFile)
         throws IOException, FTPException {
     	
     	 checkConnection(true);
     	
         lastReply = control.sendCommand("SIZE " + remoteFile);
         lastValidReply = control.validateReply(lastReply, "213");

         // parse the reply string .
         String replyText = lastValidReply.getReplyText();
         
         // trim off any trailing characters after a space, e.g. webstar
         // responds to SIZE with 213 55564 bytes
         int spacePos = replyText.indexOf(' ');
         if (spacePos >= 0)
             replyText = replyText.substring(0, spacePos);
         
         // parse the reply
         try {
             return Long.parseLong(replyText);
         }
         catch (NumberFormatException ex) {
             throw new FTPException("Failed to parse reply: " + replyText);
         }         
     }
     
     /*
      *  (non-Javadoc)
      * @see com.enterprisedt.net.ftp.FTPClientInterface#resume()
      */
     public void resume() throws FTPException {
         if (transferType.equals(FTPTransferType.ASCII))
             throw new FTPException("Resume only supported for BINARY transfers");
         resume = true;
         log.info("Resume=true");
     }
     
     /*
      *  (non-Javadoc)
      * @see com.enterprisedt.net.ftp.FTPClientInterface#resume()
      */
     public void resumeNextDownload(long offset) throws FTPException {
         resume();
         if (offset < 0)
             throw new FTPException("Offset must be >= 0");
         resumeMarker = offset;
     }
     
     /*
      *  (non-Javadoc)
      * @see com.enterprisedt.net.ftp.FTPClientInterface#cancelResume()
      */
     public void cancelResume() 
         throws IOException, FTPException {
         try {
             restart(0);
         }
         catch (FTPException ex){                
             log.debug("REST failed which is ok (" + ex.getMessage() + ")");
         }
         resumeMarker = 0;
         resume = false;
     }
     
     /**
      * Force the resume flag off. Internal use only.
      */
     protected void forceResumeOff() {
         resume = false;
         resumeMarker = 0;
     }
     
     /**
      * Issue the RESTart command to the remote server. This indicates the byte
      * position that REST is performed at. For put, bytes start at this point, while
      * for get, bytes are fetched from this point.
      * 
      * @param size     the REST param, the mark at which the restart is 
      *                  performed on the remote file. For STOR, this is retrieved
      *                  by SIZE
      * @throws IOException
      * @throws FTPException
      */
     public void restart(long size) 
         throws IOException, FTPException {
         lastReply = control.sendCommand("REST " + size);
         lastValidReply = control.validateReply(lastReply, "350");
     }
     
     
     /**
      * Get the retry count for retrying file transfers. Default
      * is 3 attempts.
      * 
      * @return number of times a transfer is retried
      */
     public int getRetryCount() {
         return retryCount;
     }

     /**
      * Set the retry count for retrying file transfers.
      * 
      * @param retryCount    new retry count
      */
     public void setRetryCount(int retryCount) {
         this.retryCount = retryCount;
     }

     /**
      * Get the retry delay between retry attempts, in milliseconds.
      * Default is 5000.
      * 
      * @return  retry delay in milliseconds
      */
     public int getRetryDelay() {
         return retryDelay;
     }

     /**
      * Set the retry delay between retry attempts, in milliseconds
      * 
      * @param  new retry delay in milliseconds
      */
     public void setRetryDelay(int retryDelay) {
         this.retryDelay = retryDelay;
     }

     
     private boolean processTransferException(Exception ex, int attemptNumber)
     {
         if (attemptNumber <= retryCount+1) {
             if (retryDelay > 0) {
                 try {
                     log.debug("Sleeping for " + retryDelay + " ms prior to retry");
                     Thread.sleep(retryDelay);
                 }
                 catch (InterruptedException ignore) {}
             }
             log.error("Transfer error on attempt #" + attemptNumber
                     + " retrying: ", ex);           
             return true;
         } else {
             if (attemptNumber > 0)
                 log.info("Failed " + attemptNumber + " attempts - giving up");
             return false;
         }
     }
     
     /*
      *  (non-Javadoc)
      * @see com.enterprisedt.net.ftp.FTPClientInterface#get(java.lang.String, java.lang.String)
      */
     public void get(String localPath, String remoteFile)
         throws IOException, FTPException {
         
         String cwd = safePwd();
         
         FTPTransferType previousType = transferType;
         FTPTransferType currentTransferType = chooseTransferMode(remoteFile);
         
         File localFile = new File(localPath);    
         if (localFile.isDirectory()) {
             localPath = localPath + File.separator + remoteFile;
             log.debug("Setting local path to " + localPath);
         }
         
         try {

            if (retryCount == 0)
                getFile(localPath, remoteFile);
            else {
                for (int attempt = 1;; attempt++) {
                    try {
                        if (attempt > 1
                                && getType().equals(FTPTransferType.BINARY))
                            resume();
                        log.debug("Attempt #" + attempt);
                        getFile(localPath, remoteFile);
                        break;
                    } catch (ControlChannelIOException ex) {
                        if (!processControlChannelException(cwd, ex, attempt))
                            throw ex;
                    } catch (MalformedReplyException ex) {
                        throw ex;
                    } catch (IOException ex) {
                        if (!processTransferException(ex, attempt))
                            throw ex;
                    }
                }
            }
         } finally {
            resetTransferMode(previousType);
         }
         
         postTransferChecks(localPath, remoteFile, currentTransferType, false);
     }
     
     /*
      * (non-Javadoc)
      * 
      * @see com.enterprisedt.net.ftp.FTPClientInterface#put(java.io.InputStream,
      *      java.lang.String, boolean)
      */
     public String put(InputStream srcStream, String remoteFile, boolean append)
         throws IOException, FTPException {
         
         FTPTransferType previousType = transferType;
         FTPTransferType currentTransferType = chooseTransferMode(remoteFile);
         
         try {
             return putStream(srcStream, remoteFile, append);
         } finally {
             resetTransferMode(previousType);
         }
     }    
     
     private boolean processControlChannelException(String cwd, Exception ex, int attemptNumber) 
         throws IOException, FTPException {
         if (attemptNumber <= retryCount+1) {
             if (retryDelay > 0) {
                 try {
                     log.debug("Sleeping for " + retryDelay + " ms prior to retry");
                     Thread.sleep(retryDelay);
                 }
                 catch (InterruptedException ignore) {}
             }
             log.error("Transfer error on attempt #" + attemptNumber
                     + ": reconnecting & retrying: ", ex);  
             reconnect(cwd);
             return true;
         } else {
             log.info("Failed " + attemptNumber + " attempts - giving up");
             return false;
         }
     }
     
     
     /**
      * Reconnect to the server
      * 
      * @param cwd      current working dir
      * @throws IOException
      * @throws FTPException
      */
     protected void reconnect(String cwd) throws IOException, FTPException {
         try {
             quitImmediately();
         }
         catch (Exception ignore) {}
         log.info("Reconnecting");
         connect();
         login(user, password);
         setType(transferType);
         if (cwd != null)
             chdir(cwd); // switch to the target directory         
     }


     /*
      *  (non-Javadoc)
      * @see com.enterprisedt.net.ftp.FTPClientInterface#put(java.lang.String, java.lang.String)
      */
    public String put(String localPath, String remoteFile)
        throws IOException, FTPException {

        return put(localPath, remoteFile, false);
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#put(java.io.InputStream, java.lang.String)
     */
    public String put(InputStream srcStream, String remoteFile)
            throws IOException, FTPException {

        return put(srcStream, remoteFile, false);
    }


    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#put(java.lang.String, java.lang.String, boolean)
     */
    public String put(String localPath, String remoteFile, boolean append)
        throws IOException, FTPException {
    	    	     
        String cwd = safePwd();
        
        FTPTransferType previousType = transferType;
        FTPTransferType currentTransferType = chooseTransferMode(remoteFile);

        try {
            InputStream srcStream = null;
            if (retryCount == 0 || append) {
                srcStream = new FileInputStream(localPath);
                remoteFile = putStream(srcStream, remoteFile, append);
            } else {
                for (int attempt = 1;; attempt++) {
                    try {
                        if (attempt > 1
                                && getType().equals(FTPTransferType.BINARY))
                            resume();
                        log.debug("Attempt #" + attempt);
                        srcStream = new FileInputStream(localPath);
                        remoteFile = putStream(srcStream, remoteFile, append);
                        break;
                    } catch (ControlChannelIOException ex) {
                        if (!processControlChannelException(cwd, ex, attempt))
                            throw ex;
                    } catch (MalformedReplyException ex) {
                        throw ex;
                    } catch (IOException ex) {
                        if (!processTransferException(ex, attempt))
                            throw ex;
                    }
                }
            }

        } finally {
            resetTransferMode(previousType);
        }
         
        postTransferChecks(localPath, remoteFile, currentTransferType, append);
        
        return remoteFile;        
     }

    /*
     *  Internal method that puts a stream to the server
     */
    private String putStream(InputStream srcStream, String remoteFile, boolean append)
        throws IOException, FTPException {

        try {        
            if (monitorEx != null)
                monitorEx.transferStarted(TransferDirection.UPLOAD, remoteFile);   
            remoteFile = putData(srcStream, remoteFile, append);
            validateTransfer();
            uploadCount++;
        }
        catch (FTPException ex) {
            throw ex;
        }
        catch (ControlChannelIOException ex) {
            throw ex;       
        }
        catch (IOException ex) {
            validateTransferOnError(ex);
            throw ex;        
        }
        finally {
            if (monitorEx != null)
                monitorEx.transferComplete(TransferDirection.UPLOAD, remoteFile);   
        }
        return remoteFile;
    }

    /**
     * Validate that the put() or get() was successful.  This method is not
     * for general use.
     */
    public void validateTransfer()
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
        // check the control response
        String[] validCodes = {"225", "226", "250"};
        lastReply = control.readReply();
        
        // if we cancelled the transfer throw an exception
        if (cancelTransfer) {
            lastValidReply = lastReply;
            log.warn("Transfer has been cancelled!");
            throw new FTPTransferCancelledException();
        }
        
        lastValidReply = control.validateReply(lastReply, validCodes);
    }
    
    /**
     * Validate a transfer when an error has occurred on the data channel.
     * Set a very short transfer in case things have hung. Set it back
     * at the end.
     * 
     * @throws IOException
     * @throws FTPException
     */
    protected void validateTransferOnError(IOException ex) 
        throws IOException, FTPException {
        
        log.debug("Validate transfer on error after exception", ex);
        checkConnection(true);
        
        control.setTimeout(SHORT_TIMEOUT);
        try {
            validateTransfer();
        }
        catch (Exception e) {
            log.warn("Validate transfer on error failed", e);
        }
        finally {
            control.setTimeout(timeout);
        }
    }
    
    /**
     * Close the data socket
     */
    private void closeDataSocket() {
        if (data != null) {
            try {
                data.close();
                data = null;
            } catch (IOException ex) {
                log.warn("Caught exception closing data socket", ex);
            }
        }
    }
    
       
    /**
     * Close stream for data socket. Not for 
     * general use!
     * 
     * @param stream    stream reference
     */
    protected void closeDataSocket(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } 
            catch (IOException ex) {
                log.warn("Caught exception closing data socket", ex);
            }
        }

        closeDataSocket();
    }
    
    
    /**
     * Close stream for data socket
     * 
     * @param stream    stream reference
     */
    protected void closeDataSocket(OutputStream stream) {
        if (stream != null) {
            try {
                 stream.close();
            } 
            catch (IOException ex) {
                log.warn("Caught exception closing data socket", ex);
            }
        }

        closeDataSocket();
    }
    
    
    /**
     * Set up the data socket
     * 
     * @throws FTPException 
     * @throws IOException 
     */
    protected void setupDataSocket() 
        throws IOException, FTPException {
        
        data = control.createDataSocket(connectMode);
        data.setTimeout(timeout);
        if (dataReceiveBufferSize > 0)
            data.setReceiveBufferSize(dataReceiveBufferSize);
        if (dataSendBufferSize > 0)
            data.setSendBufferSize(dataSendBufferSize);
     }

    /**
     * Request the server to set up the put
     * 
     * @param remoteFile
     *            name of remote file in current directory
     * @param append
     *            true if appending, false otherwise
     */
    protected String initPut(String remoteFile, boolean append)
        throws IOException, FTPException {
    	
    	checkConnection(true);
        
        // if a remote filename isn't supplied, assume STOU is to be used
        boolean storeUnique = (remoteFile == null || remoteFile.length() == 0);
        if (storeUnique) {
            remoteFile = "";
            // check STOU isn't used with append
            if (append) {
                String msg = "A remote filename must be supplied when appending";
                log.error(msg);
                throw new FTPException(msg);
            }
        }
    	
        // reset the cancel flag
        cancelTransfer = false;

        boolean close = false;
        try {
            resumeMarker = 0;
            
            // if resume is requested, we must obtain the size of the
            // remote file 
            if (resume) {
                if (transferType.equals(FTPTransferType.ASCII))
                    throw new FTPException("Resume only supported for BINARY transfers");
                try {                   
                    resumeMarker = size(remoteFile);
                }
                catch (FTPException ex) {
                    resumeMarker = 0;
                    resume = false;
                    log.warn("SIZE failed '" + remoteFile + "' - resume will not be used (" + ex.getMessage() + ")");
                }
            }
            
            // set up data channel
            setupDataSocket();
            
            // issue REST
            if (resume) {
                try {
                    restart(resumeMarker);
                }
                catch (FTPException ex) {
                    resumeMarker = 0;
                    resume = false;
                    log.warn("REST failed - resume will not be used (" + ex.getMessage() + ")");
                }
            }
    
            // send the command to store
            String cmd = append ? "APPE " : (storeUnique ? "STOU" : storeCommand);
            lastReply = control.sendCommand(cmd + remoteFile);
    
            // Can get a 125 or a 150, also allow 350 (for Global eXchange Services server)
            // JScape returns 151
            String[] validCodes = {"125", "150", "151", "350"};
            lastValidReply = control.validateReply(lastReply, validCodes);
            
            String replyText = lastValidReply.getReplyText();
            if (storeUnique) {
                int pos = replyText.indexOf(STOU_FILENAME_MARKER);
                if (pos >= 0) {
                    pos += STOU_FILENAME_MARKER.length();
                    remoteFile = replyText.substring(pos).trim();
                }
                else { // couldn't find marker, just return last word of reply
                       // e.g. 150 Opening BINARY mode data connection for FTP0000004.
                    log.debug("Could not find " + STOU_FILENAME_MARKER + " in reply - using last word instead.");
                    pos = replyText.lastIndexOf(' ');
                    remoteFile = replyText.substring(++pos);
                    int len = remoteFile.length();
                    if (len > 0 && remoteFile.charAt(len-1) == '.') {
                        remoteFile = remoteFile.substring(0, len-1);
                    }  
                }
            }
            return remoteFile;
        }
        catch (IOException ex) {
            close = true;
            log.error("Caught and rethrowing exception in initPut()", ex);
            throw ex;
        }
        catch (FTPException ex) {
            close = true;
            log.error("Caught and rethrowing exception in initPut()", ex);
            throw ex;
        }
        finally {
            if (close) {
                resume = false;
                resumeMarker = 0;
                closeDataSocket();
            }
        }
    }

    /**
     *  Put data. For ASCII, translate line terminators, coping 
     *  with \r\n, \r or \n in the local file
     *
     *  @param  srcStream   input stream of data to put
     *  @param  remoteFile  name of remote file we are writing to
     *  @param  append      true if appending, false otherwise
     */
    private String putData(InputStream srcStream, String remoteFile,
                           boolean append)
        throws IOException, FTPException {

        IOException storedEx = null;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        long size = 0;
        try {
            in = new BufferedInputStream(srcStream);
    
            remoteFile = initPut(remoteFile, append);
    
            // get an output stream
            out = new BufferedOutputStream(
                    new DataOutputStream(getOutputStream()), transferBufferSize*2);
            
            // if resuming, we skip over the unwanted bytes
            if (resume && resumeMarker > 0) {
                in.skip(resumeMarker);
            }
            else
                resumeMarker = 0;
    
            byte[] buf = new byte[transferBufferSize];
            byte[] prevBuf = new byte[FTP_LINE_SEPARATOR.length];
            int matchpos = 0;
    
            // read a chunk at a time and write to the data socket            
            long monitorCount = 0;
            int count = 0;
            boolean isASCII = getType() == FTPTransferType.ASCII;
            long start = System.currentTimeMillis();
            if (throttler != null) {
                throttler.reset();
            }
            
            while ((count = in.read(buf)) > 0 && !cancelTransfer) {
                if (isASCII) { // we want to allow \r\n, \r and \n
                    for (int i = 0; i < count; i++) {
                        // LF without preceding CR (i.e. Unix text file)
                        if (buf[i] == LINE_FEED && matchpos == 0) {
                            out.write(CARRIAGE_RETURN);
                            out.write(LINE_FEED);
                            size += 2;
                            monitorCount += 2;
                        }
                        else if (buf[i] == FTP_LINE_SEPARATOR[matchpos]) {
                            prevBuf[matchpos] = buf[i];
                            matchpos++;
                            if (matchpos == FTP_LINE_SEPARATOR.length) {
                                out.write(CARRIAGE_RETURN);
                                out.write(LINE_FEED);
                                size += 2;
                                monitorCount += 2;
                                matchpos = 0;
                            }
                        }
                        else { // no match current char 
                            // this must be a matching \r if we matched first char
                            if (matchpos > 0) {
                                out.write(CARRIAGE_RETURN);
                                out.write(LINE_FEED);
                                size += 2;
                                monitorCount += 2;
                            }
                            out.write(buf[i]);
                            size++;
                            monitorCount++;
                            matchpos = 0;
                        }                              
                    }
                }
                else { // binary
                    out.write(buf, 0, count);
                    size += count;
                    monitorCount += count;
                }
                
                if (throttler != null) {
                    throttler.throttleTransfer(size);
                }
                                    
                if (monitor != null && monitorCount > monitorInterval) {
                    monitor.bytesTransferred(size); 
                    monitorCount = 0;  
                }
                if (serverWakeupInterval > 0 && System.currentTimeMillis() - start > serverWakeupInterval*1000) {
                    start = System.currentTimeMillis();
                    sendServerWakeup();
                }
            }
            // write out anything left at the end that has been saved
            // - must be a \r which we convert into a line terminator
            if (isASCII && matchpos > 0) {
                out.write(CARRIAGE_RETURN);
                out.write(LINE_FEED);
                size += 2;
                monitorCount += 2;
            }
        }
        catch (IOException ex) {
            storedEx = ex;
            log.error("Caught and rethrowing exception in getDataAfterInitGet()", ex);
        }
        finally {
            resume = false;
            resumeMarker = 0;
            try {
                if (in != null)
                    in.close();
            }
            catch (IOException ex) {
                log.warn("Caught exception closing input stream", ex);
            }
                
            closeDataSocket(out);
            
            // if we failed to write the file, rethrow the exception
            if (storedEx != null)
                throw storedEx;
            
            // notify the final transfer size
            if (monitor != null)
                monitor.bytesTransferred(size);  
            // log bytes transferred
            log.debug("Transferred " + size + " bytes to remote host");           
        }
        return remoteFile;
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#put(byte[], java.lang.String)
     */
    public String put(byte[] bytes, String remoteFile)
        throws IOException, FTPException {

        return put(bytes, remoteFile, false);
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#put(byte[], java.lang.String, boolean)
     */
    public String put(byte[] bytes, String remoteFile, boolean append)
        throws IOException, FTPException {
        
        String cwd = safePwd();   
        
        FTPTransferType previousType = transferType;
        FTPTransferType currentTransferType = chooseTransferMode(remoteFile);
        
        String result = null;       
        try {

            ByteArrayInputStream input = null;
            if (retryCount == 0 || append) {
                input = new ByteArrayInputStream(bytes);
                result = putStream(input, remoteFile, append);
            } else {
                for (int attempt = 1;; attempt++) {
                    try {
                        if (attempt > 1
                                && getType().equals(FTPTransferType.BINARY))
                            resume();
                        log.debug("Attempt #" + attempt);
                        input = new ByteArrayInputStream(bytes);
                        result = putStream(input, remoteFile, append);
                        break;
                    } catch (ControlChannelIOException ex) {
                        if (!processControlChannelException(cwd, ex, attempt))
                            throw ex;
                    } catch (MalformedReplyException ex) {
                        throw ex;
                    } catch (IOException ex) {
                        if (!processTransferException(ex, attempt))
                            throw ex;
                    }
                }
            }
        } finally {
            resetTransferMode(previousType);
        }   
        postTransferChecks(bytes, remoteFile, currentTransferType, append);
        
        return result; 
    }

    /*
     * Internal method that gets a file
     */
    private void getFile(String localPath, String remoteFile)
        throws IOException, FTPException {

        try {        
            if (monitorEx != null)
                monitorEx.transferStarted(TransferDirection.DOWNLOAD, remoteFile);
            getData(localPath, remoteFile);
            validateTransfer();
            downloadCount++;
        }
        catch (FTPException ex) {
            throw ex;
        }
        catch (ControlChannelIOException ex) {
            throw ex;       
        }
        catch (IOException ex) {
            validateTransferOnError(ex);
            throw ex;        
        }
        finally {
            if (monitorEx != null)
                monitorEx.transferComplete(TransferDirection.DOWNLOAD, remoteFile);   
        }
    }
    
    /**
     * Can be overridden by subclasses to do any necessary post transfer 
     * checking.
     * 
     * @param localPath         local file
     * @param remotePath        remote file
     * @param transferType      binary or ASCII
     * @param append            
     */
    protected void postTransferChecks(String localPath, String remotePath, 
            FTPTransferType transferType, boolean append) throws FTPException, IOException {        
    }
    
    /**
     * Can be overridden by subclasses to do any necessary post transfer 
     * checking.
     * 
     * @param localBytes        local bytes to transfer
     * @param remotePath        remote file
     * @param transferType      binary or ASCII
     * @param append            
     */
    protected void postTransferChecks(byte[] localBytes, String remotePath, 
            FTPTransferType transferType, boolean append) throws FTPException, IOException {        
    }    

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#get(java.io.OutputStream, java.lang.String)
     */
    public void get(OutputStream destStream, String remoteFile)
        throws IOException, FTPException {

        FTPTransferType previousType = transferType;
        chooseTransferMode(remoteFile);
        boolean resetMode = true;
        try {        
            if (monitorEx != null)
                monitorEx.transferStarted(TransferDirection.DOWNLOAD, remoteFile);
            getData(destStream, remoteFile);
            validateTransfer();
            downloadCount++;
        }
        catch (FTPException ex) {
            throw ex;
        }
        catch (ControlChannelIOException ex) {
            throw ex;       
        }
        catch (IOException ex) {
            resetMode = false;
            validateTransferOnError(ex);
            throw ex;        
        }
        finally {
            if (monitorEx != null)
                monitorEx.transferComplete(TransferDirection.DOWNLOAD, remoteFile);
            if (resetMode)
                resetTransferMode(previousType);
        }
    }
    
    
    /**
     * Reset the transfer mode back to what it should be, if 
     * it has changed.
     * 
     * @param previousType      previous transfer type
     * @throws IOException
     * @throws FTPException
     */
    public void resetTransferMode(FTPTransferType previousType) 
        throws IOException, FTPException {
        
        if (!transferType.equals(previousType)) {
            setType(previousType);
        }
    }


    /**
     *  Request to the server that the get is set up
     *
     *  @param  remoteFile  name of remote file
     */
    protected void initGet(String remoteFile)
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
        // reset the cancel flag
        cancelTransfer = false;            

        boolean close = false;
        try {
            // set up data channel
            setupDataSocket();
            
            // if resume is requested, we must issue REST
            if (resume) {
                if (transferType.equals(FTPTransferType.ASCII))
                    throw new FTPException("Resume only supported for BINARY transfers");
                try {
                    restart(resumeMarker);
                }
                catch (FTPException ex) {
                    resumeMarker = 0;
                    resume = false;
                    log.warn("REST failed - resume will not be used (" + ex.getMessage() + ")");
                }
            }
            else
                resumeMarker = 0;
    
            // send the retrieve command
            lastReply = control.sendCommand("RETR " + remoteFile);
    
            // Can get a 125 or a 150
            String[] validCodes1 = {"125", "150"};
            lastValidReply = control.validateReply(lastReply, validCodes1);
        }
        catch (IOException ex) {
            close = true;
            log.error("Caught and rethrowing exception in initGet()", ex);
            throw ex;
        }
        catch (FTPException ex) {
            close = true;
            log.error("Caught and rethrowing exception in initGet()", ex);
            throw ex;
        }
        finally {
            if (close) {
                resume = false;
                resumeMarker = 0;
                closeDataSocket();
            }
        }
    }
    

    /**
     *  Get as binary file, i.e. straight transfer of data
     *
     *  @param localPath   full path of local file to write to
     *  @param remoteFile  name of remote file
     */
    private void getData(String localPath, String remoteFile)
        throws IOException, FTPException {

        // B. McKeown: Need to store the local file name so the file can be
        // deleted if necessary.
        File localFile = new File(localPath);    
        if (localFile.exists()) {
            // if resuming, we must find the marker
            if (!localFile.canWrite())
                throw new FTPException(localPath
                        + " is readonly - cannot write");
            
            if (resume) {
                if (resumeMarker == 0)
                    resumeMarker = localFile.length();
                else
                    log.debug("Resume marker already set explicitly: " + resumeMarker);
            }
            else
                resumeMarker = 0;
        }

        // B.McKeown:
        // Call initGet() before creating the FileOutputStream.
        // This will prevent being left with an empty file if a FTPException
        // is thrown by initGet().
        initGet(remoteFile);

        // create the buffered output stream for writing the file
        FileOutputStream out =
                new FileOutputStream(localPath, resume);
        
        // get a write lock if possible
        if (fileLockingEnabled) {
            String msg = "Failed to obtain an exclusive write lock: " + localPath;
            try {
                if (out.getChannel().tryLock() == null) {
                    log.warn(msg);
                }
            }
            catch (Exception ex) {
                log.warn(msg);
            }
        }
        
        try {
            getDataAfterInitGet(out);
        }
        catch (IOException ex) {
            if (deleteOnFailure) {
                localFile.delete();
                log.debug("Deleting local file '" + localFile.getAbsolutePath() + "'");
            }
            else {
                log.debug("Possibly partial local file not deleted");
            }
            throw ex;
        }        
    }

    /**
     *  Get as binary file, i.e. straight transfer of data
     *
     *  @param destStream  stream to write to
     *  @param remoteFile  name of remote file
     */
    private void getData(OutputStream destStream, String remoteFile)
        throws IOException, FTPException {

        initGet(remoteFile);        
        getDataAfterInitGet(destStream);
    }
    
    /**
     * Get the data input stream. Not for general use!
     * 
     * @return
     * @throws IOException
     */
    protected InputStream getInputStream() throws IOException {
        return data.getInputStream();
    }
    
    /**
     * Get the data input stream. Not for general use!
     * 
     * @return
     * @throws IOException
     */
    protected OutputStream getOutputStream() throws IOException {
        return data.getOutputStream();
    }
    
    
    /**
     *  Get as binary file, i.e. straight transfer of data
     *
     *  @param destStream  stream to write to
     */
    private void getDataAfterInitGet(OutputStream destStream)
        throws IOException, FTPException {

        // create the buffered output stream for writing the file
        BufferedOutputStream out =
            new BufferedOutputStream(destStream);
        
        BufferedInputStream in = null;
        long size = 0;
        IOException storedEx = null;
        try {
            // get an input stream to read data from ... AFTER we have
            // the ok to go ahead AND AFTER we've successfully opened a
            // stream for the local file
            in = new BufferedInputStream(
                    new DataInputStream(getInputStream()));
        
            // do the retrieving
            long monitorCount = 0; 
            byte [] chunk = new byte[transferBufferSize];
            int count;
            boolean isASCII = getType() == FTPTransferType.ASCII;
            long start = System.currentTimeMillis();
            if (throttler != null) {
                throttler.reset();
            }

            byte[] prevBuf = new byte[FTP_LINE_SEPARATOR.length];
            int matchpos = 0;

            // read from socket & write to file in chunks        
            while ((count = readChunk(in, chunk, transferBufferSize)) >= 0 && !cancelTransfer) {
                if (isASCII) {
                    for (int i = 0; i < count; i++) {
                        if (chunk[i] == FTP_LINE_SEPARATOR[matchpos]) {
                            prevBuf[matchpos] = chunk[i];
                            matchpos++;
                            if (matchpos == FTP_LINE_SEPARATOR.length) {
                                out.write(LINE_SEPARATOR);
                                size += LINE_SEPARATOR.length;
                                monitorCount += LINE_SEPARATOR.length;
                                matchpos = 0;
                            }
                        }
                        else { // no match
                            // write out existing matches
                            if (matchpos > 0) {
                                out.write(prevBuf, 0, matchpos);
                                size += matchpos;
                                monitorCount += matchpos;
                            }
                            out.write(chunk[i]);
                            size++;
                            monitorCount++;
                            matchpos = 0;
                        }                              
                    }                
                }
                else { // binary
                    out.write(chunk, 0, count);
                    size += count;
                    monitorCount += count;
                }
                
                if (throttler != null) {
                    throttler.throttleTransfer(size);
                }
                
                if (monitor != null && monitorCount > monitorInterval) {
                    monitor.bytesTransferred(size); 
                    monitorCount = 0;  
                }    
    
                if (serverWakeupInterval > 0 && System.currentTimeMillis() - start > serverWakeupInterval*1000) {
                    start = System.currentTimeMillis();
                    sendServerWakeup();
                }
            }            
            
            // write out anything left at the end that has been saved
            if (isASCII && matchpos > 0) {
                out.write(prevBuf, 0, matchpos);
                size += matchpos;
                monitorCount += matchpos;
            }
        }
        catch (IOException ex) {
            storedEx = ex;
            log.error("Caught and rethrowing exception in getDataAfterInitGet()", ex);
        }
        finally {
            try {
                if (out != null)
                    out.close();
            }
            catch (IOException ex) {
                log.warn("Caught exception closing output stream", ex);
            }

            resume = false;
            resumeMarker = 0;
    
            // close streams
            closeDataSocket(in);
    
            // if we failed to write the file, rethrow the exception
            if (storedEx != null)
                throw storedEx;
            else if (monitor != null)
                monitor.bytesTransferred(size);  
    
            // log bytes transferred
            log.debug("Transferred " + size + " bytes from remote host");
        }
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#get(java.lang.String)
     */
    public byte[] get(String remoteFile)
        throws IOException, FTPException {

        FTPTransferType previousType = transferType;
        chooseTransferMode(remoteFile);
        boolean resetMode = true;
        try {        
            if (monitorEx != null)
                monitorEx.transferStarted(TransferDirection.DOWNLOAD, remoteFile);
            ByteArrayOutputStream result = new ByteArrayOutputStream(transferBufferSize);
            getData(result, remoteFile);
            validateTransfer();
            downloadCount++;
            return result == null ? null : result.toByteArray();
        }
        catch (FTPException ex) {
            throw ex;
        }
        catch (ControlChannelIOException ex) {
            throw ex;       
        }
        catch (IOException ex) {
            resetMode = false;
            validateTransferOnError(ex);
            throw ex;        
        }
        finally {
            if (monitorEx != null)
                monitorEx.transferComplete(TransferDirection.DOWNLOAD, remoteFile);
            if (resetMode)
                resetTransferMode(previousType);
        }
    }


    /**
     *  Run a site-specific command on the
     *  server. Support for commands is dependent
     *  on the server
     *
     *  @param  command   the site command to run
     *  @return true if command ok, false if
     *          command not implemented
     */
    public boolean site(String command)
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
        // send the retrieve command
        lastReply = control.sendCommand("SITE " + command);

        // Can get a 200 (ok) or 202 (not impl). Some
        // FTP servers return 502 (not impl). Added 250 for leitch
        String[] validCodes = {"200", "202", "250", "502"};
        lastValidReply = control.validateReply(lastReply, validCodes);

        // return true or false? 200 is ok, 202/502 not
        // implemented
        if (lastReply.getReplyCode().equals("200"))
            return true;
        else
            return false;
    }


    /**
     *  List a directory's contents
     *
     *  @param  dirname  the name of the directory (<b>not</b> a file mask)
     *  @return a string containing the line separated
     *          directory listing
     *  @deprecated  As of FTP 1.1, replaced by {@link #dir(String)}
     */
    public String list(String dirname)
        throws IOException, FTPException {

        return list(dirname, false);
    }


    /**
     *  List a directory's contents as one string. A detailed
     *  listing is available, otherwise just filenames are provided.
     *  The detailed listing varies in details depending on OS and
     *  FTP server.
     *
     *  @param  dirname  the name of the directory(<b>not</b> a file mask)
     *  @param  full     true if detailed listing required
     *                   false otherwise
     *  @return a string containing the line separated
     *          directory listing
     *  @deprecated  As of FTP 1.1, replaced by {@link #dir(String,boolean)}
     */
    public String list(String dirname, boolean full)
        throws IOException, FTPException {

        String[] list = dir(dirname, full);

        StringBuffer result = new StringBuffer();
        String sep = System.getProperty("line.separator");

        // loop thru results and make into one string
        for (int i = 0; i < list.length; i++) {
            result.append(list[i]);
            result.append(sep);
        }

        return result.toString();
    }
    
    
    /**
     * Override the chosen file factory with a user created one - meaning
     * that a specific parser has been selected
     * 
     * @param fileFactory
     */
    public void setFTPFileFactory(FTPFileFactory fileFactory) {
        this.fileFactory = fileFactory;
        log.debug("Set new FTPFileFactory: " + fileFactory.toString());
    }
    
    /**
     * Set the locale for date parsing of dir listings
     * 
     * @param locale    new locale to use
     * @deprecated @see FTPClient#setParserLocales(Locale[])
     */
    public void setParserLocale(Locale locale) {
        listingLocales = new Locale[1];
        listingLocales[0] = locale;
    }
    
    /**
     * Set the list of locales to be tried for date parsing of dir listings
     * 
     * @param locales    locales to use
     */
    public void setParserLocales(Locale[] locales) {
        listingLocales = locales;
    }
    
    /**
     * Uses the MLST command to find out details about the
     * named file. A single filename should be supplied. Note
     * that the MLST command is not supported by many servers, and
     * the fallback is to use the SIZE and MDTM commands.
     * 
     * @param name   name of a file
     * @return  if it exists, an FTPFile object
     */
    public FTPFile fileDetails(String name)  
        throws IOException, FTPException, ParseException {
        
        checkConnection(true);
        
        try {
            lastReply = control.sendCommand("MLST " + name);
            lastValidReply = control.validateReply(lastReply, "250");
            String[] data = lastReply.getReplyData();
            if (data != null && data.length >= 2)
                return mlsxParser.parse(lastReply.getReplyData()[1]);
            else
                throw new FTPException("Failed to retrieve data");
        }
        catch (IOException ex1) {
            throw ex1;
        }
        catch (Exception ex1) {
            log.debug("MLST failed: " + ex1.getMessage() + " Trying SIZE");
            try {
                String wd = safePwd();
                long size = size(name);
                Date lastModified = modtime(name);
                FTPFile file = new FTPFile("");
                file.setName(name);
                file.setLastModified(lastModified);
                file.setSize(size);
                file.setPath(wd);
                return file;
            }
            catch (FTPException ex2) {
                String msg = "Failed to retrieve file details for " + name + ": " + ex2.getMessage();
                log.debug(msg);
                throw new FTPException(msg);
            }
        }
    }
    
    /**
     * Internal use only 
     */
    interface DirectoryCallback {
        
        public DirectoryListArgument listEntry(String entry) throws ParseException;
    }
    
    /**
     * Internal use only 
     */
    class DirectoryCallbackImpl implements DirectoryCallback {

        private FTPFileFactory fileFactory;
        private DirectoryListCallback lister;
        private String path;
        
        DirectoryCallbackImpl(FTPFileFactory fileFactory, DirectoryListCallback lister, String path) {
            this.fileFactory = fileFactory;
            this.lister = lister;
            this.path = path;
        }
        
        public DirectoryListArgument listEntry(String entry) throws ParseException {
            FTPFile file = fileFactory.parse(entry);
             if (lister != null && file != null) {
                file.setPath(path);
                DirectoryListArgument arg = new DirectoryListArgument(file);
                lister.listDirectoryEntry(arg);
                return arg;
            }
            return null;
        }
    }
    
    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#dirDetails(java.lang.String,com.enterprisedt.net.ftp.DirectoryListCallback)
     */
    public void dirDetails(String dirname, DirectoryListCallback lister) 
        throws IOException, FTPException, ParseException {

        String path = setupDirDetails(dirname);      
        DirectoryCallbackImpl callback = new DirectoryCallbackImpl(fileFactory, lister, path);        
        dir(dirname, true, null, callback);
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#dirDetails(java.lang.String)
     */
    public FTPFile[] dirDetails(String dirname) 
        throws IOException, FTPException, ParseException {
        
        String path = setupDirDetails(dirname);
                
        // get the details and parse. Set the directory for each file
        FTPFile[] result = fileFactory.parse(dir(dirname, true));
        if (path != null) {
            for (int i = 0; i < result.length; i++) {
                result[i].setPath(path);
            }
        }
                
        return result;
    }
    
    /**
     * Setup the dirDetails method
     * 
     * @param dirname
     * @return
     * @throws FTPException
     * @throws IOException
     */
    private String setupDirDetails(String dirname) throws FTPException, IOException {
        // create the factory
        if (fileFactory == null) {
            try {
                fileFactory = new FTPFileFactory(system());
            }
            catch (FTPException ex) {
                log.warn("SYST command failed - setting Unix as default parser", ex);
                fileFactory = new FTPFileFactory(FTPFileFactory.UNIX_STR);
            }
        }
        fileFactory.setLocales(listingLocales);
        
        String path = safePwd();
        
        // add dirname to path if it looks like a directory name
        // and has no obvious wildcards
        if (path != null && dirname != null && dirname.length() > 0 && 
                dirname.indexOf('*') < 0 && dirname.indexOf('?') < 0 && 
                !dirname.equals(".")) {

            path += "/" + dirname;
        }
        log.debug("setupDirDetails("+ dirname + ") returning: " + path);
        
        return path;
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#dir()
     */
    public String[] dir()
        throws IOException, FTPException {

        return dir(null, false);
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#dir(java.lang.String)
     */
    public String[] dir(String dirname)
        throws IOException, FTPException {

        return dir(dirname, false);
    }
    
    
    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#dir(java.lang.String, boolean)
     */
    private void dir(String dirname, boolean full, Vector lines, DirectoryCallback lister)
        throws IOException, FTPException, ParseException {
        
        checkConnection(true);
        
        // reset the cancel flag
        cancelTransfer = false;
        
        try {
            // set up data channel
            setupDataSocket();
    
            // send the retrieve command
            String command = full ? "LIST ":"NLST ";
            if (dirname != null)
                command += dirname;
    
            // some FTP servers bomb out if NLST has whitespace appended
            command = command.trim();
            lastReply = control.sendCommand(command);
    
            // check the control response. wu-ftp returns 550 if the
            // directory is empty, so we handle 550 appropriately. Similarly
            // proFTPD returns 450 or 226 (depending on NLST or LIST)
            String[] validCodes1 = {"125", "150", "226", "450", "550"};
            lastValidReply = control.validateReply(lastReply, validCodes1);  
    
            // an empty array of files for 450/550
            String[] result = new String[0];
            
            // a normal reply ... extract the file list
            String replyCode = lastValidReply.getReplyCode();
            if (!replyCode.equals("450") && !replyCode.equals("550") && !replyCode.equals("226")) {
                // get a character input stream to read data from .
                LineNumberReader in = null;
                 try {
                    in = new LineNumberReader(
                            new InputStreamReader(getInputStream(), controlEncoding));
    
                    // read a line at a time
                    String line = null;
                    while ((line = readLine(in)) != null && !cancelTransfer) {
                        if (lines != null)
                            lines.addElement(line);
                        if (lister != null) {
                            DirectoryListArgument arg = lister.listEntry(line);
                            if (arg != null && arg.isListingAborted()) {
                                log.warn("Aborting listing");
                                cancelTransfer = true;
                            }
                        }
                        log.log(Level.ALL, line, null);
                    }
                }
                catch (IOException ex) {
                    validateTransferOnError(ex);
                    throw ex;
                }
                finally {
                    try {
                        if (in != null)
                            in.close();
                    }
                    catch (IOException ex) {
                        log.error("Failed to close socket in dir()", ex);
                    }
                    closeDataSocket();
                }
                    
                // check the control response
                String[] validCodes2 = {"226", "250"};
                lastReply = control.readReply();
                lastValidReply = control.validateReply(lastReply, validCodes2);
    
                // empty array is default
                if (lines != null && !lines.isEmpty()) {
                    result = new String[lines.size()];
                    lines.copyInto(result);
                }
            }
            else { // throw exception if not "No files" or other message
                String replyText = lastValidReply.getReplyText().toUpperCase();
                if (!dirEmptyStrings.matches(replyText)
                        && !transferCompleteStrings.matches(replyText))
                    throw new FTPException(lastReply);
            }
        }
        finally {
            closeDataSocket();
        }        
    }
    

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#dir(java.lang.String, boolean)
     */
    public String[] dir(String dirname, boolean full)
        throws IOException, FTPException {
        
        Vector lines = new Vector();
        try {
            dir(dirname, full, lines, null);
        }
        catch (ParseException ignore) {}
        
        // empty array is default
        String[] result = new String[0];
        if (!lines.isEmpty()) {
            result = new String[lines.size()];
            lines.copyInto(result);
        }
        
        return result;
    }
    
    /**
	 * Attempts to read a specified number of bytes from the given
	 * <code>InputStream</code> and place it in the given byte-array. The
	 * purpose of this method is to permit subclasses to execute any additional
	 * code necessary when performing this operation.
	 * 
	 * @param in
	 *            The <code>InputStream</code> to read from.
	 * @param chunk
	 *            The byte-array to place read bytes in.
	 * @param chunksize
	 *            Number of bytes to read.
	 * @return Number of bytes actually read.
	 * @throws IOException
	 *             Thrown if there was an error while reading.
	 */
    public int readChunk(BufferedInputStream in, byte[] chunk, int chunksize) 
    	throws IOException {
    		
    	return in.read(chunk, 0, chunksize);
    }
    
    /**
     * Attempts to read a specified number of bytes from the given
     * <code>InputStream</code> and place it in the given byte-array. The
     * purpose of this method is to permit subclasses to execute any additional
     * code necessary when performing this operation.
     * 
     * @param in
     *            The <code>InputStream</code> to read from.
     * @param chunk
     *            The byte-array to place read bytes in.
     * @param offset
     *            Offset into chunk
     * @param chunksize
     *            Number of bytes to read.
     * @return Number of bytes actually read.
     * @throws IOException
     *             Thrown if there was an error while reading.
     */
    public int readChunk(BufferedInputStream in, byte[] chunk, int offset, int chunksize) 
        throws IOException {
            
        return in.read(chunk, offset, chunksize);
    }
    
    /**
     * Attempts to read a single character from the given <code>InputStream</code>. 
     * The purpose of this method is to permit subclasses to execute
     * any additional code necessary when performing this operation. 
     * @param in The <code>LineNumberReader</code> to read from.
     * @return The character read.
     * @throws IOException Thrown if there was an error while reading.
     */
    protected int readChar(LineNumberReader in) 
    	throws IOException {
    		
    	return in.read();
    }
    
    /**
     * Attempts to read a single line from the given <code>InputStream</code>. 
     * The purpose of this method is to permit subclasses to execute
     * any additional code necessary when performing this operation. 
     * @param in The <code>LineNumberReader</code> to read from.
     * @return The string read.
     * @throws IOException Thrown if there was an error while reading.
     */
    protected String readLine(LineNumberReader in) 
    	throws IOException {
    		
    	return in.readLine();
    }

    /**
     *  Gets the latest valid reply from the server
     *
     *  @return  reply object encapsulating last valid server response
     */
    public FTPReply getLastValidReply() {
        return lastValidReply;
    }

    /**
     *  Gets the last reply from the server, whether valid or not
     *
     *  @return  reply object encapsulating last server response
     */
    public FTPReply getLastReply() {
        return lastReply;
    }

    /**
     *  Get the current transfer type
     *
     *  @return  the current type of the transfer,
     *           i.e. BINARY or ASCII
     */
    public FTPTransferType getType() {
        return transferType;
    }

    /**
     *  Set the transfer type
     *
     *  @param  type  the transfer type to
     *                set the server to
     */
    public void setType(FTPTransferType type)
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
        // determine the character to send
        String typeStr = FTPTransferType.ASCII_CHAR;
        if (type.equals(FTPTransferType.BINARY))
            typeStr = FTPTransferType.BINARY_CHAR;

        // send the command
        String[] validCodes = {"200", "250"};
        lastReply = control.sendCommand("TYPE " + typeStr);
        lastValidReply = control.validateReply(lastReply, validCodes);

        // record the type
        transferType = type;
    }

    
    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#delete(java.lang.String)
     */
    public void delete(String remoteFile)
        throws IOException, FTPException {
    	
    	checkConnection(true);
        String[] validCodes = {"200", "250"};
        lastReply = control.sendCommand("DELE " + remoteFile);
        lastValidReply = control.validateReply(lastReply, validCodes);
        deleteCount++;
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#rename(java.lang.String, java.lang.String)
     */
    public void rename(String from, String to)
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
        lastReply = control.sendCommand("RNFR " + from);
        lastValidReply = control.validateReply(lastReply, "350");

        lastReply = control.sendCommand("RNTO " + to);
        lastValidReply = control.validateReply(lastReply, "250");
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#rmdir(java.lang.String)
     */
    public void rmdir(String dir)
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
        lastReply = control.sendCommand("RMD " + dir);

        // some servers return 200,257, technically incorrect but
        // we cater for it ...
        String[] validCodes = {"200", "250", "257"};
        lastValidReply = control.validateReply(lastReply, validCodes);
    }


    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#mkdir(java.lang.String)
     */
    public void mkdir(String dir)
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
        lastReply = control.sendCommand("MKD " + dir);
        
        // some servers return 200,257, technically incorrect but
        // we cater for it ...
        String[] validCodes = {"200", "250", "257"};
        lastValidReply = control.validateReply(lastReply, validCodes);
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#chdir(java.lang.String)
     */
    public void chdir(String dir)
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
        lastReply = control.sendCommand("CWD " + dir);
        lastValidReply = control.validateReply(lastReply, "250");
    }
    
    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#cdup()
     */
    public void cdup()
        throws IOException, FTPException {
        
        checkConnection(true);
        
        if (cdupSupported) {       
            lastReply = control.sendCommand("CDUP");
            String[] validCodes = {"200", "250"};       
            try {
                lastValidReply = control.validateReply(lastReply, validCodes);
            }
            catch (FTPException ex) {
                cdupSupported = false;
                log.debug("CDUP failed: " + ex.getMessage() + ". Trying CD");
            }
        }
        if (!cdupSupported) {
            chdir("..");
        }   
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#modtime(java.lang.String)
     */
    public Date modtime(String remoteFile)
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
        lastReply = control.sendCommand("MDTM " + remoteFile);
        lastValidReply = control.validateReply(lastReply, "213");

        // parse the reply string ...
        Date ts = tsFormat.parse(lastValidReply.getReplyText(),
                                 new ParsePosition(0));
        return ts;
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#setModTime(java.lang.String)
     */
    public void setModTime(String remoteFile, Date modTime)
        throws IOException, FTPException {
        
        checkConnection(true);
        
        String time = tsFormat.format(modTime);
        lastReply = control.sendCommand("MFMT " + time + " " + remoteFile);
        lastValidReply = control.validateReply(lastReply, "213");
    }

    
    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#pwd()
     */
    public String pwd()
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
        lastReply = control.sendCommand("PWD");
        lastValidReply = control.validateReply(lastReply, "257");

        // get the reply text and extract the dir
        // listed in quotes, if we can find it. Otherwise
        // just return the whole reply string
        String text = lastValidReply.getReplyText();
        int start = text.indexOf('"');
        int end = text.lastIndexOf('"');
        if (start >= 0 && end > start)
            return text.substring(start+1, end);
        else
            return text;
    }
    
    private String safePwd() throws IOException {
        String result = null;
        try {
            result = pwd();
        }
        catch (FTPException ex) {
            log.debug("Ignoring exception: " + ex.getMessage());
        }
        return result;
    }
    
    
    /**
     *  Get the server supplied features
     *
     *  @return   string containing server features, or null if no features or not
     *             supported
     */
    public String[] features()
        throws IOException, FTPException {
        
        checkConnection(true);
        
        lastReply = control.sendCommand("FEAT");
        String[] validCodes = {"211", "500", "502"};
        lastValidReply = control.validateReply(lastReply, validCodes);
        if (lastValidReply.getReplyCode().equals("211")) {
            
            String[] features = null;
            String[] data = lastValidReply.getReplyData();
            if (data != null && data.length > 2) {
                
                features = new String[data.length-2];
                for (int i = 0; i < data.length-2; i++)
                    features[i] = data[i+1].trim();
            }
            else {// no features but command supported
                features = new String[0];
            }
            return features;
        }
        else
            throw new FTPException(lastReply);
    }
    
    /**
     *  Get the type of the OS at the server
     *
     *  @return   the type of server OS
     */
    public String system()
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
        lastReply = control.sendCommand("SYST");
        String[] validCodes = {"200", "213", "215", "250"}; // added 250 for leitch
        lastValidReply = control.validateReply(lastReply, validCodes);
        return lastValidReply.getReplyText();
    }    
    
    /**
     *  Send a "no operation" message that does nothing. Can be
     *  called periodically to prevent the connection timing out
     */
    public void noOperation()
        throws IOException, FTPException {
        
        checkConnection(true);
        
        lastReply = control.sendCommand("NOOP");
        String[] validCodes = {"200", "250"}; // added 250 for leitch
        lastValidReply = control.validateReply(lastReply, validCodes);
    }

    /**
     *  Sends stat message to enquire about the status of a
     *  transfer. 
     */
    public String stat()
        throws IOException, FTPException {
        
        checkConnection(true);
        
        lastReply = control.sendCommand("STAT");
        String[] validCodes = {"211", "212", "213"};
        lastValidReply = control.validateReply(lastReply, validCodes);
        return lastValidReply.getReplyText();
    }
    
    /**
     * Wake up the server during a transfer to prevent a
     * timeout from occuring. This may hang or confuse the server -
     * use with caution.
     * 
     * @throws IOException 
     * @throws FTPException 
     *
     */
    public void sendServerWakeup() throws IOException, FTPException {
        noOperation();
     }
    
    /**
     *  Tries to keep the current connection alive by 
     *  sending an innocuous command to signal that the 
     *  client is still active
     */
    public void keepAlive() throws IOException, FTPException {
        log.debug("keepAlive() called");
        int op = (int)Math.ceil(Math.random()*2);
        switch (op) {
            case 1:    
                noOperation();
                break;
            case 2:
                pwd();
                break;
            default:
                pwd();
        }
    }
    
    /**
     *  Get the help text for the specified command
     *
     *  @param  command  name of the command to get help on
     *  @return help text from the server for the supplied command
     */
    public String help(String command)
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
        lastReply = control.sendCommand("HELP " + command);
        String[] validCodes = {"211", "214"};
        lastValidReply = control.validateReply(lastReply, validCodes);
        return lastValidReply.getReplyText();
    }
    
     /**
     *  Abort the current action
     */
    protected void abort()
        throws IOException, FTPException {
        
        checkConnection(true);
        
        lastReply = control.sendCommand("ABOR");
        String[] validCodes = {"426", "226"};
        lastValidReply = control.validateReply(lastReply, validCodes);
    }

    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#quit()
     */
    public void quit()
        throws IOException, FTPException {
    	
    	checkConnection(true);
    	
        try {
            lastReply = control.sendCommand("QUIT");
            String[] validCodes = {"221", "226"};
            lastValidReply = control.validateReply(lastReply, validCodes);
        }
        finally { // ensure we clean up the connection
            try {
                control.logout();
            }
            finally {
                control = null;
            }
        }
        closeDataSocket(); // ensure no data socket is hanging around
    }
    
    /*
     *  (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPClientInterface#quitImmediately()
     */
    public void quitImmediately() 
        throws IOException, FTPException {
        
        cancelTransfer();
        try {
            if (control != null && control.controlSock != null)
                control.controlSock.close();
        }
        finally {
            control = null;
        }
        closeDataSocket(); // ensure no data socket is hanging around
    }
    
    /**
     * String representation
     */
    public String toString() {
        StringBuffer result = new StringBuffer("[");
        result.append("FTP").append(",").append(remoteHost).append(",").append(controlPort).
            append(",").append(getId()).append("]");
        return result.toString();
    }



}


