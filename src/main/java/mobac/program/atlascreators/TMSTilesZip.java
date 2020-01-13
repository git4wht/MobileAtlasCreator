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

import mobac.exceptions.AtlasTestException;
import mobac.exceptions.MapCreationException;
import mobac.program.annotations.AtlasCreatorName;
import mobac.program.annotations.SupportedParameters;
import mobac.program.atlascreators.impl.MapTileWriter;
import mobac.program.atlascreators.tileprovider.TileProvider;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.interfaces.MapInterface;
import mobac.program.model.Settings;
import mobac.program.model.TileImageParameters.Name;
import mobac.utilities.Utilities;
import mobac.utilities.stream.ZipStoreSplitOutputStream;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Creates maps identical to the atlas format used by TMS.
 *
 * Please note that this atlas format ignores the defined atlas structure. It uses a separate directory for each used
 * map source and inside one directory for each zoom level.
 */
@AtlasCreatorName("TMS tile storage (ZIP)")
@SupportedParameters(names = { Name.format })
public class TMSTilesZip extends TMSTiles {

	protected ZipStoreSplitOutputStream zipStream = null;
	protected String currentMapStoreName = null;

	public void createMap() throws MapCreationException, InterruptedException {
		createTiles();
	}

	@Override
	public void startAtlasCreation(AtlasInterface atlas, File customAtlasDir) throws AtlasTestException, IOException,
			InterruptedException {
		if (customAtlasDir == null)
			customAtlasDir = Settings.getInstance().getAtlasOutputDirectory();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
		String atlasDirName = atlas.getName() + "_" + sdf.format(new Date());
		super.startAtlasCreation(atlas, customAtlasDir);
		File atlasDirSub = new File(atlasDir, atlasDirName);
		Utilities.mkDirs(atlasDirSub);
		zipStream = new ZipStoreSplitOutputStream(new File(atlasDirSub, atlasDirName + ".zip"));
		mapTileWriter = new TMSZipTileWriter();
	}

	@Override
	public void abortAtlasCreation() throws IOException {
		Utilities.closeStream(zipStream);
		super.abortAtlasCreation();
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
	public void finishLayerCreation() throws IOException {
		Utilities.closeStream(zipStream);
		super.finishLayerCreation();
	}
	@Override
	public void finishAtlasCreation() throws IOException, InterruptedException {
		Utilities.closeStream(zipStream);
		super.finishAtlasCreation();
	}
	private class TMSZipTileWriter implements MapTileWriter {

		public void finalizeMap() throws IOException {
		}

		public void writeTile(int tilex, int tiley, String tileType, byte[] tileData) throws IOException {
			tiley = ((1 << zoom) - tiley - 1);
			String tileName = currentMapStoreName + "/"
					+ String.format(tileFileNamePattern, zoom, tilex, tiley, tileType);
			zipStream.writeStoredEntry(tileName, tileData);
		}
	}
}
