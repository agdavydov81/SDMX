package it.bancaditalia.oss.sdmx.parser.v21;

import it.bancaditalia.oss.sdmx.api.Concept;
import it.bancaditalia.oss.sdmx.api.ConceptScheme;
import it.bancaditalia.oss.sdmx.api.SDMXReference;
import it.bancaditalia.oss.sdmx.client.Parser;
import it.bancaditalia.oss.sdmx.exceptions.SdmxException;
import it.bancaditalia.oss.sdmx.util.Configuration;
import it.bancaditalia.oss.sdmx.util.LocalizedText;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import static it.bancaditalia.oss.sdmx.parser.v20.DataStructureParser.*;

public class ConceptParser {
    private static final String sourceClass = CodelistParser.class.getSimpleName();
    protected static Logger logger = Configuration.getSdmxLogger();


    public static Concept parse(final StartElement conceptStartElement,
                                final XMLEventReader eventReader,
                                final List<Locale.LanguageRange> languages)
            throws XMLStreamException, SdmxException {

        final SDMXReference reference = ConceptSchemeParser.parseReference(conceptStartElement);
        logger.finer("Got " + CONCEPT + ": " + reference.getId());

        final LocalizedText name = new LocalizedText(languages);
        final LocalizedText description = new LocalizedText(languages);
        String coreRepresentation = null;

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
                    case COREREPRESENTATION:
                        coreRepresentation = parseCoreRepresentationFullId(startElement, eventReader, languages);
                        break;
                }
            }
            if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(CONCEPT)) {
                break;
            }
        }

        return new Concept(reference, name, description, coreRepresentation);
    }

    private static String parseCoreRepresentationFullId(final StartElement coreStartElement,
                                                        final XMLEventReader eventReader,
                                                        final List<Locale.LanguageRange> languages)
            throws XMLStreamException, SdmxException {

        SDMXReference ref = null;
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            logger.finest(event.toString());
            if (event.isStartElement()) {
                final StartElement startElement = event.asStartElement();

                if (startElement.getName().getLocalPart().equals(ENUMERATION)) {
                    ref = parseEnumeration(startElement, eventReader, languages);
                }
            }
            if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(COREREPRESENTATION)) {
                break;
            }
        }

        return ref != null ? ref.getFullIdentifier() : null;
    }

    private static SDMXReference parseEnumeration(final StartElement coreStartElement,
                                                  final XMLEventReader eventReader,
                                                  final List<Locale.LanguageRange> languages)
            throws XMLStreamException, SdmxException {

        SDMXReference ref = null;
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();
            logger.finest(event.toString());
            if (event.isStartElement()) {
                final StartElement startElement = event.asStartElement();

                if (startElement.getName().getLocalPart().equals(REF)) {
                    ref = ConceptSchemeParser.parseReference(startElement);
                }
            }
            if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(ENUMERATION)) {
                break;
            }
        }

        return ref;
    }
}
