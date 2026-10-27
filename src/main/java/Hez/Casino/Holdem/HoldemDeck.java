package Hez.Casino.Holdem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HoldemDeck {

    private final List<HoldemCard> cards = new ArrayList<>();

    public HoldemDeck() {
        reset();
    }

    public void reset() {
        cards.clear();
        for (HoldemCard.Rank rank : HoldemCard.Rank.values()) {
            for (HoldemCard.Suit suit : HoldemCard.Suit.values()) {
                cards.add(new HoldemCard(rank, suit));
            }
        }
        Collections.shuffle(cards);
    }

    public HoldemCard draw() {
        if (cards.isEmpty()) {
            reset();
        }
        return cards.remove(cards.size() - 1);
    }
}
