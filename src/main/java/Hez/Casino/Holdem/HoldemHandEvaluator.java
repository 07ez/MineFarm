package Hez.Casino.Holdem;

import java.util.*;

public class HoldemHandEvaluator {

    public enum HandRank {
        HIGH_CARD(1, "하이 카드"),
        ONE_PAIR(2, "원 페어"),
        TWO_PAIR(3, "투 페어"),
        THREE_OF_A_KIND(4, "트리플"),
        STRAIGHT(5, "스트레이트"),
        FLUSH(6, "플러시"),
        FULL_HOUSE(7, "풀 하우스"),
        FOUR_OF_A_KIND(8, "포카드"),
        STRAIGHT_FLUSH(9, "스트레이트 플러시"),
        ROYAL_FLUSH(10, "로얄 플러시");

        private final int score;
        private final String koreanName;

        HandRank(int score, String koreanName) {
            this.score = score;
            this.koreanName = koreanName;
        }

        public int getScore() {
            return score;
        }

        public String getKoreanName() {
            return koreanName;
        }
    }

    public static class HandScore implements Comparable<HandScore> {
        private final HandRank rank;
        private final List<Integer> tieBreakers; // 내림차순 비교용
        private final String description;

        public HandScore(HandRank rank, List<Integer> tieBreakers, String description) {
            this.rank = rank;
            this.tieBreakers = tieBreakers;
            this.description = description;
        }

        public HandRank getRank() {
            return rank;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public int compareTo(HandScore o) {
            if (this.rank.getScore() != o.rank.getScore()) {
                return Integer.compare(this.rank.getScore(), o.rank.getScore());
            }
            int minSize = Math.min(this.tieBreakers.size(), o.tieBreakers.size());
            for (int i = 0; i < minSize; i++) {
                int cmp = Integer.compare(this.tieBreakers.get(i), o.tieBreakers.get(i));
                if (cmp != 0) return cmp;
            }
            return 0;
        }
    }

    /**
     * 7장의 카드(홀 카드 2장 + 커뮤니티 5장) 중 최상의 5장 족보를 계산
     */
    public static HandScore evaluate7(List<HoldemCard> cards7) {
        if (cards7.size() < 5) {
            return new HandScore(HandRank.HIGH_CARD, List.of(0), "카드 부족");
        }

        List<List<HoldemCard>> combinations = new ArrayList<>();
        generateCombinations(cards7, 5, 0, new ArrayList<>(), combinations);

        HandScore bestScore = null;
        for (List<HoldemCard> combo5 : combinations) {
            HandScore score = evaluate5(combo5);
            if (bestScore == null || score.compareTo(bestScore) > 0) {
                bestScore = score;
            }
        }
        return bestScore;
    }

    private static void generateCombinations(List<HoldemCard> list, int k, int start,
                                            List<HoldemCard> current, List<List<HoldemCard>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < list.size(); i++) {
            current.add(list.get(i));
            generateCombinations(list, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    /**
     * 5장의 카드로 족보 평가
     */
    public static HandScore evaluate5(List<HoldemCard> cards5) {
        List<HoldemCard> sorted = new ArrayList<>(cards5);
        sorted.sort(Collections.reverseOrder()); // 높은 랭크부터 내림차순 정렬

        boolean isFlush = true;
        HoldemCard.Suit firstSuit = sorted.get(0).getSuit();
        for (int i = 1; i < 5; i++) {
            if (sorted.get(i).getSuit() != firstSuit) {
                isFlush = false;
                break;
            }
        }

        // 스트레이트 검사
        boolean isStraight = false;
        int straightHigh = 0;

        // 일반 스트레이트 (연속 5장)
        if (sorted.get(0).getRank().getValue() - sorted.get(1).getRank().getValue() == 1 &&
            sorted.get(1).getRank().getValue() - sorted.get(2).getRank().getValue() == 1 &&
            sorted.get(2).getRank().getValue() - sorted.get(3).getRank().getValue() == 1 &&
            sorted.get(3).getRank().getValue() - sorted.get(4).getRank().getValue() == 1) {
            isStraight = true;
            straightHigh = sorted.get(0).getRank().getValue();
        }
        // A-2-3-4-5 (백스트레이트 / 휠)
        else if (sorted.get(0).getRank() == HoldemCard.Rank.ACE &&
                 sorted.get(1).getRank() == HoldemCard.Rank.FIVE &&
                 sorted.get(2).getRank() == HoldemCard.Rank.FOUR &&
                 sorted.get(3).getRank() == HoldemCard.Rank.THREE &&
                 sorted.get(4).getRank() == HoldemCard.Rank.TWO) {
            isStraight = true;
            straightHigh = 5; // A-2-3-4-5에서 가장 높은 카드는 5
        }

        // 1. 로열 플러시 & 2. 스트레이트 플러시
        if (isFlush && isStraight) {
            if (straightHigh == 14) {
                return new HandScore(HandRank.ROYAL_FLUSH, List.of(14), "로얄 플러시");
            } else {
                return new HandScore(HandRank.STRAIGHT_FLUSH, List.of(straightHigh), "스트레이트 플러시 (" + straightHigh + " 탑)");
            }
        }

        // 페어 빈도수 집계
        Map<Integer, Integer> rankCounts = new HashMap<>();
        for (HoldemCard c : sorted) {
            rankCounts.put(c.getRank().getValue(), rankCounts.getOrDefault(c.getRank().getValue(), 0) + 1);
        }

        List<Map.Entry<Integer, Integer>> countList = new ArrayList<>(rankCounts.entrySet());
        // 빈도수 내림차순, 같은 빈도수면 카드 랭크 내림차순
        countList.sort((a, b) -> {
            if (!a.getValue().equals(b.getValue())) {
                return Integer.compare(b.getValue(), a.getValue());
            }
            return Integer.compare(b.getKey(), a.getKey());
        });

        // 3. 포카드 (4장 동일)
        if (countList.get(0).getValue() == 4) {
            int fourRank = countList.get(0).getKey();
            int kicker = countList.get(1).getKey();
            return new HandScore(HandRank.FOUR_OF_A_KIND, List.of(fourRank, kicker), "포카드 (" + fourRank + ")");
        }

        // 4. 풀하우스 (3장 동일 + 2장 동일)
        if (countList.get(0).getValue() == 3 && countList.get(1).getValue() == 2) {
            int threeRank = countList.get(0).getKey();
            int pairRank = countList.get(1).getKey();
            return new HandScore(HandRank.FULL_HOUSE, List.of(threeRank, pairRank), "풀하우스 (" + threeRank + " & " + pairRank + ")");
        }

        // 5. 플러시
        if (isFlush) {
            List<Integer> values = new ArrayList<>();
            for (HoldemCard c : sorted) values.add(c.getRank().getValue());
            return new HandScore(HandRank.FLUSH, values, "플러시 (" + values.get(0) + " 탑)");
        }

        // 6. 스트레이트
        if (isStraight) {
            return new HandScore(HandRank.STRAIGHT, List.of(straightHigh), "스트레이트 (" + straightHigh + " 탑)");
        }

        // 7. 트리플 (3장 동일)
        if (countList.get(0).getValue() == 3) {
            int threeRank = countList.get(0).getKey();
            List<Integer> tieBreakers = new ArrayList<>();
            tieBreakers.add(threeRank);
            for (int i = 1; i < countList.size(); i++) {
                tieBreakers.add(countList.get(i).getKey());
            }
            return new HandScore(HandRank.THREE_OF_A_KIND, tieBreakers, "트리플 (" + threeRank + ")");
        }

        // 8. 투 페어 (2장 동일 + 2장 동일)
        if (countList.get(0).getValue() == 2 && countList.get(1).getValue() == 2) {
            int highPair = Math.max(countList.get(0).getKey(), countList.get(1).getKey());
            int lowPair = Math.min(countList.get(0).getKey(), countList.get(1).getKey());
            int kicker = countList.get(2).getKey();
            return new HandScore(HandRank.TWO_PAIR, List.of(highPair, lowPair, kicker), "투 페어 (" + highPair + " & " + lowPair + ")");
        }

        // 9. 원 페어 (2장 동일)
        if (countList.get(0).getValue() == 2) {
            int pairRank = countList.get(0).getKey();
            List<Integer> tieBreakers = new ArrayList<>();
            tieBreakers.add(pairRank);
            for (int i = 1; i < countList.size(); i++) {
                tieBreakers.add(countList.get(i).getKey());
            }
            return new HandScore(HandRank.ONE_PAIR, tieBreakers, "원 페어 (" + pairRank + ")");
        }

        // 10. 하이 카드
        List<Integer> values = new ArrayList<>();
        for (HoldemCard c : sorted) values.add(c.getRank().getValue());
        return new HandScore(HandRank.HIGH_CARD, values, "하이 카드 (" + values.get(0) + " 탑)");
    }
}
