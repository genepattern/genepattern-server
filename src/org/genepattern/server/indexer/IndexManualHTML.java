/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


package org.genepattern.server.indexer;

import org.apache.lucene.demo.html.HTMLParser;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.genepattern.server.genepattern.IDocumentCreator;

import java.io.File;
import java.io.IOException;

public class IndexManualHTML implements IDocumentCreator {

	public Document index(File f) throws IOException, InterruptedException {
		Document doc = new Document();
		HTMLParser parser = new HTMLParser(f);

		// Add the tag-stripped contents as a Reader-valued Text field so it
		// will
		// get tokenized and indexed.
		doc.add(Field.Text(Indexer.MANUAL, parser.getReader()));
		doc.add(Field.Text("title", parser.getTitle()));
		return doc;
	}
}