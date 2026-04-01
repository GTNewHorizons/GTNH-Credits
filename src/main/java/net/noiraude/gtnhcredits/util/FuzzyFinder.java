package net.noiraude.gtnhcredits.util;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.github.bsideup.jabel.Desugar;

public class FuzzyFinder {

    private static final double PREFIX_DEFAULT_COST = 0.1;
    private static final double SUBSTRING_DEFAULT_COST = 5.0;
    private static final double SUBSTRING_INDEX_COST = 0.5;
    private static final double FUZZY_COST = 10.0;
    private static final double EXTRA_CHAR_COST = 0.01;

    private static final double FUZZY_PREFIX_COST = 3.0;
    private static final double FUZZY_FULL_COST = 0.5;

    public static int damerauLevenshteinDist(String source, String target) {
        if (source.equals(target)) return 0;
        int sourceLength = source.length();
        int targetLength = target.length();
        if (sourceLength == 0) return targetLength;
        if (targetLength == 0) return sourceLength;

        int[][] distance = new int[sourceLength + 1][targetLength + 1];

        for (int i = 0; i <= sourceLength; i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= targetLength; j++) {
            distance[0][j] = j;
        }

        for (int i = 1; i <= sourceLength; i++) {
            for (int j = 1; j <= targetLength; j++) {
                int cost = (source.charAt(i - 1) == target.charAt(j - 1)) ? 0 : 1;

                distance[i][j] = Math
                    .min(Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1), distance[i - 1][j - 1] + cost);

                if (transpositionPairFrom(i, j, source, target)) {
                    distance[i][j] = Math.min(distance[i][j], distance[i - 2][j - 2] + cost);
                }
            }
        }

        return distance[sourceLength][targetLength];
    }

    public static List<String> findClosestMatches(List<String> usernames, String searchTerm, int maxResults) {
        String lowerSearch = searchTerm.toLowerCase();

        List<ScoredUsername> scoredNames = usernames.stream()
            .map(username -> new ScoredUsername(username, calculateSmartScore(username, searchTerm)))
            .sorted(Comparator.comparingDouble(s -> s.score()))
            .collect(Collectors.toList());

        if (maxResults > 0 && scoredNames.size() > maxResults) {
            scoredNames = scoredNames.subList(0, maxResults);
        }

        return scoredNames.stream()
            .map(s -> s.username())
            .collect(Collectors.toList());
    }

    public static List<String> findMatchesWithThreshold(List<String> usernames, String searchTerm, double maxDistance) {

        String lowerSearch = searchTerm.toLowerCase();

        return usernames.stream()
            .map(username -> new ScoredUsername(username, calculateSmartScore(username.toLowerCase(), lowerSearch)))
            .filter(s -> s.score() <= maxDistance)
            .sorted(Comparator.comparingDouble(s -> s.score()))
            .map(s -> s.username())
            .collect(Collectors.toList());
    }

    public static double calculateSmartScore(String username, String searchTerm) {
        String usernameLower = username.toLowerCase();
        String searchLower = searchTerm.toLowerCase();

        // Perfect Match
        if (usernameLower.equals(searchLower)) {
            return 0.0;
        }

        // Prefix matching
        if (usernameLower.startsWith(searchLower)) {
            return PREFIX_DEFAULT_COST + (username.length() - searchTerm.length()) * EXTRA_CHAR_COST;
        }

        int substringIndex = usernameLower.indexOf(searchLower);
        if (substringIndex != -1) {
            return SUBSTRING_DEFAULT_COST + substringIndex * SUBSTRING_INDEX_COST
                + (username.length() - searchTerm.length()) * EXTRA_CHAR_COST;
        }

        int prefixLength = Math.min(username.length(), searchTerm.length() + 5); // Allow a few extra characters

        String usernamePrefix = usernameLower.substring(0, prefixLength);
        int prefixDistance = damerauLevenshteinDist(usernamePrefix, searchLower);

        int fullDistance = damerauLevenshteinDist(usernameLower, searchLower);

        return FUZZY_COST + (prefixDistance * 3.0) + (fullDistance * 0.5);
    }

    private static boolean transpositionPairFrom(int i, int j, String source, String target) {
        if (i <= 1) return false;
        if (j <= 1) return false;
        if (source.charAt(i - 1) != target.charAt(j - 2)) return false;
        if (source.charAt(i - 2) != target.charAt(j - 1)) return false;
        return true;
    }

    @Desugar
    public record ScoredUsername(String username, double score) {}
}
