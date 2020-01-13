/*******************************************************************************
 * Copyright (c) MOBAC developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.program.atlascreators;

import com.sleepycat.je.DatabaseException;
import mobac.exceptions.AtlasTestException;
import mobac.exceptions.MapCreationException;
import mobac.program.annotations.AtlasCreatorName;
import mobac.program.atlascreators.tileprovider.TileProvider;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.interfaces.MapInterface;
import mobac.program.interfaces.MapSource;
import mobac.program.model.Settings;
import mobac.program.tilestore.berkeleydb.ExportTileDatabase;
import mobac.program.tilestore.berkeleydb.TileDbEntry;
import mobac.utilities.Utilities;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@AtlasCreatorName(value = "Tile store export", type = "TILE_EXPORT")
public class TileStoreExport extends AtlasCreator {

  protected static final int MAX_BATCH_SIZE = 1000;

  protected File baseDir = null;
  protected String currentMapStoreName = null;
  protected ExportTileDatabase db = null;
  protected Map<String, ExportTileDatabase> dbs = new HashMap<>();

  @Override
  public void startAtlasCreation(AtlasInterface atlas, File customAtlasDir) throws IOException, AtlasTestException,
          InterruptedException {
    if (customAtlasDir == null)
      customAtlasDir = Settings.getInstance().getAtlasOutputDirectory();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
    String atlasDirName = atlas.getName() + "_" + sdf.format(new Date());
    super.startAtlasCreation(atlas, customAtlasDir);
    baseDir = new File(atlasDir, atlasDirName);
    Utilities.mkDirs(baseDir);

  }

  private void closeAll() {
    for (ExportTileDatabase db : dbs.values()) {
      if (db != null) {
        db.close();
        db = null;
      }
    }
    dbs.clear();
  }

  @Override
  public void initializeMap(MapInterface map, TileProvider mapTileProvider) {
    super.initializeMap(map, mapTileProvider);
    currentMapStoreName = map.getMapSource().getName();
    if (currentMapStoreName.indexOf("street") >= 0)
      currentMapStoreName = "street";
    else if (currentMapStoreName.indexOf("satellite") >= 0)
      currentMapStoreName = "map";
  }

  @Override
  public void abortAtlasCreation() throws IOException {
    closeAll();
    super.abortAtlasCreation();
  }

  @Override
  public void finishAtlasCreation() throws IOException, InterruptedException {
    closeAll();
    super.finishAtlasCreation();
  }


  @Override
  public boolean testMapSource(MapSource mapSource) {
    return true;
  }

  @Override
  public void createMap() throws MapCreationException, InterruptedException {
    try {
      db = dbs.get(currentMapStoreName);
      if (db == null) {
        db = new ExportTileDatabase(currentMapStoreName, new File(baseDir, "db-" + currentMapStoreName));
        dbs.put(currentMapStoreName, db);
      }
      createTiles();
    } catch (IOException | DatabaseException e) {
      throw new MapCreationException("Error create database: " + e.getMessage(), map, e);
    }
  }

  protected void createTiles() throws InterruptedException, MapCreationException {
    atlasProgress.initMapCreation((xMax - xMin + 1) * (yMax - yMin + 1));
    ImageIO.setUseCache(false);
    int batchTileCount = 0;
    for (int x = xMin; x <= xMax; x++) {
      for (int y = yMin; y <= yMax; y++) {
        checkUserAbort();
        atlasProgress.incMapCreationProgress();
        try {
          byte[] sourceTileData = getTileData(x, y);
          if (sourceTileData != null) {
            TileDbEntry entry = new TileDbEntry(x, y, zoom, sourceTileData);
            db.put(entry);
            batchTileCount++;
            if (batchTileCount >= MAX_BATCH_SIZE) {
              System.gc();
              batchTileCount = 0;
            }
          }
        } catch (Exception e) {
          throw new MapCreationException("Error writing tile image: " + e.getMessage(), map, e);
        }
      }
    }
  }

  protected byte[] getTileData(int x, int y) throws Exception{
    return mapDlTileProvider.getTileData(x, y);
  }
}
