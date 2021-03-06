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
package mobac.program.tiledatawriter;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import mobac.optional.JavaAdvancedImaging;

public class TileImagePng8DataWriterBuilder extends TileImagePngDataWriterBuilder {

	public TileImagePng8DataWriterBuilder() {
		super();
	}
	
	@Override
	public TileImagePng8DataWriter build() {
		return new TileImagePng8DataWriter();
	}



	@Override
	public void processImage(BufferedImage image, OutputStream out) throws IOException {
		BufferedImage image2 = JavaAdvancedImaging.colorReduceMedianCut(image, 256);
		super.processImage(image2, out);
	}

}
