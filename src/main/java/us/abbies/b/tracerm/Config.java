package us.abbies.b.tracerm;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Config {
    @FunctionalInterface
    public interface XMLEventReaderSupplier {
        public XMLEventReader get() throws XMLStreamException;
    }

    public static Config fromString(String configStr) {
        return fromSupplier(() -> {
                Reader r = new StringReader(configStr);
                return XMLInputFactory.newFactory().createXMLEventReader(r);
        });
    }

    public static Config fromClasspath(String name) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        if (in == null) {
            throw new ConfigException("Unable to open classpath:" + name);
        }
        return fromInputStream(in);
    }

    public static Config fromInputStream(InputStream in) {
        return fromSupplier(() -> XMLInputFactory.newFactory().createXMLEventReader(in));
    }

    public static Config fromSupplier(XMLEventReaderSupplier s) {
        try {
            XMLEventReader er = s.get();
            try {
                return loadConfig(er);
            } finally {
                er.close();
            }
        } catch (XMLStreamException e) {
            throw new ConfigException("Invalid XML", e);
        }
    }

    private static Config loadConfig(XMLEventReader er) throws XMLStreamException {
        XMLEvent tag = er.nextTag();
        String tagName = tag.asStartElement().getName().getLocalPart();
        if (!"tracer".equals(tagName)) {
            throw new ConfigException("Expected tracer element, got " + tagName);
        }

        List<Instrument> instruments = new ArrayList<>();
        while (true) {
            tag = er.nextTag();
            if (tag.isStartElement()) {
                StartElement se = tag.asStartElement();
                tagName = se.getName().getLocalPart();
                if ("instrument".equals(tagName)) {
                    instruments.add(loadInstrument(se));
                } else {
                    throw new ConfigException("Unknown element " + tagName);
                }
            } else if (tag.isEndElement()) {
                if ("tracer".equals(tag.asEndElement().getName().getLocalPart())) {
                    if (instruments.size() == 0) {
                        throw new ConfigException("Expected at least one instrument element");
                    }
                    return new Config(instruments);
                }
            }
        }
    }

    private static Instrument loadInstrument(StartElement se) {
        String targetClass = getAttr(se, "targetClass");
        String targetMethod = getAttr(se, "targetMethod");
        return new Instrument(targetClass, targetMethod);
    }

    private static String getAttr(StartElement se, String attrName) {
        Attribute attr = se.getAttributeByName(new QName(attrName));
        if (attr == null) {
            throw new ConfigException("Missing attribute " + attrName + " on element " + se.getName().getLocalPart());
        }
        return attr.getValue();
    }

    private final List<Instrument> instruments;

    public Config(List<Instrument> instruments) {
        this.instruments = Collections.unmodifiableList(instruments);
    }

    public static class Instrument {
        private final String targetClass;
        private final String targetMethod;

        public Instrument(String targetClass, String targetMethod) {
            this.targetClass = targetClass;
            this.targetMethod = targetMethod;
        }

        public String getTargetClass() {
            return targetClass;
        }

        public String getTargetMethod() {
            return targetMethod;
        }
    }

    public List<Instrument> getInstruments() {
        return instruments;
    }
}

