function dimensions = getDimensions(provider, dataflow)
	% Get the list of dimensions for this dataflow
	%
	% Usage: getDimensions(provider, dataflow)
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
    
    if nargin <2
		error([ 'Usage: getDimensions(provider, dataflow)\n' ...
                        'Arguments\n' ...
                        'provider: the name of the SDMX data provider\n' ...
                        'dataflow: the dataflow to be analyzed\n']);
    end
    %try java call
    try
        dimensions = it.bancaditalia.oss.sdmx.client.SdmxClientHandler.getDimensions(provider, dataflow);
    catch mexp
        error(['SDMX getDimension() error:\n' mexp.message]);             
    end
end

