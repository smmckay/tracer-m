package us.abbies.b.tracerm;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TraceTransformerIT {
    @DataProvider
    public Object[][] agentArgs() {
        return new Object[][] {
                {"<tracer><instrument class=\"us.abbies.b.tracerm.testmains.Basic\" method=\"f()V\"><dump><this/></dump></instrument></tracer>",
                        "us.abbies.b.tracerm.testmains.Basic",
                        "woo" + System.lineSeparator()},
        };
    }

    @Test(dataProvider = "agentArgs")
    public void testAgent(String xml, String mainClass, String expectedOutput) throws Exception {
        String agentJarPath = System.getProperty("agentJarPath");
        ProcessBuilder pb = new ProcessBuilder(
                Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                "-classpath",
                System.getProperty("java.class.path"),
                String.format("-javaagent:%s=%s", agentJarPath, xml),
                mainClass
        );
        Process p = pb.start();

        StringBuilder actualOutput = new StringBuilder();
        try (Reader stderr = new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8)) {
            for (int ch = stderr.read(); ch != -1; ch = stderr.read()) {
                actualOutput.appendCodePoint(ch);
            }
        }

        assertThat(actualOutput.toString(), equalTo(expectedOutput));
        assertThat(p.waitFor(), equalTo(0));
    }
}
