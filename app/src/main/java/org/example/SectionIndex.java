package org.example;

import org.apache.lucene.document.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SectionIndex {

    private static String indexPath = "index";

    private Directory dir;
    private Analyzer analyzer;

    public SectionIndex() {
        try {
            // TODO
            Map<String, String> stopParams = new HashMap<>();
            stopParams.put("words", "stopwords-en.txt"); // https://github.com/stopwords-iso/stopwords-iso
            stopParams.put("format", "wordset");
            stopParams.put("ignoreCase", "true");

            this.analyzer = CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("lowercase")
                .addTokenFilter("englishpossessive")
                .addTokenFilter("stop", stopParams)
                .addTokenFilter("kstem")
                .build();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void build(Bible bible) {
        try {
            this.dir = FSDirectory.open(Paths.get(indexPath));
            IndexWriterConfig iwc = new IndexWriterConfig(this.analyzer);

            // overwrite any existing index
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            // keep various files separate
            iwc.setUseCompoundFile(false);

            IndexWriter writer = new IndexWriter(dir, iwc);
            // by chapter
            for (Bible.Section chapter : bible.getChapters()) {
                indexSection(writer, chapter);
            }

            // costly but optimizes search performance for static indexes
            System.out.println("merging");
            writer.forceMerge(1);
            writer.close();

            System.out.println("done");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void indexSection(IndexWriter indexWriter, Bible.Section section) throws IOException {
        Document doc = new Document();

        doc.add(new TextField("book_name", section.bookName(), Field.Store.YES));
        doc.add(new KeywordField("book_name.keyword", section.bookName(), Field.Store.NO));

        doc.add(new LongPoint("book", section.book())); // filtering
        doc.add(new NumericDocValuesField("book", section.book())); // sorting / faceting
        doc.add(new StoredField("book", section.book())); // retrieval

        doc.add(new LongPoint("chapter", section.chapter())); // filtering
        doc.add(new NumericDocValuesField("chapter", section.chapter())); // sorting / faceting
        doc.add(new StoredField("chapter", section.chapter())); // retrieval

        section.verse().ifPresent(v ->{
            doc.add(new LongPoint("verse", v)); // filtering
            doc.add(new NumericDocValuesField("verse", v)); // sorting / faceting
            doc.add(new StoredField("verse", v)); // retrieval
        });

        doc.add(new TextField("text", section.text(), Field.Store.YES));

        System.out.println(
            "adding "+section.bookName()+
            ", chapter "+section.chapter()+
            section.verse().map(v -> ", verse "+v).orElse("")
        );

        indexWriter.addDocument(doc);
    }

    public List<TopWords.ScoredWord> getScoredWords() {
        TopWords tw = new TopWords();
        try {
            IndexReader reader = DirectoryReader.open(dir);
            Terms terms = MultiTerms.getTerms(reader, "text");
            if (terms == null) {
                System.out.println("error no terms found!");
            } else {
                TermsEnum termsEnum = terms.iterator();
                BytesRef term;
                while ((term = termsEnum.next()) != null) {
                    String termStr = term.utf8ToString();
                    long tf = termsEnum.totalTermFreq();
                    long df = termsEnum.docFreq();
                    double idf = Math.log((reader.numDocs() + 1.0) / (df + 1.0));
                    double tfidf = tf * idf;
                    tw.maybeAddWord(termStr, tfidf);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return tw.getWords();
    }

}
