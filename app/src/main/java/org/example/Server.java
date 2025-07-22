package org.example;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.IOException;
import java.util.List;

public class Server {

    Searcher searcher;
    Javalin api;
    int[] versesPerBook;

    private static final int port = 7070;

    Server(Searcher searcher, int[] versesPerBook) {
        this.searcher = searcher;
        this.versesPerBook = versesPerBook;
        this.api = Javalin.create(config -> {
            config.staticFiles.add("/public");
        }).get("/search", this::searchHandler);
    }

    void start() {
        this.api.start(port);
    }

    void searchHandler(Context ctx) throws IOException {
        long startTime = System.nanoTime();

        String q = ctx.queryParam("q");
        Searcher.Result sr;

        if (q == null || q == "") {
            // Global top terms and per book counts for initial page load
            sr = new Searcher.Result(null, null, this.searcher.globalTopTerms(), versesPerBook);
        } else {
            sr = this.searcher.search(q);
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
        int[] hitsPerBook,
        List<TopTerms.ScoredTerm> topTerms,
        Long time
    ) {}

}