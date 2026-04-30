package net.noiraude.libcredits.util;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class for use in "Fuzzy-Finding" Strings for use in a search bar
 * within the Credits screen
 * <p>
 * Algorithm Explanation:
 * <p>
 * This algorithm can be broken down into 4 "Phases" as detailed below:
 * <p>
 * - Exact Match: If the search term exactly matches a username, it gets a perfect score (0)
 * <p>
 * - Prefix Match: If the first n characters of a search term match the first n characters
 * of any username, it is given a score of PREFIX_DEFAULT + EXTRA_CHAR * (remaining_chars_of_username)
 * <p>
 * - Substring Match: If the search term exists as a substring anywhere in a given username, it is scored as
 * SUBSTRING_DEFAULT + (substring_start_index * SUBSTRING_INDEX_COST) +
 * (EXTRA_CHAR * remaining_chars_of_username)
 * <p>
 * - Failing all of these match types, the "fallback" scoring is as follows:
 * FUZZY_COST + prefix_distance * FUZZY_PREFIX_COST + full_distance * FUZZY_FULL_COST
 * Where prefix/full_distance are the Damerau-Levenshtein (D-L) distances
 * between the search term and the username
 * <p>
 * In short, the D-L distance can be summarized as "How many character changes need to be made for the strings to
 * match".
 * Valid changes are:
 * - Deletion
 * - Replacement
 * - Addition
 * - Transposition (i.e., swap character placements adjacently)
 *
 */
public class FuzzyFinder {

    private static final int PREFIX_LENIENCY = 5;
    // Costs for different matching types
    private static final double PREFIX_DEFAULT_COST = 0.1;

    private static final double SUBSTRING_DEFAULT_COST = 5.0;
    private static final double SUBSTRING_INDEX_COST = 0.5;

    private static final double FUZZY_COST = 10.0;
    private static final double FUZZY_PREFIX_COST = 3.0;
    private static final double FUZZY_FULL_COST = 0.5;

    private static final double EXTRA_CHAR_COST = 0.01;

    /**
     * Calculates the Damerau-Levenshtein (D-L) distance between two strings
     *
     * @see <a
     *      href=https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance>
     *      Damerau-Levenshtein Distance</a>
     *
     * @param source The source string to compare
     * @param target The target string to compare against
     *
     * @return The D-L distance between the two given strings
     *
     */
    public static int damerauLevenshteinDist(String source, String target) {
        if (source.equals(target)) return 0;
        int sourceLength = source.length();
        int targetLength = target.length();
        if (sourceLength == 0) return targetLength;
        if (targetLength == 0) return sourceLength;

        // Matrix to store distances
        int[][] distance = new int[sourceLength + 1][targetLength + 1];

        // Initialize first column/row of distance matrix
        for (int i = 0; i <= sourceLength; i++) {
            distance[i][0] = i;
        }
        for (int j = 0; j <= targetLength; j++) {
            distance[0][j] = j;
        }

        // Calculate distances
        for (int i = 1; i <= sourceLength; i++) {
            for (int j = 1; j <= targetLength; j++) {
                int cost = (source.charAt(i - 1) == target.charAt(j - 1)) ? 0 : 1;

                // spotless: off
                distance[i][j] = Math.min(
                    Math.min(
                        distance[i - 1][j] + 1, // deletion
                        distance[i][j - 1] + 1 // insertion
                    ),
                    distance[i - 1][j - 1] + cost // substitution
                );

                if (isTranspositionPair(i, j, source, target)) {
                    distance[i][j] = Math.min(distance[i][j], distance[i - 2][j - 2] + cost); // transposition
                }
                // spotless: on
            }
        }

        return distance[sourceLength][targetLength];
    }

    /**
     * Finds the closest matching usernames to the search term using "smart scoring"
     * based on Damerau-Levenshtein distances and returns matches meeting a score
     * threshold.
     *
     * @param usernames   List of usernames to find matches from
     * @param searchTerm  The term to search for
     * @param maxDistance Maximum allowed distance (lower = stricter matching)
     *
     * @return List of usernames within the distance threshold
     */
    public static List<String> findMatchesWithThreshold(List<String> usernames, String searchTerm, double maxDistance) {

        // Normalize search string
        String lowerSearch = searchTerm.toLowerCase();

        return usernames.stream()
            .map(username -> new ScoredUsername(username, calculateSmartScore(username.toLowerCase(), lowerSearch)))
            .filter(s -> s.score <= maxDistance)
            .sorted(Comparator.comparingDouble(s -> s.score))
            .map(s -> s.username)
            .collect(Collectors.toList());
    }

    /**
     * Calculates a "smart score" that prioritizes prefix and substring matches.
     * Lower score = Better match
     * <p>
     * Priority of calculation:
     * - Perfect match: score = 0
     * - Prefix match: score = prefix cost + (Extra char cost * extra char count)
     * - Substring match: score = substring cost + (substring start index *
     * substring index cost) + the same extra char cost
     * - No match: score = fuzzy cost + (prefix D-L distance * prefix weight) +
     * (full D-L distance * full weight)
     */
    public static double calculateSmartScore(String username, String searchTerm) {
        String usernameLower = username.toLowerCase();
        String searchLower = searchTerm.toLowerCase();

        // Perfect Match
        if (usernameLower.equals(searchLower)) {
            return 0.0;
        }

        // Prefix matching
        final double v = (username.length() - searchTerm.length()) * EXTRA_CHAR_COST;
        if (usernameLower.startsWith(searchLower)) {
            return PREFIX_DEFAULT_COST + v;
        }

        // Substring matching
        int substringIndex = usernameLower.indexOf(searchLower);
        if (substringIndex != -1) {
            return SUBSTRING_DEFAULT_COST + substringIndex * SUBSTRING_INDEX_COST + v;
        }

        // Calculate distance only on relevant prefix
        // Compare against a prefix of the username similar in length to search term
        int prefixLength = Math.min(username.length(), searchTerm.length() + PREFIX_LENIENCY);

        String usernamePrefix = usernameLower.substring(0, prefixLength);

        // Calculate D-L distance for both prefix and full username
        int prefixDistance = damerauLevenshteinDist(usernamePrefix, searchLower);
        int fullDistance = damerauLevenshteinDist(usernameLower, searchLower);

        // Weighted score favoring prefix matching, full distance for added context
        return FUZZY_COST + (prefixDistance * FUZZY_PREFIX_COST) + (fullDistance * FUZZY_FULL_COST);
    }

    /**
     * Helper method to determine whether two characters make a "transposition
     * pair", i.e., they are the same two characters, but in different adjacent
     * positions in a source vs. target
     *
     * @param sourceIndex Index of the source string being checked
     * @param targetIndex Index of the target string being checked
     * @param source      The source string to compare
     * @param target      The target string to compare against
     *
     * @return true if there is a valid transposition pair in the string at the
     *         given indices
     */
    private static boolean isTranspositionPair(int sourceIndex, int targetIndex, String source, String target) {
        if (sourceIndex <= 1) return false;
        if (targetIndex <= 1) return false;
        // Checks for transposition pairs such as
        // source substring = "ie"
        // target substring = "ei"
        if (source.charAt(sourceIndex - 1) != target.charAt(targetIndex - 2)) return false;
        return source.charAt(sourceIndex - 2) == target.charAt(targetIndex - 1);
    }

    /**
     * Helper class to pair usernames with their scores
     */
    public static final class ScoredUsername {

        public final String username;
        public final double score;

        public ScoredUsername(String username, double score) {
            this.username = username;
            this.score = score;
        }
    }
}
