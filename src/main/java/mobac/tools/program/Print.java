package mobac.tools.program;

import com.sleepycat.persist.EntityCursor;
import mobac.program.tilestore.TileStore;
import mobac.program.tilestore.berkeleydb.BerkeleyDbTileStore;
import mobac.program.tilestore.berkeleydb.BerkeleyDbTileStore.TileDatabase;
import mobac.program.tilestore.berkeleydb.TileDbEntry;
import mobac.tools.ParamTests;
import mobac.tools.Main;

import java.io.File;
import java.security.InvalidParameterException;


public class Print
        implements Runnable {
  final File dbDir;

  public Print(String dbDir) {
    this.dbDir = new File(dbDir);
    if (!ParamTests.testBerkelyDbDir(this.dbDir)) {
      throw new InvalidParameterException();
    }
  }

  public void run() {
    BerkeleyDbTileStore tileStore = (BerkeleyDbTileStore) TileStore.getInstance();
    TileDatabase db = null;
    try {

      db = tileStore.new TileDatabase("Db", this.dbDir);
      EntityCursor<TileDbEntry> cursor = db.getTileIndex().entities();
      try {
        TileDbEntry entry = cursor.next();
        while (entry != null) {
          System.out.println(entry);
          entry = cursor.next();
        }
      } finally {
        cursor.close();
      }
      System.out.println("Tile store entry count: " + db.entryCount());
    } catch (Exception e) {
      Main.log.error("Deleting of tiles failed", e);
    } finally {
      db.close(false);
    }
  }
}
