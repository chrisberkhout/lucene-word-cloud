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

    public List<TopTerms.ScoredTerm> globalTopTerms() throws IOException {
        final int numDocs = reader.numDocs();
        TopTerms tw = new TopTerms();

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

    public SearchResult query(String qStr) throws IOException {
        StandardQueryParser sqp = new StandardQueryParser(this.analyzer);

        Query q;
        try {
            q = sqp.parse(qStr, "text");
        } catch (QueryNodeException e) {
            throw new RuntimeException(e);
        }

        TopDocsAndCounts topDocsAndCounts = queryTopDocsAndCounts(q);
        List<TopTerms.ScoredTerm> topTerms = queryTopTerms(q);

        SearchResult qr = new SearchResult(
            null,
            topDocsAndCounts.totalHits(),
            topDocsAndCounts.topDocs(),
            topTerms,
            topDocsAndCounts.hitsByBook()
        );

        return qr;
    }

    private TopDocsAndCounts queryTopDocsAndCounts(Query q) throws IOException {
        // scored top N search and facet counts
        IndexSearcher searcher = new IndexSearcher(this.reader);
        FacetsCollectorManager fcm = new FacetsCollectorManager();
        FacetsCollectorManager.FacetsResult fr = FacetsCollectorManager.search(searcher, q, 100, fcm);
        TopDocs topDocs = fr.topDocs();

        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        StoredFields storedFields = searcher.storedFields();
        List<SearchResult.Hit> hits = new ArrayList<>();
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

        FacetsCollector fc = fr.facetsCollector();
        SortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(searcher.getIndexReader(), facetsConfig);
        Facets facets = new SortedSetDocValuesFacetCounts(state, fc);
        FacetResult facetResult = facets.getAllChildren("book_num");
        int[] hitsByBook = new int[66];
        if (facetResult != null) {
            for (LabelAndValue lv : facetResult.labelValues) {
                System.out.println("  " + lv.label + " (" + lv.value + ")");
                hitsByBook[Integer.parseInt(lv.label)-1] = lv.value.intValue();
            }
        }

        return new TopDocsAndCounts(hits, topDocs.totalHits.value(), hitsByBook);
    }

    private record TopDocsAndCounts(List<SearchResult.Hit> topDocs, long totalHits, int[] hitsByBook) {}

    private List<TopTerms.ScoredTerm> queryTopTerms(Query q) throws IOException {
        // unscored search to collect terms information from every match
        IndexSearcher searcher = new IndexSearcher(this.reader);

        TermFrequenciesCollector collector = new TermFrequenciesCollector(reader);
        searcher.search(q, collector);

        int totalHits = collector.getTotalHits();
        TopTerms tw = new TopTerms();
        for (Map.Entry<String,Frequencies> e : collector.getTermFrequencies().entrySet()) {
            Frequencies f = e.getValue();
            tw.maybeAddTerm(e.getKey(), f.total, f.docs, totalHits);
        }

        return tw.getTerms();
    }

}
