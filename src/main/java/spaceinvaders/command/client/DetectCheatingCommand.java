package spaceinvaders.command.client;

import static spaceinvaders.command.ProtocolEnum.UDP;

import spaceinvaders.client.mvc.Controller;
import spaceinvaders.client.mvc.View;
import spaceinvaders.command.Command;

/** Change the score of a player. */
public class DetectCheatingCommand extends Command {
  private transient Controller executor;
  private String playerName;

  DetectCheatingCommand() {
    super(DetectCheatingCommand.class.getName(),UDP);
  }

  /**
   * @param playerId the id of the player.
   * @param change a value with which the score is changed.
   */
  public DetectCheatingCommand(String playerName) {
    this();
    this.playerName = playerName;
  }

  @Override
  public void execute() {
    for (View view : executor.getViews()) {
      view.detectCheating(playerName);
    }
  }

  @Override
  public void setExecutor(Object executor) {
    if (executor instanceof Controller) {
      this.executor = (Controller) executor;
    } else {
      // This should never happen.
      throw new AssertionError();
    }
  }
}
