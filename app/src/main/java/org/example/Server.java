package org.example;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.IOException;

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
        SearchResult qr;

        if (q == null || q == "") {
            qr = new SearchResult(
                null,
                null,
                null,
                this.searcher.globalTopTerms(),
                versesPerBook
            );
        } else {
            qr = this.searcher.query(q);
        }

        long durationMillis = ((System.nanoTime()-startTime) / 1_000_000);

        ctx.json(new SearchResult(
            durationMillis,
            qr.totalHits(),
            qr.hits(),
            qr.topTerms(),
            qr.hitsByBook()
        ));
    }

}