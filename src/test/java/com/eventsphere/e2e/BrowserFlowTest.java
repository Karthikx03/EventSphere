package com.eventsphere.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the real running server over HTTP exactly like a browser does: session cookies,
 * CSRF tokens from the HTML forms, form posts and redirects. Every request also carries the
 * X-Forwarded-* headers a reverse proxy (GitHub Codespaces, Render, nginx) adds, so the
 * app is tested the way it is deployed.
 */
// own in-memory database, so shutting this context down never touches the other test contexts
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:e2e-flows;DB_CLOSE_DELAY=-1")
@DirtiesContext
class BrowserFlowTest {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"");
    private static final String PUBLIC_HOST = "demo.example.test";

    @LocalServerPort
    private int port;

    private HttpClient http;

    @BeforeEach
    void newBrowser() {
        http = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    // ------------------------------------------------------------------ flows

    @Test
    void studentCanLogInBookViewCancelAndLogOut() throws Exception {
        login("student@eventsphere.com", "student123");

        Page events = get("/events");
        assertOk(events);
        assertThat(events.body).contains("Log out").contains("student@eventsphere.com").contains("My tickets");

        String eventPath = firstMatch(events.body, "href=\"(/events/\\d+)\"");
        Page detail = get(eventPath);
        assertOk(detail);
        assertThat(detail.body).contains("Book now");

        Page booked = post(eventPath + "/book", Map.of("_csrf", csrf(detail), "seats", "2"));
        assertRedirect(booked, "/bookings");

        Page tickets = get("/bookings");
        assertOk(tickets);
        assertThat(tickets.body).contains("EVS-").contains("2 seat(s)").contains("CONFIRMED").contains("Booked!");

        // booking the same event twice shows a friendly error instead of crashing
        Page again = post(eventPath + "/book", Map.of("_csrf", csrf(get(eventPath)), "seats", "1"));
        assertRedirect(again, eventPath);
        assertThat(get(eventPath).body).contains("already have a booking");

        String cancelPath = firstMatch(tickets.body, "action=\"(/bookings/\\d+/cancel)\"");
        Page cancelled = post(cancelPath, Map.of("_csrf", csrf(tickets)));
        assertRedirect(cancelled, "/bookings");
        assertThat(get("/bookings").body).contains("CANCELLED").contains("Booking cancelled");

        Page home = get("/");
        Page loggedOut = post("/logout", Map.of("_csrf", csrf(home)));
        assertRedirect(loggedOut, "/?logout");
        assertThat(get("/events").body).contains("Log in").doesNotContain("Log out");
    }

    @Test
    void newUserCanRegisterAndLogIn() throws Exception {
        Page form = get("/register");
        assertOk(form);
        Page registered = post("/register", Map.of("_csrf", csrf(form),
                "fullName", "Asha Verma", "email", "asha@example.com",
                "password", "secret123", "confirmPassword", "secret123"));
        assertRedirect(registered, "/login");
        assertThat(get("/login").body).contains("Account created");

        login("asha@example.com", "secret123");
        assertThat(get("/bookings").body).contains("You haven't booked anything yet");
    }

    @Test
    void registrationShowsValidationErrors() throws Exception {
        Page form = get("/register");
        Page result = post("/register", Map.of("_csrf", csrf(form),
                "fullName", "", "email", "not-an-email", "password", "123", "confirmPassword", "456"));
        assertOk(result);
        assertThat(result.body).contains("Please enter your name").contains("valid email").contains("6 to 64");

        Page mismatch = post("/register", Map.of("_csrf", csrf(result),
                "fullName", "Ravi", "email", "ravi@example.com", "password", "secret123", "confirmPassword", "other123"));
        assertThat(mismatch.body).contains("Passwords do not match");
    }

    @Test
    void wrongPasswordShowsError() throws Exception {
        Page form = get("/login");
        Page result = post("/login", Map.of("_csrf", csrf(form),
                "username", "student@eventsphere.com", "password", "wrong-password"));
        assertRedirect(result, "/login?error");
        assertThat(get("/login?error").body).contains("Invalid email or password");
    }

    @Test
    void protectedPagesSendAnonymousVisitorsToLogin() throws Exception {
        assertRedirect(get("/bookings"), "/login");
        assertRedirect(get("/admin"), "/login");
    }

    @Test
    void loginReturnsToThePageTheUserWanted() throws Exception {
        assertRedirect(get("/bookings"), "/login");
        Page form = get("/login");
        Page result = post("/login", Map.of("_csrf", csrf(form),
                "username", "student@eventsphere.com", "password", "student123"));
        assertThat(result.status).isEqualTo(302);
        assertThat(path(result.location)).startsWith("/bookings");
        assertOk(get(path(result.location)));
    }

    @Test
    void studentCannotOpenAdminPages() throws Exception {
        login("student@eventsphere.com", "student123");
        Page admin = get("/admin");
        assertThat(admin.status).isEqualTo(403);
        assertThat(admin.body).contains("permission");
    }

    @Test
    void adminCanCreateEditViewAttendeesAndDeleteEvents() throws Exception {
        login("admin@eventsphere.com", "admin123");

        Page dashboard = get("/admin");
        assertOk(dashboard);
        assertThat(dashboard.body).contains("Admin dashboard").contains("Revenue");

        Page newForm = get("/admin/events/new");
        assertOk(newForm);
        String start = LocalDateTime.now().plusDays(30).withSecond(0).withNano(0)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("_csrf", csrf(newForm));
        fields.put("title", "Robotics Expo");
        fields.put("description", "Student robots on show");
        fields.put("category", "TECH");
        fields.put("venue", "Hall 7");
        fields.put("startTime", start);
        fields.put("capacity", "80");
        fields.put("price", "25.50");
        assertRedirect(post("/admin/events", fields), "/admin");

        Page afterCreate = get("/admin");
        assertThat(afterCreate.body).contains("Robotics Expo").contains("created");
        String editPath = firstMatch(afterCreate.body, "href=\"(/admin/events/\\d+/edit)\"[^>]*title=\"Edit\"",
                "Robotics Expo");

        Page editForm = get(editPath);
        assertOk(editForm);
        assertThat(editForm.body).contains("value=\"Robotics Expo\"").contains("value=\"" + start + "\"");
        String eventBase = editPath.replace("/edit", "");
        fields.put("_csrf", csrf(editForm));
        fields.put("title", "Robotics Expo 2.0");
        assertRedirect(post(eventBase, fields), "/admin");
        assertThat(get("/admin").body).contains("Robotics Expo 2.0");

        Page attendees = get(eventBase + "/bookings");
        assertOk(attendees);
        assertThat(attendees.body).contains("No bookings yet");

        assertThat(get(eventBase.replace("/admin", "")).body).contains("Robotics Expo 2.0").contains("25.50");

        assertRedirect(post(eventBase + "/delete", Map.of("_csrf", csrf(get("/admin")))), "/admin");
        assertThat(get("/admin").body).doesNotContain("Robotics Expo 2.0").contains("Event deleted");
    }

    @Test
    void adminFormShowsValidationErrors() throws Exception {
        login("admin@eventsphere.com", "admin123");
        Page form = get("/admin/events/new");
        Page result = post("/admin/events", Map.of("_csrf", csrf(form), "title", "", "description", "",
                "venue", "", "startTime", "2001-01-01T10:00", "capacity", "0", "price", "-1"));
        assertOk(result);
        assertThat(result.body).contains("Title is required").contains("must be in the future")
                .contains("Capacity must be at least 1").contains("Price cannot be negative");
    }

    @Test
    void unknownEventShowsFriendlyNotFoundPage() throws Exception {
        Page page = get("/events/999999");
        assertThat(page.status).isEqualTo(404);
        assertThat(page.body).contains("404").contains("Back to events");
    }

    @Test
    void searchAndCategoryFilterWork() throws Exception {
        assertThat(get("/events?q=hackathon").body).contains("CodeSprint").doesNotContain("Acoustic Evening");
        assertThat(get("/events?category=MUSIC").body).contains("Acoustic Evening").doesNotContain("CodeSprint");
        assertThat(get("/events?q=zzzz-no-match").body).contains("No events match");
    }

    @Test
    void pricesUseTheRupeeSign() throws Exception {
        Page events = get("/events");
        assertThat(events.contentType).containsIgnoringCase("charset=UTF-8");
        assertThat(events.body).contains("\u20B9149.00").doesNotContain("\u00E2\u0082\u00B9");
    }

    // ------------------------------------------------- reported issues (regression tests)

    @Test
    void malformedEventIdShowsFriendly404WithoutStackTrace() throws Exception {
        Page page = get("/events/abc");
        assertThat(page.status).isEqualTo(404);
        assertThat(page.contentType).containsIgnoringCase("text/html");
        assertThat(page.body).contains("That page does not exist").contains("Back to events")
                .doesNotContain("Exception").doesNotContain("at org.springframework").doesNotContain("trace");
    }

    @Test
    void unknownUrlShowsFriendly404() throws Exception {
        // logged-out visitors are sent to the login page first (secure by default), so check as a user
        login("student@eventsphere.com", "student123");
        Page page = get("/no-such-page");
        assertThat(page.status).isEqualTo(404);
        assertThat(page.body).contains("That page does not exist").doesNotContain("Exception");
    }

    @Test
    void categoryFilterAcceptsAnyCapitalisationAndIgnoresUnknownValues() throws Exception {
        for (String value : new String[]{"Seminar", "seminar", "SEMINAR"}) {
            Page page = get("/events?category=" + value);
            assertOk(page);
            assertThat(page.body).contains("Resume &amp; Interview Bootcamp").doesNotContain("CodeSprint");
        }
        Page tech = get("/events?category=Tech+Talk");
        assertOk(tech);
        assertThat(tech.body).contains("AI in Everyday Life").doesNotContain("CodeSprint");

        Page unknown = get("/events?category=Bogus");
        assertOk(unknown);
        assertThat(unknown.body).contains("7 event(s) found");
    }

    @Test
    void loginPageDoesNotShowDemoPasswordsByDefault() throws Exception {
        Page page = get("/login");
        assertThat(page.body).doesNotContain("admin123").doesNotContain("student123").doesNotContain("Demo accounts");
    }

    @Test
    void signUpFormValidatesInTheBrowser() throws Exception {
        String body = get("/register").body;
        assertThat(body).doesNotContain("novalidate");
        assertThat(Pattern.compile("<input[^>]*required").matcher(body).results().count()).isEqualTo(4);
        assertThat(body).contains("minlength=\"6\"").contains("type=\"email\"");
    }

    @Test
    void emptyPasswordShowsASingleError() throws Exception {
        Page form = get("/register");
        Page result = post("/register", Map.of("_csrf", csrf(form), "fullName", "Meera", "email", "meera@example.com",
                "password", "", "confirmPassword", ""));
        assertOk(result);
        assertThat(result.body).contains("Password must be 6 to 64 characters").doesNotContain("Please choose a password");
    }

    @Test
    void lettersInANumberFieldGiveAFriendlyMessage() throws Exception {
        login("admin@eventsphere.com", "admin123");
        Page form = get("/admin/events/new");
        Page result = post("/admin/events", Map.of("_csrf", csrf(form), "title", "T", "description", "D",
                "category", "TECH", "venue", "V", "startTime", "2099-01-01T10:00", "capacity", "lots", "price", "abc"));
        assertOk(result);
        assertThat(result.body).contains("Please enter a whole number").contains("Please enter a valid amount")
                .doesNotContain("Failed to convert");
    }

    @Test
    void unbookedEventsShowAvailabilityInsteadOfAnEmptyBar() throws Exception {
        String body = get("/events").body;
        assertThat(body).contains("All 250 seats available").doesNotContain("progress-bar");
    }

    @Test
    void footerHasNoTechStackFiller() throws Exception {
        assertThat(get("/").body).doesNotContain("Built with Spring Boot");
    }

    // ---------------------------------------------------------------- helpers

    private void login(String email, String password) throws Exception {
        Page form = get("/login");
        assertOk(form);
        Page result = post("/login", Map.of("_csrf", csrf(form), "username", email, "password", password));
        assertThat(result.status).as("login status, body: %s", result.body).isEqualTo(302);
        assertThat(result.location).as("login must redirect to the public https address")
                .startsWith("https://" + PUBLIC_HOST + "/");
        assertThat(path(result.location)).doesNotContain("error");
    }

    private record Page(int status, String location, String contentType, String body) {
    }

    private Page get(String path) throws IOException, InterruptedException {
        return send(request(path).GET().build());
    }

    private Page post(String path, Map<String, String> form) throws IOException, InterruptedException {
        String body = form.entrySet().stream()
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
        return send(request(path)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Origin", "https://" + PUBLIC_HOST)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "text/html,application/xhtml+xml")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", PUBLIC_HOST)
                .header("X-Forwarded-Port", "443");
    }

    private Page send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> r = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Page(r.statusCode(), r.headers().firstValue("Location").orElse(null),
                r.headers().firstValue("Content-Type").orElse(""), r.body());
    }

    private static void assertOk(Page page) {
        assertThat(page.status).as("expected 200 but got %s, body: %s", page.status, page.body).isEqualTo(200);
    }

    private static void assertRedirect(Page page, String expectedPath) {
        assertThat(page.status).as("expected redirect, body: %s", page.body).isEqualTo(302);
        assertThat(page.location).as("redirect must stay on the public https address")
                .startsWith("https://" + PUBLIC_HOST + "/");
        assertThat(path(page.location)).isEqualTo(expectedPath);
    }

    /** Path plus query of an absolute URL, e.g. https://host/login?error -> /login?error */
    private static String path(String url) {
        URI uri = URI.create(url);
        return uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
    }

    private static String csrf(Page page) {
        return firstMatch(page.body, CSRF.pattern());
    }

    private static String firstMatch(String html, String regex) {
        Matcher m = Pattern.compile(regex).matcher(html);
        assertThat(m.find()).as("pattern %s not found in page", regex).isTrue();
        return m.group(1);
    }

    /** First match that appears after the given marker text. */
    private static String firstMatch(String html, String regex, String after) {
        int from = html.indexOf(after);
        assertThat(from).as("%s not found in page", after).isGreaterThanOrEqualTo(0);
        return firstMatch(html.substring(from), regex);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
