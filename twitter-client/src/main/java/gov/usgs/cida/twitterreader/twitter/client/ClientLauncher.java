package gov.usgs.cida.twitterreader.twitter.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Properties;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class ClientLauncher {

    private static final Logger packageLogger = (Logger) LoggerFactory.getLogger("gov.usgs.cida.twitterreader.twitter.client");
    private static Logger logger;
    private static CmdLineParser parser;
    private File baseDirectory = null;
    private String propertiesFile = null;
    private File propertiesPath = null;
    private File logDirectory = null;
    private final Properties properties = new Properties();
    private enum LOGLEVEL {

        ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF
    };

    @Option(name = "-d",
            usage = "Application Base Directory",
            metaVar = "String",
            required = true)
    public void setBaseDirectory(File file) {
        this.baseDirectory = file;
    }

    @Option(name = "-p",
            usage = "Properties File Name",
            metaVar = "String",
            depends = {"-d"})
    public void setPropertiesFile(String file) {
        this.propertiesFile = file;
    }

    @Option(name = "-loggers.use",
            usage = "Use file-based logging to log Twitter messages and events")
    private boolean useLoggers = false;

    @Option(name = "-h",
            usage = "Print this documentation",
            required = false,
            hidden = false)
    private boolean help;

    @Option(name = "-loggers.level",
            depends = "-loggers.use",
            usage = "Sets the level of logging to be used by loggers")
    private LOGLEVEL loggersLevel;

    @Option(name = "-log.level",
            usage = "Sets the level of logging to be used by application logging (default = INFO)")
    private final LOGLEVEL loggingLevel = LOGLEVEL.INFO;

    public void run(String... args) throws FileNotFoundException, IOException {
        parser = new CmdLineParser(this);
        parser.setUsageWidth(80);
        try {
            parser.parseArgument(args);

            // Check to make sure that the base directory exists
            if (!baseDirectory.exists()) {
                throw new FileNotFoundException(String.format("Directory at %s does not exist", baseDirectory.getAbsolutePath()));
            }

            initializeLogging();

            if (propertiesFile != null) {
                processPropertiesFile();
            }

            if (useLoggers) {
                logDirectory = new File(baseDirectory, "logs");
                prepareLoggingDirectory();
            }

        } catch (CmdLineException | NullPointerException ex) {
            if (help) {
                printUsage(System.out);
            } else {
                if (ex instanceof NullPointerException) {
                    System.err.println("Null was passed into the launcher");
                } else {
                    System.err.println(ex.getMessage());
                }
                printUsage(System.err);
            }

        }
    }

    private void printUsage(PrintStream stream) {
        stream.println("java Client [options...] arguments...");
        stream.println("-------------------------------------");
        parser.printUsage(stream);

        // print option sample. This is useful some time
        stream.println();
        stream.println(" Example: java Client" + parser.printExample(OptionHandlerFilter.REQUIRED));
    }

    private void processPropertiesFile() throws FileNotFoundException, IOException {
        propertiesPath = new File(baseDirectory, propertiesFile);
        if (!propertiesPath.exists()) {
            throw new FileNotFoundException(String.format("Properties file at %s does not exist", baseDirectory.getAbsolutePath()));
        }
        properties.load(new FileInputStream(propertiesPath));
        logger.debug(String.format("Properties file at %s has been loaded", propertiesPath.getPath()));

        useLoggers = Boolean.parseBoolean(properties.getProperty("loggers.use", "false"));
    }

    private void prepareLoggingDirectory() throws IOException {
        if (!baseDirectory.exists()) {
            Files.createDirectory(baseDirectory.toPath());
        }

        if (!logDirectory.exists()) {
            Files.createDirectory(logDirectory.toPath());
        }
    }

    private void initializeLogging() {
        // Set logging level and appenders for the package and instantiate 
        // the logger for this class
        packageLogger.setLevel(Level.toLevel(loggingLevel.name()));
        logger = (Logger) LoggerFactory.getLogger(ClientLauncher.class);
        logger.info("Logger Set To {}", logger.getEffectiveLevel());
    }
}
