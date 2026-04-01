package net.noiraude.gtnhcredits.util;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.github.bsideup.jabel.Desugar;

public class FuzzyFinder {

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
            .map(username -> new ScoredUsername(username, damerauLevenshteinDist(username.toLowerCase(), lowerSearch)))
            .sorted(Comparator.comparingInt(s -> s.distance()))
            .collect(Collectors.toList());

        if (maxResults > 0 && scoredNames.size() > maxResults) {
            scoredNames = scoredNames.subList(0, maxResults);
        }

        return scoredNames.stream()
            .map(s -> s.username())
            .collect(Collectors.toList());
    }

    public static List<String> findMatchesWithThreshold(List<String> usernames, String searchTerm, int maxDistance) {

        String lowerSearch = searchTerm.toLowerCase();

        return usernames.stream()
            .map(username -> new ScoredUsername(username, damerauLevenshteinDist(username.toLowerCase(), lowerSearch)))
            .filter(s -> s.distance() <= maxDistance)
            .sorted(Comparator.comparingInt(s -> s.distance()))
            .map(s -> s.username())
            .collect(Collectors.toList());
    }

    private static boolean transpositionPairFrom(int i, int j, String source, String target) {
        if (i <= 1) return false;
        if (j <= 1) return false;
        if (source.charAt(i - 1) != target.charAt(j - 2)) return false;
        if (source.charAt(i - 2) != target.charAt(j - 1)) return false;
        return true;
    }

    @Desugar
    public record ScoredUsername(String username, int distance) {}
}
