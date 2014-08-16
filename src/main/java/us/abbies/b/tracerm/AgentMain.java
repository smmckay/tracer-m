package us.abbies.b.tracerm;

import java.lang.instrument.Instrumentation;
import java.util.logging.Logger;

public class AgentMain {
    private static final Logger logger = Logger.getLogger(AgentMain.class.getName());

    public static DumpWriter out;

    public static void premain(String agentArgs, Instrumentation inst) {
        init(agentArgs, inst);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void agentmain(String agentArgs, Instrumentation inst) {
        init(agentArgs, inst);
    }

    private static void init(String agentArgs, Instrumentation inst) {
    }
}
