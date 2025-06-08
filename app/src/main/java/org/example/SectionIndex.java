package org.example;

import org.apache.lucene.document.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.*;
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
            this.analyzer = CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("lowercase")
                .addTokenFilter("englishpossessive")
                .addTokenFilter("stop", new HashMap<>(Map.of(
                   "format", "wordset",
                   "ignoreCase", "true",
                   "words", "stopwords-en.txt" // https://github.com/stopwords-iso/stopwords-iso
                )))
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
            // by verse
            for (Bible.Section verse : bible.getVerses()) {
                indexSection(writer, verse);
            }

            // costly but optimizes search performance for static indexes
            System.out.println("merging");
            writer.forceMerge(1);
            writer.close();

            System.out.println("done");
            System.out.println("");
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

        doc.add(new TextField("text", section.text(), Field.Store.YES)); // store term vectors too?

//        System.out.println(
//            "adding "+section.bookName()+
//            ", chapter "+section.chapter()+
//            section.verse().map(v -> ", verse "+v).orElse("")
//        );

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

    public void query() {
        try {
            String qStr = "jesus walking";

            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            StoredFields storedFields = searcher.storedFields();

            StandardQueryParser sqp = new StandardQueryParser(this.analyzer);
            Query q;
            try {
                q = sqp.parse(qStr, "text");
            } catch (QueryNodeException e) {
                System.out.println("query node exception during query parsing: "+e);
                return;
            }

            TopDocs topDocs = searcher.search(q, 10);
            ScoreDoc[] hits = topDocs.scoreDocs;

            System.out.println("hits: "+topDocs.totalHits.value());
            System.out.println("");
            for (int i = 0; i < hits.length; i++) {
                Document doc = storedFields.document(hits[i].doc);
                System.out.println(
                    "hit "+i+", "+
                    "score "+hits[i].score+": "+
                    doc.get("book_name")+
                    ", chapter "+doc.get("chapter")+
                    ", verse "+doc.get("verse")+
                    ": "+doc.get("text")
                );
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

}
