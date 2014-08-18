package us.abbies.b.tracerm;

import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigLoader {
    @FunctionalInterface
    public interface XMLEventReaderSupplier {
        public XMLEventReader get() throws XMLStreamException;
    }

    private final Unmarshaller u;

    public ConfigLoader() throws JAXBException, IOException, SAXException {
        JAXBContext c = JAXBContext.newInstance(Config.class);

        List<DOMResult> results = new ArrayList<>();
        c.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String ns, String file) throws IOException {
                DOMResult result = new DOMResult();
                result.setSystemId(file);
                results.add(result);
                return result;
            }
        });

        List<DOMSource> sources = results.stream()
                .map(result -> new DOMSource(result.getNode()))
                .collect(Collectors.toList());
        SchemaFactory sFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        Schema schema = sFactory.newSchema(sources.toArray(new DOMSource[sources.size()]));

        u = c.createUnmarshaller();
        u.setSchema(schema);
    }


    public Config fromString(String configStr) {
        return fromSupplier(() -> {
            Reader r = new StringReader(configStr);
            return XMLInputFactory.newFactory().createXMLEventReader(r);
        });
    }

    public Config fromClasspath(String name) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        if (in == null) {
            throw new ConfigException("Unable to open classpath:" + name);
        }
        return fromInputStream(in);
    }

    public Config fromInputStream(InputStream in) {
        return fromSupplier(() -> XMLInputFactory.newFactory().createXMLEventReader(in));
    }

    private Config fromSupplier(XMLEventReaderSupplier s) {
        try {
            XMLEventReader er = s.get();
            try {
                return (Config) u.unmarshal(er);
            } finally {
                er.close();
            }
        } catch (Exception e) {
            throw new ConfigException("Invalid XML", e);
        }
    }
}
