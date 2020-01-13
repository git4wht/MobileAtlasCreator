package mobac.tools.program;

import mobac.program.tilestore.TileStore;
import mobac.program.tilestore.berkeleydb.BerkeleyDbTileStore;
import mobac.program.tilestore.berkeleydb.BerkeleyDbTileStore.TileDatabase;
import mobac.tools.Main;

import java.io.File;


public class Empty
        implements Runnable {
  final File dbDir;

  public Empty(String dbDir) {
    this.dbDir = new File(dbDir);
    System.out.println("Info: Drop directory \"" + dbDir + "\"\n");
    this.dbDir.deleteOnExit();
    System.out.println("Info: Create directory \"" + dbDir + "\"\n");
    this.dbDir.mkdirs();
  }

  public void run() {
    BerkeleyDbTileStore tileStore = (BerkeleyDbTileStore) TileStore.getInstance();
    TileDatabase db = null;
    try {
      db = tileStore.new TileDatabase("Db-Empty", this.dbDir);
      System.out.println("Tile store entry count: " + db.entryCount());
    } catch (Exception e) {
      Main.log.error("Create empty tile's store failed", e);
    } finally {
      db.close(false);
    }
  }
}
