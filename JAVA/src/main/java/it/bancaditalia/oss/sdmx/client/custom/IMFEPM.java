package it.bancaditalia.oss.sdmx.client.custom;

import com.microsoft.aad.msal4j.*;
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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;

public class IMFEPM extends RestSdmxClient {
    protected static final String PUBLIC_ENTRY_POINT = "https://apim-imfeid-dev-01.azure-api.net/sdmx/2.1";


    protected static final String PROTECTED_ENTRY_POINT = "https://quanthub-rls.imf-eid.projects.epam.com/api/v1/workspaces/default:integration/registry/sdmx/2.1";

    protected final static String PROTECTED_CLIENT_ID = "bf03b113-5aa3-4585-a7d4-4b98160ec4ff";
    protected final static String PROTECTED_AUTHORITY = "https://login.microsoftonline.com/b41b72d0-4e9f-4c26-8a69-f949f367c91d/";
    protected final static String PROTECTED_SCOPE = "api://quanthub-rls.imf-eid.projects.epam.com/8fd30ba9-ee91-417c-8732-3080b50fd168/Quanthub.Login";


    public IMFEPM() throws Exception {
        // this(PUBLIC_ENTRY_POINT, null, null, null);
        this(PROTECTED_ENTRY_POINT, PROTECTED_CLIENT_ID, PROTECTED_AUTHORITY, new String[]{PROTECTED_SCOPE});
    }

    private static IAuthenticationResult acquireTokenInteractive(final String clientId,
                                                                 final String authority,
                                                                 final String[] scopeArray) throws Exception {

        if (clientId == null || authority == null || scopeArray == null)
            return null;

        final Set<String> scope = new LinkedHashSet<>(Arrays.asList(scopeArray));

//        // Load token cache from file and initialize token cache aspect. The token cache will have
//        // dummy data, so the acquireTokenSilently call will fail.
//        TokenCacheAspect tokenCacheAspect = new TokenCacheAspect("sample_cache.json");
//
        PublicClientApplication pca = PublicClientApplication.builder(clientId)
                .authority(authority)
//                .setTokenCacheAccessAspect(tokenCacheAspect)
                .build();

//
//        Set<IAccount> accountsInCache = pca.getAccounts().join();
//        // Take first account in the cache. In a production application, you would filter
//        // accountsInCache to get the right account for the user authenticating.
//        IAccount account = accountsInCache.iterator().next();

        IAuthenticationResult result;
        try {
            SilentParameters silentParameters =
                    SilentParameters
                            .builder(scope)
                            .build();

            // try to acquire token silently. This call will fail since the token cache
            // does not have any data for the user you are trying to acquire a token for
            result = pca.acquireTokenSilently(silentParameters).join();
        } catch (Exception ex) {
            if (ex.getCause() instanceof MsalException) {

                InteractiveRequestParameters parameters = InteractiveRequestParameters
                        .builder(new URI("http://localhost"))
                        .scopes(scope)
                        .build();

                // Try to acquire a token interactively with system browser. If successful, you should see
                // the token and account information printed out to console
                result = pca.acquireToken(parameters).join();
            } else {
                // Handle other exceptions accordingly
                throw ex;
            }
        }
        return result;
    }

    private static String tokenToAuthorization(final String clientId,
                                               final String authority,
                                               final String[] scopeArray) throws Exception {

        IAuthenticationResult iAuthenticationResult = acquireTokenInteractive(clientId, authority, scopeArray);
        if (iAuthenticationResult == null)
            return null;

        return RestSdmxClient.authorizationBearer(iAuthenticationResult.accessToken());
    }

    public IMFEPM(final String entryPoint, final String clientId, final String authority, final String[] scope) throws Exception {
        super(IMFEPM.class.getSimpleName(), new URI(entryPoint), tokenToAuthorization(clientId, authority, scope), false, true);
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
