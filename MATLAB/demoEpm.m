%% Demo query variables
providerName = 'IMFEPM211';
preferredFlow = 'QUANTHUB,BOP6,1.2';
preferredDimensionId = 'FREQ';
preferredDimensionValue = 'A';

%providerName = 'EUROSTAT';
%preferredFlow = 'ESTAT,AACT_ALI01,1.0';
%preferredDimensionId = 'freq';
%preferredDimensionValue = 'A';


%% Add provider or find existing (added on the previous run)
provides = getProviders();
if ~strcmp(provides, providerName)
    disp(['Register new provider ' providerName]);
    addProvider(providerName, 'https://apim-imfeid-dev-01.azure-api.net/sdmx/2.1', false, true, false, 'IMF EPAM provider');
end

%% Get and display data flows
flows = getFlows(providerName);
flowKeys = flows.keys;
if isempty(flowKeys)
    error('No flows are found');
end
disp('Next flows are found:');
disp([flowKeys; flows.values]);

%% Select data flow
dataFlow = findPreferred(flowKeys, preferredFlow, 'data flow');

%% Get and display DSD information
dsd = getDataFlowStructure(providerName, dataFlow);

disp('Next DSD is found:');
disp(dsd);

disp('With dimensions:');
disp({dsd.dimensions.id; dsd.dimensions.name});

%% Select dimension
dimensionSel.id = findPreferred({dsd.dimensions.id}, preferredDimensionId, 'dimension');
dimensionSel.index = find(strcmp({dsd.dimensions.id}, dimensionSel.id), 1);
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
        result = values(1);
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

