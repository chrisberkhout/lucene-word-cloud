package org.example;

import org.apache.lucene.document.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.facet.*;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SectionIndex {

    private static String indexPath = "index";

    private Directory dir;
    private Analyzer analyzer;

    private FacetsConfig facetsConfig = new FacetsConfig();

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

        doc.add(new LongPoint("book", section.book())); // filtering
        doc.add(new SortedSetDocValuesFacetField("book", Integer.toString(section.book())));
//        doc.add(new NumericDocValuesField("book", section.book())); // sorting / faceting
        doc.add(new StoredField("book", section.book())); // retrieval

        doc.add(new LongPoint("chapter", section.chapter())); // filtering
        doc.add(new NumericDocValuesField("chapter", section.chapter())); // sorting / faceting
        doc.add(new StoredField("chapter", section.chapter())); // retrieval

        section.verse().ifPresent(v ->{
            doc.add(new LongPoint("verse", v)); // filtering
            doc.add(new NumericDocValuesField("verse", v)); // sorting / faceting
            doc.add(new StoredField("verse", v)); // retrieval
        });

        FieldType textWithTV = new FieldType(TextField.TYPE_STORED) {{
            setStoreTermVectors(true);
            freeze();
        }};
        doc.add(new Field("text", section.text(), textWithTV));

//        System.out.println(
//            "adding "+section.bookName()+
//            ", chapter "+section.chapter()+
//            section.verse().map(v -> ", verse "+v).orElse("")
//        );

        indexWriter.addDocument(facetsConfig.build(doc));
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

    public QueryResult nullQuery() {
        int[] versesCountPerChapter = new int[]{
            1533, 1213, 859, 1288, 959, 658, 618, 85, 810, 695, 816, 719, 942, 822, 280, 406, 167, 1070, 2461, 915, 222, 117, 1292, 1364, 154, 1273, 357, 197, 73, 146, 21, 48, 105, 47, 56, 53, 38, 211, 55, 1071, 678, 1151, 879, 1007, 433, 437, 257, 149, 155, 104, 95, 89, 47, 113, 83, 46, 25, 303, 108, 105, 61, 105, 13, 14, 25, 404
        };
        System.out.println("returning global result for null query");

        return new QueryResult(
            0,
            0,
            List.of(),
            getScoredWords(),
            versesCountPerChapter
        );
    }

    public QueryResult query(String qStr) {
        if (qStr == null || qStr == "") {
            return nullQuery();
        }
        long start = System.nanoTime();
        List<QueryResult.Hit> hits = new ArrayList<>();
        try {
            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            StoredFields storedFields = searcher.storedFields();

            StandardQueryParser sqp = new StandardQueryParser(this.analyzer);
            Query q;
            try {
                q = sqp.parse(qStr, "text");
            } catch (QueryNodeException e) {
                System.out.println("query node exception during query parsing: "+e);
                throw new RuntimeException(e);
            }

            // scored top N search

            FacetsCollectorManager fcm = new FacetsCollectorManager();
            FacetsCollectorManager.FacetsResult fr = FacetsCollectorManager.search(searcher, q, 100, fcm);
            TopDocs topDocs = fr.topDocs();

            FacetsCollector fc = fr.facetsCollector();
            SortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(searcher.getIndexReader(), facetsConfig);
            Facets facets = new SortedSetDocValuesFacetCounts(state, fc);
            FacetResult facetResult = facets.getAllChildren("book");

            System.out.println("Facet counts for 'book':");
            int[] hitsByBook = new int[66];
            if (facetResult != null) {
                for (LabelAndValue lv : facetResult.labelValues) {
                    System.out.println("  " + lv.label + " (" + lv.value + ")");
                    hitsByBook[Integer.parseInt(lv.label)-1] = lv.value.intValue();
                }
            } else {
                System.out.println("(none)");
            }

            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            System.out.println("totalHits: "+topDocs.totalHits.value());
            System.out.println("");
            for (int i = 0; i < scoreDocs.length; i++) {
                Document doc = storedFields.document(scoreDocs[i].doc);
                hits.add(
                    new QueryResult.Hit(
                        scoreDocs[i].score,
                        doc.get("book_name"),
                        Long.parseLong(doc.get("chapter")),
                        Long.parseLong(doc.get("verse")),
                        doc.get("text")
                    )
                );
            }
//            System.out.println("");

            // unscored search to collect terms information from every match

            class Freqs {
                long total;
                long docs;
                Freqs(long total, long docs) {
                    this.total = total;
                    this.docs = docs;
                }
            }
            Map<String,Freqs> freqs = new HashMap();

            Collector collector = new SimpleCollector() {
                private LeafReaderContext context;
                private int docBase;

                @Override
                protected void doSetNextReader(LeafReaderContext context) throws IOException {
                    this.context = context;
                    this.docBase = context.docBase;
                }

                @Override
                public void collect(int doc) throws IOException {
                    int globalDocId = docBase + doc;
                    Terms terms = reader.termVectors().get(globalDocId, "text");
                    if (terms == null) {
                        System.out.println("terms from term vectors null");
                    } else {
                        TermsEnum termsEnum = terms.iterator();
                        BytesRef term;
                        while ((term = termsEnum.next()) != null) {
                            String termStr = term.utf8ToString();
                            Freqs c = freqs.computeIfAbsent(termStr, k -> new Freqs(0,0));
                            c.total += termsEnum.totalTermFreq();
                            c.docs += 1;
                        }
                    }
                }

                @Override
                public ScoreMode scoreMode() {
                    return ScoreMode.COMPLETE_NO_SCORES;
                }
            };
            searcher.search(q, collector);

            TopWords tw = new TopWords();

            for (Map.Entry<String,Freqs> e : freqs.entrySet()) {
                Freqs f = e.getValue();
                double idf = Math.log((topDocs.totalHits.value() + 1.0) / (f.docs + 1.0));
                double tfidf = f.total * idf;
                tw.maybeAddWord(e.getKey(), tfidf);
            }


            long end = System.nanoTime();
            QueryResult qr = new QueryResult(
                ((end-start) / 1_000_000),
                topDocs.totalHits.value(),
                hits,
                tw.getWords(),
                hitsByBook
            );

            return qr;


        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

}
