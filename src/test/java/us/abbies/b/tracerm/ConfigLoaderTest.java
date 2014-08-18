package us.abbies.b.tracerm;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ConfigLoaderTest {
    private static Config loadFromClasspath(String name) throws JAXBException, IOException, SAXException {
        return new ConfigLoader().fromClasspath("us/abbies/b/tracerm/" + name);
    }

    @Test
    public void testLoad() throws JAXBException, IOException, SAXException {
        Config c = loadFromClasspath("config1.xml");
        assertThat(c.getInstruments(), hasSize(1));
        assertThat(c.getInstruments().get(0).getTargetClass(), is("us.abbies.b.tracerm.testmains.Basic"));
        assertThat(c.getInstruments().get(0).getTargetMethod(), is("f()V"));
        assertThat(c.getInstruments().get(0).getTransformers(), hasSize(1));
        assertThat(c.getInstruments().get(0).getTransformers().get(0), is("DumpThis"));

        c = loadFromClasspath("config2.xml");
        assertThat(c.getInstruments(), hasSize(2));
        assertThat(c.getInstruments().get(0).getTargetClass(), is("us.abbies.b.tracerm.testmains.Basic"));
        assertThat(c.getInstruments().get(0).getTargetMethod(), is("f()V"));
        assertThat(c.getInstruments().get(0).getTransformers(), hasSize(1));
        assertThat(c.getInstruments().get(0).getTransformers().get(0), is("DumpThis"));
        assertThat(c.getInstruments().get(1).getTargetClass(), is("us.abbies.b.tracerm.testmains.Complex"));
        assertThat(c.getInstruments().get(1).getTargetMethod(), is("f()V"));
        assertThat(c.getInstruments().get(1).getTransformers(), hasSize(1));
        assertThat(c.getInstruments().get(1).getTransformers().get(0), is("DumpThis"));
    }

    @Test
    public void testLoadFromString() throws JAXBException, IOException, SAXException {
        Config c = new ConfigLoader().fromString("<tracer><instrument targetClass=\"us.abbies.b.tracerm.testmains.Basic\" targetMethod=\"f()V\"><transformer>us.abbies.b.tracerm.transformers.DumpThis</transformer></instrument></tracer>");
        assertThat(c.getInstruments(), hasSize(1));
        assertThat(c.getInstruments().get(0).getTargetClass(), is("us.abbies.b.tracerm.testmains.Basic"));
        assertThat(c.getInstruments().get(0).getTargetMethod(), is("f()V"));
    }

    @DataProvider
    public Object[][] failureMessages() {
        return new Object[][] {
                {"config3.xml"},
                {"config4.xml"},
                {"config5.xml"},
                {"config6.xml"},
                {"config7.xml"},
                {"config8.xml"},
                {"config-missing.xml"}
        };
    }

    @Test(dataProvider = "failureMessages", expectedExceptions = ConfigException.class)
    public void testInvalid(String configName) throws JAXBException, IOException, SAXException {
        loadFromClasspath(configName);
    }
}
