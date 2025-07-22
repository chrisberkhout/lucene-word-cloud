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

        // Overwrite any existing index
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        // Keep various files separate, for easier inspection
        iwc.setUseCompoundFile(false);

        IndexWriter writer = new IndexWriter(dir, iwc);
        for (Bible.Verse verse : bible.getVerses()) {
            indexVerse(writer, verse);
        }

        // It's a static index
        writer.forceMerge(1);
        writer.close();
    }

    private void indexVerse(IndexWriter indexWriter, Bible.Verse verse) throws IOException {
        Document doc = new Document();

        doc.add(new TextField("book", verse.bookName(), Field.Store.YES));
        doc.add(new SortedSetDocValuesFacetField("book_num", Integer.toString(verse.book())));
        doc.add(new StoredField("book_num", verse.book()));
        doc.add(new StoredField("chapter_num", verse.chapter()));
        doc.add(new StoredField("verse_num", verse.verse()));

        FieldType textWithTV = new FieldType(TextField.TYPE_STORED) {{
            setStoreTermVectors(true);
            freeze();
        }};
        doc.add(new Field("text", verse.text(), textWithTV));

        indexWriter.addDocument(facetsConfig.build(doc));
    }

}
