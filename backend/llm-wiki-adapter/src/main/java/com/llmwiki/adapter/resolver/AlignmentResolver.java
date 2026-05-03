package com.llmwiki.adapter.resolver;

import com.llmwiki.common.enums.AlignmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Port of LangExtract's resolver.py 3-tier alignment strategy.
 *
 * Alignment tiers:
 * 1. Exact match via String.indexOf()
 * 2. Fuzzy match via LCS (Longest Common Subsequence) similarity
 * 3. Token-level alignment with density threshold
 */
@Component
public class AlignmentResolver {

    private static final Logger log = LoggerFactory.getLogger(AlignmentResolver.class);

    /** Minimum LCS ratio for a fuzzy match (0.75) */
    private static final double FUZZY_THRESHOLD = 0.75;

    /** Minimum token density for token-level fallback (1/3) */
    private static final double TOKEN_DENSITY_MIN = 1.0 / 3.0;

    /**
     * Result of aligning a single entity/concept name to the source text.
     */
    public static class AlignmentResult {
        private final int startOffset;
        private final int endOffset;
        private final AlignmentStatus status;

        public AlignmentResult(int startOffset, int endOffset, AlignmentStatus status) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.status = status;
        }

        public int getStartOffset() { return startOffset; }
        public int getEndOffset() { return endOffset; }
        public AlignmentStatus getStatus() { return status; }
    }

    /**
     * Align a single entity name to the source text using 3-tier strategy.
     *
     * @param entityName the entity/concept name to align
     * @param sourceText the original source text
     * @return AlignmentResult with position and status, or null if no match found
     */
    public AlignmentResult alignEntity(String entityName, String sourceText) {
        if (entityName == null || entityName.isEmpty() || sourceText == null || sourceText.isEmpty()) {
            return null;
        }

        // Tier 1: Exact match
        int exactIdx = sourceText.indexOf(entityName);
        if (exactIdx >= 0) {
            return new AlignmentResult(exactIdx, exactIdx + entityName.length(), AlignmentStatus.EXACT);
        }

        // Tier 2: Fuzzy match via sliding window LCS
        AlignmentResult fuzzyResult = fuzzyAlign(entityName, sourceText);
        if (fuzzyResult != null) {
            return fuzzyResult;
        }

        // Tier 3: Token-level fallback
        return tokenLevelAlign(entityName, sourceText);
    }

    /**
     * Align multiple entity names to the source text.
     *
     * @param entityNames list of entity/concept names to align
     * @param sourceText  the original source text
     * @return map from entity name to AlignmentResult (null if no match)
     */
    public Map<String, AlignmentResult> alignEntityBatch(List<String> entityNames, String sourceText) {
        Map<String, AlignmentResult> results = new LinkedHashMap<>();
        for (String name : entityNames) {
            results.put(name, alignEntity(name, sourceText));
        }
        return results;
    }

    /**
     * Tier 2: Fuzzy alignment using sliding window LCS similarity.
     * Slides a window of varying sizes across the source text and computes
     * LCS ratio against the entity name. If the best ratio exceeds the
     * fuzzy threshold, returns a FUZZY result.
     */
    private AlignmentResult fuzzyAlign(String entityName, String sourceText) {
        int entityLen = entityName.length();
        int sourceLen = sourceText.length();

        if (entityLen == 0 || sourceLen == 0) return null;

        int bestStart = -1;
        int bestEnd = -1;
        double bestRatio = 0;

        // Try windows of sizes: entityLen-2 to entityLen+2 (clamped to valid range)
        int minWin = Math.max(1, entityLen - 2);
        int maxWin = Math.min(sourceLen, entityLen + 2);

        for (int winSize = minWin; winSize <= maxWin; winSize++) {
            for (int start = 0; start <= sourceLen - winSize; start++) {
                int end = start + winSize;
                String window = sourceText.substring(start, end);
                double ratio = lcsRatio(entityName, window);
                if (ratio > bestRatio) {
                    bestRatio = ratio;
                    bestStart = start;
                    bestEnd = end;
                }
            }
        }

        if (bestRatio >= FUZZY_THRESHOLD) {
            log.debug("Fuzzy matched '{}' at [..{}] ratio={}", entityName, bestEnd, String.format("%.2f", bestRatio));
            AlignmentStatus status = bestRatio > entityName.length() / (double) (bestEnd - bestStart + 1)
                    ? AlignmentStatus.GREATER : AlignmentStatus.FUZZY;
            // Simplified: always FUZZY for fuzzy tier matches
            return new AlignmentResult(bestStart, bestEnd, AlignmentStatus.FUZZY);
        }

        return null;
    }

    /**
     * Tier 3: Token-level alignment.
     * Split both entity name and source text into tokens (words).
     * Find the densest window of source tokens containing entity tokens.
     * If density >= TOKEN_DENSITY_MIN, return LESSER status.
     */
    private AlignmentResult tokenLevelAlign(String entityName, String sourceText) {
        String[] entityTokens = entityName.toLowerCase().split("\\s+");
        String[] sourceTokens = sourceText.split("\\s+");

        if (entityTokens.length == 0 || sourceTokens.length == 0) return null;

        Set<String> entityTokenSet = new HashSet<>(Arrays.asList(entityTokens));

        int bestStart = -1;
        int bestEnd = -1;
        double bestDensity = 0;

        // Sliding window over source tokens
        for (int start = 0; start < sourceTokens.length; start++) {
            for (int end = start + 1; end <= Math.min(start + entityTokens.length + 2, sourceTokens.length); end++) {
                int matchCount = 0;
                for (int i = start; i < end; i++) {
                    String token = sourceTokens[i].toLowerCase().replaceAll("[^a-z0-9]", "");
                    if (entityTokenSet.contains(token)) {
                        matchCount++;
                    }
                }
                double density = (double) matchCount / (end - start);
                if (density > bestDensity) {
                    bestDensity = density;
                    bestStart = start;
                    bestEnd = end;
                }
            }
        }

        if (bestDensity >= TOKEN_DENSITY_MIN) {
            // Convert token positions back to char offsets
            int charStart = findCharOffset(sourceText, sourceTokens, bestStart);
            int charEnd = findCharOffset(sourceText, sourceTokens, bestEnd - 1)
                    + sourceTokens[bestEnd - 1].length();

            log.debug("Token-level matched '{}' at [{}..{}] density={}",
                    entityName, charStart, charEnd, String.format("%.2f", bestDensity));
            return new AlignmentResult(charStart, charEnd, AlignmentStatus.LESSER);
        }

        return null;
    }

    /**
     * Compute the LCS (Longest Common Subsequence) ratio between two strings.
     * Returns LCS length / max(len1, len2).
     */
    private double lcsRatio(String s1, String s2) {
        int lcsLength = lcsLength(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        return maxLen == 0 ? 0 : (double) lcsLength / maxLen;
    }

    /**
     * Compute the length of the Longest Common Subsequence of two strings.
     */
    private int lcsLength(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        if (m == 0 || n == 0) return 0;

        // Use two-row DP to save memory
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    curr[j] = prev[j - 1] + 1;
                } else {
                    curr[j] = Math.max(prev[j], curr[j - 1]);
                }
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[n];
    }

    /**
     * Find the character offset of the start of token at tokenIndex in the source text.
     */
    private int findCharOffset(String sourceText, String[] tokens, int tokenIndex) {
        int offset = 0;
        for (int i = 0; i < tokenIndex; i++) {
            int idx = sourceText.indexOf(tokens[i], offset);
            if (idx >= 0) {
                offset = idx + tokens[i].length();
            }
        }
        int idx = sourceText.indexOf(tokens[tokenIndex], offset);
        return idx >= 0 ? idx : 0;
    }
}
