package com.chrisberkhout.lucenewordcloud;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;

import java.io.IOException;
import java.util.List;

public class Server {

    private static final int port = 7070;
    private static final int topDocsNumber = 100;
    private static final int topTermsNumber = 200;

    private Searcher searcher;
    private Javalin api;
    private long[] versesPerBook;

    Server(Searcher searcher, long[] versesPerBook) {
        this.searcher = searcher;
        this.versesPerBook = versesPerBook;
        api = Javalin.create(config -> {
            config.staticFiles.add("/public");
        }).get("/search", this::searchHandler);
    }

    void start() {
        api.start(port);
    }

    void searchHandler(Context ctx) throws IOException, QueryNodeException {
        long startTime = System.nanoTime();

        String q = ctx.queryParam("q");
        Searcher.Result sr;

        if (q == null || q.isBlank()) {
            // Global top terms and per book counts for initial page load
            sr = new Searcher.Result(
                null,
                null,
                searcher.globalTopTerms(topTermsNumber),
                versesPerBook
            );
        } else {
            sr = searcher.search(q, topDocsNumber, topTermsNumber);
        }

        long durationMillis = ((System.nanoTime()-startTime) / 1_000_000);

        ctx.json(new SearchResponse(
            sr.hits(),
            sr.totalHits(),
            sr.hitsPerBook(),
            sr.topTerms(),
            durationMillis
        ));
    }

    record SearchResponse(
        List<Searcher.Hit> hits,
        Long totalHits,
        long[] hitsPerBook,
        List<TopTerms.ScoredTerm> topTerms,
        Long time
    ) {}

}