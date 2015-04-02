
% Simple script for DEMO/TEST purposes
%
% #############################################################################################
% Copyright 2010,2014 Bank Of Italy
%
% Licensed under the EUPL, Version 1.1 or as soon as they
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
getProviders

getFlows('ECB')
ans.keys

getDimensions('ECB', 'EXR')

getTimeSeries('ECB', 'EXR.M.USD|GBP.EUR.SP00.A')
plot(ans{1})

getFlows('EUROSTAT')
ans.keys

getDimensions('EUROSTAT', 'prc_hicp_midx')

getTimeSeries('EUROSTAT', 'prc_hicp_midx/..CP00.EU+DE+FR')
plot(ans{1})

sdmxHelp

