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

import com.sleepycat.bind.EntityBinding;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.utilint.CmdUtil;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

@AtlasCreatorName(value = "Tile store dump", type = "TILE_DUMP")
public class TileStoreDump extends AtlasCreator {

	private static final int VERSION = 3;
	protected File baseDir = null;
	protected String currentMapStoreName = null;
	protected PrintStream outputFile = null;

	protected  ExportTileDatabase db = null;
	protected EntityBinding  binding = null;

	@Override
	public void startAtlasCreation(AtlasInterface atlas, File customAtlasDir) throws IOException, AtlasTestException,
			InterruptedException {
		if (customAtlasDir == null)
			customAtlasDir = Settings.getInstance().getAtlasOutputDirectory();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
		String atlasDirName = atlas.getName() + "_" + sdf.format(new Date());
		super.startAtlasCreation(atlas, customAtlasDir);
		baseDir= new File(atlasDir, atlasDirName);
		Utilities.mkDirs(baseDir);

		try {
			db = new ExportTileDatabase(currentMapStoreName, new File(baseDir, "db-dump"));
			binding = db.getTileIndex().getEntityBinding();
		}catch (DatabaseException e){
			throw new IOException("Error create database: db-dump",e );
		}
	}

	@Override
	public void initializeMap(MapInterface map, TileProvider mapTileProvider) {
		super.initializeMap(map, mapTileProvider);
		currentMapStoreName = map.getMapSource().getName();
		if (currentMapStoreName.indexOf("street")>=0)
			currentMapStoreName = "street";
		else if (currentMapStoreName.indexOf("satellite")>=0)
			currentMapStoreName = "map";
	}

	@Override
	public void abortAtlasCreation() throws IOException {
		if(outputFile!=null) {
			outputFile.close();
			outputFile = null;
		}
		if(db!=null) {
			db.close();
			db = null;
		}
		super.abortAtlasCreation();
	}

	@Override
	public void finishAtlasCreation() throws IOException, InterruptedException {
		if(outputFile!=null) {
			outputFile.close();
			outputFile = null;
		}
		if(db!=null) {
			db.close();
			db = null;
		}
		super.finishAtlasCreation();
	}


	@Override
	public boolean testMapSource(MapSource mapSource) {
		return true;
	}

	@Override
	public void createMap() throws MapCreationException, InterruptedException {
		try {
			File dumpFile  = new File(baseDir,currentMapStoreName+".dump");
			outputFile = new PrintStream(new FileOutputStream(dumpFile));
			createTiles();
		}catch (IOException e){
			throw new MapCreationException("Error create database: " + e.getMessage(),map,e);
		}
	}

	protected void createTiles() throws InterruptedException, MapCreationException {
		atlasProgress.initMapCreation((xMax - xMin + 1) * (yMax - yMin + 1));
		ImageIO.setUseCache(false);
		printHeader(outputFile);
		for (int x = xMin; x <= xMax; x++) {
			for (int y = yMin; y <= yMax; y++) {
				checkUserAbort();
				atlasProgress.incMapCreationProgress();
				try {

					byte[] sourceTileData = getTileData(x, y);
					if (sourceTileData != null) {
						DatabaseEntry foundKey = new DatabaseEntry();
						DatabaseEntry foundData = new DatabaseEntry();

						TileDbEntry entry = new TileDbEntry(x,y,zoom,sourceTileData);
						binding.objectToKey(entry,foundKey);
						binding.objectToData(entry,foundData);
						dumpOne(outputFile, foundKey.getData());
						dumpOne(outputFile, foundData.getData());

					}
				} catch (Exception  e) {
					throw new MapCreationException("Error writing tile image: " + e.getMessage(), map, e);
				}
			}
		}
		printFooter(outputFile);
		outputFile.close();
	}

	protected void printHeader(PrintStream o) {
		o.println("VERSION=" + VERSION);
		o.println("format=bytevalue");
		o.println("type=btree");
		o.println("dupsort=0");
		o.println("HEADER=END");
	}

	protected void printFooter(PrintStream o) {
		o.println("DATA=END");
	}

	protected void dumpOne(PrintStream o, byte[] ba) {
		StringBuffer sb = new StringBuffer();
		sb.append(' ');
		CmdUtil.formatEntry(sb, ba, false);
		o.println(sb.toString());
	}

	protected byte[] getTileData(int x, int y) throws Exception{
		return mapDlTileProvider.getTileData(x, y);
	}
}
