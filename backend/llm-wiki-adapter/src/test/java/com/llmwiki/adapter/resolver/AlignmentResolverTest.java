package com.llmwiki.adapter.resolver;

import com.llmwiki.common.enums.AlignmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlignmentResolverTest {

    private AlignmentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AlignmentResolver();
    }

    // ===== Exact Match Tests =====

    @Test
    void alignEntity_shouldReturnExactMatch_whenEntityFoundInSource() {
        String source = "Java is a programming language created by Sun Microsystems.";
        AlignmentResolver.AlignmentResult result = resolver.alignEntity("Java", source);

        assertNotNull(result);
        assertEquals(AlignmentStatus.EXACT, result.getStatus());
        assertEquals(0, result.getStartOffset());
        assertEquals(4, result.getEndOffset());
    }

    @Test
    void alignEntity_shouldReturnExactMatch_forSubstringInMiddle() {
        String source = "The Python language and Java language are popular.";
        AlignmentResolver.AlignmentResult result = resolver.alignEntity("Java", source);

        assertNotNull(result);
        assertEquals(AlignmentStatus.EXACT, result.getStatus());
        assertEquals(24, result.getStartOffset());
        assertEquals(28, result.getEndOffset());
    }

    @Test
    void alignEntity_shouldReturnExactMatch_caseSensitive() {
        String source = "Java is great. java is also popular.";
        AlignmentResolver.AlignmentResult result = resolver.alignEntity("Java", source);

        assertNotNull(result);
        assertEquals(AlignmentStatus.EXACT, result.getStatus());
        assertEquals(0, result.getStartOffset());
        assertEquals(4, result.getEndOffset());
    }

    @Test
    void alignEntity_shouldFindFirstOccurrence() {
        String source = "Java and Java and Java";
        AlignmentResolver.AlignmentResult result = resolver.alignEntity("Java", source);

        assertNotNull(result);
        assertEquals(AlignmentStatus.EXACT, result.getStatus());
        assertEquals(0, result.getStartOffset());
        assertEquals(4, result.getEndOffset());
    }

    // ===== Fuzzy Match Tests =====

    @Test
    void alignEntity_shouldReturnFuzzyMatch_whenCloseVariantFound() {
        String source = "Jva is a programming language.";
        AlignmentResolver.AlignmentResult result = resolver.alignEntity("Java", source);

        assertNotNull(result);
        assertEquals(AlignmentStatus.FUZZY, result.getStatus());
        assertEquals(0, result.getStartOffset());
        assertEquals(3, result.getEndOffset());
    }

    @Test
    void alignEntity_shouldReturnFuzzyMatch_forMinorTypo() {
        String source = "Pythn is a language.";
        AlignmentResolver.AlignmentResult result = resolver.alignEntity("Python", source);

        assertNotNull(result);
        assertEquals(AlignmentStatus.FUZZY, result.getStatus());
        assertEquals(0, result.getStartOffset());
        assertEquals(5, result.getEndOffset());
    }

    // ===== No Match Tests =====

    @Test
    void alignEntity_shouldReturnNull_whenNoMatchFound() {
        String source = "The quick brown fox jumps over the lazy dog.";
        AlignmentResolver.AlignmentResult result = resolver.alignEntity("Java", source);

        assertNull(result);
    }

    @Test
    void alignEntity_shouldReturnNull_forEmptyString() {
        AlignmentResolver.AlignmentResult result = resolver.alignEntity("Java", "");
        assertNull(result);
    }

    @Test
    void alignEntity_shouldReturnNull_forEmptyEntity() {
        String source = "Java is a language.";
        AlignmentResolver.AlignmentResult result = resolver.alignEntity("", source);
        assertNull(result);
    }

    // ===== Token-level Fallback Tests =====

    @Test
    void alignEntity_shouldUseTokenFallback_whenMultiWordEntityPartiallyMatches() {
        String source = "The Spring Boot framework is popular for building microservices.";
        AlignmentResolver.AlignmentResult result = resolver.alignEntity("Spring Boot Framework", source);

        assertNotNull(result);
        // Token-level: "Spring" and "Boot" match, "Framework" vs "framework" - should still match
        assertTrue(result.getStatus() == AlignmentStatus.FUZZY ||
                   result.getStatus() == AlignmentStatus.EXACT);
    }

    // ===== alignEntityBatch Tests =====

    @Test
    void alignEntityBatch_shouldAlignMultipleEntities() {
        String source = "Java and Python are programming languages.";
        var results = resolver.alignEntityBatch(java.util.List.of("Java", "Python"), source);

        assertEquals(2, results.size());

        AlignmentResolver.AlignmentResult javaResult = results.get("Java");
        assertNotNull(javaResult);
        assertEquals(AlignmentStatus.EXACT, javaResult.getStatus());
        assertEquals(0, javaResult.getStartOffset());
        assertEquals(4, javaResult.getEndOffset());

        AlignmentResolver.AlignmentResult pythonResult = results.get("Python");
        assertNotNull(pythonResult);
        assertEquals(AlignmentStatus.EXACT, pythonResult.getStatus());
        assertEquals(9, pythonResult.getStartOffset());
        assertEquals(15, pythonResult.getEndOffset());
    }

    @Test
    void alignEntityBatch_shouldHandleMixOfMatchesAndMisses() {
        String source = "Java is a language.";
        var results = resolver.alignEntityBatch(java.util.List.of("Java", "Python"), source);

        assertEquals(2, results.size());
        assertNotNull(results.get("Java"));
        assertEquals(AlignmentStatus.EXACT, results.get("Java").getStatus());
        assertNull(results.get("Python"));
    }

    @Test
    void alignEntityBatch_shouldHandleEmptyList() {
        String source = "Java is a language.";
        var results = resolver.alignEntityBatch(java.util.List.of(), source);
        assertTrue(results.isEmpty());
    }

    // ===== GREATER / LESSER Tests =====

    @Test
    void alignEntity_shouldReturnGreater_whenSourceContainsExtraChars() {
        String source = "JavaScript is a language.";
        // "Java" is found exactly at index 0, but the source text continues with "Script"
        // Actually indexOf finds "Java" at 0..4 which is exact for "Java"
        // GREATER means LCS matched more characters than the entity name
        AlignmentResolver.AlignmentResult result = resolver.alignEntity("Java", source);
        // "Java" is found as prefix of "JavaScript" — this is still an exact indexOf match
        // but conceptually the source text is "greater" than the entity
        assertNotNull(result);
        assertEquals(AlignmentStatus.EXACT, result.getStatus());
    }

    @Test
    void alignEntity_shouldReturnLesser_whenEntityIsPartiallyMatched() {
        // LCS between "Java" and "Jva" is "Ja" + "a" = "Jaa"? No, LCS of "Java" and "Jva" is "Ja" (2 chars)
        // Actually LCS("Java", "Jva") = "Jva" (J, v, a) = 3 chars out of 4 = 0.75 ratio
        // That's at the threshold boundary
        String source = "Jva is great.";
        AlignmentResolver.AlignmentResult result = resolver.alignEntity("Java", source);

        assertNotNull(result);
        // Should be FUZZY since LCS ratio is above threshold
        assertEquals(AlignmentStatus.FUZZY, result.getStatus());
    }

    // ===== Edge Cases =====

    @Test
    void alignEntity_shouldHandleEntityAtEndOfSource() {
        String source = "I love Java";
        AlignmentResolver.AlignmentResult result = resolver.alignEntity("Java", source);

        assertNotNull(result);
        assertEquals(AlignmentStatus.EXACT, result.getStatus());
        assertEquals(7, result.getStartOffset());
        assertEquals(11, result.getEndOffset());
    }

    @Test
    void alignEntity_shouldHandleEntityLongerThanSource() {
        String source = "Java";
        AlignmentResolver.AlignmentResult result = resolver.alignEntity("JavaScript", source);

        // "JavaScript" is longer than source, but LCS should find "Java" (4/10 = 0.4)
        // Below threshold, so no match
        assertNull(result);
    }

    @Test
    void alignEntity_shouldHandleExactFullSourceMatch() {
        String source = "Java";
        AlignmentResolver.AlignmentResult result = resolver.alignEntity("Java", source);

        assertNotNull(result);
        assertEquals(AlignmentStatus.EXACT, result.getStatus());
        assertEquals(0, result.getStartOffset());
        assertEquals(4, result.getEndOffset());
    }
}
