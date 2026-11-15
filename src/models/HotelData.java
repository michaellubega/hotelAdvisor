
package models;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class HotelData {
    public static final int TOTAL_ROOMS = 15;
    public static final double BASE_RATE = 150000.00;
    public static final double MIN_RATE = 80000.00;

    // Local Events (Date -> Event Name)
    public static final Map<LocalDate, String> LOCAL_EVENTS = new HashMap<>();
    public static Object BOOKING_HISTORY;
    static {
        LOCAL_EVENTS.put(LocalDate.of(2026, 7, 15), "Local Music Festival");
        LOCAL_EVENTS.put(LocalDate.of(2026, 7, 16), "Local Music Festival");
        LOCAL_EVENTS.put(LocalDate.of(2026, 11, 10), "Business Conference");
        LOCAL_EVENTS.put(LocalDate.of(2027, 4, 20), "City Marathon");
    }

    // NOTE: The old BOOKING_HISTORY map has been REMOVED.
    // Occupancy is now calculated live in BookingService.java.

   

       
    
    }
