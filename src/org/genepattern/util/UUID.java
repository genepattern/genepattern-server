/*
 * (C) Copyright IBM Corp. 2000  All rights reserved.
 *
 * The program is provided "AS IS" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */

package org.genepattern.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Random;



/**
 * Generates a unique identifier.
 *
 * A Universally Unique Identifier (UUID) is a 128 bit number generated
 * according to an algorithm that is garanteed to be unique in time and
 * space from all other UUIDs. It consists of an IEEE 802 Internet Address
 * and various time stamps to ensure uniqueness. For a complete specification,
 * see ftp://ietf.org/internet-drafts/draft-leach-uuids-guids-01.txt [leach].
 *
 * Code from: http://groups.google.com/groups?q=generating+a+unique+name&hl=en&group=comp.lang.java.*&rnum=1&selm=98sopr%2459e%241%40plutonium.compulink.co.uk
 *
 * @author Jim Amsden &lt;jamsden@us.ibm.com&gt;
 * @authorModifications by Torgeir Veimo &lt;torgeir.veimo@ecomda.de&gt; for Ecomda GmbH.
 * @author Aravind Subramanian - minor porting related changes for GenePattern
 * @version %I%, %G%
 *
 * see edu.mit.genome.gp.xtest.performance.UUIDTest for performance tests.
 */

public class UUID implements Serializable {

    private static byte[] internetAddress = null;
    private static String uuidFile = null;
    private long time;
    private short clockSequence;
    private byte version = 1;
    private byte[] node = new byte[6];
    private static final int UUIDsPerTick = 128;
    private static long lastTime = new Date().getTime();
    private static int uuidsThisTick = UUIDsPerTick;
    private static UUID previousUUID = null; // initialized from saved state
    private static long nextSave = new Date().getTime();
    private static Random randomGenerator = new Random(new Date().getTime());
    private static char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /** For logging support */
    private transient static XLogger log = XLogger.getLogger(UUID.class);

    //---------------------------------------------------------------------
    static {
        try {
            internetAddress = InetAddress.getLocalHost().getAddress();
            //log.debug("host address=" + internetAddress);
        } catch (Exception exc) {
            System.err.println("Can't get host address: " + exc);
            if( log != null )
                log.info("Can't get host address: " + exc, exc);
        }

        try {
            uuidFile = System.getProperty("UUID_HOME");
            if (uuidFile == null) {
                uuidFile = System.getProperty("user.home");
            }
            if (!uuidFile.endsWith(File.separator)) {
                uuidFile = uuidFile + File.separator;
            }

            uuidFile = uuidFile + "UUID";
            previousUUID = getUUIDState();
            //log.debug("uuidFile=" + uuidFile);
            } catch (Exception exc) {
                System.err.println("UUID failed in static initializer");
                exc.printStackTrace();
                if( log != null )
                    log.info("While static-block initializing", exc);
            }
    } // End static block
    //---------------------------------------------------------------------

    /**
     * Class constructor.
     * Generate a UUID for this host using version 1 of [leach].
     */
    public UUID() {
        synchronized (this) {
            time = getCurrentTime();
            //node = getIEEEAddress();
            node = previousUUID.getNode();
            if (previousUUID == null || nodeChanged(previousUUID)) {
                // if there is no saved state, or the node address changed,
                // generate a random clock sequence
                clockSequence = (short) random();
            } else if (time < previousUUID.getTime()) {
            // if the clock was turned back, increment the clock sequence
                clockSequence++;
            }
            previousUUID = this;    // for next time

            // save for the next UUID
            setUUIDState(this);
        }
    }

    /**
     * Class constructor.
     * Generate a UUID for this host using version 1 of [leach].
     *
     * @param node the node to use in the UUID
     */
    public UUID(byte[] node) {
        synchronized (this) {
            time = getCurrentTime();
            this.node = node;

            // save for the next UUID
            setUUIDState(this);
        }
    }

    /**
     * Class constructor.
     * Generate a UUID from a name (NOT IMPLEMENTED)
     */
    public UUID(UUID context, String name) {
    }

    /**
     * Lexically compare this UUID with withUUID. Note: lexical ordering
     * is not temporal ordering.
     *
     * @param withUUID the UUID to compare with
     * @return
     * <ul>
     *    <li>-1 if this UUID is less than withUUID
     *    <li>0 if this UUID is equal to withUUID
     *    <li>1 if this UUID is greater than withUUID
     * </ul>
     */
    public int compare(UUID withUUID) {
        if (time < withUUID.getTime()) {
                return -1;
        } else if (time > withUUID.getTime()) {
                return 1;
        }
        if (version < withUUID.getVersion()) {
                return -1;
        } else if (version > withUUID.getVersion()) {
                return 1;
        }
        if (clockSequence < withUUID.getClockSequence()) {
                return -1;
        } else if (clockSequence > withUUID.getClockSequence()) {
                return 1;
        }
        byte[] withNode = withUUID.getNode();
        for (int i = 0; i < 6; i++) {
                if (node[i] < withNode[i]) {
                        return -1;
                } else if (node[i] > withNode[i]) {
                        return 1;
                }
        }
        return 0;
    }

    /**
     * Get a 48 bit cryptographic quality random number to use as the node field
     * of a UUID as specified in section 6.4.1 of version 10 of the WebDAV spec.
     * This is an alternative to the IEEE 802 host address which is not available
     * from Java. The number will not conflict with any IEEE 802 host address because
     * the most significant bit of the first octet is set to 1.
     *
     * @return a 48 bit number specifying an id for this node
     */
    private static byte[] computeNodeAddress() {
        byte[] address = new byte[6];
        // create a random number by concatenating:
        // the hash code for the current thread
        // the current time in milli-seconds
        // the internet address for this node
        int thread = Thread.currentThread().hashCode();
        long time = System.currentTimeMillis();
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);

        try {
            if (internetAddress != null) {
                out.write(internetAddress);
            }
            out.write(thread);
            out.writeLong(time);
            out.close();
        } catch (IOException exc) {
        }

        byte[] rand = byteOut.toByteArray();
        MessageDigest md5 = null;

        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception exc) {
        }

        md5.update(rand);
        byte[] temp = md5.digest();
        // pick the middle 6 bytes of the MD5 digest
        for (int i = 0; i < 6; i++) {
            address[i] = temp[i+5];
        }
        // set the MSB of the first octet to 1 to distinguish from IEEE node addresses
        address[0] = (byte)(address[0] | (byte)0x80);
        return address;
    }

    /**
     * Get the clock sequence number.
     * @return the clock sequence number
     */
    public int getClockSequence() {
        return clockSequence;
    }

    /**
     * Get the current time compensating for the fact that the real
     * clock resolution may be less than 100ns.
     *
     * @return the current date and time
     */
    private static long getCurrentTime() {
        long now = 0;
        boolean waitForTick = true;
        while (waitForTick) {
            now = new Date().getTime();
            if (lastTime < now) {
                // got a new tick, make sure uuidsPerTick doesn't cause an overrun
                uuidsThisTick = 0;
                waitForTick = false;
            } else if (uuidsThisTick < UUIDsPerTick) {
                //
                uuidsThisTick++;
                waitForTick = false;
            }
        }

        // add the uuidsThisTick to the time to increase the clock resolution
        now += uuidsThisTick;
        lastTime = now;
        return now;
    }

    /**
     * Get the 48 bit IEEE 802 host address.
     * NOT IMPLEMENTED
     *
     * @return a 48 bit number specifying a unique location
     */
    private static byte[] getIEEEAddress() {
        byte[] address = new byte[6];
        // TODO: get the IEEE 802 host address
        return address;
    }

    /**
     * Get the spatially unique portion of the UUID. This is either
     * the 48 bit IEEE 802 host address, or if on is not available, a random
     * number that will not conflict with any IEEE 802 host address.
     *
     * @return node portion of the UUID
     */
    public byte[] getNode() {
        return node;
    }

    /**
     * Get the temporal unique portion of the UUID.
     * @return the time portion of the UUID
     */
    public long getTime() {
        return time;
    }

    /**
     * Get the 128 bit UUID.
     */
    public byte[] getUUID() {
        byte[] uuid = new byte[16];
        long t = time;
        for (int i = 0; i < 8; i++) {
                uuid[i] = (byte) ((t >> 8 * i) & 0xFF); // time
        }
        uuid[7] |= (byte) (version << 12); // time hi and version
        uuid[8] = (byte) (clockSequence & 0xFF);
        uuid[9] = (byte) ((clockSequence & 0x3F00) >> 8);
        uuid[9] |= 0x80; // clock sequence hi and reserved
        for (int i = 0; i < 6; i++) {
                uuid[10 + i] = node[i]; // node
        }

        return uuid;
    }

    /**
     * Get the UUID generator state. This consists of the last (or
     * nearly last) UUID generated. This state is used in the construction
     * of the next UUID. May return null if the UUID state is not
     * available.
     * @return the last UUID generator state
     */
    private static UUID getUUIDState() {
        UUID uuid = null;
        FileInputStream in = null;
        try {
            //System.out.println("uuidFile="+uuidFile);
            in = new FileInputStream(uuidFile);
            ObjectInputStream s = new ObjectInputStream(in);
            uuid = (UUID) s.readObject();
        } catch (Exception exc) {
            uuid = new UUID(computeNodeAddress());
            if( log != null )
                log.error("Can't get saved UUID state: " + exc, exc);
        } finally {
            try {
                if( in != null )
                    in.close();
            } catch (IOException ex) {} //don't need to care about this Exception
        }
        return uuid;
    }

    /**
     * Get the UUID version number.
     */
    public int getVersion() {
        return version;
    }

    /**
     * Compare two UUIDs
     * @return true if the UUIDs are equal
     */
    public boolean isEqual(UUID toUUID) {
        return compare(toUUID) == 0;
    }

    /**
     * Determine if the node changed with respect to previousUUID.
     * @param previousUUID the UUID to compare with
     * @return true if the the previousUUID has a different node than this UUID
     */
    private boolean nodeChanged(UUID previousUUID) {
        byte[] previousNode = previousUUID.getNode();
        boolean nodeChanged = false;
        int i = 0;
        while (!nodeChanged && i < 6) {
                nodeChanged = node[i] != previousNode[i];
                i++;
        }
        return nodeChanged;
    }

    /**
     * Generate a crypto-quality random number. This implementation
     * doesn't do that.
     * @return a random number
     */
     private static int random() {
        return randomGenerator.nextInt();
    }

    /**
     * Set the persistent UUID state.
     * @param aUUID the UUID to save
     */
    private static void setUUIDState(UUID aUUID) {
        if (aUUID.getTime() > nextSave) {
            try {
                FileOutputStream f = new FileOutputStream(uuidFile);
                ObjectOutputStream s = new ObjectOutputStream(f);
                s.writeObject(aUUID);
                s.close();
                nextSave = aUUID.getTime() + 10 * 10 * 1000 * 1000;
            } catch (Exception exc) {
                log.error("Can't save UUID state: " + exc, exc);
            }
        }
    }

    /**
     * Provide a String representation of a UUID as specified in section
     * 3.5 of [leach].
     */
    public String toString() {
        byte[] uuid = getUUID();
        StringWriter s = new StringWriter();
        for (int i = 0; i < 16; i++) {
            s.write(hexDigits[(uuid[i] & 0xF0) >> 4]);
            s.write(hexDigits[uuid[i] & 0x0F]);
            if (i == 3 || i == 5 || i == 7 || i == 9) {
                s.write('-');
            }
        }
        return s.toString();
    }

} // End UUID
