package org.genepattern.server.analysis.genepattern;

import org.apache.lucene.demo.html.HTMLParser;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import java.io.File;
import java.io.IOException;

public class IndexManualHTML implements IDocumentCreator {

    public Document index(File f) throws IOException, InterruptedException {
        Document doc = new Document();
	HTMLParser parser = new HTMLParser(f);

	// Add the tag-stripped contents as a Reader-valued Text field so it will
	// get tokenized and indexed.
	doc.add(Field.Text(Indexer.MANUAL, parser.getReader()));
	doc.add(Field.Text("title", parser.getTitle()));
	return doc;
    }
}