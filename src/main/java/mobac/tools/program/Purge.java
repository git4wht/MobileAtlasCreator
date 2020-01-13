package mobac.tools.program;

import mobac.program.tilestore.TileStore;
import mobac.program.tilestore.berkeleydb.BerkeleyDbTileStore;
import mobac.tools.Main;
import mobac.tools.ParamTests;

import java.io.File;
import java.security.InvalidParameterException;


public class Purge
        implements Runnable {
  final File databaseDir;

  public Purge(String databaseDir) {
    this.databaseDir = new File(databaseDir);
    if (!ParamTests.testBerkelyDbDir(this.databaseDir))
      throw new InvalidParameterException();
  }

  public void run() {
    BerkeleyDbTileStore tileStore = (BerkeleyDbTileStore) TileStore.getInstance();
    BerkeleyDbTileStore.TileDatabase tileDatabase = null;
    try {
      tileDatabase = tileStore.new TileDatabase("Source", this.databaseDir);
      Main.log.info("Database purge initiated");
      tileDatabase.purge();
      Main.log.info("Database purge completed");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      tileDatabase.close(false);
    }
  }
}

