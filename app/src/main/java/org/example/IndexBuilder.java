package org.example;

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
        for (Bible.Section verse : bible.getVerses()) {
            indexSection(writer, verse);
        }

        // costly but optimizes search performance for static indexes
        writer.forceMerge(1);
        writer.close();
    }

    private void indexSection(IndexWriter indexWriter, Bible.Section section) throws IOException {
        Document doc = new Document();

        doc.add(new TextField("book", section.bookName(), Field.Store.YES));

        doc.add(new LongPoint("book_num", section.book())); // filtering
        doc.add(new SortedSetDocValuesFacetField("book_num", Integer.toString(section.book())));
//        doc.add(new NumericDocValuesField("book_num", section.book())); // sorting / faceting
        doc.add(new StoredField("book_num", section.book())); // retrieval

        doc.add(new LongPoint("chapter_num", section.chapter())); // filtering
        doc.add(new NumericDocValuesField("chapter_num", section.chapter())); // sorting / faceting
        doc.add(new StoredField("chapter_num", section.chapter())); // retrieval

        section.verse().ifPresent(v ->{
            doc.add(new LongPoint("verse_num", v)); // filtering
            doc.add(new NumericDocValuesField("verse_num", v)); // sorting / faceting
            doc.add(new StoredField("verse_num", v)); // retrieval
        });

        FieldType textWithTV = new FieldType(TextField.TYPE_STORED) {{
            setStoreTermVectors(true);
            freeze();
        }};
        doc.add(new Field("text", section.text(), textWithTV));

        indexWriter.addDocument(facetsConfig.build(doc));
    }


}
