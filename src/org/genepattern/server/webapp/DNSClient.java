/*---------------------------------------------------------------------------
 * Copyright (C) 1999,2000 Dallas Semiconductor Corporation, All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY,  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL DALLAS SEMICONDUCTOR BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Dallas Semiconductor
 * shall not be used except as stated in the Dallas Semiconductor
 * Branding Policy.
 *---------------------------------------------------------------------------
 */

package org.genepattern.server.webapp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * The <code>DNSClient</code> class allows DNS (Domain Name System) lookups
 * per RFC 1035. Can perform forward or reverse lookups. Modified to support RFC
 * 1886. NOTE: We don't support A6/DNAME yet, as nobody seems to be using them
 * yet. Saves some space......
 */
public class DNSClient {
	private static final byte DNS_A = 1; // DNS query types

	private static final byte DNS_PTR = 12;

	private static final byte DNS_MX = 15;

	private static final byte DNS_AAAA = 28;

	private static final byte DNS_A6 = 38;

	private static final boolean AAAA_Queries = false; // set to false if you
													   // don't want AAAA DNS
													   // queries

	private static final int firstQuery = DNS_AAAA; // modify to change order of
													// DNS lookups

	private static final int secondQuery = DNS_A;

	private static final byte[] HEADER = { (byte) 0x55, (byte) 0x44,
			(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

	private InetAddress DNS1 = null;

	private InetAddress DNS2 = null;

	private int DNSTimeout = 1000;

	/**
	 * Create an instance of <code>DNSClient</code>.
	 */
	public DNSClient() {
		try {
			//DNS1 = InetAddress.getLocalHost();
			DNS1 = InetAddress.getByName("18.70.0.160");
		} catch (java.net.UnknownHostException uhe) {
		}
	}

	/**
	 * Sets the DNS timeout.
	 * 
	 * @param timeout
	 *            Set to 0 for exponential backoff timeout or non-zero value for
	 *            single timeout in milliseconds.
	 */
	public void setDNSTimeout(int timeout) {
		DNSTimeout = timeout;
	}

	/**
	 * Performs a DNS IP address lookup.
	 * 
	 * @param name
	 *            FQDN to look up
	 * @return DNS response(s), or <code>null</code> for failed lookup
	 */
	public String[] getByName(String name) {
		byte[] QUESTION = makeQuery(name, firstQuery);

		if (AAAA_Queries) {
			String[] result;

			result = transmit(QUESTION);
			// check whether we received any AAAA record or just junk
			// like CNAME, etc.
			if (result != null) {
				for (int i = 0; i < result.length; i++) {
					if (result[i] != null)
						return result;
				}
			}
		}

		QUESTION[QUESTION.length - 3] = secondQuery;

		return transmit(QUESTION);
	}

	/**
	 * Performs a DNS MX record lookup.
	 * 
	 * @param name
	 *            domain to look up
	 * @return DNS response(s), or <code>null</code> for failed lookup
	 */
	public String[] getMX(String name) {
		byte[] QUESTION = makeQuery(name, DNS_MX);

		return transmit(QUESTION);
	}

	/**
	 * Build query for specified host
	 * 
	 * 
	 * @param host
	 * 
	 * @return query
	 *  
	 */
	private byte[] makeQuery(String host, int recordType) {
		int d1 = 0; //the location of the last dot
		byte[] QUESTION = null;

		QUESTION = new byte[host.length() + 6];

		int i = 0;
		byte[] s = host.getBytes();

		for (i = 0; i < s.length; i++) {
			if (s[i] == (byte) '.') {
				QUESTION[d1] = (byte) (i - d1);
				d1 = i + 1;
			} else
				QUESTION[i + 1] = s[i];
		}

		QUESTION[d1] = (byte) (i - d1);
		QUESTION[i + 1] = 0;

		QUESTION[i + 2] = 0;
		QUESTION[i + 3] = (byte) recordType;
		QUESTION[i + 4] = 0;
		QUESTION[i + 5] = 1; // class IN

		return QUESTION;
	}

	/**
	 * Transmit data query.
	 * 
	 * 
	 * @param data
	 * 
	 * @return strings returned from DNS lookup
	 *  
	 */
	private String[] transmit(byte[] data) {
		byte[] senddata = new byte[data.length + HEADER.length];

		System.arraycopy(HEADER, 0, senddata, 0, HEADER.length);
		System.arraycopy(data, 0, senddata, HEADER.length, data.length);

		String[] reply = null;

		if (DNS1 != null)
			reply = send(senddata, DNS1);

		if (reply == null)
			if (DNS2 != null)
				reply = send(senddata, DNS2);

		return reply;
	}

	/**
	 * Method send
	 * 
	 * 
	 * @param outbuf
	 * @param dnsserver
	 * 
	 * @return
	 *  
	 */
	private String[] send(byte[] outbuf, InetAddress dnsserver) {
		DatagramSocket sock = null;
		DatagramPacket pack;
		int maxRetry;
		int currentTimeout;

		if (DNSTimeout == 0) {

			/* Start at 2000ms, and double four times */
			currentTimeout = 2000;
			maxRetry = 4;
		} else {
			currentTimeout = DNSTimeout;
			maxRetry = 1;
		}

		for (int retry = 0; retry < maxRetry; retry++, currentTimeout *= 2) {
			try {
				sock = new DatagramSocket();

				sock.setSoTimeout(currentTimeout);

				//System.out.println("after getbyname dnsserver");
				pack = new DatagramPacket(outbuf, outbuf.length, dnsserver, 53);

				sock.send(pack);

				//                  System.out.print("new-sent...");
				DatagramPacket rec = new DatagramPacket(new byte[512], 512);

				sock.receive(rec);
				sock.close();

				//                  System.out.println("got it\n\n");
				byte[] data = rec.getData();

				//System.out.println("length "+rec.getLength());

				/*
				 * for (int i = 0;i < rec.getLength();i++) { if (i % 16 == 0)
				 * System.out.println(); int b = data[i] & 0xFF; int nibble;
				 * 
				 * if (b < 16) System.out.write('0'); else { nibble = (b >>> 4) &
				 * 0x0F; System.out.write((nibble > 9)?((nibble - 10) +
				 * 'A'):(nibble + '0')); } nibble = b & 0x0F;
				 * System.out.write((nibble > 9)?((nibble - 10) + 'A'):(nibble +
				 * '0')); System.out.write(' '); } System.out.println();
				 */

				// Check for RCODE. (errors)
				int x = data[3] & 0x0f;

				if (x != 0)
					return null;

				// Get the question count
				int qn = ((data[4] << 8) | (data[5] & 0x0ff)) & 0x0ffff;

				//                  System.out.println("QUESTIONS : "+Integer.toHexString(x));
				// Get the answer count
				int an = ((data[6] << 8) | (data[7] & 0x0ff)) & 0x0ffff;

				//                  System.out.println("ANSWERS : "+Integer.toHexString(x));
				// may need to handle this for recursion not implemented
				if (an == 0)
					return null;

				// Index past the question we sent. We get copied back on the
				// question exactly, so we should just be able to index past the
				// length of outbuf.
				int index = outbuf.length;

				//                  System.out.println("\n\nANSWERS:--------------------");
				// Parse the answer and return an array of strings back to
				// the caller.
				return parse(an, index, data);
			} catch (IOException e) {
				if (sock != null)
					sock.close();
			} catch (Exception e) {

				//                  System.out.println("Caught an exception: "+e.toString());
				//                  e.printStackTrace();
			} finally {
				if (sock != null)
					sock.close();
			}
		}

		return null;
	}

	/**
	 * Method parse
	 * 
	 * 
	 * @param num
	 * @param index
	 * @param data
	 * 
	 * @return
	 *  
	 */
	private String[] parse(int num, int index, byte[] data) {

		// Array of strings to return.
		String[] ret = new String[num];

		for (int i = 0; i < num; i++) {

			ret[i] = null;

			// Skip the domain name data, it's one of the following
			// 1. A single compressed ID offset
			// 2. A domain name string with a single compressed ID offset at the
			// end.
			// 3. A list of names with no compressed ID offset fields.
			int count = data[index] & 0x0ff;

			index++;

			if ((count & 0xc0) == 0xc0) {
				index++;
			} else
				while (count != 0) {

					// If the byte is a compression byte, skip the next byte
					// also.
					int y = count;

					for (int z = 0; z < y; z++) {

						//                                      System.out.print(Integer.toHexString(data[index]&0x0ff)+"
						// ");
						index++;
					}

					count = data[index] & 0x0ff;

					index++;

					//                              System.out.print(" ");
				}

			// Get the query type
			int type = ((data[index] << 8) | (data[index + 1] & 0x0ff)) & 0x0ffff;

			// Get the query class
			int clss = ((data[index + 2] << 8) | (data[index + 3] & 0x0ff)) & 0x0ffff;

			index = index + 4;

			// Get time to live
			int ttl = ((data[index] << 8) | (data[index + 1] & 0x0ff)) & 0x0ffff;

			ttl = ttl
					| (((data[index + 2] << 8) | (data[index + 3] & 0x0ff)) & 0x0ffff);

			// Get the resource data length.
			int rdl = ((data[index + 4] << 8) | (data[index + 5] & 0x0ff)) & 0x0ffff;

			index = index + 6;

			// If this is a PTR query (IP address to name)
			if (type == DNS_PTR) {
				ret[i] = parseDomainName(null, data, index, rdl);
				index += rdl;
			} else if (type == DNS_A) {
				// get the 4 byte ip address
				byte[] tempdata = new byte[4];
				System.arraycopy(data, index, tempdata, 0, 4);
				//            ret [i] = TININet.ipv4ToString(tempdata);
				System.err.println("DNS_A needs TININet");
				index += 4;
			} else if (type == DNS_AAAA) {
				// get the 16 byte ip address
				byte[] tempdata = new byte[16];
				System.arraycopy(data, index, tempdata, 0, 16);
				//            ret [i] = TININet.ipv6ToString(tempdata);
				System.err.println("DNS_AAAA needs TININet");
				index += 16;
			} else if (type == DNS_MX) {
				// get the priority and host names
				//	    int pref = com.dalsemi.system.ArrayUtils.getShort(data,
				// index);
				int pref = ((data[index] << 8) & 0xff00)
						| ((data[index + 1] << 0) & 0xff);
				// Skip preference
				ret[i] = parseDomainName(Integer.toString(pref), data,
						index + 2, rdl - 2);
				index += rdl;
			} else {
				// Skip this resource, we don't want it.
				index += rdl;
			}
		}

		return ret;
	}

	private static String parseDomainName(String prefix, byte[] data,
			int index, int rdl_left) {
		StringBuffer sb;
		boolean first = true;

		if (prefix != null) {
			sb = new StringBuffer(prefix);
			sb.append(' ');
		} else
			sb = new StringBuffer();

		int strlen = data[index] & 0x0ff;

		index++;
		rdl_left--;

		while (rdl_left > 0) {

			// If the byte is a compression byte, skip the next byte also.
			if ((strlen & 0xc0) == 0xc0) {
				// Append the compressed string to the buffer
				int offs = ((strlen & 0x3f) << 8) + data[index] & 0xff;
				int l = 0;
				do {
					l = data[offs] & 0xff;
					offs++;

					// Follow pointers...
					while ((l & 0xc0) == 0xc0) {
						offs = ((l & 0x3f) << 8) + data[offs] & 0xff;
						l = data[offs] & 0xff;
						offs++;
					}

					if ((!first) && (l > 0))
						sb.append('.');
					for (int j = 0; j < l; j++) {
						sb.append((char) data[offs]);
						offs++;
					}
					first = false;
				} while (l > 0);

				rdl_left--;
				index++;
			}

			else {
				if ((!first) && (strlen > 0))
					sb.append('.');

				// Append the next segment of the string to the buffer
				for (int i = 0; i < strlen; i++) {

					//                                      System.out.print(Integer.toHexString(data[index]&0x0ff)+"
					// ");
					sb.append((char) data[index]);

					index++;
					rdl_left--;
				}
				first = false;
			}

			strlen = data[index] & 0x0ff;

			index++;
			rdl_left--;

		}

		return sb.toString();
	}

	public TreeMap findMXServers(String domain) {
		TreeMap mxServers = null;
		boolean debug = false;

		String[] serversWithPriority = getMX(domain);
		if (serversWithPriority != null) {
			mxServers = new TreeMap();
			int j;
			for (int i = 0; i < serversWithPriority.length; i++) {
				if (debug)
					System.out.println("MX server: " + serversWithPriority[i]);
				j = serversWithPriority[i].indexOf(" ");
				Integer priority = new Integer(serversWithPriority[i]
						.substring(0, j));
				String serverName = serversWithPriority[i].substring(j + 1);
				mxServers.put(priority, serverName);
			}
		}
		if (mxServers == null || mxServers.size() == 0) {
			if (debug)
				System.out.println("There were no MX servers found for "
						+ domain);
			return null;
		}
		if (debug)
			System.out.println("There are " + mxServers.size() + " MX records");
		return mxServers;
	}

	public static void main(String[] args) {
		for (int arg = 0; arg < args.length; arg++) {
			DNSClient dnsClient = new DNSClient();
			TreeMap tmMX = dnsClient.findMXServers(args[arg]);
			if (tmMX == null) {
				System.out.println("No MX servers found for " + args[arg]);
				continue;
			}
			Integer key;
			String host;
			for (Iterator eHosts = tmMX.keySet().iterator(); eHosts.hasNext();) {
				key = (Integer) eHosts.next();
				host = (String) tmMX.get(key); // eg. "genome.wi.mit.edu";
				System.out.println(host + " at priority " + key);
			}
		}
		System.exit(1);
	}

}