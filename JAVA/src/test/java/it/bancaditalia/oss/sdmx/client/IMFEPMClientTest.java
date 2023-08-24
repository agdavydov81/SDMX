package it.bancaditalia.oss.sdmx.client;

import it.bancaditalia.oss.sdmx.api.DataFlowStructure;
import it.bancaditalia.oss.sdmx.api.Dimension;
import it.bancaditalia.oss.sdmx.api.PortableTimeSeries;
import it.bancaditalia.oss.sdmx.client.custom.IMFEPM;
import it.bancaditalia.oss.sdmx.exceptions.SdmxException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import static it.bancaditalia.oss.sdmx.client.SdmxClientHandler.*;

public class IMFEPMClientTest {
    static final String providerName = IMFEPM.class.getSimpleName();

    static final String preferredFlow = "QUANTHUB,BOP6,1.2";
    static final String preferredDimensionId = "FREQ";
    static final String preferredDimensionValue = "A";

    public static void main(String[] args) {
        try {
            final SortedMap<String, Boolean> providers = getProviders();
            final Boolean needCredentials = providers.get(providerName);
            if (needCredentials == null)
                throw new RuntimeException("Can't final the " + providerName + " provider.");
            if (needCredentials)
                throw new RuntimeException("This provider must be freely available.");

            // Get and display data flows
            final Map<String, String> flows = getFlows(providerName, "");
            if (flows.isEmpty())
                throw new RuntimeException("No flows are found");

            // Select data flow
            final String dataFlow = findPreferred(flows.keySet(), preferredFlow, "data flow");

            // Get and display DSD information
            final DataFlowStructure dsd = getDataFlowStructure(providerName, dataFlow);

            // Select dimension
            final String dimensionSelId = preferredDimensionId;// findPreferred(dsd.getDimensions().get(0).getId(), preferredDimensionId, "dimension");
            //dimensionSel.index = find(strcmp({dsd.dimensions.id}, dimensionSel.id), 1);
            final String dimensionSelValue = preferredDimensionValue; //findPreferred(dsd.dimensions(dimensionSel.index).codelist.keys, preferredDimensionValue, 'dimension value');

            //Select all time series with specified preferred dimension name and value
            final String tsListRequest = selectDimension(dataFlow, dsd, dimensionSelId, dimensionSelValue);
            List<PortableTimeSeries<Double>> tsList = getTimeSeries(providerName, tsListRequest, null, null);

            System.out.println("The " + tsList.size() + " series are returned.");
        }
        catch (final Throwable e) {
            e.printStackTrace();
        }

        //%% Display first time series
        //figure;
        //ts = tsList{1};
        //ts.plot();
        //
        //%% Or get time series as table
        //tsTbl = getTimeSeriesTable(providerName, regexprep(ts.Name, '\.[^\.]+$', '.*'));
        //
        //
    }

    private static String findPreferred(final Collection<String> values, final String preferred, final String valuesName) {
        String result = null;
        String firstValue = null;
        for(final String value : values) {
            if (firstValue == null) {
                firstValue = value;
            }
            if (value.equals(preferred)) {
                result = preferred;
                break;
            }
        }
        if (result == null) {
            System.out.println("Can't find preferred " +  valuesName + ": " + preferred);
            result = firstValue;
        }

        System.out.println("Select " + valuesName + ": " + result);
        return result;
    }

    private static String selectDimension(final String dataFlow, final DataFlowStructure dsd, final String dimensionSelId, final String dimensionSelValue) {
        final StringBuilder sb = new StringBuilder(dataFlow).append('/');

        final List<Dimension> dimensions = dsd.getDimensions();
        for (int di = 0, de = dimensions.size(); di < de; ++di) {
            if (di > 0)
                sb.append('.');
            if (dimensions.get(di).getId().equals(dimensionSelId))
                sb.append(dimensionSelValue);
        }
        return sb.toString();
    }
}
