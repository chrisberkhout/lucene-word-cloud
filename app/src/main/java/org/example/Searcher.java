package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.*;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Searcher {

    private Analyzer analyzer;
    private IndexReader reader;
    private FacetsConfig facetsConfig = new FacetsConfig();

    public Searcher(Analyzer analyzer, BaseDirectory dir) throws IOException {
        this.analyzer = analyzer;
        this.reader = DirectoryReader.open(dir);
    }

    public List<TopTerms.ScoredTerm> getScoredWords() {
        TopTerms tw = new TopTerms();
        try {
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
                    tw.maybeAddTerm(termStr, tfidf);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return tw.getTerms();
    }

    public SearchResult nullQuery() {
        // TODO replace with Bible#getVersesPerBook
        int[] versesCountPerBook = new int[]{
            1533, 1213, 859, 1288, 959, 658, 618, 85, 810, 695, 816, 719, 942, 822, 280, 406, 167, 1070, 2461, 915, 222, 117, 1292, 1364, 154, 1273, 357, 197, 73, 146, 21, 48, 105, 47, 56, 53, 38, 211, 55, 1071, 678, 1151, 879, 1007, 433, 437, 257, 149, 155, 104, 95, 89, 47, 113, 83, 46, 25, 303, 108, 105, 61, 105, 13, 14, 25, 404
        };
        System.out.println("returning global result for null query");

        return new SearchResult(
            0,
            0,
            List.of(),
            getScoredWords(),
            versesCountPerBook
        );
    }

    public SearchResult query(String qStr) {
        if (qStr == null || qStr == "") {
            return nullQuery();
        }
        long start = System.nanoTime();
        List<SearchResult.Hit> hits = new ArrayList<>();
        try {
            IndexSearcher searcher = new IndexSearcher(this.reader);
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
            FacetResult facetResult = facets.getAllChildren("book_num");

            System.out.println("Facet counts for 'book_num':");
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
                    new SearchResult.Hit(
                        scoreDocs[i].score,
                        doc.get("book"),
                        Long.parseLong(doc.get("chapter_num")),
                        Long.parseLong(doc.get("verse_num")),
                        doc.get("text")
                    )
                );
            }

            // unscored search to collect terms information from every match
            TermFrequenciesCollector collector = new TermFrequenciesCollector(reader);
            searcher.search(q, collector);

            TopTerms tw = new TopTerms();

            for (Map.Entry<String,Frequencies> e : collector.getTermFrequencies().entrySet()) {
                Frequencies f = e.getValue();
                double idf = Math.log((topDocs.totalHits.value() + 1.0) / (f.docs + 1.0));
                double tfidf = f.total * idf;
                tw.maybeAddTerm(e.getKey(), tfidf);
            }


            long end = System.nanoTime();
            SearchResult qr = new SearchResult(
                ((end-start) / 1_000_000),
                topDocs.totalHits.value(),
                hits,
                tw.getTerms(),
                hitsByBook
            );

            return qr;


        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
