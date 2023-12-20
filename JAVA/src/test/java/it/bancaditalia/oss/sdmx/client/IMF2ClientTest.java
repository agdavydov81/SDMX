package it.bancaditalia.oss.sdmx.client;

import it.bancaditalia.oss.sdmx.api.DataFlowStructure;
import it.bancaditalia.oss.sdmx.api.Dataflow;
import it.bancaditalia.oss.sdmx.api.PortableTimeSeries;
import it.bancaditalia.oss.sdmx.exceptions.SdmxException;
import it.bancaditalia.oss.sdmx.util.Configuration;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static it.bancaditalia.oss.sdmx.client.SdmxClientHandler.getFlow;
import static it.bancaditalia.oss.sdmx.client.SdmxClientHandler.getTimeSeries;

public class IMF2ClientTest {
    public static void main(String[] args) throws SdmxException, InterruptedException {
        Logger sdmxLogger = Configuration.getSdmxLogger();

//        // http://dataservices.imf.org/REST/SDMX_XML.svc/CompactData/AFRREO201510/..?detail=serieskeysonly
//        DataFlowStructure imf2 = SdmxClientHandler.getDataFlowStructure("IMF2", "CompactData");

        final String provider = "IMF-EPM-OPEN";

        // https://apim-imfeid-dev-01.azure-api.net/sdmx/2.1/data/QUANTHUB%2CBOP6%2C1.2/.136...?detail=serieskeysonly
        SdmxClientHandler.addProvider(provider, "https://apim-imfeid-dev-01.azure-api.net/sdmx/2.1",
                false, false, false, "Open IMF-EPM endpoint");

//        Map<String, String> flows = SdmxClientHandler.getFlows(provider, "");

//        Dataflow dataflow = getFlow(provider, "QUANTHUB,BOP6,1.2");
        List<PortableTimeSeries<Double>> names = getTimeSeries(provider, "QUANTHUB,BOP6,1.2", ".136...", null, null, null, true, null, false);
//        return new AbstractMap.SimpleEntry<>(dataflow, names);

    }
}
