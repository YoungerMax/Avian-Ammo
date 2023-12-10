package avianammo;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import avianammo.networking.Client;
import avianammo.networking.GameSocket;
import avianammo.networking.Server;
import avianammo.networking.GameSocket.GameState;
import avianammo.pages.GameResultsPage;
import avianammo.pages.HomePage;
import avianammo.pages.WaitingPage;
import avianammo.pages.TimerPage;

public class Window extends JFrame {

    private final HomePage home;

    public Window() throws IOException, InterruptedException {
        super("Avian Ammo");

        this.setSize(1024, 1024);
        this.setLocationRelativeTo(null);

        CountDownLatch gameRoleLatch = new CountDownLatch(1);

        home = new HomePage(gameRoleLatch);
        add(home);

        setVisible(true);

        gameRoleLatch.await();

        loadAndPlayGame(home.awaitGameRoleChoice());
    }

    public void loadAndPlayGame(GameRole role) throws IOException, InterruptedException {
        remove(home);

        WaitingPage waitingPage = new WaitingPage();
        add(waitingPage);

        SwingUtilities.updateComponentTreeUI(this);

        GameSocket gameSocket;

        Position initialPosition;
        Direction initialDirection;

        if (role == GameRole.HOST) {
            Server server = new Server();
            gameSocket = server.listen(4000);
            initialPosition = Seagull.DEFAULT_POSITION;
            initialDirection = Seagull.DEFAULT_DIRECTION;
        } else {
            Client client = new Client();
            gameSocket = client.connect(4000);
            initialPosition = Seagull.OPPONENT_DEFAULT_POSITION;
            initialDirection = Seagull.OPPONENT_DEFAULT_DIRECTION;
        }
        // Game socket now connected

        gameSocket.listenForPackets();

        gameSocket.sendReady();

        gameSocket.awaitGameState(GameState.COUNTING_DOWN);

        remove(waitingPage);

        TimerPage timerPage = new TimerPage(3);
        add(timerPage);

        SwingUtilities.updateComponentTreeUI(this);

        for(int i = 3; i > 0; i--) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            timerPage.countOneSecond();
        }

        remove(timerPage);

        Game game = new Game(this, gameSocket, initialPosition, initialDirection);
        game.start();

        gameSocket.startPlay();

        gameSocket.awaitGameStateChangeFrom(GameState.PLAYING);

        game.stop();

        remove(game.getCanvas());
        
        // Wait for all extra packets to come through
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        GameResultsPage resultsPage = new GameResultsPage(gameSocket.getGameState());
        add(resultsPage);

        setVisible(true);
    }
}