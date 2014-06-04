package us.abbies.b.tracerm;

import us.abbies.b.tracerm.jaxb.NamedOutput;
import us.abbies.b.tracerm.jaxb.Output;
import us.abbies.b.tracerm.jaxb.Tracer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import java.io.*;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AgentMain {
    public static DumpWriter out;

    public static void premain(String agentArgs, Instrumentation inst) {
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void agentmain(String agentArgs, Instrumentation inst) {
        Tracer tracer = loadTracer(agentArgs);
        Output output = tracer.getOutput();
        out = makeDumpWriter(output);
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
