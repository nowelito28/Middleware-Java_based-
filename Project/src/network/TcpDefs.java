package network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Utilizar slog (log4j) --> para depurar (.info -> info, .error -> error, .debug -> debug, .trace -> trace)
// Configurar properties log4j.properties (logger.name.name=nombre, logger.slog.level=DEBUG)
public class TcpDefs {
    public static final Logger slog = LogManager.getLogger("slog");
    public static final Logger ulog = LogManager.getLogger("ulog");
    public static final Logger mlog = LogManager.getLogger("mlog");

}
