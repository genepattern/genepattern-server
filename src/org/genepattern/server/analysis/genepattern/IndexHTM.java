package org.genepattern.server.analysis.genepattern;

import org.apache.lucene.demo.HTMLDocument;
import org.apache.lucene.document.Document;
import java.io.File;
import java.io.IOException;

public class IndexHTM implements IDocumentCreator {

    public Document index(File f) throws IOException, InterruptedException {
	return HTMLDocument.Document(f);
    }
}