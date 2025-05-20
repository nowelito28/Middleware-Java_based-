package protocol;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Declarar loggers a utilizar en los programas<br>
 * Utilizar slog (log4j) --> para depurar (.info -> info, .error -> error, .debug -> debug, .trace -> trace)<br>
 * Configurar properties log4j2.properties (logger.name.name=nombre, logger.slog.level=DEBUG)
 */
public class ToolDefs {
    public static final Logger slog = LogManager.getLogger("slog");
    public static final Logger ulog = LogManager.getLogger("ulog");
    public static final Logger svclog = LogManager.getLogger("svclog");
    public static final Logger drawlog = LogManager.getLogger("drawlog");
}
