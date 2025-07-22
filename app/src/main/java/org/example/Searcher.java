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

    Searcher(Analyzer analyzer, BaseDirectory dir) throws IOException {
        this.analyzer = analyzer;
        this.reader = DirectoryReader.open(dir);
    }

    public List<TopTerms.ScoredTerm> globalTopTerms(int n) throws IOException {
        final int numDocs = reader.numDocs();
        TopTerms tw = new TopTerms(n);

        Terms terms = MultiTerms.getTerms(reader, "text");
        if (terms != null) {
            TermsEnum termsEnum = terms.iterator();
            BytesRef term;
            while ((term = termsEnum.next()) != null) {
                String termStr = term.utf8ToString();
                long tf = termsEnum.totalTermFreq();
                long df = termsEnum.docFreq();
                tw.maybeAddTerm(termStr, tf, df, numDocs);
            }
        }
        return tw.getTerms();
    }

    public Result search(String qStr, int topDocsNumber, int topTermsNumber) throws IOException {
        StandardQueryParser sqp = new StandardQueryParser(this.analyzer);

        Query q;
        try {
            q = sqp.parse(qStr, "text");
        } catch (QueryNodeException e) {
            throw new RuntimeException(e);
        }

        TopDocsAndCounts topDocsAndCounts = searchTopDocsAndCounts(q, topDocsNumber);
        List<TopTerms.ScoredTerm> topTerms = searchTopTerms(q, topTermsNumber);

        Result qr = new Result(
            topDocsAndCounts.totalHits(),
            topDocsAndCounts.topDocs(),
            topTerms,
            topDocsAndCounts.hitsPerBook()
        );

        return qr;
    }

    record Result(
        Long totalHits,
        List<Hit> hits,
        List<TopTerms.ScoredTerm> topTerms,
        int[] hitsPerBook
    ) {}

    public record Hit(
        double score,
        String book,
        long chapter,
        long verse,
        String text
    ) {}

    private TopDocsAndCounts searchTopDocsAndCounts(Query q, int n) throws IOException {
        IndexSearcher searcher = new IndexSearcher(this.reader);
        FacetsCollectorManager fcm = new FacetsCollectorManager();
        FacetsCollectorManager.FacetsResult fr = FacetsCollectorManager.search(searcher, q, n, fcm);
        TopDocs topDocs = fr.topDocs();

        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        StoredFields storedFields = searcher.storedFields();
        List<Hit> hits = new ArrayList<>();
        for (int i = 0; i < scoreDocs.length; i++) {
            Document doc = storedFields.document(scoreDocs[i].doc);
            hits.add(
                new Hit(
                    scoreDocs[i].score,
                    doc.get("book"),
                    Long.parseLong(doc.get("chapter_num")),
                    Long.parseLong(doc.get("verse_num")),
                    doc.get("text")
                )
            );
        }

        FacetsCollector fc = fr.facetsCollector();
        SortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(searcher.getIndexReader(), facetsConfig);
        Facets facets = new SortedSetDocValuesFacetCounts(state, fc);
        FacetResult facetResult = facets.getAllChildren("book_num");
        int[] hitsPerBook = new int[66];
        if (facetResult != null) {
            for (LabelAndValue lv : facetResult.labelValues) {
                System.out.println("  " + lv.label + " (" + lv.value + ")");
                hitsPerBook[Integer.parseInt(lv.label)-1] = lv.value.intValue();
            }
        }

        return new TopDocsAndCounts(hits, topDocs.totalHits.value(), hitsPerBook);
    }

    private record TopDocsAndCounts(List<Hit> topDocs, long totalHits, int[] hitsPerBook) {}

    /*
     * An unscored search to collect terms information from every hit.
     *
     * It's a separate search for simplicity. It could be combined in a MultiCollector with the unscored facet counts
     * collection, but not with the scored top docs search.
     */
    private List<TopTerms.ScoredTerm> searchTopTerms(Query q, int n) throws IOException {
        IndexSearcher searcher = new IndexSearcher(this.reader);

        TermFrequenciesCollector collector = new TermFrequenciesCollector(reader);
        searcher.search(q, collector);

        int totalHits = collector.getTotalHits();
        TopTerms tw = new TopTerms(n);
        for (Map.Entry<String,TermFrequenciesCollector.Frequencies> e : collector.getTermFrequencies().entrySet()) {
            TermFrequenciesCollector.Frequencies f = e.getValue();
            tw.maybeAddTerm(e.getKey(), f.total, f.docs, totalHits);
        }

        return tw.getTerms();
    }

}