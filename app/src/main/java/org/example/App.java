package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class App {
    private static String indexPath = "index";

    public void buildIndex() {
        try {
            Bible bible = new Bible();
            bible.load();
            List<Bible.Verse> verses = bible.getVerses();

            Directory dir = FSDirectory.open(Paths.get(indexPath));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            // overwrite any existing index
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            // keep various files separate
            iwc.setUseCompoundFile(false);

            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
                for (Bible.Verse verse : verses) {
                    indexVerse(writer, verse);
                }
                // costly but optimizes search performance for static indexes
                System.out.println("merging");
                writer.forceMerge(1);
            }
            System.out.println("done");
        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
        }
    }

    private void indexVerse(IndexWriter indexWriter, Bible.Verse verse) throws IOException {
        Document doc = new Document();

        doc.add(new TextField("book_name", verse.bookName(), Field.Store.YES));
        doc.add(new KeywordField("book_name.keyword", verse.bookName(), Field.Store.NO));

        doc.add(new LongPoint("book", verse.book())); // filtering
        doc.add(new NumericDocValuesField("book", verse.book())); // sorting / faceting
        doc.add(new StoredField("book", verse.book())); // retrieval

        doc.add(new LongPoint("chapter", verse.chapter())); // filtering
        doc.add(new NumericDocValuesField("chapter", verse.chapter())); // sorting / faceting
        doc.add(new StoredField("chapter", verse.chapter())); // retrieval

        doc.add(new LongPoint("verse", verse.verse())); // filtering
        doc.add(new NumericDocValuesField("verse", verse.verse())); // sorting / faceting
        doc.add(new StoredField("verse", verse.verse())); // retrieval

        doc.add(new TextField("text", verse.text(), Field.Store.YES));

        System.out.println("adding "+verse.bookName()+", chapter "+verse.chapter()+", verse "+verse.verse());

        indexWriter.addDocument(doc);
    }

    public static void main(String[] args) {
        new App().buildIndex();
    }
}
