//mvn exec:java -Dexec.args="/home/nurlan/Documents/Algorithm/Biedaalt2/Java/src/main/java/org/example/input.txt 50 mn"
package org.example;
import org.apache.fop.hyphenation.HyphenationTree;
import org.xml.sax.InputSource;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public class Main {
    public static void main(String[] args) {
        if (args.length >= 3) {
            run(args);
        }
    }
    private static void run(String[] args) {
        String inputFile = args[0];
        int maxWidth;
        String lang = args[2].toLowerCase();

        try {
            maxWidth = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Алдаа: maxWidth нь тоо байх ёстой.");
            System.exit(1);
            return;
        }

        String text;
        try {
            text = new String(Files.readAllBytes(Paths.get(inputFile)), "UTF-8");
        } catch (IOException e) {
            System.err.println("Алдаа: Файл уншиж чадсангүй - " + inputFile);
            System.exit(1);
            return;
        }
        String[] words = text.trim().split("\\s+");

        // Apache FOP Hyphenator ашиглах
        Hyphenator hyphenator;
        String langName;

        if (lang.equals("mn") || lang.equals("mongolian")) {
            hyphenator = new Hyphenator("hyph_mn_MN.xml", null, 2, 2);
            langName = "Монгол";
        } else if (lang.equals("en") || lang.equals("english")) {
            hyphenator = new Hyphenator("hyph_en_US.xml", null, 2, 3);
            langName = "Англи";
        } else {
            System.err.println("Алдаа: Дэмжигдээгүй хэл - " + lang);
            System.exit(1);
            return;
        }

        TextJustifier greedy = new GreedyJustifier(hyphenator);
        TextJustifier dp = new DPJustifier(hyphenator);

        List<String> greedyLine = greedy.justify(words, maxWidth);
        List<String> dpLines = dp.justify(words, maxWidth);
        System.out.println("=".repeat(60));
        System.out.println("Файл: " + inputFile);
        System.out.println("Мөрийн дээд урт: " + maxWidth);
        System.out.println("Хэл: " + langName);
        System.out.println("=".repeat(60));

        System.out.println("\n--- Greedy алгоритм ---");
        for (String line : greedyLine) {
            System.out.println("|" + line + "|");
        }

        System.out.println("\n--- Dynamic Programming алгоритм ---");
        for (String line : dpLines) {
            System.out.println("|" + line + "|");
        }
        System.out.println("\n=== Алгоритмийн харьцуулалт ===");

        long start1 = System.nanoTime();
        greedy.justify(words, maxWidth);
        long greedyTime = System.nanoTime() - start1;

        long start2 = System.nanoTime();
        dp.justify(words, maxWidth);
        long dpTime = System.nanoTime() - start2;

        int greedyLinesCount = greedyLine.size();
        int dpLinesCount = dpLines.size();

        System.out.println("Greedy хугацаа: " + greedyTime / 1_000_000.0 + " ms");
        System.out.println("DP хугацаа: " + dpTime / 1_000_000.0 + " ms");

        System.out.println("Greedy мөрийн тоо: " + greedyLinesCount);
        System.out.println("DP мөрийн тоо: " + dpLinesCount);


    }


    static class Hyphenator {
        private final HyphenationTree hyphenationTree;
        private final int leftMin;
        private final int rightMin;

        public Hyphenator(String xmlParam, String unused, int leftMin, int rightMin) {
            this.leftMin = leftMin;
            this.rightMin = rightMin;
            this.hyphenationTree = new HyphenationTree();

            String[] prefixes = { "Dict-mn/", "Dict-en/", "" };
            InputStream is = null;

            for (String prefix : prefixes) {
                String path = prefix + xmlParam;
                is = getClass().getClassLoader().getResourceAsStream(path);
                if (is != null)
                    break;
            }

            if (is != null) {
                try {
                    hyphenationTree.loadPatterns(new InputSource(is));
                } catch (Exception e) {
                    System.err.println("Error loading hyphenation patterns: " + e.getMessage());
                }
            } else {
                System.err.println("Hyphenation pattern file not found: " + xmlParam);
            }
        }

        public Hyphenation hyphenate(String word) {
            try {
                org.apache.fop.hyphenation.Hyphenation fopHyphenation =
                        hyphenationTree.hyphenate(word, leftMin, rightMin);
                if (fopHyphenation == null) {
                    return null;
                }

                int[] points = fopHyphenation.getHyphenationPoints();
                if (points == null || points.length == 0) {
                    return null;
                }

                List<String> parts = new ArrayList<>();
                int start = 0;
                for (int point : points) {
                    parts.add(word.substring(start, point));
                    start = point;
                }
                parts.add(word.substring(start));

                return new Hyphenation(parts);

            } catch (Exception e) {
                return null;
            }
        }

        public static class Hyphenation {
            private final List<String> parts;

            public Hyphenation(List<String> parts) {
                this.parts = parts;
            }

            public int length() {
                return parts.size();
            }

            public String getPart(int i) {
                return parts.get(i);
            }
        }
    }

    interface TextJustifier {
        List<String> justify(String[] words, int maxWidth);

        static String buildLine(String[] words, int from, int to, int maxWidth, boolean isLastLine) {
            int numWords = to - from;

            if (numWords == 1 || isLastLine) {
                StringBuilder sb = new StringBuilder();
                sb.append(words[from]);
                for (int i = from + 1; i < to; i++) {
                    sb.append(' ');
                    sb.append(words[i]);
                }
                while (sb.length() < maxWidth) {
                    sb.append(' ');
                }
                return sb.toString();
            }

            int totalWordLen = 0;
            for (int i = from; i < to; i++) {
                totalWordLen += words[i].length();
            }

            int totalSpaces = maxWidth - totalWordLen;
            int gaps = numWords - 1;

            int baseSpaces = totalSpaces / gaps;
            int extraSpaces = totalSpaces % gaps;

            StringBuilder sb = new StringBuilder();

            for (int i = from; i < to; i++) {
                sb.append(words[i]);
                if (i == to - 1)
                    break;

                int spacesToInsert = baseSpaces + (extraSpaces > 0 ? 1 : 0);
                if (extraSpaces > 0)
                    extraSpaces--;

                for (int s = 0; s < spacesToInsert; s++) {
                    sb.append(' ');
                }
            }

            return sb.toString();
        }
    }

    static class GreedyJustifier implements TextJustifier {
        private final Hyphenator hyphenator;

        GreedyJustifier(Hyphenator hyphenator) {
            this.hyphenator = hyphenator;
        }

        @Override
        public List<String> justify(String[] words, int maxWidth) {
            List<String> wordList = new ArrayList<>(List.of(words));
            List<String> lines = new ArrayList<>();

            int i = 0;
            while (i < wordList.size()) {
                int n = wordList.size();
                if (wordList.get(i).length() > maxWidth) {
                    splitFirstWordIfTooLong(wordList, i, maxWidth);
                }

                int lineLen = wordList.get(i).length();
                int j = i + 1;

                while (j < n) {
                    String w = wordList.get(j);
                    int need = lineLen + 1 + w.length();

                    if (need <= maxWidth) {
                        lineLen = need;
                        j++;
                    } else {
                        int remaining = maxWidth - (lineLen + 1);
                        if (remaining > 0) {
                            String[] split = bestHyphenSplit(w, remaining);

                            if (split != null) {
                                String left = split[0];
                                String right = split[1];

                                wordList.set(j, left);
                                wordList.add(j + 1, right);

                                lineLen = lineLen + 1 + left.length();
                                j++;
                            }
                        }
                        break;
                    }
                }

                boolean isLastLine = (j == wordList.size());
                String[] currentWords = wordList.toArray(new String[0]);
                lines.add(TextJustifier.buildLine(currentWords, i, j, maxWidth, isLastLine));
                i = j;
            }

            return lines;
        }

        private void splitFirstWordIfTooLong(List<String> wordList, int index, int maxWidth) {
            String w = wordList.get(index);
            String[] split = bestHyphenSplit(w, maxWidth);
            if (split == null) {
                if (w.length() > maxWidth) {
                    String forcedLeft = w.substring(0, maxWidth - 1) + "-";
                    String forcedRight = w.substring(maxWidth - 1);
                    wordList.set(index, forcedLeft);
                    wordList.add(index + 1, forcedRight);
                }
            } else {
                wordList.set(index, split[0]);
                wordList.add(index + 1, split[1]);
            }
        }

        private String[] bestHyphenSplit(String word, int remainingWidth) {
            Hyphenator.Hyphenation h = hyphenator.hyphenate(word);
            if (h == null)
                return null;

            String bestLeft = null;
            String bestRight = null;

            StringBuilder currentLeft = new StringBuilder();

            for (int k = 0; k < h.length() - 1; k++) {
                currentLeft.append(h.getPart(k));

                String candidateLeft = currentLeft.toString() + "-";
                if (candidateLeft.length() <= remainingWidth) {
                    bestLeft = candidateLeft;
                    StringBuilder rightSb = new StringBuilder();
                    for (int m = k + 1; m < h.length(); m++) {
                        rightSb.append(h.getPart(m));
                    }
                    bestRight = rightSb.toString();
                } else {
                    break;
                }
            }

            if (bestLeft != null) {
                return new String[] { bestLeft, bestRight };
            }
            return null;
        }
    }

    static class DPJustifier implements TextJustifier {
        private final Hyphenator hyphenator;
        private final Map<String, List<String[]>> hyphenCache = new HashMap<>();
        private static final double HYPHEN_PENALTY = 1.0;
        private static final double OVERFLOW_PENALTY = 10000.0;
        private static final double FALLBACK_PENALTY = 20000.0;

        DPJustifier(Hyphenator hyphenator) {
            this.hyphenator = hyphenator;
        }
        private static class State {
            final int index;
            final String carry;

            State(int index, String carry) {
                this.index = index;
                this.carry = carry;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof State))
                    return false;
                State s = (State) o;
                return index == s.index && Objects.equals(carry, s.carry);
            }

            @Override
            public int hashCode() {
                return Objects.hash(index, carry);
            }
        }

        private static class Result {
            final double cost;
            final List<String> lines;

            Result(double cost, List<String> lines) {
                this.cost = cost;
                this.lines = lines;
            }
        }

        @Override
        public List<String> justify(String[] words, int maxWidth) {
            return solve(words, maxWidth, 0, null, new HashMap<>()).lines;
        }

        private Result solve(String[] words, int maxWidth, int i, String carry, Map<State, Result> memo) {
            State state = new State(i, carry);
            if (memo.containsKey(state))
                return memo.get(state);

            if (i >= words.length && carry == null) {
                return memoize(state, memo, new Result(0.0, new ArrayList<>()));
            }

            if (carry != null && carry.length() > maxWidth) {
                return memoize(state, memo, handleOverflowCarry(words, maxWidth, i, carry, memo));
            }

            Result best = findBestLayout(words, maxWidth, i, carry, memo);

            if (best == null) {
                best = createFallback(words, maxWidth, i, carry);
            }

            return memoize(state, memo, best);
        }

        private Result handleOverflowCarry(String[] words, int maxWidth, int i, String carry, Map<State, Result> memo) {
            List<String> lines = new ArrayList<>();
            String remaining = carry;

            while (remaining.length() > maxWidth) {
                lines.add(remaining.substring(0, maxWidth));
                remaining = remaining.substring(maxWidth);
            }

            Result rest = solve(words, maxWidth, i, remaining, memo);
            lines.addAll(rest.lines);
            return new Result(OVERFLOW_PENALTY + rest.cost, lines);
        }

        private Result findBestLayout(String[] words, int maxWidth, int i, String carry, Map<State, Result> memo) {
            double bestCost = Double.POSITIVE_INFINITY;
            List<String> bestLines = null;

            List<String> lineTokens = new ArrayList<>();
            List<Integer> tokenIndices = new ArrayList<>();
            int lineLen = 0;
            int nextWord = i;
            boolean firstToken = false;

            while (true) {
                TokenInfo token = getNextToken(words, carry, nextWord, firstToken, i);
                if (token == null)
                    break;
                firstToken = true;

                int newLen = lineTokens.isEmpty() ? token.text.length() : lineLen + 1 + token.text.length();
                if (newLen > maxWidth)
                    break;

                lineTokens.add(token.text);
                tokenIndices.add(token.wordIndex);
                lineLen = newLen;
                if (token.wordIndex >= 0)
                    nextWord++;

                Result candidate = evaluateLine(words, maxWidth, i, lineTokens, tokenIndices, false, memo);
                if (candidate.cost < bestCost) {
                    bestCost = candidate.cost;
                    bestLines = candidate.lines;
                }

                if (token.wordIndex >= 0) {
                    Result hyphenated = tryHyphenation(words, maxWidth, i, lineTokens, tokenIndices,
                            lineLen, token.text, memo);
                    if (hyphenated != null && hyphenated.cost < bestCost) {
                        bestCost = hyphenated.cost;
                        bestLines = hyphenated.lines;
                    }
                }
            }

            if (lineTokens.isEmpty()) {
                Result single = handleSingleToken(words, maxWidth, i, carry, memo);
                if (single != null && (bestLines == null || single.cost < bestCost)) {
                    return single;
                }
            }

            return bestLines != null ? new Result(bestCost, bestLines) : null;
        }

        private static class TokenInfo {
            String text;
            int wordIndex;

            TokenInfo(String text, int wordIndex) {
                this.text = text;
                this.wordIndex = wordIndex;
            }
        }

        private TokenInfo getNextToken(String[] words, String carry, int nextWord, boolean firstToken, int i) {
            if (!firstToken && carry != null) {
                return new TokenInfo(carry, -1);
            }
            if (nextWord >= words.length)
                return null;
            return new TokenInfo(words[nextWord], nextWord);
        }

        private Result evaluateLine(String[] words, int maxWidth, int i, List<String> tokens,
                                    List<Integer> indices, boolean isHyphen, Map<State, Result> memo) {
            int consumed = countConsumed(indices);
            int nextI = i + consumed;
            boolean isLast = (nextI >= words.length);

            double lineCost = isLast ? 0.0 : computeLineCost(tokens, maxWidth);
            if (isHyphen)
                lineCost += HYPHEN_PENALTY;

            Result rest = solve(words, maxWidth, nextI, null, memo);
            List<String> lines = new ArrayList<>();
            lines.add(TextJustifier.buildLine(tokens.toArray(new String[0]), 0, tokens.size(), maxWidth, isLast));
            lines.addAll(rest.lines);

            return new Result(lineCost + rest.cost, lines);
        }

        private Result tryHyphenation(String[] words, int maxWidth, int i, List<String> baseTokens,
                                      List<Integer> baseIndices, int baseLen, String word, Map<State, Result> memo) {
            Result best = null;

            for (String[] split : getHyphenSplits(word)) {
                String left = split[0] + "-";
                String right = split[1];

                int hyphLen = baseLen - word.length() + left.length();
                if (hyphLen > maxWidth)
                    continue;

                List<String> hyphTokens = new ArrayList<>(baseTokens);
                hyphTokens.set(hyphTokens.size() - 1, left);

                int consumed = countConsumed(baseIndices);
                int nextI = i + consumed;
                boolean isLast = (nextI >= words.length && right.isEmpty());

                double lineCost = isLast ? 0.0 : computeLineCost(hyphTokens, maxWidth) + HYPHEN_PENALTY;
                Result rest = solve(words, maxWidth, nextI, right, memo);

                List<String> lines = new ArrayList<>();
                lines.add(TextJustifier.buildLine(hyphTokens.toArray(new String[0]), 0, hyphTokens.size(), maxWidth, isLast));
                lines.addAll(rest.lines);

                Result candidate = new Result(lineCost + rest.cost, lines);
                if (best == null || candidate.cost < best.cost) {
                    best = candidate;
                }
            }

            return best;
        }

        private Result handleSingleToken(String[] words, int maxWidth, int i, String carry, Map<State, Result> memo) {
            String text = carry != null ? carry : (i < words.length ? words[i] : null);
            if (text == null)
                return new Result(0.0, new ArrayList<>());

            boolean isCarry = (carry != null);
            Result best = null;

            if (!isCarry) {
                for (String[] split : getHyphenSplits(text)) {
                    String left = split[0] + "-";
                    if (left.length() > maxWidth)
                        continue;

                    Result rest = solve(words, maxWidth, i + 1, split[1], memo);
                    List<String> lines = new ArrayList<>();
                    lines.add(TextJustifier.buildLine(new String[] { left }, 0, 1, maxWidth, false));
                    lines.addAll(rest.lines);

                    Result candidate = new Result(computeLineCost(List.of(left), maxWidth) + HYPHEN_PENALTY + rest.cost, lines);
                    if (best == null || candidate.cost < best.cost) {
                        best = candidate;
                    }
                }
            }

            if (best == null) {
                String chunk = text.substring(0, Math.min(text.length(), maxWidth));
                String nextCarry = chunk.length() < text.length() ? text.substring(chunk.length()) : null;
                int nextI = isCarry ? i : i + 1;

                Result rest = solve(words, maxWidth, nextI, nextCarry, memo);
                List<String> lines = new ArrayList<>();
                lines.add(TextJustifier.buildLine(new String[] { chunk }, 0, 1, maxWidth,
                        nextCarry == null && nextI >= words.length));
                lines.addAll(rest.lines);

                best = new Result(OVERFLOW_PENALTY + rest.cost, lines);
            }

            return best;
        }

        private Result createFallback(String[] words, int maxWidth, int i, String carry) {
            List<String> lines = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            if (carry != null)
                sb.append(carry).append(' ');
            for (int k = i; k < words.length; k++) {
                if (sb.length() > 0)
                    sb.append(' ');
                sb.append(words[k]);
            }

            String text = sb.toString().trim();
            while (text.length() > 0) {
                int len = Math.min(text.length(), maxWidth);
                lines.add(TextJustifier.buildLine(new String[] { text.substring(0, len) }, 0, 1, maxWidth, false));
                text = text.substring(len).trim();
            }

            return new Result(FALLBACK_PENALTY, lines);
        }

        private int countConsumed(List<Integer> indices) {
            return (int) indices.stream().filter(idx -> idx != null && idx >= 0).count();
        }

        private double computeLineCost(List<String> tokens, int maxWidth) {
            if (tokens.isEmpty())
                return 0.0;
            int totalLen = tokens.stream().mapToInt(String::length).sum();
            int gaps = Math.max(0, tokens.size() - 1);
            int extra = maxWidth - totalLen - gaps;
            return extra < 0 ? Double.POSITIVE_INFINITY : Math.pow(extra, 3);
        }

        private List<String[]> getHyphenSplits(String word) {
            return hyphenCache.computeIfAbsent(word, w -> {
                List<String[]> splits = new ArrayList<>();
                Hyphenator.Hyphenation h = hyphenator.hyphenate(w);
                if (h == null || h.length() < 2)
                    return splits;

                StringBuilder left = new StringBuilder();
                for (int k = 0; k < h.length() - 1; k++) {
                    left.append(h.getPart(k));
                    StringBuilder right = new StringBuilder();
                    for (int m = k + 1; m < h.length(); m++) {
                        right.append(h.getPart(m));
                    }
                    splits.add(new String[] { left.toString(), right.toString() });
                }
                return splits;
            });
        }

        private Result memoize(State state, Map<State, Result> memo, Result result) {
            memo.put(state, result);
            return result;
        }
    }
}