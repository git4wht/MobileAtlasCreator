package mobac.tools.program;

import com.sleepycat.persist.PrimaryIndex;
import mobac.program.tilestore.TileStore;
import mobac.program.tilestore.berkeleydb.BerkeleyDbTileStore;
import mobac.program.tilestore.berkeleydb.BerkeleyDbTileStore.TileDatabase;
import mobac.program.tilestore.berkeleydb.TileDbEntry;
import mobac.tools.Main;
import mobac.tools.ParamTests;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class DeleteTiles
        implements Runnable {
  final List<String> tiles;
  final File dbDir;

  public DeleteTiles(String dbDir, List<String> tiles) {
    this.tiles = tiles;
    this.dbDir = new File(dbDir);
    if (!ParamTests.testBerkelyDbDir(this.dbDir)) {
      throw new InvalidParameterException();
    }
  }


  public void run() {
    Pattern p = Pattern.compile("z?([0-9]+)/([0-9]+)/([0-9]+)");
    List<TileDbEntry.TileDbKey> tileKeys = new LinkedList<TileDbEntry.TileDbKey>();
    for (String t : this.tiles) {
      Matcher m = p.matcher(t);
      boolean valid = m.matches();

      int zoom = -1;
      int x = -1;
      int y = -1;
      if (valid) {
        zoom = Integer.parseInt(m.group(1));
        x = Integer.parseInt(m.group(2));
        y = Integer.parseInt(m.group(3));
        valid &= ((zoom >= 0 && zoom <= 22));
        valid &= ((x >= 0 && y >= 0));
      }

      if (!valid) {
        System.err.println("Invalid tile coordinate: " + t);
        System.exit(-1);
      }

      tileKeys.add(new TileDbEntry.TileDbKey(x, y, zoom));
    }
    System.out.println("Deleting the following tiles:");
    for (TileDbEntry.TileDbKey key : tileKeys) {
      System.out.println("\t" + key);
    }

    BerkeleyDbTileStore tileStore = (BerkeleyDbTileStore) TileStore.getInstance();
    TileDatabase db = null;
    try {
      db =tileStore.new TileDatabase("Db", this.dbDir);
      Main.log.info("Tile store entry count: " + db.entryCount() + " (before deleting)");
      PrimaryIndex<TileDbEntry.TileDbKey, TileDbEntry> tileIndex = db.getTileIndex();
      for (TileDbEntry.TileDbKey key : tileKeys) {
        if (!tileIndex.delete(key)) {
          Main.log.trace("Failed to delete " + key);
        }
      }
      db.purge();
      Main.log.info("Tile store entry count: " + db.entryCount() + " (after deleting)");
    } catch (Exception e) {
      Main.log.error("Deleting of tiles failed", e);
    } finally {
      db.close(false);
    }
  }

  public static interface DeleteTileFilter {
    boolean canDeleteTile(TileDbEntry param1TileDbEntry);

    String getInfoMessage();
  }

  public static class ETagDeleteTileFilter
          implements DeleteTileFilter {
    final String eTagValue;

    public ETagDeleteTileFilter(String eTagValue) {
      this.eTagValue = eTagValue;
    }


    public boolean canDeleteTile(TileDbEntry entry) {
      String eTag = "" + entry.geteTag();
      return eTag.equals(this.eTagValue);
    }


    public String getInfoMessage() {
      return "tiles with an etag of: \"" + this.eTagValue + "\"";
    }
  }


  public static class ZoomDeleteTileFilter
          implements DeleteTileFilter {
    final int zoom;


    public ZoomDeleteTileFilter(int zoom) {
      this.zoom = zoom;
    }


    public boolean canDeleteTile(TileDbEntry entry) {
      return (entry.getZoom() == this.zoom);
    }


    public String getInfoMessage() {
      return "tiles with an zoom level of " + this.zoom;
    }
  }


  public static class XDeleteTileFilter
          implements DeleteTileFilter {
    final int x;


    public XDeleteTileFilter(int x) {
      this.x = x;
    }


    public boolean canDeleteTile(TileDbEntry entry) {
      return (entry.getX() == this.x);
    }


    public String getInfoMessage() {
      return "tiles with an x coordinate of " + this.x;
    }
  }


  public static class YDeleteTileFilter
          implements DeleteTileFilter {
    final int y;


    public YDeleteTileFilter(int y) {
      this.y = y;
    }


    public boolean canDeleteTile(TileDbEntry entry) {
      return (entry.getY() == this.y);
    }


    public String getInfoMessage() {
      return "tiles with an y coordinate of " + this.y;
    }
  }
}

