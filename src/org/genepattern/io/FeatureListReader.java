package org.genepattern.io;
import java.io.*;
import java.util.*;

/**
 *  Feature list reader.
 *
 * @author    Joshua Gould
 */
public class FeatureListReader extends AbstractReader {

	public FeatureListReader() {
		super(new String[]{"grp"}, "grp");
	}


	/**
	 *  Gets the list of features contained in the given file
	 *
	 * @param  fileName         The file path
	 * @return                  The feature list
	 * @exception  IOException  If an error occurs while reading the file
	 */
	public List read(String fileName) throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileName));
			String s = null;
			List probeList = new ArrayList();
			while((s = br.readLine()) != null) {
				String feature = s.trim();
				if(!feature.equals("")) {
					probeList.add(feature);
				}
			}
			return probeList;
		} finally {
			if(br != null) {
				br.close();
			}
		}
	}
}
