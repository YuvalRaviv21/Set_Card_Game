package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    // added by us
    public final int size;
    /**
     * A boolean matrix representation of placed tokens for each player.
     * playerTokenGrid = boolean[amount of players][table size].
     */
    protected boolean[][] playerTokenGrid;
    /**
     * enum representing the using status of the table.
     */
    STATUS status = STATUS.USING;
    enum STATUS{
        USING,
        NOTINUSE;
        public boolean inUSE(){
            return this == STATUS.USING|this == STATUS.NOTINUSE;
        }
    }
    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        this.size = env.config.rows*env.config.columns;
        this.playerTokenGrid = new boolean[env.config.players][env.config.rows*env.config.columns];
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;
        env.ui.placeCard(card,slot);

        // TODO implement
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
        int card = slotToCard[slot];
        cardToSlot[card] = null;
        slotToCard[slot] = null;
        env.ui.removeCard(slot);
        for(int i = 0; i<playerTokenGrid.length;i++){
            removeToken(i,slot);
        }
        // TODO implement
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        // TODO implement
        if(slotToCard[slot]!=null){
            playerTokenGrid[player][slot]= true;
            env.ui.placeToken(player, slot);}
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
        // TODO implement
        boolean before = playerTokenGrid[player][slot];
        if(before) {
            playerTokenGrid[player][slot] = !playerTokenGrid[player][slot];
            env.ui.removeToken(player,slot);
        }
        return before;
    }
    /**
     * @post for 0<i<env.config.players and 0<i<table.size playerTokenGrid[i][j]=false;
     */
    public void removeTokens(){
        playerTokenGrid = new boolean[env.config.players][env.config.rows*env.config.columns];
    }
    /**
     * @return if table exists on the table;;
     */
    public boolean existSetsOnTable(){
        List<Integer> a = new LinkedList<>();
        for(Integer i : slotToCard){
            if(i!=null)
                a.add(i);
        }
        return env.util.findSets(a,1).size()!=0;
    }
}