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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.genepattern.server.genepattern.IDocumentCreator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class IndexTXT implements IDocumentCreator {

	public Document index(File f) throws IOException {
		Document doc = new Document();
		doc.add(Field.Text(Indexer.TASK_DOC, new FileReader(f)));
		return doc;
	}
}