/* Copyright 2010,2014 Bank Of Italy
*
* Licensed under the EUPL, Version 1.1 or - as soon they
* will be approved by the European Commission - subsequent
* versions of the EUPL (the "Licence");
* You may not use this work except in compliance with the
* Licence.
* You may obtain a copy of the Licence at:
*
*
* http://ec.europa.eu/idabc/eupl
*
* Unless required by applicable law or agreed to in
* writing, software distributed under the Licence is
* distributed on an "AS IS" basis,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied.
* See the Licence for the specific language governing
* permissions and limitations under the Licence.
*/
package it.bancaditalia.oss.sdmx.parser.v21;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale.LanguageRange;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import it.bancaditalia.oss.sdmx.api.*;
import it.bancaditalia.oss.sdmx.client.Parser;
import it.bancaditalia.oss.sdmx.exceptions.SdmxException;
import it.bancaditalia.oss.sdmx.exceptions.SdmxXmlContentException;
import it.bancaditalia.oss.sdmx.util.Configuration;
import it.bancaditalia.oss.sdmx.util.LocalizedText;

/**
 * @author Attilio Mattiocco
 *
 */
public class DataStructureParser implements Parser<List<DataFlowStructure>>
{
	private static final String	sourceClass				= DataStructureParser.class.getSimpleName();
	protected static Logger		logger					= Configuration.getSdmxLogger();

	static final String			DATASTRUCTURE			= "DataStructure";

	static final String			CODELISTS				= "Codelists";
	static final String			CODELIST				= "Codelist";
	static final String			CODE					= "Code";

	static final String			CONCEPTS				= "Concepts";
	static final String			CONCEPT					= "Concept";

	static final String			NAME					= "Name";

	static final String			DIMENSIONLIST			= "DimensionList";
	static final String			GROUP					= "Group";
	static final String			ATTRIBUTELIST			= "AttributeList";
	static final String			MEASURELIST				= "MeasureList";

	static final String			DIMENSION				= "Dimension";
	static final String			ATTRIBUTE				= "Attribute";
	static final String			TIMEDIMENSION			= "TimeDimension";
	static final String			PRIMARYMEASURE			= "PrimaryMeasure";

	static final String			POSITION				= "position";
	static final String			ID						= "id";
	static final String			AGENCYID				= "agencyID";
	static final String			VERSION					= "version";

	static final String			LOCAL_REPRESENTATION	= "LocalRepresentation";
	static final String			REF						= "Ref";
	
	@Override
	public List<DataFlowStructure> parse(XMLEventReader eventReader, List<LanguageRange> languages)
			throws XMLStreamException, SdmxException
	{
		final String sourceMethod = "parse";
		logger.entering(sourceClass, sourceMethod);

		List<DataFlowStructure> result = new ArrayList<>();
		Map<String, Codelist> codelists = null;
		Map<String, ConceptScheme> concepts = null;
		DataFlowStructure currentStructure = null;

		LocalizedText currentName = new LocalizedText(languages);
		while (eventReader.hasNext())
		{	
			XMLEvent event = eventReader.nextEvent();
			logger.finest(event.toString());

			if (event.isStartElement())
			{
				StartElement startElement = event.asStartElement();

				switch (startElement.getName().getLocalPart())
				{
					case DATASTRUCTURE: {
						logger.finer("Got data structure.");
						currentName = new LocalizedText(languages);
						String id = null, agency = null, version = null;
						for (final Attribute attr: (Iterable<Attribute>) startElement::getAttributes)
							switch (attr.getName().toString())
							{
								case ID: id = attr.getValue(); break;
								case AGENCYID: agency = attr.getValue(); break;
								case VERSION: version = attr.getValue(); break;
							}

						currentStructure = new DataFlowStructure(id, agency, version);
						break;
					}
					case NAME: currentName.setText(startElement, eventReader); break;
					case CODELISTS: codelists = getCodelists(eventReader, languages); break;
					case CONCEPTS: concepts = ConceptSchemeParser.parse(eventReader, languages); break;
					case DIMENSIONLIST: setStructureDimensions(currentStructure, eventReader, codelists, concepts); break;
					case GROUP: setStructureGroups(currentStructure, eventReader); break;
					case ATTRIBUTELIST: setStructureAttributes(currentStructure, eventReader, codelists, concepts); break;
					case MEASURELIST: setStructureMeasures(currentStructure, eventReader); break;
				}
			}

			if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(DATASTRUCTURE))
			{
				if (currentStructure != null) {
					logger.finer("Adding data structure. " + currentStructure);
					currentStructure.setName(currentName.getText());
					result.add(currentStructure);
				}
			}
		}

		logger.exiting(sourceClass, sourceMethod);
		return result;
	}

	private static void setStructureAttributes(DataFlowStructure currentStructure, XMLEventReader eventReader,
			Map<String, Codelist> codelists, Map<String, ConceptScheme> concepts) throws XMLStreamException
	{
		final String sourceMethod = "setStructureAttributes";
		logger.entering(sourceClass, sourceMethod);
		SdmxAttribute currentAttribute = null;
		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();
			logger.finest(event.toString());
			if (event.isStartElement()) {
				final StartElement startElement = event.asStartElement();
				final String startNameLocalPart = startElement.getName().getLocalPart();
                switch (startNameLocalPart) {
                    case ATTRIBUTE:
                        logger.finer("Got attribute");

                        String id = null;
                        for (Attribute attribute : (Iterable<Attribute>) startElement::getAttributes)
                            if (attribute.getName().toString().equals(ID))
                                id = attribute.getValue();

                        if (id != null && !id.isEmpty())
                            currentAttribute = new SdmxAttribute(id);
                        else
                            throw new RuntimeException("Error during Structure Parsing. Invalid attribute id: " + id);
                        break;
                    case LOCAL_REPRESENTATION:
                        logger.finer("Got codelist");
                        SDMXReference coordinates = getRefCoordinates(eventReader);
                        // now set codes
                        if (codelists != null && currentAttribute != null && coordinates != null) {
                            if (codelists.containsKey(coordinates.getFullIdentifier())) {
                                currentAttribute.setCodeList(codelists.get(coordinates.getFullIdentifier()));
                            } else {
                                // Store only the codelist ref to retrieve the codes later
                                currentAttribute.setCodeList(new Codelist(coordinates, null, null));
                            }
                        }
                        break;
                    case "ConceptIdentity":
                        logger.finer("Got concept identity");
                        if (concepts != null && currentAttribute != null) {
							setConceptToMeta(currentAttribute, codelists, concepts, eventReader);
						}
                        break;
                }
			}

			if (event.isEndElement()) {
				final String endNameLocalPart = event.asEndElement().getName().getLocalPart();
				if (endNameLocalPart.equals(ATTRIBUTE)) {
					if (currentStructure != null && currentAttribute != null) {
						logger.finer("Adding attribute: " + currentAttribute);
						currentStructure.setAttribute(currentAttribute);
					} else {
						throw new RuntimeException("Error during Structure Parsing. Null current structure or dimension.");
					}
				}
				else if (endNameLocalPart.equals(ATTRIBUTELIST)) {
					break;
				}
			}
		}
		logger.exiting(sourceClass, sourceMethod);
	}

	private static void setStructureDimensions(DataFlowStructure currentStructure, XMLEventReader eventReader,
			Map<String, Codelist> codelists, Map<String, ConceptScheme> concepts)
			throws XMLStreamException, SdmxXmlContentException
	{
		final String sourceMethod = "setStructureDimensions";
		logger.entering(sourceClass, sourceMethod);

		Dimension currentDimension = null;
		int position = 0;

		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();
			logger.finest(event.toString());
			if (event.isStartElement()) {
				final StartElement startElement = event.asStartElement();
				final String startNameLocalPart = startElement.getName().getLocalPart();
                switch (startNameLocalPart) {
                    case DIMENSION: {
                        logger.finer("Got dimension");

                        String id = null;
                        for (final Attribute attribute : (Iterable<Attribute>) startElement::getAttributes) {
                            if (attribute.getName().toString().equals(ID)) {
                                id = attribute.getValue();
                            }
                        }

                        if (id != null && !id.isEmpty())
                            /*
                             * Uhm, we could rely on the position attribute but it seems not a good idea:
                             * From the SDMX2.1 specs: "The position attribute specifies the position of the
                             * dimension in the data structure definition. It is optional an the position of
                             * the dimension in the key descriptor (DimensionList element) always takes
                             * precedence over the value supplied here. This is strictly for informational
                             * purposes only."
                             *
                             * So we rely on the position in the key descriptor.
                             */
                            currentDimension = new Dimension(id, ++position);
                            // see above for the position
                            // if (attribute.getName().toString().equals(POSITION)) {
                            //     position=Integer.parseInt(attribute.getValue());
                            // }
                        else
                            throw new RuntimeException("Error during Structure Parsing. Invalid dimension id: " + id);
                        break;
                    }
                    case TIMEDIMENSION: {
                        logger.finer("Got time dimension");
                        currentDimension = null;
                        String id = null;
                        for (final Attribute attribute : (Iterable<Attribute>) startElement::getAttributes) {
                            if (attribute.getName().toString().equals(ID))
                                id = attribute.getValue();
                        }
                        if (id != null && !id.isEmpty()) {
                            if (currentStructure != null) {
                                logger.finer("Adding time dimension: " + id);
                                currentStructure.setTimeDimension(id);
                            } else
                                throw new RuntimeException("Error during Structure Parsing. Null current Structure.");
                        } else
                            throw new RuntimeException("Error during Structure Parsing. Invalid time dimension: " + id);
                        continue;
                    }
                    case LOCAL_REPRESENTATION:
                        logger.finer("Got codelist");
                        SDMXReference coordinates = getRefCoordinates(eventReader);
						if (coordinates != null) {
							final String coordinatesFullId = coordinates.getFullIdentifier();
							// now set codes
							if (currentDimension != null) {
								Codelist codelist = codelists != null ? codelists.get(coordinatesFullId) : null;
								if (codelists != null && codelists.containsKey(coordinatesFullId))
									currentDimension.setCodeList(codelist);
								else
									// Store only the codelist ref to retrieve the codes later
									currentDimension.setCodeList(new Codelist(coordinates, null, null));
							}
						}
                        break;
                    case "ConceptIdentity":
                        logger.finer("Got concept identity");
                        if (concepts != null && currentDimension != null) {
							setConceptToMeta(currentDimension, codelists, concepts, eventReader);
						}
                        break;
                }
			}

			if (event.isEndElement()) {
				final String endNameLocalPart = event.asEndElement().getName().getLocalPart();

				if (endNameLocalPart.equals(DIMENSION)) {
					if (currentStructure != null && currentDimension != null) {
						logger.finer("Adding dimension: " + currentDimension);
						currentStructure.setDimension(currentDimension);
					}
					else {
						throw new RuntimeException("Error during Structure Parsing. Null current structure or dimension.");
					}
				}
				else if (endNameLocalPart.equals(DIMENSIONLIST)) {
					break;
				}
			}
		}
		logger.exiting(sourceClass, sourceMethod);
	}

	private static SDMXReference getRefCoordinates(XMLEventReader eventReader) throws XMLStreamException
	{
		final String sourceMethod = "getRefCoordinates";
		logger.entering(sourceClass, sourceMethod);
		SDMXReference coords = null;
		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();
			logger.finest(event.toString());
			if (event.isStartElement()) {
				StartElement startElement = event.asStartElement();
				if (startElement.getName().getLocalPart().equals(REF)) {
					logger.finer("Got <Ref>");
					String id = null, agency = null, version = null;
					for (Attribute attr: (Iterable<Attribute>) startElement::getAttributes)
						switch (attr.getName().toString()) {
							case ID: id = attr.getValue(); break;
							case AGENCYID: agency = attr.getValue(); break;
							case VERSION: version = attr.getValue(); break;
						}

					if (id != null && !id.isEmpty()) {
						coords = new SDMXReference(id, agency, version);
						logger.finer("Found coordinates: " + coords.getFullIdentifier());
					} else {
						throw new RuntimeException("Error during Structure Parsing. Invalid time dimension: " + id);
					}
				}
			}
			else if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(LOCAL_REPRESENTATION))
				break;
		}
		
		logger.exiting(sourceClass, sourceMethod);
		return coords;
	}

	private static void setStructureGroups(DataFlowStructure currentStructure, XMLEventReader eventReader)
			throws XMLStreamException
	{
		final String sourceMethod = "setStructureGroups";
		logger.entering(sourceClass, sourceMethod);
		while (eventReader.hasNext())
		{
			// TODO skip for now

			XMLEvent event = eventReader.nextEvent();
			logger.finest(event.toString());
			if (event.isEndElement())
			{
				if (event.asEndElement().getName().getLocalPart().equals(GROUP))
				{
					break;
				}
			}
		}
		logger.exiting(sourceClass, sourceMethod);
	}

	private static void setStructureMeasures(DataFlowStructure currentStructure, XMLEventReader eventReader)
			throws XMLStreamException
	{
		final String sourceMethod = "setStructureMeasures";
		logger.entering(sourceClass, sourceMethod);
		while (eventReader.hasNext())
		{
			XMLEvent event = eventReader.nextEvent();
			logger.finest(event.toString());

			if (event.isStartElement())
			{
				StartElement startElement = event.asStartElement();
				if (startElement.getName().getLocalPart().equals((PRIMARYMEASURE)))
				{
					logger.finer("Got primary measure");
					@SuppressWarnings("unchecked")
					String id = null;
					for (final Attribute attribute : (Iterable<Attribute>)startElement::getAttributes)
					{
						if (attribute.getName().toString().equals(ID))
						{
							id = attribute.getValue();
						}
					}
					if (id != null && !id.isEmpty())
					{
						if (currentStructure != null)
						{
							logger.finer("Adding primary measure: " + id);
							currentStructure.setMeasure(id);
						}
						else
						{
							throw new RuntimeException("Error during Structure Parsing. Null current Structure.");
						}
					}
					else
					{
						throw new RuntimeException("Error during Structure Parsing. Invalid primary measure: " + id);
					}
					continue;
				}
			}

			if (event.isEndElement())
			{
				if (event.asEndElement().getName().getLocalPart().equals(MEASURELIST))
				{
					break;
				}
			}
		}
		logger.exiting(sourceClass, sourceMethod);
	}

	private static Map<String, Codelist> getCodelists(XMLEventReader eventReader, List<LanguageRange> languages)
			throws XMLStreamException, SdmxException
	{
		Map<String, Codelist> codelists = new HashMap<>();
		while (eventReader.hasNext())
		{
			XMLEvent event = eventReader.nextEvent();
			logger.finest(event.toString());
			if (event.isStartElement())
			{
				StartElement startElement = event.asStartElement();

				if (startElement.getName().getLocalPart().equals(CODELIST))
				{
					@SuppressWarnings("unchecked")
					String id = null;
					String agency = null;
					String version = null;
					for (final Attribute attr : (Iterable<Attribute>) startElement::getAttributes) {
						switch (attr.getName().toString()) {
							case ID: id = attr.getValue(); break;
							case AGENCYID: agency = attr.getValue(); break;
							case VERSION: version = attr.getValue(); break;
						}
					}
					logger.finer("Got codelist: " + id);
					Codelist codes = CodelistParser.getCodes(new SDMXReference(id, agency, version), eventReader, languages);
					codelists.put(codes.getFullIdentifier(), codes);
				}
			}
			if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(CODELISTS)) {
				break;
			}
		}

		return codelists;
	}

	private static Map<String, String> getConcepts(XMLEventReader eventReader, List<LanguageRange> languages)
			throws XMLStreamException, SdmxException
	{
		Map<String, String> concepts = new HashMap<>();
		String agency = "";
		String version = "";
		while (eventReader.hasNext())
		{
			XMLEvent event = eventReader.nextEvent();
			logger.finest(event.toString());
			if (event.isStartElement())
			{
				StartElement startElement = event.asStartElement();

				switch (startElement.getName().getLocalPart()) {
					case "ConceptScheme": {
						for (final Attribute attr : (Iterable<Attribute>) startElement::getAttributes) {
							switch (attr.getName().toString()) {
								case AGENCYID: agency = attr.getValue(); break;
								case VERSION: version = attr.getValue(); break;
							}
						}
						break;
					}

					case CONCEPT: {
						String id = null;
						String conceptName = "";
						for (final Attribute attr : (Iterable<Attribute>) startElement::getAttributes) {
							if (attr.getName().toString().equals(ID)) {
								id = attr.getValue();
							}
						}
						conceptName = agency + "/" + id + "/" + version;
						logger.finer("Got concept: " + conceptName);
						concepts.put(conceptName, getConceptName(eventReader, languages));
						break;
					}
				}
			}
			if (event.isEndElement())
			{
				if (event.asEndElement().getName().getLocalPart().equals(CONCEPTS))
				{
					break;
				}
			}
		}
		return concepts;
	}

	private static String getConceptName(XMLEventReader eventReader, List<LanguageRange> languages)
			throws XMLStreamException, SdmxException
	{
		LocalizedText value = new LocalizedText(languages);
		while (eventReader.hasNext())
		{
			XMLEvent event = eventReader.nextEvent();
			logger.finest(event.toString());
			if (event.isStartElement())
			{
				StartElement startElement = event.asStartElement();
				if (startElement.getName().getLocalPart().equals("Name"))
				{
					value.setText(startElement, eventReader);
				}

			}

			if (event.isEndElement())
			{
				String eventName = event.asEndElement().getName().getLocalPart();
				if (eventName.equals(CONCEPT))
				{
					break;
				}
			}
		}
		return value.getText();
	}

	private static void setConceptToMeta(final SdmxMetaElement sdmxMetaElement,
										 final Map<String, Codelist> codelists,
										 final Map<String, ConceptScheme> concepts,
										 final XMLEventReader eventReader)
			throws XMLStreamException
	{
		Concept concept = null;
		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();
			logger.finest(event.toString());
			if (event.isStartElement()) {
				StartElement startElement = event.asStartElement();
				if (startElement.getName().getLocalPart().equals(REF)) {
					String id = null;
					String maintainableParentID = null;
					String maintainableParentVersion = null;
					String agency = null;
					for(final Attribute attribute : (Iterable<Attribute>) startElement::getAttributes) {
						switch (attribute.getName().toString()) {
							case ID: id = attribute.getValue(); break;
							case AGENCYID: agency = attribute.getValue(); break;
							case "maintainableParentID": maintainableParentID = attribute.getValue(); break;
							case "maintainableParentVersion": maintainableParentVersion = attribute.getValue(); break;
						}
					}
					final String maintainableId = SDMXReference.getFullIdentifier(maintainableParentID, agency, maintainableParentVersion);
					final ConceptScheme conceptScheme = concepts.get(maintainableId);
					concept = conceptScheme != null ? conceptScheme.get(id) : null;
				}
			}

			if (event.isEndElement()) {
				String eventName = event.asEndElement().getName().getLocalPart();
				if (eventName.equals("ConceptIdentity")) {
					break;
				}
			}
		}

		if (sdmxMetaElement != null && concept != null) {
			sdmxMetaElement.setName(concept.getName());
			if (codelists != null) {
				final Codelist codelist = codelists.get(concept.getCoreRepresentationFullId());
				if (codelist != null) {
					sdmxMetaElement.setCodeList(codelist);
				}
			}
		}
	}
}
