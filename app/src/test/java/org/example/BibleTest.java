package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BibleTest {
    private Bible bible;

    @BeforeEach
    void setUp() {
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
    void firstChapterIsCorrect() {
        Bible.Chapter chapter = bible.getChapters().getFirst();
        assertTrue(chapter.text().startsWith("In the beginning God created the heavens and the earth. Now"));
        assertTrue(chapter.text().endsWith("There was evening and there was morning, a sixth day."));
        assertEquals(chapter.bookName(), "Genesis");
        assertEquals(chapter.book(), 1);
        assertEquals(chapter.chapter(), 1);
    }

    @Test
    void lastChapterIsCorrect() {
        Bible.Chapter chapter = bible.getChapters().getLast();
        assertTrue(chapter.text().startsWith("He showed me a river of water of life, clear as crystal, "+
            "proceeding out of the throne of God and of the Lamb, in the middle of its street."));
        assertTrue(chapter.text().endsWith("with all the saints.  Amen."));
        assertEquals(chapter.bookName(), "Revelation");
        assertEquals(chapter.book(), 66);
        assertEquals(chapter.chapter(), 22);
    }
}