package gov.usgs.cida.twitterreader.twitter.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.twitter.hbc.core.endpoint.Location;
import gov.usgs.cida.twitter.reader.data.client.TwitterClient;
import gov.usgs.cida.twitter.reader.data.client.auth.IAuthType;
import gov.usgs.cida.twitter.reader.data.client.auth.OAuth;
import gov.usgs.cida.twitter.reader.data.client.auth.UserPasswordAuth;
import gov.usgs.cida.twitterreader.commons.logging.LoggerType;
import gov.usgs.cida.twitterreader.commons.logging.TwitterAppenderFactory;
import gov.usgs.cida.twitterreader.commons.logging.TwitterLoggerContext;
import gov.usgs.cida.twitterreader.commons.observer.LoggingEventObserver;
import gov.usgs.cida.twitterreader.commons.observer.LoggingMessageObserver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class ClientLauncher {

    private static final Logger logger = (Logger) LoggerFactory.getLogger("gov.usgs.cida.twitterreader");
    private static CmdLineParser parser;
    private File baseDirectory = null;
    private String propertiesFiles = null;
    private File logDirectory = null;
    private final Properties properties = new Properties();
    private static TwitterClient client;
    private String oauthConsumerKey;
    private String oauthConsumerSecret;
    private String oauthToken;
    private String oauthSecret;
    private String simpleUsername;
    private String simplePassword;
    private final List<Long> userIds = new ArrayList<>();
    private final List<String> terms = new ArrayList<>();
    private final List<Location> locations = new ArrayList<>();
    private boolean oauth;
    private boolean simple;
    private IAuthType authType;
    private boolean processedCommandline = false;

    @Option(name = "-h",
            aliases = {"--help"},
            usage = "Print this documentation",
            required = false,
            hidden = false)
    private boolean help;

    @Option(name = "-d",
            aliases = {"--directory.base"},
            usage = "Application Base Directory (Required)",
            metaVar = "String",
            required = true)
    public void setBaseDirectory(File file) {
        this.baseDirectory = file;
    }

    @Option(name = "-p",
            aliases = {"--property.files"},
            usage = "One or more comma separated properties filenames. Minimally contains authentication credentials. (Required)",
            metaVar = "String",
            depends = {"-d"},
            required = true)
    public void setPropertiesFile(String files) {
        this.propertiesFiles = files;
    }

    @Option(name = "-l",
            aliases = {"--logging.use"},
            usage = "Use file-based logging to log Twitter messages and events (default: false)")
    private boolean useLoggers = false;

    @Option(name = "--debug",
            usage = "Enable debug logging. (default: false)")
    private boolean debugLogging = false;

    @Option(name = "--track.userids",
            usage = "Comma separated list of user ids to track",
            metaVar = "Long")
    public void setUserIds(String incomingUids) {
        if (StringUtils.isNotBlank(incomingUids)) {
            for (String userId : incomingUids.split(",")) {
                Long idLong;

                try {
                    idLong = Long.parseLong(userId);
                    userIds.add(idLong);
                } catch (NumberFormatException nfe) {
                    logger.debug(String.format("Could not parse incoming userid %s", userId));
                }
            }
        }
    }

    @Option(name = "--track.terms",
            usage = "Comma separated list of terms to track",
            metaVar = "String")
    public void setTerms(String incomingUids) {
        if (StringUtils.isNotBlank(incomingUids)) {
            for (String term : incomingUids.split(",")) {
                if (StringUtils.isNotBlank(term)) {
                    terms.add(term);
                }
            }
        }
    }

    @Option(name = "--track.locations",
            usage = "Comma separated set of list of doubles per location in the "
            + "format of (southwest-lon,southwest-lat,northeast-lon,northeast-lat). "
            + "Sets are separated by pipe (|) character. Example: \"1.00,2.00,3.00,4.00|2.00,3.00,4.00,5.00\"",
            metaVar = "Double,Double,Double,Double")
    public void setLocations(String locationList) {
        String[] incomingLocations = locationList.split("\\|");
        for (String locationString : incomingLocations) {
            String[] locationStringArray = locationString.split(",");
            if (locationStringArray.length != 4) {
                throw new IllegalArgumentException("track.locations elements must be in the format of southwest-lon,southwest-lat,northeast-lon,northeast-lat");
            }

            try {
                Double swLon = Double.parseDouble(locationStringArray[0].trim());
                Double swLat = Double.parseDouble(locationStringArray[1].trim());
                Double neLon = Double.parseDouble(locationStringArray[2].trim());
                Double neLat = Double.parseDouble(locationStringArray[3].trim());
                Location.Coordinate swCoord = new Location.Coordinate(swLon, swLat);
                Location.Coordinate neCoord = new Location.Coordinate(neLon, neLat);
                Location location = new Location(swCoord, neCoord);
                locations.add(location);
            } catch (NumberFormatException nfe) {
                logger.debug(String.format("Could not convert location element to Double"));
            }
        }
    }

    private void printUsage(PrintStream stream) {
        stream.println("java Client [options...] arguments...");
        stream.println("-------------------------------------");
        parser.printUsage(stream);
    }

    public TwitterClient buildClient(String... args) throws IOException, CmdLineException {
        // Process the flags coming in on the command line 
        processCommandLine(args);

        if (processedCommandline && (client == null || TwitterClient.isStopped())) {
            ClientBuilder clientBuilder = new ClientBuilder(authType)
                    .addObserver(new LoggingEventObserver(logger))
                    .addObserver(new LoggingMessageObserver(logger))
                    .setUserIds(userIds)
                    .setTerms(terms)
                    .setLocations(locations)
                    .setLogger(logger);
            client = clientBuilder.build();
        }

        return client;
    }

    private void processCommandLine(String[] args) throws IOException, CmdLineException {
        parser = new CmdLineParser(this);
        parser.setUsageWidth(80);
        try {
            parser.parseArgument(args);

            // Check to make sure that the base directory exists
            if (!baseDirectory.exists()) {
                throw new FileNotFoundException(String.format("Directory at %s does not exist", baseDirectory.getAbsolutePath()));
            }

            // If a properties file exists, use that
            if (propertiesFiles != null) {
                processPropertiesFile();
            }

            // If I am using file based logging, set up logging for files
            if (useLoggers) {
                logDirectory = new File(baseDirectory, "logs");
                prepareLoggingDirectory();

                // We are using loggers so I need to create a file logger for 
                // this application, get its appender and attach it to the current logger
                // so all logs will go to console and file
                TwitterLoggerContext tlc = new TwitterLoggerContext(logger.getName());
                tlc.setOutputDirectory(logDirectory);
                tlc.setLoggerType(LoggerType.FILE);

                logger.addAppender(new TwitterAppenderFactory(tlc).createAppender());
                logger.debug("File appender added");
                logger.debug(String.format("Will be logging to: %s", tlc.getOutputDirectory().getPath()));
            }

            // Set logging level and appenders for the package and instantiate 
            // the logger for this class
            Level logLevel = debugLogging ? Level.DEBUG : Level.INFO;
            logger.setLevel(logLevel);
            logger.info("Logger Set To {}", logger.getEffectiveLevel());

            processedCommandline = true;
        } catch (CmdLineException ex) {
            // User may not have passed in any arguments
            boolean missingArgmentList = "no argument is allowed:".equals(ex.getMessage().trim().toLowerCase());

            if (help || missingArgmentList) {
                printUsage(System.out);
            } else {
                throw ex;
            }
        }
    }

    private void processPropertiesFile() throws FileNotFoundException, IOException {
        String[] propertiesFilesArr = this.propertiesFiles.split(",");
        for (String prtopertyFile : propertiesFilesArr) {
            File propertiesPath = new File(baseDirectory, prtopertyFile);
            if (!propertiesPath.exists()) {
                throw new FileNotFoundException(String.format("Properties file at %s does not exist", propertiesPath.getAbsolutePath()));
            }
            this.properties.load(new FileInputStream(propertiesPath));
            logger.info(String.format("Properties file at %s has been loaded", propertiesPath.getPath()));
        }

        this.oauthConsumerKey = properties.getProperty("auth.oauth.consumerkey");
        this.oauthConsumerSecret = properties.getProperty("auth.oauth.consumersecret");
        this.oauthToken = properties.getProperty("auth.oauth.token");
        this.oauthSecret = properties.getProperty("auth.oauth.secret");
        this.simpleUsername = properties.getProperty("auth.simple.username");
        this.simplePassword = properties.getProperty("auth.simple.password");

        this.oauth = StringUtils.isNotBlank(this.oauthConsumerKey) && StringUtils.isNotBlank(this.oauthConsumerSecret)
                && StringUtils.isNotBlank(this.oauthToken) && StringUtils.isNotBlank(this.oauthSecret);
        this.simple = StringUtils.isNotBlank(this.simpleUsername) && StringUtils.isNotBlank(this.simplePassword);
        if (!oauth && !simple) {
            throw new IllegalArgumentException("Required: OAuth credentials or Simple Authentication credentials");
        } else if (oauth) {
            this.authType = new OAuth(this.oauthConsumerKey, this.oauthConsumerSecret, this.oauthToken, this.oauthSecret);
            logger.debug(String.format("Client launcher using consumer key %s for authentication", this.oauthConsumerKey ));
        } else {
            this.authType = new UserPasswordAuth(this.simpleUsername, this.simplePassword);
            logger.debug(String.format("Client launcher using username %s for authentication",  this.simpleUsername));
        }

        if (properties.containsKey("logging.use")) {
            useLoggers = true;
        }

        if (properties.containsKey("debugging")) {
            debugLogging = true;
        }

        if (properties.containsKey("track.userids")) {
            this.setUserIds(properties.getProperty("track.userids"));
        }

        if (properties.containsKey("track.terms")) {
            this.setTerms(properties.getProperty("track.terms"));
        }

        if (properties.containsKey("track.locations")) {
            this.setLocations(properties.getProperty("track.locations"));
        }

    }

    private void prepareLoggingDirectory() throws IOException {
        if (!baseDirectory.exists()) {
            Files.createDirectory(baseDirectory.toPath());
        }

        if (!logDirectory.exists()) {
            Files.createDirectory(logDirectory.toPath());
        }
    }

    public TwitterClient getClient() {
        return client;
    }

}
