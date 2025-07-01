package se.buscatcher.buscatcherapi.Controller;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.view.RedirectView;
import se.buscatcher.buscatcherapi.Model.GoogleTokens;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.sql.Ref;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class handles the Google Calendar API requests.
 */

// "http://localhost:8080/api/google/calendars"
@CrossOrigin(origins = "http://localhost:5500", allowedHeaders = "*")
@RestController
@RequestMapping("/api/google")
public class GoogleAPIController {

    @Value("${google.places.api.key}")
    private String PLACES_API_KEY;
    private static final String PLACES_API_URL = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CLIENT_SECRETS_PATH = "/client_secrets.json";
    private static final String CLIENT_SECRETs_PLACES_PATH = "/client_secrets_places.json";
    private static final String REDIRECT_URL = "http://localhost:8080/api/google/oauth2callback"; // TODO localhost
                                                                                                  // doesn't work from
                                                                                                  // external device, is
                                                                                                  // it possible to
                                                                                                  // change to dynamic
                                                                                                  // ip?

    /**
     * Redirects the user to the Google OAuth2 consent screen to grant the
     * application access to the user's
     * Google Calendar data.
     * <p>
     * 1. The GoogleAuthorizationCodeFlow object is created using the
     * GoogleClientSecrets object, the NetHttpTransport object, and the JSON_FACTORY
     * object.
     * </p>
     * <p>
     * 2. The GoogleAuthorizationCodeFlow object is used to create the
     * AuthorizationCodeRequestUrl object.
     * </p>
     * <p>
     * 3. The AuthorizationCodeRequestUrl object is used to create the redirect URL.
     * </p>
     * <p>
     * 4. The redirect URL is returned to the client.
     * </p>
     * <p>
     * 5. The client is redirected to the Google OAuth2 consent screen.
     * </p>
     * <p>
     * 6. The user grants the application access to the user's Google Calendar data.
     * </p>
     * <p>
     * 7. The user is redirected back to the application.
     * </p>
     * <p>
     * 8. The code parameter is retrieved from the request.
     * </p>
     * <p>
     * 9. The code parameter is used to create the AuthorizationCodeTokenRequest
     * object.
     * </p>
     * <p>
     * 10. The AuthorizationCodeTokenRequest object is used to create the
     * TokenResponse object.
     * </p>
     * <p>
     * 11. The access token, refresh token, and expires_in are retrieved from the
     * TokenResponse object.
     * </p>
     * <p>
     * 12. The access token and refresh token are stored in the session.
     * </p>
     * <p>
     * 13. The access token is used to make requests to the Google Calendar API.
     * </p>
     * <p>
     * 14. The refresh token is used to retrieve a new access token when the current
     * access token expires.
     * </p>
     * <p>
     * 15. The expires_in value is used to determine when the access token expires.
     * </p>
     * <p>
     * 16. The access token expires after 3600 seconds (1 hour).
     * </p>
     * <p>
     * 17. Checks if the access token exists in the session, if it doesn't, redirect
     * to google login, otherwise redirect to settings.html(if the user is new) or
     * to main page if the user has already chosen
     * a calendar group.
     * </p>
     * 
     * @return a redirect to the Google OAuth2 consent screen
     * @throws IOException
     * @throws GeneralSecurityException
     */
    @GetMapping("/login")
    public RedirectView redirectToGoogle(HttpServletRequest request/*@RequestHeader("Authorization") String authorizationHeader*/) throws IOException, GeneralSecurityException {
        // TODO: Check if access token exists in session, if it does, redirect to
        // settings.html, otherwise redirect to google login
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attr.getRequest().getSession(false); // false == don't allow create
        if (session != null) {// && authorizationHeader != null && !authorizationHeader.isEmpty()) {
            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader != null && !authorizationHeader.isEmpty()) {
                String[] parts = authorizationHeader.split(" ");
                if (parts.length != 2 || !"Bearer".equals(parts[0])) {
                    throw new IllegalArgumentException("Invalid authorization header. It should be a 'Bearer <token>'.");
                }

                // The actual token is the second element of the array
                String token = parts[1];
                String accessToken = (String) session.getAttribute("accessToken||" + token);
                System.out.println("login accessToken: " + accessToken);
                if (accessToken != null && !accessToken.isEmpty()) {
                    System.out.println("login if accessToken: " + accessToken);
                    return new RedirectView("http://localhost:5500/settings.html");
                }
            }
        }
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(GoogleAPIController.class.getResourceAsStream(CLIENT_SECRETS_PATH)));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                JSON_FACTORY,
                clientSecrets,
                Collections.singleton(CalendarScopes.CALENDAR_READONLY))
                .setAccessType("offline")
                .build();

        AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URL);

        HashMap<String, String> output = new HashMap<>();
        output.put("redirect", authorizationUrl.toString());

        Gson gson = new Gson();
        return new RedirectView(authorizationUrl.toString());// gson.toJson(output);// "redirect:" + authorizationUrl;
    }

    /**
     * Handles the redirect from the Google OAuth2 consent screen.
     * <p>
     * Retrieves the access token, refresh token, and expires_in from the
     * TokenResponse object.
     * </p>
     * <p>
     * Stores the access token, refresh token, and expires_in in the session.
     * </p>
     * <p>
     * Redirects the user to the settings page or to the main page.
     * </p>
     * 
     * @param code
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     */
    @GetMapping("/oauth2callback")
    public RedirectView handleGoogleRedirect(@RequestParam("code") String code)
            throws IOException, GeneralSecurityException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(GoogleAPIController.class.getResourceAsStream(CLIENT_SECRETS_PATH)));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY,
                clientSecrets, Collections.singleton(CalendarScopes.CALENDAR_READONLY))
                .setAccessType("offline")
                .build();

        AuthorizationCodeTokenRequest tokenRequest = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URL);
        TokenResponse tokenResponse = tokenRequest.execute();

        // Retrieve the access token from the TokenResponse object
        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();
        Long expiresIn = tokenResponse.getExpiresInSeconds();

        // Store the tokens in the session
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attr.getRequest().getSession(true); // true == allow create
        //session.setAttribute("access_token", accessToken);
        //session.setAttribute("refresh_token", refreshToken);
        //session.setAttribute("expires_in", expiresIn + System.currentTimeMillis() / 1000);

        // Generate a unique session token
        String sessionToken = UUID.randomUUID().toString();
        System.out.println("sessionToken: " + sessionToken);
        System.out.println("accessToken: " + accessToken);
        System.out.println("refreshToken: " + refreshToken);
        System.out.println("expiresIn: " + expiresIn);
        //GoogleTokens googleTokens = new GoogleTokens(accessToken, refreshToken, expiresIn + System.currentTimeMillis() / 1000);
        //TODO:OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO
        //String googleTokens = accessToken + "|" + refreshToken + "|" + (expiresIn + System.currentTimeMillis() / 1000);
        //System.out.println("googleTokens AccessToken: " + googleTokens.getAccessToken());
        //System.out.println("googleTokens RefreshToken: " + googleTokens.getRefreshToken());
        //System.out.println("googleTokens ExpiresIn: " + googleTokens.getExpiresIn());
        //session.setAttribute(sessionToken, googleTokens);
        session.setAttribute("accessToken||" + sessionToken, accessToken);
        session.setAttribute("refreshToken||" + sessionToken, refreshToken);
        session.setAttribute("expiresIn||" + sessionToken, expiresIn + System.currentTimeMillis() / 1000);
        System.out.println("Get Session:" + session.getAttribute(sessionToken));

        // Append the session token to the redirect URL
        //String redirectUrl = "http://localhost:5500/settings.html?session_token=" + sessionToken;
        String redirectUrl = "http://localhost:5500/settings.html?access_token=" + accessToken + "&refresh_token=" + refreshToken + "&expires_in=" + (expiresIn + System.currentTimeMillis() / 1000);
        // Use the Calendar object to make requests to the Google Calendar API
        // TODO redirect to settings.html if user is new, otherwise redirect to main
        // page
        return new RedirectView(redirectUrl);//"http://localhost:5500/settings.html");// "Successfully authenticated with Google";
    }

    /**
     * Retrieves the user's calendars from the Google Calendar API.
     * <p>
     * The access token is retrieved from the session.
     * </p>
     * <p>
     * The access token is used to make requests to the Google Calendar API.
     * </p>
     * 
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     */
    @GetMapping("/calendars")
    public String getCalendars(@RequestHeader("AccessToken") String AccessToken, @RequestHeader("RefreshToken") String RefreshToken, @RequestHeader("ExpiresIn") String ExpiresIn) throws IOException, GeneralSecurityException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY,
                new InputStreamReader(
                        GoogleAPIController.class.getResourceAsStream(CLIENT_SECRETS_PATH)));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                JSON_FACTORY,
                clientSecrets,
                Collections.singleton(CalendarScopes.CALENDAR_READONLY))
                .setAccessType("offline")
                .build();

        // TODO: Retrieve the access token from the session or other secure storage
        // String accessToken = "retrieved-access-token"; // Replace with the actual
        // access token

        String newTokens = checkIfTokenExpired(AccessToken, RefreshToken, ExpiresIn);
        String accessToken = "";
        String expiresIn = "";
        if (!newTokens.isEmpty()) {
            String[] parts = newTokens.split("\\|\\|");
            accessToken = parts[0];
            expiresIn = parts[1];
        } else {
            accessToken = AccessToken;
            expiresIn = ExpiresIn;
        }
        System.out.println("getCalendars AccessToken: " + AccessToken);
        System.out.println("getCalendars RefreshToken: " + RefreshToken);
        System.out.println("getCalendars ExpiresIn: " + ExpiresIn);
        Calendar calendarService = new Calendar.Builder(httpTransport, JSON_FACTORY,
                new GoogleCredential().setAccessToken(accessToken))
                .setApplicationName("BusCatcher")
                .build();

        // Use the Calendar object to make requests to the Google Calendar API
        CalendarList calendarList = calendarService.calendarList().list().setPageToken(null).execute();

        return parseCalendarList(calendarList, accessToken, expiresIn);
    }

    /**
     * Parses the CalendarList object into a JSON string.
     * 
     * @param calendarLists
     * @return
     */
    public String parseCalendarList(CalendarList calendarLists, String AccessToken, String ExpiresIn) {
        List<Map<String, String>> parsedEvents = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        for (CalendarListEntry calendarList : calendarLists.getItems()) {
            Map<String, String> parsedEvent = new HashMap<>();
            parsedEvent.put("backgroundColor", calendarList.getBackgroundColor());
            parsedEvent.put("title", calendarList.getSummary());
            parsedEvent.put("id", calendarList.getId());
            parsedEvent.put("description", calendarList.getDescription());
            if (AccessToken != null && !AccessToken.isEmpty() && ExpiresIn != null && !ExpiresIn.isEmpty()) {
                parsedEvent.put("AccessToken", AccessToken);
                parsedEvent.put("ExpiresIn", ExpiresIn);
            }
            parsedEvents.add(parsedEvent);
        }

        Gson gson = new Gson();
        return gson.toJson(parsedEvents);
    }

    /**
     * Checks if the access token is expired, if it is, refresh it.
     * 
     * @throws GeneralSecurityException
     * @throws IOException
     */
    private String checkIfTokenExpired(String AccessToken, String RefreshToken, String ExpiresIn) throws GeneralSecurityException, IOException {
        // Retrieve the access token, refresh token, and expires_in from the session
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attr.getRequest().getSession(true); // true == allow create

        String accessToken = AccessToken;
        System.out.println("checkIfTokenExpired accessToken: " + accessToken);
        String refreshToken = RefreshToken;
        System.out.println("checkIfTokenExpired refreshToken: " + refreshToken);
        Long expiresIn = Long.parseLong(ExpiresIn);
        System.out.println("checkIfTokenExpired expiresIn: " + expiresIn);
        System.out.println("checkIfTokenExpired session: " + session);
        System.out.println("checkIfTokenExpired session.getId(): " + session.getId());
        System.out.println("checkIfTokenExpired session.getCreationTime(): " + session.getCreationTime());
        System.out.println("checkIfTokenExpired session.getLastAccessedTime(): " + session.getLastAccessedTime());
        // If the access token is expired, refresh it
        if (expiresIn != null && System.currentTimeMillis() / 1000 >= expiresIn) {// if (System.currentTimeMillis() /
                                                                                  // 1000 >= expiresIn) {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                    new InputStreamReader(GoogleAPIController.class.getResourceAsStream(CLIENT_SECRETS_PATH)));
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY,
                    clientSecrets, Collections.singleton(CalendarScopes.CALENDAR_READONLY))
                    .setAccessType("offline")
                    .build();
            AuthorizationCodeTokenRequest tokenRequest = flow.newTokenRequest(refreshToken);
            TokenResponse tokenResponse = tokenRequest.execute();

            // Update the access token and expires_in in the session
            accessToken = tokenResponse.getAccessToken();
            expiresIn = tokenResponse.getExpiresInSeconds() + System.currentTimeMillis() / 1000;

            return accessToken + "||" + expiresIn;
            //GoogleTokens newGoogleTokens = new GoogleTokens(accessToken, refreshToken, expiresIn + System.currentTimeMillis() / 1000);
            //session.setAttribute(token, newGoogleTokens);
        }
        return "";
    }

    /**
     * Retrieves the events from the specified calendar for the current week.
     * <p>
     * The access token is retrieved from the session.
     * </p>
     * <p>
     * The access token is used to make requests to the Google Calendar API.
     * </p>
     * <p>
     * The start and end of the current week are retrieved.
     * </p>
     * <p>
     * The events from the specified calendar for the current week are retrieved.
     * </p>
     * <p>
     * The events are parsed into a JSON string.
     * </p>
     *
     * @param calendarId
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     */
    @GetMapping("/weekEvents")
    public String getWeekEvents(@RequestHeader("AccessToken") String AccessToken, @RequestHeader("RefreshToken") String RefreshToken, @RequestHeader("ExpiresIn") String ExpiresIn, @RequestParam("calendarId") String calendarId)
            throws IOException, GeneralSecurityException {
        // Validate the calendarId
        if (calendarId == null || calendarId.isEmpty()) {
            throw new IllegalArgumentException("Invalid calendarId: calendarId cannot be null or empty");
        }

        // Sanitize the calendarId
        String sanitizedCalendarId = calendarId.replaceAll("[^a-zA-Z0-9-_.@]", "");
        if (!sanitizedCalendarId.equals(calendarId)) {
            throw new IllegalArgumentException("Invalid calendarId: calendarId contains illegal characters");
        }
        //checkIfTokenExpired(authorizationHeader);
        String newTokens = checkIfTokenExpired(AccessToken, RefreshToken, ExpiresIn);
        String accessToken = "";
        String expiresIn = "";
        if (!newTokens.isEmpty()) {
            String[] parts = newTokens.split("\\|\\|");
            accessToken = parts[0];
            expiresIn = parts[1];
        } else {
            accessToken = AccessToken;
            expiresIn = ExpiresIn;
        }

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Calendar calendarService = new Calendar.Builder(httpTransport, JSON_FACTORY,
                new GoogleCredential().setAccessToken(accessToken))
                .setApplicationName("BusCatcher")
                .build();

        // Get the start and end of the current week
        java.util.Calendar now = java.util.Calendar.getInstance();
        now.set(java.util.Calendar.HOUR_OF_DAY, 0);
        now.set(java.util.Calendar.MINUTE, 0);
        now.set(java.util.Calendar.SECOND, 0);
        now.set(java.util.Calendar.MILLISECOND, 0);
        now.set(java.util.Calendar.DAY_OF_WEEK, now.getFirstDayOfWeek());

        DateTime startOfWeek = new DateTime(now.getTimeInMillis());
        now.add(java.util.Calendar.WEEK_OF_YEAR, 1);
        DateTime endOfWeek = new DateTime(now.getTimeInMillis());

        // Get the events from the specified calendar for the current week
        Events events = calendarService.events().list(calendarId)
                .setTimeMin(startOfWeek)
                .setTimeMax(endOfWeek)
                .execute();

        return parseEvents(events);
    }

    /**
     * Parses the Events object into a JSON string.
     * <p>
     * The events are sorted based on the start datetime.
     * </p>
     * 
     * @param events
     * @return
     */
    public String parseEvents(Events events) {
        List<Map<String, String>> parsedEvents = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        for (Event event : events.getItems()) {
            Map<String, String> parsedEvent = new HashMap<>();
            parsedEvent.put("start", sdf.format(new Date(event.getStart().getDateTime().getValue())));
            parsedEvent.put("end", sdf.format(new Date(event.getEnd().getDateTime().getValue())));
            parsedEvent.put("title", event.getSummary());
            parsedEvents.add(parsedEvent);
        }

        // Sort the parsedEvents list based on the start datetime
        Collections.sort(parsedEvents, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> o1, Map<String, String> o2) {
                try {
                    return sdf.parse(o1.get("start")).compareTo(sdf.parse(o2.get("start")));
                } catch (ParseException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        });

        Gson gson = new Gson();
        return gson.toJson(parsedEvents);
    }

    /*
     * TODO: Make an endpoint that receives chosen event ids, and based on them make
     * an API call to ResRobot
     * TODO: to get the departure time and arrival time of the busstation near the
     * location of the user/event.
     */

    /**
     * Logs the user out.
     * <p>
     * Invalidates the session to remove the access token and refresh token.
     * </p>
     * 
     * @return
     */
    @GetMapping("/logout")
    public String logout(@RequestHeader("Authorization") String authorizationHeader) {//TODO
        // Invalidate the session to remove the access token and refresh token
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attr.getRequest().getSession(false); // false == don't allow create
        /*if (session != null) {
            session.invalidate();
        }*/
        String[] parts = authorizationHeader.split(" ");
        if (parts.length != 2 || !"Bearer".equals(parts[0])) {
            throw new IllegalArgumentException("Invalid authorization header. It should be 'Bearer <token>'.");
        }
        // The actual token is the second element of the array
        String token = parts[1];
        String accessToken = (String) session.getAttribute("accessToken||" + token);
        String refreshToken = (String) session.getAttribute("refreshToken||" + token);
        Long expiresIn = (Long) session.getAttribute("expiresIn||" + token);
        if (accessToken != null || refreshToken != null || expiresIn != null) {
            session.removeAttribute("accessToken||" + token);
            session.removeAttribute("refreshToken||" + token);
            session.removeAttribute("expiresIn||" + token);
        }
        return "Successfully logged out";
    }

    // TODO: GOOGLE PLACES API
    // private static final JsonFactory JSON_FACTORY =
    // JacksonFactory.getDefaultInstance();
    @GetMapping("/places/{prompt}")//@GetMapping("/places/{prompt}/{ipbias}")
    public String findPlace(@PathVariable String prompt/*, @PathVariable(name = "ipbias", required = false) String ipBias*/)
            throws IOException, GeneralSecurityException {

        // Set up the HTTP transport and JSON factory
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        HttpRequestFactory requestFactory = httpTransport.createRequestFactory(
                request -> request.setParser(new JsonObjectParser(JSON_FACTORY)));


        // Construct the URL for the Find Place request
        GenericUrl url = new GenericUrl(PLACES_API_URL);
        url.set("input", prompt);
        url.set("inputtype", "textquery");
        url.set("fields", "name,formatted_address,geometry");
        url.set("key", PLACES_API_KEY);

        // Add the ipbias parameter if provided
        /*if (ipBias != null && !ipBias.isEmpty()) {
            url.set("ipbias", ipBias);
        }*/

        // Make the HTTP request using the Google API Client Library
        HttpRequest request = requestFactory.buildGetRequest(url);
        HttpResponse response = request.execute();

        // Parse and return the JSON response
        return response.parseAsString();
    }
}
