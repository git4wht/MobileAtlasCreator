package mobac.tools;

import mobac.program.Logging;
import mobac.program.ProgramInfo;
import mobac.program.tilestore.TileStore;
import mobac.program.tilestore.berkeleydb.DelayedInterruptThread;
import mobac.tools.program.Delete;
import mobac.tools.program.DeleteTiles;
import mobac.tools.program.Empty;
import mobac.tools.program.Extract;
import mobac.tools.program.Merge;
import mobac.tools.program.Print;
import mobac.tools.program.Purge;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;


public class Main {
  public static Logger log;
  public static String version = "?";

  static Runnable commandImplementation = null;

  public static void main(String[] args) {
    Logger.getRootLogger().removeAllAppenders();
    Logging.configureConsoleLogging(Level.INFO, new PatternLayout("%d{HH:mm:ss} %-5p %c{1}: %m%n"));
    log = Logger.getLogger("TileStoreUtil");
    log.setLevel(Level.DEBUG);
    ProgramInfo.initialize();

    Properties prop = new Properties();
    try {
      prop.load(Main.class.getResourceAsStream("ts-util.properties"));
      version = prop.getProperty("ts-util.version");
    } catch (IOException e) {
      log.error("", e);
    }

    if (args.length == 0) {
      showHelp(true);
    }
    try {
      String modeStr = args[0].toLowerCase();
      if ("help".equalsIgnoreCase(modeStr) || "?".equalsIgnoreCase(modeStr) || "-?".equalsIgnoreCase(modeStr)) {
        showHelp(true);
      }
      if (args.length >= 2 && modeStr.equals("deletetiles")) {
        LinkedList<String> tiles = new LinkedList<String>(Arrays.asList(args));
        tiles.removeFirst();
        tiles.removeFirst();

        commandImplementation = new DeleteTiles(args[1], tiles);
      }
      if (args.length >= 3) {
        LinkedList<String> deleteFilter;
        switch (modeStr) {
          case "merge":
            commandImplementation = new Merge(args[1], args[2]);
            break;
          case "extract":
            commandImplementation = new Extract(args[1], args[2]);
            break;
          case "delete":
            deleteFilter = new LinkedList<String>(Arrays.asList(args));
            deleteFilter.removeFirst();
            deleteFilter.removeFirst();
            commandImplementation = new Delete(args[1], deleteFilter);
            break;
        }
      } else if (args.length == 2) {
        switch (modeStr) {
          case "purge":
            commandImplementation = new Purge(args[1]);
            break;
          case "print":
            commandImplementation = new Print(args[1]);
            break;
          case "empty":
            commandImplementation = new Empty(args[1]);
            break;
        }

      }
    } catch (InvalidParameterException e) {
      commandImplementation = null;
    }
    if (commandImplementation == null) {
      showHelp(false);
    }
    DelayedInterruptThread delayedInterruptThread = new DelayedInterruptThread("TileStoreUtil") {
      public void run() {
        TileStore.initialize();
        Main.commandImplementation.run();
      }
    };

    delayedInterruptThread.start();
  }

  private static void showHelp(boolean longHelp) {
    System.out.println(getNameAndVersion());
    printResource("help.txt");
    if (longHelp)
      printResource("help_long.txt");
    System.exit(1);
  }

  public static void printResource(String resouceName) {
    InputStream in = TileStoreUtil.class.getResourceAsStream(resouceName);
    InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
    char[] buf = new char[4096];
    int read = 0;
    try {
      while ((read = reader.read(buf)) > 0) {
        char[] buf2 = buf;
        if (read < buf2.length) {
          buf2 = new char[read];
          System.arraycopy(buf, 0, buf2, 0, buf2.length);
        }
        System.out.print(buf2);
      }
    } catch (IOException e) {
      log.error("", e);
    }
  }


  public static String getNameAndVersion() {
    return "MOBAC TileStore utility v" + version;
  }
}

