package org.genepattern.server.indexer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.genepattern.server.genepattern.IDocumentCreator;

import javax.swing.text.rtf.RTFEditorKit;
import javax.swing.text.BadLocationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class IndexRTF implements IDocumentCreator {

    public Document index(File f) throws IOException, BadLocationException {
	FileInputStream stream = new FileInputStream(f);
	RTFEditorKit kit = new RTFEditorKit();
	javax.swing.text.Document rtfDoc = kit.createDefaultDocument();
	kit.read(stream, rtfDoc, 0);
	String plainText = rtfDoc.getText(0, rtfDoc.getLength());
        Document doc = new Document();
        doc.add(Field.Text(Indexer.TASK_DOC, plainText));
	return doc;
    }
}