%% Demo query variables
providerName = 'IMFEPM-CUSTOM-002';
preferredFlow = 'IMF,IMF_CPI_DATAFLOW,6.0.0';
preferredDimensionId = 'FREQUENCY';
preferredDimensionValue = 'M';

%providerName = 'EUROSTAT';
%preferredFlow = 'ESTAT,AACT_ALI01,1.0';
%preferredDimensionId = 'freq';
%preferredDimensionValue = 'A';

initClasspath;

%% Add provider or find existing (added on the previous run)
provides = getProviders();
if ~strcmp(provides, providerName)
    disp(['Register new provider ' providerName]);

    % final String entryPoint, final String clientId, final String authority, final String[] scope
%{
    constructorArguments = javaArray('java.lang.Object', 4);
    constructorArguments(1) = javaObject('java.lang.String', 'https://quanthub-rls.imf-eid.projects.epam.com/api/v1/workspaces/default:integration/registry/sdmx/2.1');
    constructorArguments(2) = javaObject('java.lang.String', 'bf03b113-5aa3-4585-a7d4-4b98160ec4ff');
    constructorArguments(3) = javaObject('java.lang.String', 'https://login.microsoftonline.com/b41b72d0-4e9f-4c26-8a69-f949f367c91d/');
    scope = javaArray('java.lang.String', 1);
    scope(1) = javaObject('java.lang.String', 'api://quanthub-rls.imf-eid.projects.epam.com/8fd30ba9-ee91-417c-8732-3080b50fd168/Quanthub.Login');
    constructorArguments(4) = scope;
%}
    constructorArguments = javaArray('java.lang.Object', 1);
    constructorArguments(1) = javaObject('java.lang.Boolean', true);

    addProvider(providerName, ['Description of the ' providerName], 'it.bancaditalia.oss.sdmx.client.custom.IMFEPM', constructorArguments);
end

%% Get and display data flows
flows = getFlows(providerName);
flowKeys = flows.keys;
if isempty(flowKeys)
    error('No flows are found');
end
disp('Next flows are found:');
disp(flowKeys);

%% Select data flow
dataFlow = findPreferred(flowKeys, preferredFlow, 'data flow');

%% Get and display DSD information
dsd = getDataFlowStructure(providerName, dataFlow);

disp('Next DSD is found:');
disp(dsd);

disp('With dimensions:');
dsdDimensionsId = cellfun(@char, {dsd.dimensions.id}, 'UniformOutput',false);
disp(dsdDimensionsId);

%% Select dimension
dimensionSel.id = findPreferred(dsdDimensionsId, preferredDimensionId, 'dimension');
dimensionSel.index = find(strcmp(dsdDimensionsId, dimensionSel.id), 1);
dimensionSel.value = findPreferred(dsd.dimensions(dimensionSel.index).codelist.keys, preferredDimensionValue, 'dimension value');

%% Select all time series with specified preferred dimension name and value
tsListRequest = selectDimension(dataFlow, dsd, dimensionSel);
tsList = getTimeSeries(providerName, tsListRequest);

%% Display first time series
figure;
ts = tsList{1};
ts.plot();

%% Or get time series as table
tsTbl = getTimeSeriesTable(providerName, regexprep(ts.Name, '\.[^\.]+$', '.*'));


%% Auxiliary functions
function result = findPreferred(values, preferred, valuesName)
    result = values(find(strcmp(values, preferred), 1));
    if isempty(result)
        disp(['Can''t find preferred ' valuesName ': ' preferred]);
        if isempty(values)
            error(['There are no elements in ' valuesName]);
        else
            result = values(1);
        end
    end
    result = result{1};
    disp(['Select ' valuesName ': ' result]);
end

function request = selectDimension(dataFlow, dsd, dimensionSel)
    dims = cell(2, length(dsd.dimensions));
    dims{1, dimensionSel.index} = dimensionSel.value;
    dims(2, 1:end-1) = {'.'};
    request = [dataFlow '/' dims{:}];
end

