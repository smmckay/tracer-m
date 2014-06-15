package us.abbies.b.tracerm;

import org.hamcrest.Matcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static us.abbies.b.tracerm.matchers.RegexMatcher.matchesRegex;

public class DumpWriterTest {

    @DataProvider
    public Object[][] invalidConstructorArgs() {
        return new Object[][]{
                {false, null, null},
                {false, null, new Logger[0]},
                {false, new Writer[0], null}
        };
    }

    @Test(dataProvider = "invalidConstructorArgs",
            expectedExceptions = IllegalArgumentException.class)
    public void throwIaeOnInvalidConstructor(boolean dumpToStderr, Writer[] files, Logger[] loggers) {
        new DumpWriter(dumpToStderr, files, loggers);
    }

    @DataProvider
    public Object[][] validConstructorArgs() {
        return new Object[][]{
                {false, new Writer[0], new Logger[0]},
                {false, new Writer[5], new Logger[0]},
                {false, new Writer[0], new Logger[5]},
                {false, new Writer[5], new Logger[5]}
        };
    }

    @Test(dataProvider = "validConstructorArgs")
    public void allowNullWriters(boolean dumpToStderr, Writer[] files, Logger[] loggers) {
        DumpWriter dw = new DumpWriter(dumpToStderr, files, loggers);
        dw.dumpObject(this);
    }

    @DataProvider
    public Object[][] prettyPrintable() {
        return new Object[][]{
                {null, true},
                {1, true},
                {new Object[]{this}, true},
                {new Throwable(), true},
                {new byte[]{1}, true},
                {new short[]{1}, true},
                {new int[]{1}, true},
                {new long[]{1}, true},
                {new char[]{1}, true},
                {new float[]{1}, true},
                {new double[]{1}, true},
                {new boolean[]{true}, true},
                {new Object(), false}
        };
    }

    private final Matcher<CharSequence> pretty = not(matchesRegex("[a-zA-Z0-9.]+@[0-9a-f]+"));

    @Test(dataProvider = "prettyPrintable")
    public void testPrettyPrinting(Object toDump, boolean shouldPrettyPrint) throws IOException {
        assertThat(DumpWriter.fancyToString(toDump), shouldPrettyPrint ? pretty : not(pretty));
    }

    @Test
    public void testToStringFailure() {
        DumpWriter.fancyToString(new StupidObject());
    }

    private static class StupidObject {
        public String toString() {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void testWriteFailure() {
        DumpWriter dw = new DumpWriter(false, new Writer[]{new StupidWriter()}, new Logger[]{new StupidLogger()});
        dw.dumpObject(this);
        dw.dumpObject(this);
        dw.dumpObject(this);
    }

    private static class StupidWriter extends Writer {
        public void write(char[] cbuf, int off, int len) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void flush() throws IOException {
            throw new UnsupportedOperationException();
        }

        public void close() throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    private static class StupidLogger extends Logger {
        protected StupidLogger() {
            super("stupid", null);
        }

        @Override
        public void log(LogRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void log(Level level, String msg) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void log(Level level, String msg, Object param1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void log(Level level, String msg, Object[] params) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void log(Level level, String msg, Throwable thrown) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void logp(Level level, String sourceClass, String sourceMethod, String msg) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object param1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object[] params) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void logp(Level level, String sourceClass, String sourceMethod, String msg, Throwable thrown) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object param1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object[] params) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Throwable thrown) {
            throw new UnsupportedOperationException();
        }
    }
}