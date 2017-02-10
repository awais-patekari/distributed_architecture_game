package spaceinvaders.server.game.world;

import static spaceinvaders.game.EntityEnum.PLAYER;

import spaceinvaders.game.GameConfig;

/** Human player character. */
class HumanPlayer extends LogicEntity {
  private final GameConfig config = GameConfig.getInstance();

  /**
   * @param id - player ID.
   * @param posX - X coordinate.
   * @param posY - Y coordinate.
   */
  public HumanPlayer(int id, int posX, int posY) {
    super(PLAYER,posX,posY,
          GameConfig.getInstance().player().getWidth(),
          GameConfig.getInstance().player().getHeight());
    setId(id);
  }
}
