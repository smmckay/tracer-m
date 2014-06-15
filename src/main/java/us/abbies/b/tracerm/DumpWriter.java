package us.abbies.b.tracerm;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DumpWriter {
    private static final Logger logger = Logger.getLogger(DumpWriter.class.getName());

    private final boolean dumpToStderr;
    private final Writer[] files;
    private final Logger[] loggers;
    private final boolean nop;

    public DumpWriter(boolean dumpToStderr, Writer[] files, Logger[] loggers) {
        if (files == null) {
            throw new IllegalArgumentException("files");
        }
        if (loggers == null) {
            throw new IllegalArgumentException("loggers");
        }

        this.dumpToStderr = dumpToStderr;
        this.files = files;
        this.loggers = loggers;

        nop = !dumpToStderr && files.length == 0 && loggers.length == 0;
    }

    public void dumpObject(Object o) {
        if (nop) {
            return;
        }

        String repr = fancyToString(o);

        if (dumpToStderr) {
            System.err.println(repr);
        }

        for (int i = 0; i < files.length; i++) {
            Writer file = files[i];
            if (file == null) {
                continue;
            }

            try {
                file.write(repr);
                file.write(System.lineSeparator());
            } catch (IOException | RuntimeException e) {
                files[i] = null;
                logger.log(Level.WARNING, "dump to file failed, closing file " + i, e);
                try {
                    file.close();
                } catch (IOException | RuntimeException e1) {
                    // nothing we can do
                }
            }
        }

        for (int i = 0; i < loggers.length; i++) {
            Logger l = loggers[i];
            if (l == null) {
                continue;
            }

            try {
                if (l.isLoggable(Level.INFO)) {
                    l.info(repr);
                }
            } catch (RuntimeException e) {
                loggers[i] = null;
                logger.log(Level.WARNING, "logging failed, ignoring logger " + i, e);
            }
        }
    }

    static String fancyToString(Object o) {
        String repr;
        try {
            if (o instanceof Object[]) {
                repr = Arrays.deepToString((Object[]) o);
            } else if (o instanceof byte[]) {
                repr = Arrays.toString((byte[]) o);
            } else if (o instanceof short[]) {
                repr = Arrays.toString((short[]) o);
            } else if (o instanceof int[]) {
                repr = Arrays.toString((int[]) o);
            } else if (o instanceof long[]) {
                repr = Arrays.toString((long[]) o);
            } else if (o instanceof char[]) {
                repr = Arrays.toString((char[]) o);
            } else if (o instanceof float[]) {
                repr = Arrays.toString((float[]) o);
            } else if (o instanceof double[]) {
                repr = Arrays.toString((double[]) o);
            } else if (o instanceof boolean[]) {
                repr = Arrays.toString((boolean[]) o);
            } else if (o instanceof Throwable) {
                repr = throwableToString((Throwable) o);
            } else {
                repr = Objects.toString(o);
            }
        } catch (RuntimeException e) {
            repr = throwableToString(e);
        }
        return repr;
    }

    private static String throwableToString(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t);
        for (StackTraceElement ste : t.getStackTrace()) {
            sb.append("\t at ").append(ste);
        }
        return sb.toString();
    }
}
