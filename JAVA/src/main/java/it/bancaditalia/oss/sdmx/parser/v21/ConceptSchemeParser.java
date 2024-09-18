package it.bancaditalia.oss.sdmx.parser.v21;

import it.bancaditalia.oss.sdmx.api.Codelist;
import it.bancaditalia.oss.sdmx.api.Concept;
import it.bancaditalia.oss.sdmx.api.ConceptScheme;
import it.bancaditalia.oss.sdmx.api.SDMXReference;
import it.bancaditalia.oss.sdmx.client.Parser;
import it.bancaditalia.oss.sdmx.exceptions.SdmxException;
import it.bancaditalia.oss.sdmx.util.Configuration;
import it.bancaditalia.oss.sdmx.util.LocalizedText;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.*;
import java.util.logging.Logger;

import static it.bancaditalia.oss.sdmx.parser.v20.DataStructureParser.*;
import static it.bancaditalia.oss.sdmx.parser.v21.DataStructureParser.VERSION;

public class ConceptSchemeParser {
    private static final String sourceClass = CodelistParser.class.getSimpleName();
    protected static Logger logger = Configuration.getSdmxLogger();

    public static SDMXReference parseReference(final StartElement startElement) {
        String id = null;
        String agency = null;
        String version = null;
        for (final Attribute attr : (Iterable<Attribute>)startElement::getAttributes) {
            System.out.println(attr);
            switch (attr.getName().toString()) {
                case ID:
                    id = attr.getValue();
                    break;
                case AGENCYID:
                    agency = attr.getValue();
                    break;
                case VERSION:
                    version = attr.getValue();
                    break;
            }
        }

        return new SDMXReference(id, agency, version);
    }

    public static Map<String, ConceptScheme> parse(XMLEventReader eventReader, List<Locale.LanguageRange> languages)
            throws XMLStreamException, SdmxException {

        Map<String, ConceptScheme> concepts = new HashMap<>();
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            logger.finest(event.toString());
            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();

                if (startElement.getName().getLocalPart().equals(CONCEPTSCHEME)) {
                    ConceptScheme conceptScheme = parse(startElement, eventReader, languages);
                    concepts.put(conceptScheme.getFullIdentifier(), conceptScheme);
                }
            }
            if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(CONCEPTS)) {
                break;
            }
        }

        return concepts;
    }

    public static ConceptScheme parse(StartElement conceptSchemeStartElement,
                                      XMLEventReader eventReader,
                                      List<Locale.LanguageRange> languages)
            throws XMLStreamException, SdmxException {

        final SDMXReference reference = parseReference(conceptSchemeStartElement);
        logger.finer("Got " + CONCEPTSCHEME + ": " + reference.getId());

        final LocalizedText name = new LocalizedText(languages);
        final LocalizedText description = new LocalizedText(languages);
        final Map<String, Concept> concepts = new HashMap<>();
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            logger.finest(event.toString());
            if (event.isStartElement()) {
                final StartElement startElement = event.asStartElement();

                switch (startElement.getName().getLocalPart()) {
                    case NAME:
                        name.setText(startElement, eventReader);
                        break;
                    case DESCRIPTION:
                        description.setText(startElement, eventReader);
                        break;
                    case CONCEPT: {
                        final Concept concept = ConceptParser.parse(startElement, eventReader, languages);
                        concepts.put(concept.getFullIdentifier(), concept);
                        break;
                    }
                }
            }
            if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(CONCEPTSCHEME)) {
                break;
            }
        }

        return new ConceptScheme(reference, name, description, concepts);
    }
}
