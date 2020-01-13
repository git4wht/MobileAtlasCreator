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
import java.util.LinkedList;
import java.util.List;

public class Delete
        implements Runnable {
  final List<String> conditions;
  final File dbDir;

  public Delete(String dbDir, List<String> conditions) {
    this.conditions = conditions;
    this.dbDir = new File(dbDir);
    if (!ParamTests.testBerkelyDbDir(this.dbDir)) {
      throw new InvalidParameterException();
    }
  }


  public void run() {
    List<DeleteTileFilter> tileFilters = new LinkedList<DeleteTileFilter>();
    for (String cond : this.conditions) {
      String[] conditionSplit = cond.split(":");
      if (conditionSplit.length == 2) {
        String eTagValue, filterOn = conditionSplit[0].toLowerCase().trim();
        String filterValue = conditionSplit[1].trim();
        switch (filterOn) {

          case "etag":
            eTagValue = "";
            if (conditionSplit.length > 1)
              eTagValue = conditionSplit[1].trim();
            if (!"null".equals(eTagValue) && !eTagValue.startsWith("\""))
              eTagValue = "\"" + eTagValue + "\"";
            tileFilters.add(new ETagDeleteTileFilter(eTagValue));
            continue;
          case "zoom":
          case "z":
            tileFilters.add(new ZoomDeleteTileFilter(Integer.parseInt(filterValue)));
            continue;
          case "x":
            tileFilters.add(new XDeleteTileFilter(Integer.parseInt(filterValue)));
            continue;
          case "y":
            tileFilters.add(new YDeleteTileFilter(Integer.parseInt(filterValue)));
            continue;
        }
      }
      System.err.println("Invalid condition: " + cond);
      System.exit(-1);
    }

    System.out.println("Deleting all tiles that match all of the following condition(s):");
    for (DeleteTileFilter tf : tileFilters) {
      System.out.println("\t" + tf.getInfoMessage());
    }

    BerkeleyDbTileStore tileStore = (BerkeleyDbTileStore) TileStore.getInstance();
    TileDatabase db = null;
    try {

      db = tileStore.new TileDatabase("Db", this.dbDir);
      Main.log.info("Tile store entry count: " + db.entryCount() + " (before deleting)");
      EntityCursor<TileDbEntry> cursor = db.getTileIndex().entities();
      try {
        TileDbEntry entry;
        next_entity:
        while ((entry = cursor.next()) != null) {
          for (DeleteTileFilter tf : tileFilters) {
            if (!tf.canDeleteTile(entry)) {
              continue next_entity;
            }
          }
          Main.log.trace("Deleting " + entry);
          cursor.delete();
        }
      } finally {
        cursor.close();
      }
      Main.log.info("Tile store entry count: " + db.entryCount() + " (after deleting)");
    } catch (Exception e) {
      Main.log.error("Deleting of tiles failed", e);
    } finally {
      db.close(false);
    }
  }

  public interface DeleteTileFilter {
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
