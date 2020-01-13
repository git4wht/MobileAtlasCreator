package mobac.mapsources.loader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JOptionPane;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import mobac.exceptions.MapSourceCreateException;
import mobac.mapsources.MapSourcesManager;
import mobac.mapsources.custom.CustomCloudMade;
import mobac.mapsources.custom.CustomCombinedMapSource;
import mobac.mapsources.custom.CustomLocalTileFilesMapSource;
import mobac.mapsources.custom.CustomLocalTileSQliteMapSource;
import mobac.mapsources.custom.CustomLocalTileZipMapSource;
import mobac.mapsources.custom.CustomMapSource;
import mobac.mapsources.custom.CustomMapsforge;
import mobac.mapsources.custom.CustomMultiLayerMapSource;
import mobac.mapsources.custom.CustomWmsMapSource;
import mobac.program.interfaces.FileBasedMapSource;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.WrappedMapSource;
import mobac.program.model.MapSourceLoaderInfo;
import mobac.program.model.MapSourceLoaderInfo.LoaderType;
import mobac.utilities.Utilities;
import mobac.utilities.file.DirOrFileExtFilter;

public class CustomMapSourceLoader {

	private final Logger log = Logger.getLogger(MapPackManager.class);
	private final MapSourcesManager mapSourcesManager;
	private final File mapSourcesDir;

	private final DocumentBuilderFactory dbFactory;
	private final DocumentBuilder dBuilder;
	private final JAXBContext context;

	public CustomMapSourceLoader(MapSourcesManager mapSourceManager, File mapSourcesDir) {
		this.mapSourcesManager = mapSourceManager;
		this.mapSourcesDir = mapSourcesDir;
		dbFactory = DocumentBuilderFactory.newInstance();
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
		try {
			Class<?>[] customMapClasses = new Class[] { //
					//
					CustomMapSource.class, //
					CustomWmsMapSource.class, //
					CustomMultiLayerMapSource.class, //
					CustomCombinedMapSource.class, // WHT CUSTOM
					CustomMapsforge.class, //
					CustomCloudMade.class, //
					CustomLocalTileFilesMapSource.class, //
					CustomLocalTileZipMapSource.class, //
					CustomLocalTileSQliteMapSource.class //
			};
			context = JAXBContext.newInstance(customMapClasses);
		} catch (JAXBException e) {
			throw new RuntimeException("Unable to create JAXB context for custom map sources", e);
		}
	}

	public void loadCustomMapSources() {
		List<File> customMapSourceFiles = Utilities.traverseFolder(mapSourcesDir, new DirOrFileExtFilter(".xml"));
		/*
		 * It is important to sort the files to be loaded, otherwise the order would be random which makes it difficult
		 * to reference custom map sources in a multi-layer map source if the referenced map source has not been loaded
		 * before.
		 * 
		 * See https://sourceforge.net/p/mobac/bugs/294/
		 */
		Collections.sort(customMapSourceFiles, new Comparator<File>() {
			@Override
			public int compare(File f1, File f2) {
				return f1.getName().compareTo(f2.getName());
			}
		});

		for (File f : customMapSourceFiles) {
			try {
				MapSource customMapSource = loadCustomMapSource(f);
				if (customMapSource == null) {
					log.info("Ingnoring xml file \"" + f.getName() + "\" - not a custom MOBAC XML map file");
					continue; // an element to be ignored
				}
				if (!(customMapSource instanceof FileBasedMapSource) && customMapSource.getTileImageType() == null)
					log.warn("A problem occured while loading \"" + f.getName()
							+ "\": tileType is null - some atlas formats will produce an error!");
				log.trace("Custom map source loaded: " + customMapSource + " from file \"" + f.getName() + "\"");
				mapSourcesManager.addMapSource(customMapSource);
			} catch (Exception e) {
				log.error("failed to load custom map source \"" + f.getName() + "\": " + e.getMessage(), e);
			}
		}
	}

	public MapSource loadCustomMapSource(File mapSourceFile)
			throws MapSourceCreateException, JAXBException, IOException, SAXException {

		try {
			Document doc = dBuilder.parse(mapSourceFile);
			Element elem = doc.getDocumentElement();
			if ("rendertheme".equals(elem.getTagName())) {
				return null;
			}
		} catch (Exception e) {
			log.error("Failed to load custom map source file \"" + mapSourceFile + "\": " + e);
		}
		try (FileReader reader = new FileReader(mapSourceFile)) {
			return internalLoadMapSource(new InputSource(reader), mapSourceFile);
		}
	}

	public MapSource loadCustomMapSource(InputStream in)
			throws MapSourceCreateException, SAXException, IOException, JAXBException {
		return internalLoadMapSource(new InputSource(in), null);
	}

	/**
	 * Load custom map source from XML document DOM
	 * 
	 * @param source
	 * @param loaderInfoFile
	 * @return
	 * @throws MapSourceCreateException
	 * @throws JAXBException
	 * @throws SAXException
	 * @throws IOException
	 */
	protected MapSource internalLoadMapSource(InputSource source, final File loaderInfoFile)
			throws MapSourceCreateException, SAXException, IOException, JAXBException {
		MapSource customMapSource;

		// check for mapsforge rendertheme and if it is one ignore it

		Unmarshaller unmarshaller = context.createUnmarshaller();
		unmarshaller.setEventHandler(new ValidationEventHandler() {

			@Override
			public boolean handleEvent(ValidationEvent event) {
				ValidationEventLocator loc = event.getLocator();
				String file = "";
				if (loaderInfoFile != null) {
					file = loaderInfoFile.getName();
				}
				int lastSlash = file.lastIndexOf('/');
				if (lastSlash > 0)
					file = file.substring(lastSlash + 1);

				String errorMsg = event.getMessage();
				if (errorMsg == null) {
					Throwable t = event.getLinkedException();
					while (t != null && errorMsg == null) {
						errorMsg = t.getMessage();
						t = t.getCause();
					}
				}

				JOptionPane.showMessageDialog(null,
						"<html><h3>Failed to load a custom map</h3><p><i>" + errorMsg + "</i></p><br><p>file: \"<b>"
								+ file + "</b>\"<br>line/column: <i>" + loc.getLineNumber() + "/"
								+ loc.getColumnNumber() + "</i></p>",
						"Error: custom map loading failed", JOptionPane.ERROR_MESSAGE);
				log.error(event.toString());
				return false;
			}
		});
		Object o = unmarshaller.unmarshal(source);
		if (o instanceof WrappedMapSource)
			customMapSource = ((WrappedMapSource) o).getMapSource();
		else
			customMapSource = (MapSource) o;
		customMapSource.setLoaderInfo(new MapSourceLoaderInfo(LoaderType.XML, loaderInfoFile));
		log.trace("Custom map source loaded: " + customMapSource);
		return customMapSource;
	}

}
