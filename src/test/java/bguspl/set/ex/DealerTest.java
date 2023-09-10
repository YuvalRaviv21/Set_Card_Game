package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import bguspl.set.ex.TableTest.MockLogger;
import bguspl.set.ex.TableTest.MockUserInterface;
import bguspl.set.ex.TableTest.MockUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;
import java.util.*; //added

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class DealerTest {
    
    private Player[] players;
    private Integer[] slotToCard;
    private Integer[] cardToSlot;
    
    Dealer dealer;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Table table;
    @Mock
    private Logger logger;


    
    @BeforeEach
    void setUp() {
        
        Properties properties = new Properties();
        properties.put("Rows", "2");
        properties.put("Columns", "2");
        properties.put("FeatureSize", "3");
        properties.put("FeatureCount", "4");
        properties.put("TableDelaySeconds", "0");
        properties.put("PlayerKeys1", "81,87,69,82");
        properties.put("PlayerKeys2", "85,73,79,80");
        MockLogger logger = new MockLogger();
        Config config = new Config(logger, properties);
        slotToCard = new Integer[config.tableSize];
        cardToSlot = new Integer[config.deckSize];
        
        Env env = new Env(logger, config, new MockUserInterface(), new MockUtil());
        table = new Table(env, slotToCard, cardToSlot);
        dealer = new Dealer(env, table, players);
    }
    
    private int fillSomeSlots() {
        slotToCard[1] = 3;
        slotToCard[2] = 5;
        cardToSlot[3] = 1;
        cardToSlot[5] = 2;

        return 2;
    }

    
    //checking if dealer removes all the slots from the
    @Test
    void removeAllCardsFromTableTest(){
        fillSomeSlots();
        dealer.removeAllCardsFromTableT();
        assertEquals(0, table.countCards());
        
    }
    
    //checking if dealer placed the cards on the table
    @Test
    void placeCardsOnTableTest(){
        dealer.placeCardsOnTableT();
        assertEquals(4,table.countCards());

    }
    

        
    
}
