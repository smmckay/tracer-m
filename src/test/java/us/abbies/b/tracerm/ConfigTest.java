package us.abbies.b.tracerm;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ConfigTest {
    private static Config loadFromClasspath(String name) {
        return Config.fromClasspath("us/abbies/b/tracerm/" + name);
    }

    @Test
    public void testLoad() {
        Config c = loadFromClasspath("config1.xml");
        assertThat(c.getInstruments(), hasSize(1));
        assertThat(c.getInstruments().get(0).getTargetClass(), is("us.abbies.b.tracerm.testmains.Basic"));

        c = loadFromClasspath("config2.xml");
        assertThat(c.getInstruments(), hasSize(2));
        assertThat(c.getInstruments().get(0).getTargetClass(), is("us.abbies.b.tracerm.testmains.Basic"));
        assertThat(c.getInstruments().get(1).getTargetClass(), is("us.abbies.b.tracerm.testmains.Complex"));
    }

    @Test
    public void testLoadFromString() {
        Config c = Config.fromString("<tracer><instrument targetClass=\"us.abbies.b.tracerm.testmains.Basic\"/></tracer>");
        assertThat(c.getInstruments(), hasSize(1));
        assertThat(c.getInstruments().get(0).getTargetClass(), is("us.abbies.b.tracerm.testmains.Basic"));
    }

    @DataProvider
    public Object[][] failureMessages() {
        return new Object[][] {
                {"config3.xml", "Missing attribute targetClass on element instrument"},
                {"config4.xml", "Expected tracer element, got html"},
                {"config5.xml", "Unknown element test"},
                {"config6.xml", "Expected at least one instrument element"},
                {"config-missing.xml", "Unable to open classpath:us/abbies/b/tracerm/config-missing.xml"}
        };
    }

    @Test(dataProvider = "failureMessages")
    public void testInvalid(String configName, String expectedMessage) {
        try {
            loadFromClasspath(configName);
            assertThat("Loading config " + configName + " succeeded", false);
        } catch (ConfigException e) {
            assertThat(e.getMessage(), equalTo(expectedMessage));
        }
    }
}
