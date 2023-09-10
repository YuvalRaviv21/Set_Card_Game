package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerTest {

    Player player;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Table table;
    @Mock
    private Dealer dealer;
    @Mock
    private Logger logger;
    @Mock
    private Env env;
  

    void assertInvariants() {
        assertTrue(player.id >= 0);
        assertTrue(player.getScore() >= 0);
    }

    @BeforeEach
    void setUp() {
        // purposely do not find the configuration files (use defaults here).
        env = new Env(logger, new Config(logger, ""), ui, util);
        player = new Player(env, dealer, table, 0, false);
        assertInvariants();
    }

    @AfterEach
    void tearDown() {
        assertInvariants();
    }

    @Test
    void point() {

        // force table.countCards to return 3
        when(table.countCards()).thenReturn(3); // this part is just for demonstration

        // calculate the expected score for later
        int expectedScore = player.getScore() + 1;

        // call the method we are testing
        player.point();

        // check that the score was increased correctly
        assertEquals(expectedScore, player.getScore());

        // check that ui.setScore was called with the player's id and the correct score
        verify(ui).setScore(eq(player.id), eq(expectedScore));
    }

    //our tests
    @Test
    void CheckFreezeTime() {
        player.point();

        // check that ui.setFreeze was called with the player's id and the correct freeze time;
        verify(ui).setFreeze(eq(player.id), eq(env.config.pointFreezeMillis));

        player.penalty();

        // check that ui.setFreeze was called with the player's id and the correct freeze time;
        verify(ui).setFreeze(eq(player.id), eq(env.config.penaltyFreezeMillis));
    }

    @Test
    // check if terminate closes the thread
    void threadIsTerminated() throws InterruptedException {
        // initializing player thread;
        Thread newthread = new Thread(player,""+player.id);
        //starting player thread;
        newthread.start();
        //terminating player thread;
        player.terminate();

        //checking termintate value change
        assertTrue(player.getTerminateValue());
        newthread.join();
    }



}