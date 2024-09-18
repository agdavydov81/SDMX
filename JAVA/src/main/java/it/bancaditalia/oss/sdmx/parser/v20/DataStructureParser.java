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
package it.bancaditalia.oss.sdmx.parser.v20;

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

import it.bancaditalia.oss.sdmx.api.Codelist;
import it.bancaditalia.oss.sdmx.api.DataFlowStructure;
import it.bancaditalia.oss.sdmx.api.Dimension;
import it.bancaditalia.oss.sdmx.api.SDMXReference;
import it.bancaditalia.oss.sdmx.api.SdmxAttribute;
import it.bancaditalia.oss.sdmx.api.SdmxMetaElement;
import it.bancaditalia.oss.sdmx.client.Parser;
import it.bancaditalia.oss.sdmx.exceptions.SdmxException;
import it.bancaditalia.oss.sdmx.parser.v21.CodelistParser;
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

	public static final String			DATASTRUCTURE			= "KeyFamily";

	public static final String			CODELISTS				= "CodeLists";
	public static final String			CODELIST				= "CodeList";
	public static final String			CODELISTAGENCY			= "codelistAgency";
	public static final String 		CODE 					= "Code";
	public static final String 		CODE_ID 				= "value";
	public static final String 		CODE_DESCRIPTION 		= "Description";

	public static final String			CONCEPTS				= "Concepts";
	public static final String			CONCEPTSCHEME			= "ConceptScheme";
	public static final String			CONCEPT					= "Concept";

	public static final String			COMPONENTS				= "Components";
	public static final String			NAME					= "Name";
	public static final String 			DESCRIPTION 			= "Description";
	public static final String			COREREPRESENTATION		= "CoreRepresentation";
	public static final String 			ENUMERATION				= "Enumeration";

	public static final String			DIMENSION				= "Dimension";
	public static final String			ATTRIBUTE				= "Attribute";
	public static final String			TIMEDIMENSION			= "TimeDimension";
	public static final String			PRIMARYMEASURE			= "PrimaryMeasure";
	public static final String			CONCEPT_REF				= "conceptRef";

	public static final String			ID						= "id";
	public static final String			AGENCYID				= "agencyID";

	public static final String			LOCAL_REPRESENTATION	= "LocalRepresentation";
	public static final String			REF						= "Ref";

	@Override
	public List<DataFlowStructure> parse(XMLEventReader eventReader, List<LanguageRange> languages)
			throws XMLStreamException, SdmxException
	{
		final String sourceMethod = "parse";
		logger.entering(sourceClass, sourceMethod);

		List<DataFlowStructure> result = new ArrayList<>();
		Map<String, Codelist> codelists = null;
		Map<String, String> concepts = null;
		DataFlowStructure currentStructure = null;

		LocalizedText currentName = new LocalizedText(languages);
		while (eventReader.hasNext())
		{
			XMLEvent event = eventReader.nextEvent();
			logger.finest(event.toString());

			if (event.isStartElement())
			{
				StartElement startElement = event.asStartElement();

				if (startElement.getName().getLocalPart().equals(CODELISTS))
				{
					codelists = getCodelists(eventReader, languages);
				}
				else if (startElement.getName().getLocalPart().equals(CONCEPTS))
				{
					concepts = getConcepts(eventReader, languages);
				}
				else if (startElement.getName().getLocalPart().equals(DATASTRUCTURE))
				{
					currentName = new LocalizedText(languages);
					String id = null, agency = null;
					for (Attribute attr: (Iterable<Attribute>) startElement::getAttributes)
						switch (attr.getName().toString())
						{
							case ID: id = attr.getValue(); break;
							case AGENCYID: agency = attr.getValue(); break;
						}
					// TODO: No version?
					currentStructure = new DataFlowStructure(id, agency, null);
				}
				else if (startElement.getName().getLocalPart().equals(NAME))
				{
					// this has to be checked better
					if (currentStructure != null)
					{
						currentName.setText(startElement, eventReader);
					}
				}
				else if (startElement.getName().getLocalPart().equals(COMPONENTS))
				{
					if (currentStructure != null)
					{
						setStructureDimensionsAndAttributes(currentStructure, eventReader, codelists, concepts);
					}
					else
					{
						throw new RuntimeException("Error during Structure Parsing. Null current structure.");
					}
				}
			}

			if (event.isEndElement())
			{
				if (event.asEndElement().getName().getLocalPart().equals(DATASTRUCTURE))
				{
					logger.finer("Adding data structure. " + currentStructure);
					currentStructure.setName(currentName.getText());
					result.add(currentStructure);
				}
			}
		}
		logger.exiting(sourceClass, sourceMethod);
		return result;
	}

	private static void setStructureDimensionsAndAttributes(DataFlowStructure currentStructure,
			XMLEventReader eventReader, Map<String, Codelist> codelists, Map<String, String> concepts)
			throws XMLStreamException
	{
		final String sourceMethod = "setStructureDimensions";
		logger.entering(sourceClass, sourceMethod);

		String agency = currentStructure.getAgency();
		SdmxMetaElement currentElement = null;
		int position = 0;

		while (eventReader.hasNext())
		{
			XMLEvent event = eventReader.nextEvent();
			logger.finest(event.toString());
			if (event.isStartElement())
			{
				StartElement startElement = event.asStartElement();
				if (startElement.getName().getLocalPart().equals(DIMENSION)
						|| startElement.getName().getLocalPart().equals(ATTRIBUTE))
				{
					boolean isDimension = startElement.getName().getLocalPart().equals(DIMENSION);
					logger.finer(isDimension ? "Got dimension" : "Got attribute");
					
					String id = null;
					String codelistID = null;
					String codelistAgency = null;

					for (final Attribute attribute : (Iterable<Attribute>) startElement::getAttributes) {
						if (attribute.getName().toString().equals(CONCEPT_REF))
							id = attribute.getValue();
						else if (attribute.getName().toString().equalsIgnoreCase(CODELIST))
							codelistID = attribute.getValue();
						else if (attribute.getName().toString().equals(CODELISTAGENCY))
							codelistAgency = attribute.getValue();
					}

					if (id != null && !id.isEmpty())
					{
						currentElement = isDimension ? new Dimension(id, ++position) : new SdmxAttribute(id);
						if (concepts != null)
							currentElement.setName(concepts.get(agency + "/" + id));
					}
					else
					{
						throw new RuntimeException("Error during Structure Parsing. Invalid id: " + id);
					}
					if (codelistID != null && !codelistID.isEmpty())
					{
						SDMXReference cl = new SDMXReference(codelistID, codelistAgency != null ? codelistAgency : agency, null);
						if (codelists != null)
							currentElement.setCodeList(codelists.get(cl.getFullIdentifier()));
					}
					else
					{
						if (isDimension)
							throw new RuntimeException(
									"Error during Structure Parsing. Invalid CODELIST: " + codelistID);
					}
				}
				else if (startElement.getName().getLocalPart().equals((TIMEDIMENSION)))
				{
					logger.finer("Got time dimension");
					@SuppressWarnings("unchecked")
					String id = null;
					for (final Attribute attribute : (Iterable<Attribute>) startElement::getAttributes)
					{
						if (attribute.getName().toString().equals(CONCEPT_REF))
						{
							id = attribute.getValue();
						}
					}
					if (id != null && !id.isEmpty())
					{
						if (currentStructure != null)
						{
							logger.finer("Adding time dimension: " + id);
							currentStructure.setTimeDimension(id);
						}
						else
						{
							throw new RuntimeException("Error during Structure Parsing. Null current Structure.");
						}
					}
					else
					{
						throw new RuntimeException("Error during Structure Parsing. Invalid time dimension: " + id);
					}
					continue;
				}
				else if (startElement.getName().getLocalPart().equals((PRIMARYMEASURE)))
				{
					logger.finer("Got primary measure");
					@SuppressWarnings("unchecked")
					String id = null;
					for (final Attribute attribute : (Iterable<Attribute>) startElement::getAttributes)
					{
						if (attribute.getName().toString().equals(CONCEPT_REF))
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

				if (event.asEndElement().getName().getLocalPart().equals(DIMENSION))
				{
					if (currentStructure != null && currentElement != null)
					{
						logger.finer("Adding dimension: " + currentElement);
						currentStructure.setDimension((Dimension) currentElement);
					}
					else
					{
						throw new RuntimeException(
								"Error during Structure Parsing. Null current structure or dimension.");
					}
				}
				else if (event.asEndElement().getName().getLocalPart().equals(ATTRIBUTE))
				{
					if (currentStructure != null && currentElement != null)
					{
						logger.finer("Adding attribute: " + currentElement);
						currentStructure.setAttribute((SdmxAttribute) currentElement);
					}
					else
					{
						throw new RuntimeException(
								"Error during Structure Parsing. Null current structure or dimension.");
					}
				}
				else if (event.asEndElement().getName().getLocalPart().equals(COMPONENTS))
				{
					break;
				}
			}
		}

		logger.exiting(sourceClass, sourceMethod);
	}

	private static Map<String, Codelist> getCodelists(XMLEventReader eventReader,
			List<LanguageRange> languages) throws XMLStreamException, SdmxException
	{
		Map<String, Codelist> codelists = new HashMap<>();
		while (eventReader.hasNext())
		{
			XMLEvent event = eventReader.nextEvent();
			logger.finest(event.toString());
			if (event.isStartElement())
			{
				StartElement startElement = event.asStartElement();

				if (startElement.getName().getLocalPart().equalsIgnoreCase(CODELIST))
				{
					String id = null;
					String agency = null;
					for (final Attribute attr : (Iterable<Attribute>) startElement::getAttributes)
					{
						switch (attr.getName().toString()) {
							case ID:    id = attr.getValue();   break;
							case AGENCYID:  agency = attr.getValue();   break;
						}
					}

					logger.finer("Got codelist: " + id);
					Codelist codes = CodelistParser.getCodes(new SDMXReference(id, agency, null), eventReader, languages, 
							CODE_ID, CODE_DESCRIPTION);
					codelists.put(codes.getFullIdentifier(), codes);
				}
			}
			else if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(CODELISTS))
				break;
		}

		return codelists;
	}

	private static Map<String, String> getConcepts(XMLEventReader eventReader, List<LanguageRange> languages)
			throws XMLStreamException, SdmxException
	{
		Map<String, String> concepts = new HashMap<>();
		String conceptSchemeAgency = null;
		while (eventReader.hasNext())
		{
			XMLEvent event = eventReader.nextEvent();
			logger.finest(event.toString());
			if (event.isStartElement())
			{
				StartElement startElement = event.asStartElement();

				if (startElement.getName().getLocalPart().equals(CONCEPTSCHEME))
				{
					conceptSchemeAgency = null;
					for (final Attribute attr : (Iterable<Attribute>) startElement::getAttributes)
					{
						if (attr.getName().toString().equals(AGENCYID))
							conceptSchemeAgency = attr.getValue();
					}
					logger.finer("Got conceptSchemeAgency: " + conceptSchemeAgency);
				}
				else if (startElement.getName().getLocalPart().equals(CONCEPT))
				{
					String id = null;
					String agency = null;
					String conceptName = "";
					for (final Attribute attr : (Iterable<Attribute>) startElement::getAttributes)
					{
						switch (attr.getName().toString()) {
							case ID:    id = attr.getValue();   break;
							case AGENCYID:  agency = attr.getValue();   break;
						}
					}

					if (agency == null && conceptSchemeAgency != null)
						agency = conceptSchemeAgency;

					conceptName = agency + "/" + id;
					logger.finer("Got concept: " + conceptName);
					concepts.put(conceptName, getConceptName(eventReader, languages));
				}
			}
			else if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(CONCEPTS))
				break;
		}

		return (concepts);
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
			else if (event.isEndElement() && CONCEPT.equals(event.asEndElement().getName().getLocalPart()))
				break;
		}

		return value.getText();
	}

}
