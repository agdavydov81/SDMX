function dfs = getDataFlowStructure(provider, dataflow)
	% Get the data flow structure
	%
	% Usage: getDataFlowStructure(provider, dataflow)
	%
	% Arguments
	%
	% provider: the name of the SDMX data provider
	% dataflow: the dataflow to be analyzed
	%
	% #############################################################################################
	% Copyright 2010,2014 Bank Of Italy
	%
	% Licensed under the EUPL, Version 1.1 or - as soon they
	% will be approved by the European Commission - subsequent
	% versions of the EUPL (the "Licence");
	% You may not use this work except in compliance with the
	% Licence.
	% You may obtain a copy of the Licence at:
	%
	%
	% http://ec.europa.eu/idabc/eupl
	%
	% Unless required by applicable law or agreed to in
	% writing, software distributed under the Licence is
	% distributed on an "AS IS" basis,
	% WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
	% express or implied.
	% See the Licence for the specific language governing
	% permissions and limitations under the Licence.
	%

    initClasspath;

    if nargin <2
        error(sprintf([ '\nUsage: getDataFlowStructure(provider, dataflow)\n\n' ...
                    'Arguments\n\n' ...
                    'provider: the name of the SDMX data provider\n' ...
                    'dataflow: the dataflow to be analyzed\n' ...
                    ]));
    end
    %try java code
    try
        jdfs = javaMethod('getDataFlowStructure', 'it.bancaditalia.oss.sdmx.client.SdmxClientHandler', provider, dataflow);
    catch mexp
        error(['SDMX getDataFlowStructure() error:\n' mexp.message]);
    end

    dimensions = javaMethod('getDimensions', jdfs);
    attributes = javaMethod('getAttributes', jdfs);

    dfs = struct('id', javaMethod('getId', jdfs), ...
                 'agency', javaMethod('getAgency', jdfs), ...
                 'version', javaMethod('getVersion', jdfs), ...
                 'name', javaMethod('getName', jdfs), ...
                 'timeDimension', javaMethod('getTimeDimension', jdfs), ...
                 'primaryMeasure', javaMethod('getMeasure', jdfs), ...
                 'dimensions', mapDimensions(dimensions), ...
                 'attributes', mapAttributes(attributes));
end

function mds = mapDimensions(jds)
    mds = cell(javaMethod('size', jds), 1);
    for di = 1:length(mds)
        mds{di} = mapMetaElement(javaMethod('get', jds, di - 1));
    end
    mds = cell2mat(mds);
end

function md = mapMetaElement(jd)
    md = struct('id', javaMethod('getId', jd), ...
                'name', javaMethod('getName', jd), ...
                'codelist', mapCodeList(javaMethod('getCodeList', jd)));
end

function mcl = mapCodeList(jcl)
    if isempty(jcl)
        mcl = containers.Map();
    else
        jKeys = jcl.keySet().toArray();
        jValues = jcl.values().toArray();
        sz = javaMethod('getLength', 'java.lang.reflect.Array', jKeys);

        keys = arrayfun(@(i) javaMethod('get', 'java.lang.reflect.Array', jKeys, i), (0:sz-1)', 'UniformOutput', false);
  	    values = arrayfun(@(i) javaMethod('get', 'java.lang.reflect.Array', jValues, i), (0:sz-1)', 'UniformOutput', false);

        mcl = containers.Map(keys, values);
    end
end

function mas = mapAttributes(jas)
    mas = cell(javaMethod('size', jas), 1);
    for di = 1:length(mas)
        mas{di} = mapMetaElement(javaMethod('get', jas, di - 1));
    end
    mas = cell2mat(mas);
end

