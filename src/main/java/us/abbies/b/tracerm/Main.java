package us.abbies.b.tracerm;

import us.abbies.b.tracerm.jaxb.Tracer;

import javax.xml.bind.JAXBContext;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Main {
    public static void main(String[] args) {
        int pid = 0;
        String def = null;

        Iterator<String> argIt = Arrays.asList(args).iterator();
        while (argIt.hasNext()) {
            String arg = argIt.next();
            try {
                switch (arg) {
                case "--help":
                    printHelp();
                    System.exit(0);
                case "--pid":
                    String v = argIt.next();
                    try {
                        pid = Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        printErr("Unrecognized pid: " + v, true);
                    }
                    break;
                case "--def":
                    String xmlPath = argIt.next();
                    def = parseXmlDef(xmlPath);
                    break;
                default:
                    printErr("Unrecognized option " + arg, true);
                }
            } catch (NoSuchElementException e) {
                printErr(arg + "requires an argument", true);
            }
        }

        if (pid == 0) {
            printErr("missing --pid", true);
        }

        if (def == null) {
            printErr("missing tracer definition", true);
        }

        Path toolsJarPath = Paths.get(System.getProperty("java.home"), "..", "lib", "tools.jar")
                .toAbsolutePath();
        checkPath(toolsJarPath);

        Path agentPath;
        String agentPathStr = System.getProperty("tracer-m.agentPath");
        if (agentPathStr != null) {
            agentPath = Paths.get(agentPathStr).toAbsolutePath();
        } else {
            String cp = System.getProperty("java.class.path");
            if (cp.contains(System.getProperty("path.separator"))) {
                printErr("Classpath contains more than one jar, I don't know which to load in the target: " + cp);
            }
            agentPath = Paths.get(cp).toAbsolutePath();
        }
        checkPath(agentPath);

        URL[] urls = null;
        try {
            urls = new URL[]{new URL("file://" + toolsJarPath)};
        } catch (MalformedURLException e) {
            printErr("Please report a bug: " + e);
        }
        ClassLoader parent = Main.class.getClassLoader();
        try (URLClassLoader loader = new URLClassLoader(urls, parent)) {
            Class<?> vmClass = loader.loadClass("com.sun.tools.attach.VirtualMachine");
            Object vm = vmClass.getMethod("attach", String.class).invoke(null, String.valueOf(pid));
            try {
                vmClass.getMethod("loadAgent", String.class, String.class).invoke(vm, agentPath.toString(), def);
                vmClass.getMethod("detach").invoke(vm);
            } catch (InvocationTargetException e) {
                printErr("Unable to load jar " + agentPath + " into " + pid + ": " + e.getCause());
            } catch (Exception e) {
                printErr("Unable to load jar " + agentPath + " into " + pid + ": " + e);
            }
        } catch (IOException e) {
            printErr("Unable to open tools.jar: " + e);
        } catch (ClassNotFoundException e) {
            printErr("Unable to load com.sun.tools.attach.VirtualMachine. Make sure you're using OpenJDK/Oracle JDK.");
        } catch (NoSuchMethodException e) {
            printErr("Unable to find com.sun.tools.attach.VirtualMachine.attach(String). Make sure you're using OpenJDK/Oracle JDK.");
        } catch (InvocationTargetException e) {
            printErr("Attaching to " + pid + " failed: " + e.getCause());
        } catch (IllegalAccessException e) {
            printErr("com.sun.tools.attach.VirtualMachine.attach(String) isn't accessible. Make sure you're using OpenJDK/Oracle JDK.");
        }
    }

    private static void checkPath(Path filePath) {
        File file = filePath.toFile();
        if (!file.exists()) {
            printErr(file + " not found");
        }
        if (!file.isFile()) {
            printErr(file + " not a regular file");
        }
        if (!file.canRead()) {
            printErr("Unable to read " + file);
        }
    }

    private static void printErr(String msg) {
        printErr(msg, false);
    }

    private static void printErr(String msg, boolean printHelp) {
        System.err.println(msg);
        System.err.println();
        if (printHelp) {
            printHelp();
        }
        System.exit(1);
    }

    private static void printHelp() {
        throw new RuntimeException("Not implemented");
    }

    private static String parseXmlDef(String xmlPath) {
        try {
            JAXBContext context = JAXBContext.newInstance(Tracer.class);
            context.createUnmarshaller().unmarshal(new File(xmlPath));
            return new String(Files.readAllBytes(Paths.get(xmlPath)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            printErr("Unable to load " + xmlPath + ": " + e);
            throw new RuntimeException("BUG");
        }
    }
}
