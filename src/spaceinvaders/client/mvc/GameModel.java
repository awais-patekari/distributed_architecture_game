package spaceinvaders.client.mvc;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Observable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import spaceinvaders.client.ClientConfig;
import spaceinvaders.client.network.NetworkConnection;
import spaceinvaders.command.Command;
import spaceinvaders.exceptions.ClosingSocketException;
import spaceinvaders.exceptions.ConnectionTimeoutException;
import spaceinvaders.exceptions.InterruptedServiceException;
import spaceinvaders.utility.ServiceState;

/**
 * Provides the game data.
 *
 * <p>It acts like a gateway to the game's logic and physics, which all happen at the server side.
 * This model is used to access the game data comming from the server, and sending data to it.
 *
 * @see spaceinvaders.client.mvc.GameController
 * @see spaceinvaders.client.mvc.GameView
 */
public class GameModel extends Observable implements Model {
  private static final Logger LOGGER = Logger.getLogger(GameModel.class.getName());
  private static final long SERVER_TIMEOUT_SECONDS = 3;

  private BlockingQueue<String> forwardingQueue;
  private BlockingQueue<String> receivingQueue;
  private NetworkConnection serverConnection;
  private ServiceState state;

  private ExecutorService dataReceiverExecutor;
  private ExecutorService networkExecutor;
  private ExecutorService notifyObserversExecutor;

  /**
   * Constructs a new model that uses the specified config.
   */
  public GameModel(ClientConfig config) {
    forwardingQueue = new LinkedBlockingQueue<>();
    receivingQueue = new LinkedBlockingQueue<>();
    serverConnection = new NetworkConnection(config,receivingQueue,forwardingQueue);
    state = new ServiceState(false);

    dataReceiverExecutor = Executors.newSingleThreadExecutor();
    networkExecutor = Executors.newSingleThreadExecutor();
    notifyObserversExecutor = Executors.newSingleThreadExecutor();
  }
  
  @Override
  public void addController(Controller controller) {
    addObserver(controller);
  }

  @Override
  public void doCommand(Command command) {
    try {
      forwardingQueue.put(command.toJson());
    } catch (InterruptedException exception) {
      setChanged();
      notifyObserversInSeparateThread(new InterruptedServiceException(exception));
      LOGGER.log(Level.SEVERE,exception.toString(),exception);
    }
  }

  @Override
  public void initNewGame() {
    state.set(true);
    forwardingQueue.clear();
    receivingQueue.clear();

    dataReceiverExecutor.submit(new Callable<Void>() {
      @Override
      public Void call() {
        while (state.get()) {
          String data = null;
          try {
            data = receivingQueue.poll(SERVER_TIMEOUT_SECONDS,SECONDS);
          } catch (InterruptedException exception) {
            if (state.get()) {
              exceptionFired(new InterruptedServiceException(exception));
            }
            break;
          }
          if (data == null) {
            if (state.get()) {
              exceptionFired(new ConnectionTimeoutException());
              break;
            }
          }
          setChanged();
          notifyObserversInSeparateThread(data);
        }
        return null;
      }
    });

    networkExecutor.submit(new Callable<Void>() {
      @Override
      public Void call() {
        try {
          serverConnection.call();
        } catch (Exception exception) {
          exceptionFired(exception);
        }
        return null;
      }
    });
  }

  @Override
  public void exitGame() {
    state.set(false);
    if (serverConnection != null) {
      try {
        serverConnection.close();   
      } catch (ClosingSocketException exception) {
        LOGGER.log(Level.SEVERE,exception.getMessage(),exception);
      }
    }
  }

  @Override
  public void shutdown() {
    serverConnection.shutdown();
    dataReceiverExecutor.shutdownNow();
    networkExecutor.shutdownNow();
    notifyObserversExecutor.shutdownNow();
  }

  /**
   * Handles an exception that gets fired in a child thread.
   */
  private void exceptionFired(Exception exception) {
    state.set(false);
    setChanged();
    notifyObserversInSeparateThread(exception);
    LOGGER.log(Level.SEVERE,exception.getMessage(),exception);
  }

  private void notifyObserversInSeparateThread(Object obj) {
    notifyObserversExecutor.submit(new Callable<Void>() {
      @Override
      public Void call() {
        notifyObservers(obj);
        return null;
      }
    });
  }
}