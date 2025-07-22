package com.chrisberkhout.lucenewordcloud;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.BaseDirectory;

import java.io.IOException;

public class IndexBuilder {

    private BaseDirectory dir;
    private Analyzer analyzer;
    private FacetsConfig facetsConfig = new FacetsConfig();

    public IndexBuilder(Analyzer analyzer, BaseDirectory dir) {
        this.analyzer = analyzer;
        this.dir = dir;
    }

    public void build(Bible bible) throws IOException {
        IndexWriterConfig iwc = new IndexWriterConfig(this.analyzer);

        // overwrite any existing index
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        // keep various files separate
        iwc.setUseCompoundFile(false);

        IndexWriter writer = new IndexWriter(dir, iwc);
        // by verse
        for (Bible.Verse verse : bible.getVerses()) {
            indexVerse(writer, verse);
        }

        // costly but optimizes search performance for static indexes
        writer.forceMerge(1);
        writer.close();
    }

    private void indexVerse(IndexWriter indexWriter, Bible.Verse verse) throws IOException {
        Document doc = new Document();

        doc.add(new TextField("book", verse.bookName(), Field.Store.YES));

        doc.add(new LongPoint("book_num", verse.book())); // filtering
        doc.add(new SortedSetDocValuesFacetField("book_num", Integer.toString(verse.book())));
//        doc.add(new NumericDocValuesField("book_num", verse.book())); // sorting / faceting
        doc.add(new StoredField("book_num", verse.book())); // retrieval

        doc.add(new LongPoint("chapter_num", verse.chapter())); // filtering
        doc.add(new NumericDocValuesField("chapter_num", verse.chapter())); // sorting / faceting
        doc.add(new StoredField("chapter_num", verse.chapter())); // retrieval

        doc.add(new LongPoint("verse_num", verse.verse())); // filtering
        doc.add(new NumericDocValuesField("verse_num", verse.verse())); // sorting / faceting
        doc.add(new StoredField("verse_num", verse.verse())); // retrieval

        FieldType textWithTV = new FieldType(TextField.TYPE_STORED) {{
            setStoreTermVectors(true);
            freeze();
        }};
        doc.add(new Field("text", verse.text(), textWithTV));

        indexWriter.addDocument(facetsConfig.build(doc));
    }

}
