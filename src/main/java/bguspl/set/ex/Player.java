package bguspl.set.ex;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;
    /**
     * @return playerThread;
     */
    public final Thread getPlayerThread(){
        return playerThread;
    }

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    public final boolean getTerminateValue(){
        return terminate;
    }
//    public void setTerminate(){
//        terminate = true;
//    }

    /**
     * The current score of the player.
     */
    private int score;
    /**
     * The dealer of the game.
     */
    private Dealer dealer;

    //added by me
    final String name;

    final long AI_WAITING_TIME = TimeUnit.SECONDS.toMillis(1);
    /**
     * @inv keyPressedQueue <= 0
     */
    private final Queue<Integer> keyPressedQueue = new PriorityQueue<>();
    /**
     * @inv currSet <= 0
     */
    final static int MAX_SET_SIZE = 3;
    public LinkedList<Integer> currSet = new LinkedList<>();
    SET_STATUS set_status = SET_STATUS.CHECKED;
    SCORE_INDICATOR score_indicator = SCORE_INDICATOR.Idle;
    int[] cardArray =new int[3];
    enum SCORE_INDICATOR {
        Idle,
        Point,
        Penalty;
    }
    enum SET_STATUS{
        CHECKED,
        UNCHECKED;
    }
    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer = dealer;
        this.name = env.config.playerNames[id];
    }
    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        // System.out.println("Thread " + Thread.currentThread().getName() + " starting.");
        playerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");
        if (!human) createArtificialIntelligence();
        while (!terminate) {
            if (table.status == Table.STATUS.NOTINUSE) {
                synchronized (keyPressedQueue) {
                    giveScore();
                    while(keyPressedQueue.isEmpty() & !terminate) {
                        try {keyPressedQueue.wait();} catch (InterruptedException e) {}
                    }
                    while (!keyPressedQueue.isEmpty()) {
                        Integer last_key = keyPressedQueue.poll();
                        if (currSet.size() == MAX_SET_SIZE && set_status == SET_STATUS.CHECKED) {
                            if (currSet.remove((Integer) last_key)) {
                                synchronized (table) {
                                    table.removeToken(id, last_key);
                                }
                            }
                        } else if (currSet.size() < MAX_SET_SIZE) {
                            synchronized (table) {
                                if (currSet.remove((Integer) last_key)) {
                                    table.removeToken(id, last_key);
                                } else {
                                    table.placeToken(id, last_key);
                                    // System.out.println(id +" is inputing "+ last_key);
                                    currSet.add(last_key);
                                }
                            }
                            if (currSet.size() == MAX_SET_SIZE)
                                set_status = SET_STATUS.UNCHECKED;

                        }
                    }
                    keyPressedQueue.notifyAll();
                }
                if (currSet.size() == MAX_SET_SIZE && set_status == SET_STATUS.UNCHECKED &!terminate) {
                    synchronized (currSet) {
                        try {
                            cardArray = currSet.stream().mapToInt(i -> table.slotToCard[i]).toArray();
                            dealer.Sets_IDs_To_Check.add(this);
                            dealer.getDealerThread().interrupt();
                            currSet.wait();
                        }catch(NullPointerException e){cardArray = new int[3];set_status = SET_STATUS.CHECKED;}
                        catch (InterruptedException e) {}
                        finally {
                            if (!human){aiThread.interrupt();}
                        }
                    }
                }
            }
        }
        if (!human) try {aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        // System.out.println("Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
            // System.out.println("Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
                int key = (int)(Math.random()* table.size);
                keyPressed(key);
                if(env.config.tableDelayMillis> dealer.ZERO && currSet.size() <MAX_SET_SIZE) {
                    synchronized (this) {
                        try {wait(AI_WAITING_TIME);} catch (InterruptedException ignored) {}
                    }
                }
                while (!terminate && keyPressedQueue.size()==MAX_SET_SIZE && currSet.size() == MAX_SET_SIZE && set_status == SET_STATUS.UNCHECKED) {
                    synchronized (keyPressedQueue) {
                        try {keyPressedQueue.wait();} catch (InterruptedException ignored) {}
                    }
                }
            }
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate()  {
        // TODO implement
        terminate = true;
        if(!human){try{aiThread.interrupt();}catch(NullPointerException ignored){}}
    }
    public void removeSlot(Integer slot){
        currSet.remove(slot);
        keyPressedQueue.remove(slot);

    }
    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     * @pre keyPressedQueue <=3.
     * @post keyPressedQueue <=3.
     */
    public void keyPressed(int slot) {
        // TODO implement
        //not good
        if(table.status == Table.STATUS.NOTINUSE) {
            synchronized (keyPressedQueue) {
                if (keyPressedQueue.size() < MAX_SET_SIZE) {
                    keyPressedQueue.add(slot);
                    keyPressedQueue.notifyAll();
                }
            }
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement
        try {
            env.ui.setFreeze(id,env.config.pointFreezeMillis);
            Thread.sleep(env.config.pointFreezeMillis);
        } catch (InterruptedException ignored) {}
        env.ui.setFreeze(id,0);
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        score_indicator = SCORE_INDICATOR.Idle;
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement
        try {
            env.ui.setFreeze(id,env.config.penaltyFreezeMillis);
            Thread.sleep(env.config.penaltyFreezeMillis);
        } catch (InterruptedException ignored) {}
        env.ui.setFreeze(id,0);
        score_indicator = SCORE_INDICATOR.Idle;

    }
        public void giveScore() {
            switch(score_indicator){
                case Idle:
                    break;
                case Point:
                    point();
                    break;
                case Penalty:
                    penalty();
                    break;
            }
        }

    public int getScore() {
        return score;
    }
}