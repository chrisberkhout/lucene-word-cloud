package org.example;

import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.List;

public class App {
    public static void main(String[] args) {
        Bible bible = new Bible();
        bible.load();
        SectionIndex index = new SectionIndex();
        index.build(bible);
        for (TopWords.ScoredWord w : index.getScoredWords()) {
            System.out.println(w.score() + "  " + w.word());
        }
    }
}
