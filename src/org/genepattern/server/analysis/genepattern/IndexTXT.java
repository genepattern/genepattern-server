package org.genepattern.server.analysis.genepattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
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