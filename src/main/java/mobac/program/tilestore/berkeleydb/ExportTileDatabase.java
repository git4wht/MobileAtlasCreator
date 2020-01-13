package mobac.program.tilestore.berkeleydb;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import mobac.program.tilestore.berkeleydb.TileDbEntry.TileDbKey;
import mobac.utilities.GUIExceptionHandler;
import mobac.utilities.Utilities;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentLockedException;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.evolve.Mutations;
import com.sleepycat.persist.evolve.Renamer;

import org.apache.log4j.Logger;

public class ExportTileDatabase {


    private static final String LOG_FILE_MAX = String.valueOf(1L<<30); // = 2^30 (1G)
    protected Logger log = Logger.getLogger(ExportTileDatabase.class);;
    final String mapSourceName;
    final Environment env;
    final EntityStore store;
    final PrimaryIndex<TileDbKey, TileDbEntry> tileIndex;
    boolean dbClosed = false;

    long lastAccess;
    private EnvironmentConfig envConfig;
    private Mutations mutations;


    public ExportTileDatabase(String mapSourceName, File databaseDirectory) throws IOException,
            EnvironmentLockedException, DatabaseException {
        log.debug("Opening tile store db: \"" + databaseDirectory + "\"");
        File storeDir = databaseDirectory;
        DelayedInterruptThread t = null;
        if(Thread.currentThread() instanceof DelayedInterruptThread ) {
            t = (DelayedInterruptThread) Thread.currentThread();
        }else {
            t = new DelayedInterruptThread(Thread.currentThread());
        }
        try {
            t.pauseInterrupt();

            envConfig = new EnvironmentConfig();
            envConfig.setTransactional(false);
            envConfig.setLocking(true);
            envConfig.setExceptionListener(GUIExceptionHandler.getInstance());
            envConfig.setAllowCreate(true);
            envConfig.setSharedCache(true);
            envConfig.setCachePercent(50);
            envConfig.setConfigParam(EnvironmentConfig.LOG_FILE_MAX, LOG_FILE_MAX);

            mutations = new Mutations();

            String oldPackage1 = "tac.tilestore.berkeleydb";
            String oldPackage2 = "tac.program.tilestore.berkeleydb";
            String entry = ".TileDbEntry";
            String key = ".TileDbEntry$TileDbKey";
            mutations.addRenamer(new Renamer(oldPackage1 + entry, 0, TileDbEntry.class.getName()));
            mutations.addRenamer(new Renamer(oldPackage1 + key, 0, TileDbKey.class.getName()));
            mutations.addRenamer(new Renamer(oldPackage1 + entry, 1, TileDbEntry.class.getName()));
            mutations.addRenamer(new Renamer(oldPackage1 + key, 1, TileDbKey.class.getName()));
            mutations.addRenamer(new Renamer(oldPackage2 + entry, 2, TileDbEntry.class.getName()));
            mutations.addRenamer(new Renamer(oldPackage2 + key, 2, TileDbKey.class.getName()));


            this.mapSourceName = mapSourceName;
            lastAccess = System.currentTimeMillis();

            Utilities.mkDirs(storeDir);

            env = new Environment(storeDir, envConfig);

            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setTransactional(false);
            storeConfig.setMutations(mutations);
            store = new EntityStore(env, "TilesEntityStore", storeConfig);

            tileIndex = store.getPrimaryIndex(TileDbEntry.TileDbKey.class, TileDbEntry.class);
        } finally {
            if (t.interruptedWhilePaused())
                close();
            t.resumeInterrupt();
        }
    }

    public boolean isClosed() {
        return dbClosed;
    }

    public long entryCount() throws DatabaseException {
        return tileIndex.count();
    }

    public void put(TileDbEntry tile) throws DatabaseException {
        DelayedInterruptThread t = null;
        if(Thread.currentThread() instanceof DelayedInterruptThread ) {
            t = (DelayedInterruptThread) Thread.currentThread();
        }else {
            t = new DelayedInterruptThread(Thread.currentThread());
        }
        try {
            t.pauseInterrupt();
            tileIndex.put(tile);
        } finally {
            if (t.interruptedWhilePaused())
                close();
            t.resumeInterrupt();
        }
    }

    public boolean contains(TileDbKey key) throws DatabaseException {
        return tileIndex.contains(key);
    }

    public TileDbEntry get(TileDbKey key) throws DatabaseException {
        return tileIndex.get(key);
    }

    public PrimaryIndex<TileDbKey, TileDbEntry> getTileIndex() {
        return tileIndex;
    }

    public BufferedImage getCacheCoverage(int zoom, Point tileNumMin, Point tileNumMax) throws DatabaseException,
            InterruptedException {
        log.debug("Loading cache coverage for region " + tileNumMin + " " + tileNumMax + " of zoom level " + zoom);
        DelayedInterruptThread t = null;
        if(Thread.currentThread() instanceof DelayedInterruptThread ) {
            t = (DelayedInterruptThread) Thread.currentThread();
        }else {
            t = new DelayedInterruptThread(Thread.currentThread());
        }
        int width = tileNumMax.x - tileNumMin.x + 1;
        int height = tileNumMax.y - tileNumMin.y + 1;
        byte ff = (byte) 0xFF;
        byte[] colors = new byte[] { 120, 120, 120, 120, // alpha-gray
                10, ff, 0, 120 // alpha-green
        };
        IndexColorModel colorModel = new IndexColorModel(2, 2, colors, 0, true);
        BufferedImage image = null;
        try {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
        } catch (Throwable e) {
            log.error("Failed to create coverage image: " + e.toString());
            image = null;
            System.gc();
            return null;
        }
        WritableRaster raster = image.getRaster();

        // We are loading the coverage of the selected area column by column which is much faster than loading the
        // whole region at once
        for (int x = tileNumMin.x; x <= tileNumMax.x; x++) {
            TileDbKey fromKey = new TileDbKey(x, tileNumMin.y, zoom);
            TileDbKey toKey = new TileDbKey(x, tileNumMax.y, zoom);
            EntityCursor<TileDbKey> cursor = tileIndex.keys(fromKey, true, toKey, true);
            try {
                TileDbKey key = cursor.next();
                while (key != null) {
                    int pixelx = key.x - tileNumMin.x;
                    int pixely = key.y - tileNumMin.y;
                    raster.setSample(pixelx, pixely, 0, 1);
                    key = cursor.next();
                    if (t.isInterrupted()) {
                        log.debug("Cache coverage loading aborted");
                        throw new InterruptedException();
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return image;
    }

    protected void purge() {
        try {
            store.sync();
            env.cleanLog();
        } catch (DatabaseException e) {
            log.error("database compression failed: ", e);
        }
    }

    public void close() {
        if (dbClosed)
            return;
        DelayedInterruptThread t = null;
        if(Thread.currentThread() instanceof DelayedInterruptThread ) {
            t = (DelayedInterruptThread) Thread.currentThread();
        }else {
            t = new DelayedInterruptThread(Thread.currentThread());
        }
        try {
            t.pauseInterrupt();
            try {
                log.debug("Closing tile store db \"" + mapSourceName + "\"");
                if (store != null)
                    store.close();
            } catch (Exception e) {
                log.error("", e);
            }
            try {
                env.close();
            } catch (Exception e) {
                log.error("", e);
            } finally {
                dbClosed = true;
            }
        } finally {
            if (t.interruptedWhilePaused())
                close();
            t.resumeInterrupt();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
