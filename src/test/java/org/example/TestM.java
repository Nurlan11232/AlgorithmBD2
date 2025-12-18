package org.example;
import  org.example.Main.Hyphenator;
import org.example.Main.GreedyJustifier;
import org.example.Main.DPJustifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Arrays;

public class TestM {
    private Hyphenator enHyphenator;
    private Hyphenator mnHyphenator;

    @BeforeEach
    public void setUp() {
        enHyphenator = new Hyphenator("hyph_en_US.xml", null, 2, 3);
        mnHyphenator = new Hyphenator("hyph_mn_MN.xml", null, 2, 2);
    }

    @Test
    public void testEnglishHyphenationWithFOP() {
        Hyphenator.Hyphenation hyp = enHyphenator.hyphenate("computer");
        assertNotNull(hyp, "Computer гэдэг үг хуваагдах ёстой");

        assertEquals(2, hyp.length());
        assertEquals("com", hyp.getPart(0));
        assertEquals("puter", hyp.getPart(1));
    }

    @Test
    public void testMongolianSpecificHyphenation() {
        Hyphenator.Hyphenation hyp = mnHyphenator.hyphenate("хатагтай");

        assertNotNull(hyp, "Хатагтай гэдэг үг хуваагдах ёстой");

        assertEquals(3, hyp.length(), "Үг яг 3 хэсэгт (ха-таг-тай) хуваагдсан байх ёстой");

        assertEquals("ха", hyp.getPart(0), "Эхний хэсэг 'ха' байх ёстой");
        assertEquals("таг", hyp.getPart(1), "Хоёр дахь хэсэг 'таг' байх ёстой");
        assertEquals("тай", hyp.getPart(2), "Гурван дахь хэсэг 'тай' байх ёстой");
    }
    @Test
    public void testGreedyJustifierAccuracy() {
        GreedyJustifier greedy = new GreedyJustifier(enHyphenator);
        String[] words = {"Test", "for", "the", "greedy", "justifier", "with", "a", "short", "width."};
        int width = 15;

        List<String> lines = greedy.justify(words, width);

        assertEquals(4, lines.size(), "4 мөр үүсэх ёстой");
        assertEquals("Test for the   ", lines.get(0));
        assertEquals("greedy         ", lines.get(1));
        assertEquals("justifier with ", lines.get(2));
        assertEquals("a short width. ", lines.get(3));
    }

    @Test
    public void testDPJustifierOptimalResult() {
        DPJustifier dp = new DPJustifier(enHyphenator);
        String[] words = {"The", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog"};
        int width = 10;

        List<String> lines = dp.justify(words, width);

        assertEquals(4, lines.size());
        assertEquals("The quick ", lines.get(0));
        assertEquals("brown fox ", lines.get(1));
        assertEquals("jumps over", lines.get(2));
        assertEquals("the lazy  ", lines.get(3));
    }
}
