package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;
    //added by us;
    private long startTime;

    boolean hints;

    //const time values
    final int ONE = 1;
    final int ZERO = 0;
    final int TURN_TIME_INDICATOR = 0;
    final long ALMOST_SECOND = TimeUnit.MILLISECONDS.toMillis(999);
    final long GENERAL_SLEEP_TIME = TimeUnit.MILLISECONDS.toMillis(100);
    final long WARNING_SLEEP_TIME = TimeUnit.MILLISECONDS.toMillis(10);
    //
    Thread dealerThread;

    public final Thread getDealerThread(){
        return dealerThread;
    }

    public final boolean getTerminateValue(){
        return terminate;
    }

    /**
     * the order of placing/removing cards;
     */
    List<Integer> placeRemoveOrder;
    public Queue<Player> Sets_IDs_To_Check = new ArrayDeque<>();
    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = new LinkedList<>(IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList()));
        placeRemoveOrder = IntStream.rangeClosed(0, table.size-1).boxed().collect(Collectors.toList());
        hints = env.config.hints;
    }
    
    private void initializePlayers(){
        for(Player a : players) {
            Thread aThread = new Thread(a,""+a.id);
            aThread.start();
        }
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        dealerThread = Thread.currentThread();
        placeCardsOnTable();
        initializePlayers();
        while (!shouldFinish()) {
            placeCardsOnTable();
            updateTimerDisplay(true);
            timerLoop();
            removeAllCardsFromTable();
        }
        announceWinners();
        terminate();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
        stopPlayers();
        terminate = true;
    }

    private void stopPlayers(){
        ListIterator<Player> playersIter = Arrays.asList(players).listIterator(players.length);
        while(playersIter.hasPrevious()){
            Player a = playersIter.previous();
            try {
                a.terminate();
                a.getPlayerThread().interrupt(); 
                a.getPlayerThread().join();
            } catch (InterruptedException e) {}
        }
    }
    /* 
     * for testing
    */
    public void placeCardsOnTableT(){
        placeCardsOnTable();
    }
    /* 
     * for testing
    */
    public void removeAllCardsFromTableT(){
        removeAllCardsFromTable();
    }
    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        // TODO implement
        table.status = Table.STATUS.USING;
        synchronized (table) {
            if (!Sets_IDs_To_Check.isEmpty()) {
                Player t = Sets_IDs_To_Check.poll();
                // making ann array representation of card coresponding to the slots in set;
                synchronized (t.currSet) {
                    if (t.currSet.size() == Player.MAX_SET_SIZE) {
                        boolean legel_id_sent = true;
                        for(int i = 0;legel_id_sent && i<Player.MAX_SET_SIZE;i++) {
                            if(!(table.slotToCard[t.currSet.get(i)] == t.cardArray[i])){
                                legel_id_sent = false;
                            }
                        }
                        if(legel_id_sent) {
                                if (env.util.testSet(t.cardArray)) {
                                    while (!t.currSet.isEmpty()) {
                                        Integer slot = t.currSet.remove();
                                        table.removeCard(slot);
                                        for (Player a : players) {
                                            a.removeSlot(slot);
                                        }
                                    }
                                    t.score_indicator = Player.SCORE_INDICATOR.Point;
                                    updateTimerDisplay(true);
                                }
                                // if set is not true;
                                else {
                                    t.score_indicator = Player.SCORE_INDICATOR.Penalty;
                                }
                        }
                        else {
                            while(!t.currSet.isEmpty()) {
                                Integer a = t.currSet.remove();
                                table.removeToken(t.id, a);
                            }
                        }
                    }
                    t.set_status = Player.SET_STATUS.CHECKED;
                    t.currSet.notifyAll();
                }
            }
        }
        table.status = Table.STATUS.NOTINUSE;
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
        table.status = Table.STATUS.USING;
        synchronized (table) {
            boolean change = false;
            int amountCards = table.countCards();
            if (amountCards != table.size && !deck.isEmpty()) {
                if (amountCards == ZERO) {
                    Collections.shuffle(placeRemoveOrder);
                    Collections.shuffle(deck);
                   //System.out.println(TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS));
                }
                for (int slot = 0; slot < table.size; slot++) {
                    if (table.slotToCard[placeRemoveOrder.get(slot)] == null) {
                        if (!deck.isEmpty()) {
                            int card = deck.remove(ZERO); // like remove first;
                            table.placeCard(card, placeRemoveOrder.get(slot));
                            amountCards++;
                        }
                    }
                    change = true;
                }
                if(env.config.turnTimeoutMillis<=TURN_TIME_INDICATOR){
//                    System.out.println("yay " +table.existSetsOnTable());
                    if(!table.existSetsOnTable()){
                        removeAllCardsFromTable();
                        if(!shouldFinish()){
                            placeCardsOnTable();
                        }
                        else{
                            terminate=true;
                        }
                    }
                }
            }
            if (amountCards != table.size && deck.isEmpty()) {
                if(!table.existSetsOnTable()){
                    removeAllCardsFromTable();
                    terminate=true;
                }
            }
            if(change & hints) {
                table.hints();
                System.out.println("_______________________________________________________________-");
            }
        }
        table.status = Table.STATUS.NOTINUSE;
    }
//    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        //should sleep for delta time; not implemented yet
        try {
            synchronized (this) {
                if(reshuffleTime-System.currentTimeMillis()>env.config.turnTimeoutWarningMillis+GENERAL_SLEEP_TIME) {
                    wait(GENERAL_SLEEP_TIME);
                }
                else{
                    wait(WARNING_SLEEP_TIME);
                }
            }
        } catch (Exception e) {}
    }
                

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        if(reset){
            reshuffleTime = Long.MAX_VALUE;
            startTime = System.currentTimeMillis();
            if(env.config.turnTimeoutMillis>TURN_TIME_INDICATOR)
                reshuffleTime = startTime + env.config.turnTimeoutMillis+ ALMOST_SECOND;
        }
        if(env.config.turnTimeoutMillis>TURN_TIME_INDICATOR)
        {
            long timeLeft = reshuffleTime-System.currentTimeMillis();
            boolean warn = timeLeft<env.config.turnTimeoutWarningMillis;
            if(timeLeft<ZERO)
                timeLeft = ZERO;
            env.ui.setCountdown(timeLeft, warn);
        }
        else if(env.config.turnTimeoutMillis==TURN_TIME_INDICATOR)
        {
            long timePassed = System.currentTimeMillis()-startTime;
            env.ui.setElapsed(timePassed);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        table.status = Table.STATUS.USING;
        synchronized (table) {
            if (!terminate) {
                synchronized (table) {
                    table.removeTokens();
                    env.ui.removeTokens();
                    for (int index : placeRemoveOrder) {
                        try {
                            if (table.slotToCard[index] != null)
                                deck.add(table.slotToCard[index]);
                            table.removeCard(index);
                        } catch (NullPointerException ignored) {
                        }
                    }
                }
            }
        }
        table.status = Table.STATUS.NOTINUSE;
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
        table.removeTokens();
        env.ui.setCountdown(ZERO,false);
        String logString ="";
        Collection<Integer> playersScores = Arrays.stream(players).map((player)-> player.getScore()).collect(Collectors.toList());
        Integer maxScore = Collections.max(playersScores);
        List<Integer> answerList= new LinkedList<>();
        for(Player a : players) {
            logString = logString +"["+a.name+",(players["+a.id+"]),"+a.getScore()+"],";
            if (a.getScore() >= maxScore){
                answerList.add(a.id);}
        }
        int[] answerArray =answerList.stream().mapToInt(i->i).toArray();
        env.logger.log(Level.INFO, "[Name,players[i],scores] are: "+logString.substring(ZERO,logString.length()-ONE));
        env.ui.announceWinner(answerArray);
    }
}