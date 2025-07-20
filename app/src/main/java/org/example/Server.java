package org.example;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.io.IOException;

public class Server {

    Searcher searcher;
    Javalin api;

    private static final int port = 7070;

    Server(Searcher searcher) {
        this.searcher = searcher;
        this.api = Javalin.create(config -> {
            config.staticFiles.add("/public");
        }).get("/search", this::searchHandler);
    }

    void start() {
        this.api.start(port);
    }

    void searchHandler(Context ctx) throws IOException {
        String q = ctx.queryParam("q");
        SearchResult qr = this.searcher.query(q);
        ctx.json(qr);
    }

}