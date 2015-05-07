/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.util;

import java.util.*;
import java.io.*;

public class KeySortedProperties extends Properties {

public KeySortedProperties(){
	super();
}

public KeySortedProperties(Properties props){
	super(props);
}


public void store(OutputStream out, String header) throws IOException
	{
	// The spec says that the file must be encoded using ISO-8859-1.
	PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "ISO-8859-1"));
	if (header != null)
		writer.println("#" + header);
	writer.println ("#" + Calendar.getInstance ().getTime ());
 	
	TreeSet keys = new TreeSet(keySet());

	Iterator iter = keys.iterator ();

	StringBuffer s = new StringBuffer (); // Reuse the same buffer.
	
	while (iter.hasNext()) {
		String key = (String) iter.next();
		
		formatForOutput (key, s, true);
		s.append ('=');
		formatForOutput (getProperty(key), s, false);
		writer.println (s);
	}
  	
      writer.flush ();
	}

 /**
    * Formats a key or value for output in a properties file.
    * See store for a description of the format.
    *
    * @param str the string to format
    * @param buffer the buffer to add it to
    * @param key true if all ' ' must be escaped for the key, false if only
    *        leading spaces must be escaped for the value
    * @see #store(OutputStream, String)
    */
private void formatForOutput(String str, StringBuffer buffer, boolean key)
   {
     if (key)
       {
         buffer.setLength(0);
         buffer.ensureCapacity(str.length());
       }
     else
       buffer.ensureCapacity(buffer.length() + str.length());
     boolean head = true;
     int size = str.length();
     for (int i = 0; i < size; i++)
       {
         char c = str.charAt(i);
         switch (c)
           {
           case '\n':
             buffer.append("\\n");
             break;
           case '\r':
             buffer.append("\\r");
             break;
           case '\t':
             buffer.append("\\t");
             break;
           case ' ':
             buffer.append(head ? "\\ " : " ");
             break;
           case '\\':
           case '!':
           case '#':
           case '=':
           case ':':
             buffer.append('\\').append(c);
             break;
           default:
             if (c < ' ' || c > '~')
               {
                 String hex = Integer.toHexString(c);
                 buffer.append("\\u0000".substring(0, 6 - hex.length()));
                 buffer.append(hex);
               }
             else
               buffer.append(c);
           }
         if (c != ' ')
           head = key;
       }
   }
}

