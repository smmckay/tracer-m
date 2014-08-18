package us.abbies.b.tracerm;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "tracer")
public class Config {
    public static class Instrument {
        @XmlAttribute(name = "targetClass", required = true)
        private String targetClass;

        @XmlAttribute(name = "targetMethod", required = true)
        private String targetMethod;

        @XmlElement(name = "transformer", required = true)
        private List<String> transformers;


        public String getTargetClass() {
            return targetClass;
        }


        public String getTargetMethod() {
            return targetMethod;
        }

        public List<String> getTransformers() {
            return transformers;
        }
    }

    @XmlElement(name = "instrument", required = true)
    private List<Instrument> instruments;

    public List<Instrument> getInstruments() {
        return instruments;
    }
}

