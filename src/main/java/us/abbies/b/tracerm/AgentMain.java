package us.abbies.b.tracerm;

import us.abbies.b.tracerm.jaxb.Instrument;
import us.abbies.b.tracerm.jaxb.NamedOutput;
import us.abbies.b.tracerm.jaxb.Output;
import us.abbies.b.tracerm.jaxb.Tracer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AgentMain {
    private static final Logger logger = Logger.getLogger(AgentMain.class.getName());

    public static DumpWriter out;

    public static void premain(String agentArgs, Instrumentation inst) {
        init(agentArgs, inst, false);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void agentmain(String agentArgs, Instrumentation inst) {
        init(agentArgs, inst, true);
    }

    private static void init(String agentArgs, Instrumentation inst, boolean allowRetransform) {
        try {
            Tracer tracer = loadTracer(agentArgs);
            Output output = tracer.getOutput();
            out = makeDumpWriter(output);

            List<Class<?>> classesToRetransform = new ArrayList<>();
            for (Instrument i : tracer.getInstrument()) {
                ClassFileTransformer t = makeTransformer(i);
                inst.addTransformer(t, true);
                if (allowRetransform && i.isRetransform()) {
                    classesToRetransform.add(Class.forName(i.getClazz()));
                }
            }
            if (!classesToRetransform.isEmpty()) {
                inst.retransformClasses(classesToRetransform.toArray(new Class<?>[classesToRetransform.size()]));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "tracer-m agent init failed", e);
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    private static ClassFileTransformer makeTransformer(Instrument i) {
        String dmStr = i.getDumpMembers();
        Set<String> dumpMembers;
        if (dmStr == null || dmStr.isEmpty()) {
            dumpMembers = Collections.emptySet();
        } else {
            dumpMembers = new HashSet<>();
            for (String dm : dmStr.split(",")) {
                if (!dm.isEmpty()) {
                    dumpMembers.add(dm);
                }
            }
        }
        return new TraceTransformer(i.getClazz(), i.getMethod(), i.isDumpStack(), i.isDumpThis(), dumpMembers);
    }

    private static DumpWriter makeDumpWriter(Output output) throws FileNotFoundException {
        if (output == null) {
            return new DumpWriter(true, new Writer[0], new Logger[0]);
        }

        List<Writer> files = new ArrayList<>();
        List<Logger> loggers = new ArrayList<>();
        for (JAXBElement<NamedOutput> elem : output.getFileOrLogger()) {
            switch (elem.getName().getLocalPart()) {
            case "file":
                files.add(new OutputStreamWriter(
                        new FileOutputStream(elem.getValue().getName()),
                        StandardCharsets.UTF_8));
                break;
            case "logger":
                loggers.add(Logger.getLogger(elem.getValue().getName()));
                break;
            }
        }

        return new DumpWriter(output.isStderr(),
                files.toArray(new Writer[files.size()]),
                loggers.toArray(new Logger[loggers.size()])
        );
    }

    private static Tracer loadTracer(String agentArgs) {
        if (agentArgs == null || agentArgs.isEmpty()) {
            throw new IllegalArgumentException("empty options, missing tracer definition");
        }

        try {
            JAXBContext context = JAXBContext.newInstance(Tracer.class);
            return (Tracer) context.createUnmarshaller().unmarshal(new StringReader(agentArgs));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to load tracer definition", e);
        }
    }
}
