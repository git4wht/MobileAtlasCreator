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

import mobac.exceptions.MapCreationException;
import mobac.mapsources.mapspace.MercatorPower2MapSpace;
import mobac.program.annotations.AtlasCreatorName;
import mobac.program.annotations.SupportedParameters;
import mobac.program.atlascreators.impl.MapTileWriter;
import mobac.program.atlascreators.tileprovider.ConvertedRawTileProvider;
import mobac.program.atlascreators.tileprovider.TileProvider;
import mobac.program.interfaces.MapInterface;
import mobac.program.interfaces.MapSource;
import mobac.program.model.TileImageParameters.Name;
import mobac.utilities.Utilities;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Creates maps identical to the atlas format used by TMS.
 *
 * Please note that this atlas format ignores the defined atlas structure. It uses a separate directory for each used
 * map source and inside one directory for each zoom level.
 */
@AtlasCreatorName("TMS tile storage")
@SupportedParameters(names = { Name.format })
public class TMSTiles extends OSMTracker {

	public void createMap() throws MapCreationException, InterruptedException {
		// This means there should not be any resizing of the tiles.
		if (mapTileWriter == null)
			mapTileWriter = new TMSTileWriter();
		createTiles();
	}

	protected class TMSTileWriter extends OSMTileWriter {
		public void writeTile(int tilex, int tiley, String tileType, byte[] tileData) throws IOException {
			tiley = ((1 << zoom) - tiley - 1);
			File file = new File(mapDir, String.format(tileFileNamePattern, zoom, tilex, tiley, tileType));
			writeTile(file, tileData);
		}
	}
}
