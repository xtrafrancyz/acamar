package net.xtrafrancyz.acamar.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author xtrafrancyz
 */
public class LogFormatter extends Formatter {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public String format(LogRecord lr) {
        String message = dateFormat.format(new Date(lr.getMillis())) + " [" + lr.getLevel().getName() + "] " + lr.getMessage();
        if (lr.getThrown() != null) {
            StringWriter sw = new StringWriter();
            lr.getThrown().printStackTrace(new PrintWriter(sw));
            message += sw.toString();
        }
        message += "\n\r";
        return message;
    }
}
