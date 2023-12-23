package it.bancaditalia.oss.sdmx.client.custom;

import it.bancaditalia.oss.sdmx.api.DataFlowStructure;
import it.bancaditalia.oss.sdmx.api.Dataflow;
import it.bancaditalia.oss.sdmx.api.Message;
import it.bancaditalia.oss.sdmx.client.RestSdmxClient;
import it.bancaditalia.oss.sdmx.event.DataFooterMessageEvent;
import it.bancaditalia.oss.sdmx.event.RestSdmxEvent;
import it.bancaditalia.oss.sdmx.exceptions.SdmxException;
import it.bancaditalia.oss.sdmx.exceptions.SdmxInvalidParameterException;
import it.bancaditalia.oss.sdmx.parser.v21.CompactDataParser;
import it.bancaditalia.oss.sdmx.parser.v21.DataParsingResult;
import it.bancaditalia.oss.sdmx.parser.v21.Sdmx21Queries;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;

public class IMFEPM extends RestSdmxClient {
    protected static final String ENPTY_POINT = "https://apim-imfeid-dev-01.azure-api.net/sdmx/2.1";

    public IMFEPM() throws URISyntaxException {
        super(IMFEPM.class.getSimpleName(), new URI(ENPTY_POINT), false, false, true);
    }

//    @Override
//    protected DataParsingResult getData(Dataflow dataflow, DataFlowStructure dsd, String resource, String startTime, String endTime, boolean serieskeysonly,
//                                        String updatedAfter, boolean includeHistory) throws SdmxException {
//        URL query = buildDataQuery(dataflow, resource, startTime, endTime, serieskeysonly, updatedAfter, includeHistory);
//        String dumpName = "data_" + dataflow.getId() + "_" + resource; //.replaceAll("\\p{Punct}", "_");
//        DataParsingResult ts = runQuery(new CompactDataParser(dsd, dataflow, !serieskeysonly), query,
//                ACCEPT_DEFAULT, dumpName);
//        Message msg = ts.getMessage();
//        if (msg != null) {
//            LOGGER.log(Level.INFO, "The sdmx call returned messages in the footer:\n {0}", msg);
//            RestSdmxEvent event = new DataFooterMessageEvent(query, msg);
//            dataFooterMessageEventListener.onSdmxEvent(event);
//        }
//        return ts;
//    }

    protected URL buildDSDQuery(String dsd, String agency, String version, boolean full) throws SdmxException {
        if (endpoint != null && agency != null && !agency.isEmpty() && dsd != null && !dsd.isEmpty()) {
            final Sdmx21Queries query = Sdmx21Queries.createStructureQuery(endpoint, dsd, agency, version, false);
            if (full) {
                query.addParam("references", "descendants");
            }
            return query.buildSdmx21Query();
        } else {
            throw new RuntimeException("Invalid query parameters: agency=" + agency + " dsd=" + dsd + " endpoint=" + endpoint);
        }
    }
}
