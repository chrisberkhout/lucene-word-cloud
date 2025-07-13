package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BibleTest {
    private Bible bible;

    @BeforeEach
    void setUp() {
        bible = new Bible();
        bible.load();
    }

    @Test
    void loadsTheFirstVerseCorrectly() {
        Bible.Section verse = bible.getVerses().getFirst();
        assertEquals(verse.bookName(), "Genesis");
        assertEquals(verse.book(), 1);
        assertEquals(verse.chapter(), 1);
        assertEquals(verse.verse().get(), 1);
        assertEquals(verse.text(), "In the beginning God created the heavens and the earth.");
    }

    @Test
    void loadsTheExpectedNumberOfVerses() {
        assertEquals(bible.getVerses().size(), 31102);
    }

    @Test
    void loadsTheLastVerseCorrectly() {
        Bible.Section verse = bible.getVerses().getLast();
        assertEquals(verse.bookName(), "Revelation");
        assertEquals(verse.book(), 66);
        assertEquals(verse.chapter(), 22);
        assertEquals(verse.verse().get(), 21);
        assertEquals(verse.text(), "The grace of the Lord Jesus Christ be with all the saints.  Amen.");
    }

    @Test
    void countsVersesPerBookCorrectly() {
        int[] counts = bible.getVersesPerBook();
        assertEquals(counts.length, 66);
        assertEquals(counts[0], 1533);
        assertEquals(counts[counts.length-1], 404);
    }

    @Test
    void firstChapterIsCorrect() {
        Bible.Section chapter = bible.getChapters().getFirst();
        assertTrue(chapter.text().startsWith("In the beginning God created the heavens and the earth. Now"));
        assertTrue(chapter.text().endsWith("There was evening and there was morning, a sixth day."));
        assertEquals(chapter.bookName(), "Genesis");
        assertEquals(chapter.book(), 1);
        assertEquals(chapter.chapter(), 1);
    }

    @Test
    void lastChapterIsCorrect() {
        Bible.Section chapter = bible.getChapters().getLast();
        assertTrue(chapter.text().startsWith("He showed me a river of water of life, clear as crystal, "+
            "proceeding out of the throne of God and of the Lamb, in the middle of its street."));
        assertTrue(chapter.text().endsWith("with all the saints.  Amen."));
        assertEquals(chapter.bookName(), "Revelation");
        assertEquals(chapter.book(), 66);
        assertEquals(chapter.chapter(), 22);
    }
}