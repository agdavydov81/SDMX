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

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IMF_DATA extends RestSdmxClient {
    public static final String PUBLIC_ENTRY_POINT = "https://apim-imfeid-dev-01.azure-api.net/sdmx/2.1";


    public static final  String PROTECTED_ENTRY_POINT = "https://quanthub-rls.imf-eid.projects.epam.com/api/v1/workspaces/default:integration/registry/sdmx/2.1";

    public static final String PROTECTED_CLIENT_ID = "bf03b113-5aa3-4585-a7d4-4b98160ec4ff";
    public static final String PROTECTED_AUTHORITY = "https://login.microsoftonline.com/b41b72d0-4e9f-4c26-8a69-f949f367c91d/";
    public static final String PROTECTED_SCOPE = "api://quanthub-rls.imf-eid.projects.epam.com/8fd30ba9-ee91-417c-8732-3080b50fd168/Quanthub.Login";

    public static final String ENTRY_POINT_VAR = "entryPoint";
    public static final String CLIENT_ID_VAR = "clientId";
    public static final String AUTHORITY_VAR = "authority";
    public static final String SCOPE_VAR = "scope";
    public static final String OPTIONAL_HEADERS_VAR = "optionalHeaders";

    public static class EntryPointAndAuth {
        final String entryPoint;
        final String clientId;
        final String authority;
        final String[] scope;
        final String[][] optionalHeaders;

        public EntryPointAndAuth(final String entryPoint, final String clientId,
                                 final String authority, final String[] scope,
                                 final String[][] optionalHeaders) {
            this.entryPoint = entryPoint;
            this.clientId = clientId;
            this.authority = authority;
            this.scope = scope;
            this.optionalHeaders = optionalHeaders;
        }

        // From https://developers.cloudflare.com/rules/transform/request-header-modification/reference/header-format/
        public static final Pattern optionalHeaderPattern = Pattern.compile("\\s*([a-zA-Z0-9_\\-]+?):\\s*([a-zA-Z0-9_ :;.,\\\\/\"'?!(){}\\[\\]@<>=\\-+*#$&`|~^%]+)\\s*");

        public static EntryPointAndAuth inputDialog(final String argEntryPoint) {
            final Preferences preferences = Preferences.userNodeForPackage(IMF_DATA.class);

            final JTextField entryPoint = new JTextField(argEntryPoint != null && !argEntryPoint.trim().isEmpty()
                    ? argEntryPoint.trim() : preferences.get(ENTRY_POINT_VAR, PROTECTED_ENTRY_POINT));
            final JTextField clientId = new JTextField(preferences.get(CLIENT_ID_VAR, PROTECTED_CLIENT_ID));
            final JTextField authority = new JTextField(preferences.get(AUTHORITY_VAR, PROTECTED_AUTHORITY));

            final JTextArea scope = new JTextArea(ensureNewLines(preferences.get(SCOPE_VAR, PROTECTED_SCOPE), 2));
            scope.setFont(authority.getFont());
            final JScrollPane scopeScroll = new JScrollPane(scope);

            final JTextArea optionalHeaders = new JTextArea(ensureNewLines(preferences.get(OPTIONAL_HEADERS_VAR, ""), 2));
            optionalHeaders.setFont(authority.getFont());
            final JScrollPane optionalHeadersScroll = new JScrollPane(optionalHeaders);

            Object[] message = {
                    "Entry point:", entryPoint,
                    "Client ID:", clientId,
                    "Authority:", authority,
                    "Scope (one per line):", scopeScroll,
                    "Optional request headers (one per line in format   HeaderName: HeaderValue ):", optionalHeadersScroll
            };

            int option = JOptionPane.showConfirmDialog(null, message, "Provider configuration", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                final String curEntryPoint = entryPoint.getText().trim();
                final String curClientId = clientId.getText().trim();
                final String curAuthority = authority.getText().trim();
                final String curScope = scope.getText();
                final String curOptionalHeaders = optionalHeaders.getText();

                preferences.put(ENTRY_POINT_VAR, curEntryPoint);
                preferences.put(CLIENT_ID_VAR, curClientId);
                preferences.put(AUTHORITY_VAR, curAuthority);
                preferences.put(SCOPE_VAR, curScope);
                preferences.put(OPTIONAL_HEADERS_VAR, curOptionalHeaders);

                final String[] curScopeArray = Arrays.stream(curScope.split("[\r\n]"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);

                final List<String[]> curOptionalHeadersProcessed = new ArrayList<>();
                final String[] curOptionalHeadersParts = curOptionalHeaders.split("[\r\n]");
                for (int pi = 0; pi < curOptionalHeadersParts.length; ++pi) {
                    final String str = curOptionalHeadersParts[pi].trim();
                    if (str.isEmpty())
                        continue;

                    final Matcher matcher = optionalHeaderPattern.matcher(str);
                    if (!matcher.matches())
                        throw new RuntimeException("The optional header[" + pi + "](=" + str + ") does not matches acceptable pattern.");
                    final String g1 = matcher.group(1);
                    final String g2 = matcher.group(2);
                    curOptionalHeadersProcessed.add(new String[]{g1, g2});
                }

                return new EntryPointAndAuth(emptyToNull(curEntryPoint), emptyToNull(curClientId), emptyToNull(curAuthority),
                        curScopeArray, curOptionalHeadersProcessed.toArray(new String[][]{}));
            } else {
                return new EntryPointAndAuth(PUBLIC_ENTRY_POINT, null, null, null, null);
            }
        }

        private static String emptyToNull(final String str) {
            return str.isEmpty() ? null : str;
        }
    }

    private static String ensureNewLines(String str, int newLinesMinCount) {
        final String newLine = System.lineSeparator();
        int newLinesCount = 0;
        for (int i = 0; i < str.length(); ++i) {
            final char c = str.charAt(i);
            if (c == '\n') {
                newLinesCount++;
            }
        }

        if (newLinesCount < newLinesMinCount) {
            StringBuilder sb = new StringBuilder(str);
            for (; newLinesCount < newLinesMinCount; ++newLinesCount) {
                sb.append(newLine);
            }
            str = sb.toString();
        }

        return str;
    }

    public IMF_DATA() throws Exception {
        this(false);
        // this(PUBLIC_ENTRY_POINT, null, null, null);
        // this(PROTECTED_ENTRY_POINT, PROTECTED_CLIENT_ID, PROTECTED_AUTHORITY, new String[]{PROTECTED_SCOPE});
    }

    public IMF_DATA(final Boolean showInputDialog) throws Exception {
        this(showInputDialog == null || showInputDialog
                ? EntryPointAndAuth.inputDialog(null)
                : new EntryPointAndAuth(PUBLIC_ENTRY_POINT, null, null, null, null));
    }

    public IMF_DATA(final String entryPoint) throws Exception {
        this(EntryPointAndAuth.inputDialog(entryPoint));
    }

    private static String enforceTrailingSlash(String authority) {
        authority = authority.toLowerCase();
        if (!authority.endsWith("/")) {
            authority = authority + "/";
        }

        return authority;
    }

    private static boolean isB2CAuthority(String host, String firstPath) {
        return host.contains("b2clogin.com") || firstPath.compareToIgnoreCase("tfp") == 0;
    }

    private static boolean stringIsBlank(final String str) {
        return str == null || str.trim().isEmpty();
    }

    private static boolean msalIsB2C(String authority) throws MalformedURLException {
        if (authority == null)
            return false;

        authority = enforceTrailingSlash(authority);
        final URL authorityUrl = new URL(authority);

        String path = authorityUrl.getPath().substring(1);
        if (stringIsBlank(path))
            return false;

        String host = authorityUrl.getHost();
        String firstPath = path.substring(0, path.indexOf("/"));

        return isB2CAuthority(host, firstPath);
    }

    private static IAuthenticationResult acquireTokenInteractive(final String clientId,
                                                                 final String authority,
                                                                 final String[] scopeArray) throws Exception {

        if (clientId == null || clientId.isEmpty())
            throw new IllegalArgumentException("The clientId argument is null or empty.");
        if (authority == null || authority.isEmpty())
            throw new IllegalArgumentException("The authority argument is null or empty.");
        if (scopeArray == null)
            throw new IllegalArgumentException("The scopeArray argument is null.");

        final Set<String> scope = new LinkedHashSet<>(Arrays.asList(scopeArray));

//        // Load token cache from file and initialize token cache aspect. The token cache will have
//        // dummy data, so the acquireTokenSilently call will fail.
//        TokenCacheAspect tokenCacheAspect = new TokenCacheAspect("sample_cache.json");
//
        PublicClientApplication.Builder builder = PublicClientApplication.builder(clientId);
        if (msalIsB2C(authority)) {
            builder = builder.b2cAuthority(authority);
        } else {
            builder = builder.authority(authority);
        }

        PublicClientApplication pca = builder
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

        if (clientId == null && authority == null && (scopeArray == null || scopeArray.length == 0))
            return null;

        IAuthenticationResult iAuthenticationResult = acquireTokenInteractive(clientId, authority, scopeArray);

        return RestSdmxClient.authorizationBearer(iAuthenticationResult.accessToken());
    }

    public IMF_DATA(final EntryPointAndAuth args) throws Exception {
        this(args.entryPoint, args.clientId, args.authority, args.scope, args.optionalHeaders);
    }

    public IMF_DATA(final String entryPoint, final String clientId, final String authority, final String[] scope, final String[][] optionalHeaders) throws Exception {
        super(IMF_DATA.class.getSimpleName(), new URI(entryPoint), tokenToAuthorization(clientId, authority, scope), false, true, optionalHeaders);
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
                query.addParam(REFERENCES, DESCENDANTS);
            }
            return query.buildSdmx21Query();
        } else {
            throw new RuntimeException("Invalid query parameters: agency=" + agency + " dsd=" + dsd + " endpoint=" + endpoint);
        }
    }
}
