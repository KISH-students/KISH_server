package org.kish;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class MainLogger {
    public static final Logger LOGGER = LogManager.getLogger(MainLogger.class);

    public static void debug(Object content){
        LOGGER.debug(content);
    }

    public static void info(Object content){
        LOGGER.info(content);
    }

    public static void warn(Object content){
        LOGGER.warn(content);
    }

    public static void error(Object content){
        if(KishServer.firebaseManager != null){
            KishServer.firebaseManager.sendFCMToAdmin("오류 발생", content.toString(), new HashMap<>());
        }
        LOGGER.error(content);
    }

    public static void error(Object content, Throwable t){
        if(KishServer.firebaseManager != null){
            String[] stackTrace = ExceptionUtils.getStackTrace(t).split("\n");
            KishServer.firebaseManager.sendFCMToAdmin("오류 발생", stackTrace[0] + "\n" + stackTrace[1], new HashMap<>());
        }
        LOGGER.error(content, t);
    }

}
