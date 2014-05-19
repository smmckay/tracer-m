package us.abbies.b.tracerm;

import us.abbies.b.tracerm.jaxb.Instrument;
import us.abbies.b.tracerm.jaxb.NamedOutput;
import us.abbies.b.tracerm.jaxb.Output;
import us.abbies.b.tracerm.jaxb.Tracer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
            printErr("Unable to find VirtualMachine.attach(String). Make sure you're using OpenJDK/Oracle JDK.");
        } catch (InvocationTargetException e) {
            printErr("Attaching to " + pid + " failed: " + e.getCause());
        } catch (IllegalAccessException e) {
            printErr("VirtualMachine.attach(String) isn't accessible. Make sure you're using OpenJDK/Oracle JDK.");
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

    }

    private static String parseXmlDef(String xmlPath) {
        StringBuilder defBuilder = new StringBuilder();
        try {
            JAXBContext context = JAXBContext.newInstance(Tracer.class);
            Tracer tracer = (Tracer) context.createUnmarshaller().unmarshal(new File(xmlPath));

            Output output = tracer.getOutput();
            if (output == null) {
                defBuilder.append("stderr\0");
            } else {
                if (output.isStderr()) {
                    defBuilder.append("stderr\0");
                }
                for (JAXBElement<NamedOutput> elem : output.getFileOrLogger()) {
                    defBuilder.append(elem.getName().getLocalPart())
                            .append('=')
                            .append(elem.getValue().getName())
                            .append('\0');
                }
            }
            defBuilder.append('\0');

            for (Instrument i : tracer.getInstrument()) {
                defBuilder.append(i.getClazz())
                        .append('\0')
                        .append(i.getMethod())
                        .append('\0');
                if (i.isDumpStack()) {
                    defBuilder.append("dumpStack\0");
                }
                if (i.isDumpThis()) {
                    defBuilder.append("dumpThis\0");
                }
                String dumpMembers = i.getDumpMembers();
                if (dumpMembers != null) {
                    defBuilder.append("dumpMembers=").append(dumpMembers).append('\0');
                }
                defBuilder.append('\0');
            }
        } catch (Exception e) {
            printErr("Unable to load " + xmlPath + ": " + e);
        }
        return defBuilder.toString();
    }
}
