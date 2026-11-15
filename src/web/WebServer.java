package web;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import models.Booking;
import models.User;
import services.BookingService;
import services.RateEngine;
import services.UserRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebServer {

    private static final Gson gson = new Gson();
    private static final RateEngine rateEngine = new RateEngine();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", WebServer::serveStatic);
        server.createContext("/api/signup", WebServer::handleSignup);
        server.createContext("/api/login", WebServer::handleLogin);
        server.createContext("/api/rate", WebServer::handleRate);
        server.createContext("/api/rooms", WebServer::handleRooms);
        server.createContext("/api/bookings", WebServer::handleBookings);
        server.createContext("/api/book", WebServer::handleBook);
        server.createContext("/api/cancel", WebServer::handleCancel);

        server.setExecutor(null);
        server.start();

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   ✅ Hotel Advisor Server Started        ║");
        System.out.println("║                                          ║");
        System.out.println("║   Open your browser at:                  ║");
        System.out.println("║   http://localhost:8080                  ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }

    private static void serveStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";

        InputStream is = WebServer.class.getResourceAsStream("/web" + path);
        if (is == null) {
            sendRaw(exchange, 404, "text/plain",
                    ("Not Found: " + path).getBytes(StandardCharsets.UTF_8));
            return;
        }

        byte[] bytes = is.readAllBytes();
        String contentType = path.endsWith(".css") ? "text/css"
                            : path.endsWith(".js")  ? "application/javascript"
                            : (path.endsWith(".jpg") || path.endsWith(".jpeg")) ? "image/jpeg"
                            : path.endsWith(".png") ? "image/png"
                            : "text/html";

        sendRaw(exchange, 200, contentType + "; charset=UTF-8", bytes);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> readJsonMap(HttpExchange exchange) throws IOException {
        String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (raw.isEmpty()) return new HashMap<>();
        return gson.fromJson(raw, Map.class);
    }

    private static void handleSignup(HttpExchange exchange) throws IOException {
        Map<String, String> body = readJsonMap(exchange);
        String firstName = body.get("firstName");
        String lastName = body.get("lastName");
        String email = body.get("email");
        String phone = body.get("phone");
        String location = body.get("location");
        String password = body.get("password");

        String username = email.split("@")[0] + (System.currentTimeMillis() % 1000);

        User newUser = new User(username, password, firstName, lastName, email, phone, location,
                                User.Role.CUSTOMER, 500);

        boolean ok = UserRepository.addUser(newUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", ok);
        if (ok) {
            response.put("username", username);
            response.put("fullName", newUser.getFullName());
            response.put("email", email);
            response.put("phone", phone);
            response.put("location", location);
            response.put("loyaltyPoints", 500);
            response.put("role", "CUSTOMER");
        } else {
            response.put("message", "Email already registered");
        }
        sendJson(exchange, response);
    }

    private static void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, String> body = readJsonMap(exchange);
        String identifier = body.get("identifier");
        String password = body.get("password");
        String roleStr = body.get("role");

        User.Role role = "STAFF".equalsIgnoreCase(roleStr) ? User.Role.STAFF : User.Role.CUSTOMER;
        User user = UserRepository.authenticate(identifier, password, role);

        Map<String, Object> response = new HashMap<>();
        if (user != null) {
            response.put("success", true);
            response.put("username", user.getUsername());
            response.put("fullName", user.getFullName());
            response.put("email", user.getEmail());
            response.put("phone", user.getPhone());
            response.put("location", user.getLocation());
            response.put("role", user.getRole().toString());
            response.put("loyaltyPoints", user.getLoyaltyPoints());
        } else {
            response.put("success", false);
            response.put("message", "Invalid credentials for this role");
        }
        sendJson(exchange, response);
    }

    private static void handleRate(HttpExchange exchange) throws IOException {
        Map<String, String> body = readJsonMap(exchange);
        LocalDate date = LocalDate.parse(body.get("date"));
        boolean compSoldOut = Boolean.parseBoolean(body.getOrDefault("competitorsSoldOut", "false"));

        RateEngine.RateResult r = rateEngine.calculateRecommendedRate(date, compSoldOut);

        Map<String, Object> response = new HashMap<>();
        response.put("finalRate", r.finalRate);
        response.put("seasonMultiplier", r.seasonMultiplier);
        response.put("eventMultiplier", r.eventMultiplier);
        response.put("occupancyMultiplier", r.occupancyMultiplier);
        response.put("competitorMultiplier", r.competitorMultiplier);
        response.put("seasonName", r.seasonName);
        response.put("eventName", r.eventName);
        sendJson(exchange, response);
    }

    private static void handleRooms(HttpExchange exchange) throws IOException {
        Map<String, String> body = readJsonMap(exchange);
        LocalDate date = LocalDate.parse(body.get("date"));
        boolean compSoldOut = Boolean.parseBoolean(body.getOrDefault("competitorsSoldOut", "false"));

        RateEngine.RateResult r = rateEngine.calculateRecommendedRate(date, compSoldOut);
        double baseRate = r.finalRate;

        String[][] rooms = {
            { "standard",  "Standard Room",  "Cozy room with queen bed",     "1.0" },
            { "deluxe",    "Deluxe Suite",   "Spacious suite with king bed", "1.4" },
            { "executive", "Executive Room", "Premium with lounge access",   "1.8" },
            { "family",    "Family Room",    "Two connected rooms",          "2.2" }
        };

        List<Map<String, Object>> result = new ArrayList<>();
        for (String[] room : rooms) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", room[0]);
            m.put("name", room[1]);
            m.put("desc", room[2]);
            m.put("price", baseRate * Double.parseDouble(room[3]));
            result.add(m);
        }
        sendJson(exchange, result);
    }

    private static void handleBookings(HttpExchange exchange) throws IOException {
        Map<String, String> body = readJsonMap(exchange);
        String username = body.get("username");
        List<Booking> bookings = BookingService.getBookingsForCustomer(username);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Booking b : bookings) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getBookingId());
            m.put("roomType", b.getRoomType());
            m.put("checkIn", b.getCheckInDate().toString());
            m.put("checkOut", b.getCheckOutDate().toString());
            m.put("nights", b.getNights());
            m.put("rate", b.getNightlyRate());
            m.put("total", b.getTotalCost());
            result.add(m);
        }
        sendJson(exchange, result);
    }

    private static void handleBook(HttpExchange exchange) throws IOException {
        Map<String, String> body = readJsonMap(exchange);
        String username = body.get("username");
        LocalDate checkIn = LocalDate.parse(body.get("checkIn"));
        LocalDate checkOut = LocalDate.parse(body.get("checkOut"));
        String roomType = body.get("roomType");
        double rate = Double.parseDouble(body.get("rate"));

        Booking b = BookingService.createBooking(username, checkIn, checkOut, roomType, rate);

        Map<String, Object> response = new HashMap<>();
        if (b != null) {
            response.put("success", true);
            response.put("bookingId", b.getBookingId());
            response.put("totalCost", b.getTotalCost());
            response.put("nights", b.getNights());
        } else {
            response.put("success", false);
            response.put("message", "Hotel is fully booked for those dates");
        }
        sendJson(exchange, response);
    }

    private static void handleCancel(HttpExchange exchange) throws IOException {
        Map<String, String> body = readJsonMap(exchange);
        int bookingId = (int) Double.parseDouble(body.get("bookingId"));
        boolean ok = BookingService.cancelBooking(bookingId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", ok);
        sendJson(exchange, response);
    }

    private static void sendJson(HttpExchange exchange, Object obj) throws IOException {
        String json = gson.toJson(obj);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        sendRaw(exchange, 200, "application/json; charset=UTF-8",
                json.getBytes(StandardCharsets.UTF_8));
    }

    private static void sendRaw(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
