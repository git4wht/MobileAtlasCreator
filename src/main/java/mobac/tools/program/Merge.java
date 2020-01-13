package mobac.tools.program;

import com.sleepycat.persist.EntityCursor;
import mobac.program.tilestore.TileStore;
import mobac.program.tilestore.berkeleydb.BerkeleyDbTileStore;
import mobac.program.tilestore.berkeleydb.BerkeleyDbTileStore.TileDatabase;
import mobac.program.tilestore.berkeleydb.TileDbEntry;
import mobac.tools.Main;
import mobac.tools.ParamTests;

import java.io.File;
import java.security.InvalidParameterException;


public class Merge
        implements Runnable {
  final File sourceDir;
  final File destDir;

  public Merge(String sourceDir, String destDir) {
    this.sourceDir = new File(sourceDir);
    this.destDir = new File(destDir);
    if (!ParamTests.testBerkelyDbDir(this.sourceDir))
      throw new InvalidParameterException();
    if (!ParamTests.testBerkelyDbDir(this.destDir))
      throw new InvalidParameterException();
  }

  public void run() {
    BerkeleyDbTileStore tileStore = (BerkeleyDbTileStore) TileStore.getInstance();
    TileDatabase dbSource = null;
    TileDatabase dbDest = null;
    try {

      dbSource = tileStore.new TileDatabase("Source", this.sourceDir);
      Main.log.info("Source tile store entry count: " + dbSource.entryCount());

      dbDest = tileStore.new TileDatabase("Destination", this.destDir);
      Main.log.info("Destination tile store entry count: " + dbDest.entryCount() + " (before merging)");
      dbDest.purge();
      EntityCursor<TileDbEntry> cursor = dbSource.getTileIndex().entities();
      long cnt = 0;
      try {
        TileDbEntry entry = cursor.next();
        while (entry != null) {
          Main.log.trace("Adding " + entry);
          dbDest.put(entry);
          entry = cursor.next();
        }
        if(++cnt % 100000 == 0){
          Main.log.trace("Processed entry cont: " + cnt);
        }
      } finally {
        cursor.close();
      }
      Main.log.info("Destination tile store entry count: " + dbDest.entryCount() + " (after merging)");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (dbSource != null)
        dbSource.close(false);
      if (dbDest != null)
        dbDest.close(false);
    }
  }
}

