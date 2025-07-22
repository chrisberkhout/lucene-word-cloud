package com.chrisberkhout.lucenewordcloud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibleTest {
    private Bible bible;

    @BeforeEach
    void setUp() throws IOException {
        bible = new Bible();
        bible.load();
    }

    @Test
    void loadsTheFirstVerseCorrectly() {
        Bible.Verse verse = bible.getVerses().getFirst();
        assertEquals(verse.bookName(), "Genesis");
        assertEquals(verse.book(), 1);
        assertEquals(verse.chapter(), 1);
        assertEquals(verse.verse(), 1);
        assertEquals(verse.text(), "In the beginning God created the heavens and the earth.");
    }

    @Test
    void loadsTheExpectedNumberOfVerses() {
        assertEquals(bible.getVerses().size(), 31102);
    }

    @Test
    void loadsTheLastVerseCorrectly() {
        Bible.Verse verse = bible.getVerses().getLast();
        assertEquals(verse.bookName(), "Revelation");
        assertEquals(verse.book(), 66);
        assertEquals(verse.chapter(), 22);
        assertEquals(verse.verse(), 21);
        assertEquals(verse.text(), "The grace of the Lord Jesus Christ be with all the saints.  Amen.");
    }

    @Test
    void countsVersesPerBookCorrectly() {
        long[] counts = bible.getVersesPerBook();
        assertEquals(counts.length, 66);
        assertEquals(counts[0], 1533);
        assertEquals(counts[counts.length-1], 404);
    }

}