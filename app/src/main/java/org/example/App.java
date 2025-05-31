package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class App {
    private static String indexPath = "index";

    private Directory dir;
    private Analyzer analyzer;

    public App() {
        try {
            this.dir = FSDirectory.open(Paths.get(this.indexPath));
            this.analyzer = CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("lowercase")
                .addTokenFilter("englishpossessive")
                .addTokenFilter("stop")
                .addTokenFilter("kstem")
                .build();

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
        }
    }

    public void buildIndex() {
        try {
            Bible bible = new Bible();
            bible.load();
            List<Bible.Verse> verses = bible.getVerses();

            IndexWriterConfig iwc = new IndexWriterConfig(this.analyzer);

            // overwrite any existing index
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            // keep various files separate
            iwc.setUseCompoundFile(false);

            try (IndexWriter writer = new IndexWriter(dir, iwc)) {

                // by verse
//                for (Bible.Verse verse : verses) {
//                    indexVerse(writer, verse);
//                }

                // by chapter
                verses.stream().collect(Collectors.groupingBy(
                    v -> List.of(v.bookName(), v.book(), v.chapter())
                )).forEach((bookChap, group) -> {
                    try {
                        indexChapter(
                            writer,
                            (String)bookChap.get(0),
                            (Integer)bookChap.get(1),
                            (Integer)bookChap.get(2),
                            group.stream().map(Bible.Verse::text).collect(Collectors.joining(" "))
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                });

                // costly but optimizes search performance for static indexes
                System.out.println("merging");
                writer.forceMerge(1);
            }
            System.out.println("done");
        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
        }
    }

    private void indexChapter(IndexWriter indexWriter, String bookName, int book, int chapter, String text) throws IOException {
        Document doc = new Document();

        doc.add(new TextField("book_name", bookName, Field.Store.YES));
        doc.add(new KeywordField("book_name.keyword", bookName, Field.Store.NO));

        doc.add(new LongPoint("book", book)); // filtering
        doc.add(new NumericDocValuesField("book", book)); // sorting / faceting
        doc.add(new StoredField("book", book)); // retrieval

        doc.add(new LongPoint("chapter", chapter)); // filtering
        doc.add(new NumericDocValuesField("chapter", chapter)); // sorting / faceting
        doc.add(new StoredField("chapter", chapter)); // retrieval

        doc.add(new TextField("text", text, Field.Store.YES));

        System.out.println("adding "+bookName+", chapter "+chapter);

        indexWriter.addDocument(doc);
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


    public void getWords() {
        TopWords tw = new TopWords();
        try {
            IndexReader reader = DirectoryReader.open(this.dir);

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

            for (TopWords.ScoredWord w : tw.getWords()) {
                System.out.println(w.score() + "  " + w.word());
            }

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        App app = new App();
        app.buildIndex();
        app.getWords();
    }
}
