package org.genepattern.server.genepattern;

import org.apache.lucene.analysis.*;
import java.io.Reader;

public class GPLuceneAnalyzer extends Analyzer {
	public TokenStream tokenStream(String fieldName, Reader reader) {
		return new GPTokenizer(reader);
	}
}