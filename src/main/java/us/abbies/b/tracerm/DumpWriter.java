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
        this.dumpToStderr = dumpToStderr;
        this.files = Objects.requireNonNull(files, "files");
        this.loggers = Objects.requireNonNull(loggers, "loggers");

        nop = !dumpToStderr && files.length == 0 && loggers.length == 0;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void dumpObject(Object o) {
        if (nop) {
            return;
        }

        String repr;
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
            StringBuilder sb = new StringBuilder();
            sb.append(o);
            for (StackTraceElement ste : ((Throwable) o).getStackTrace()) {
                sb.append("\t at ").append(ste);
            }
            repr = sb.toString();
        } else {
            repr = Objects.toString(o);
        }

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
            } catch (IOException e) {
                files[i] = null;
                logger.log(Level.WARNING, "dump to file failed, closing file", e);
                try {
                    file.close();
                } catch (IOException e1) {
                    // nothing we can do
                }
            }
        }

        for (Logger l : loggers) {
            if (l == null) {
                continue;
            }

            if (l.isLoggable(Level.INFO)) {
                l.info(repr);
            }
        }
    }
}
