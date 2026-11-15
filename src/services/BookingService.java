package services;

import models.Booking;
import models.HotelData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BookingService {

    // Simulated database of all bookings
    private static final List<Booking> allBookings = new ArrayList<>();

    // Pre-populate with a sample booking for John Doe
    static {
        allBookings.add(new Booking(
            "john",
            LocalDate.now().plusDays(7),
            LocalDate.now().plusDays(10),
            "Deluxe Suite",
            210.00
        ));
    }

    /**
     * Create a new booking for the given customer.
     */
    public static synchronized Booking createBooking(String username, LocalDate checkIn, LocalDate checkOut,
                                         String roomType, double rate) {
        if (username == null || username.trim().isEmpty()
                || checkIn == null || checkOut == null
                || roomType == null || roomType.trim().isEmpty()
                || !checkOut.isAfter(checkIn)
                || checkIn.isBefore(LocalDate.now())
                || !Double.isFinite(rate) || rate <= 0) {
            return null;
        }

        // Enforce the hotel's physical inventory for every night of the stay.
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            if (countActiveBookingsForDate(night) >= HotelData.TOTAL_ROOMS) {
                return null;
            }
        }

        Booking booking = new Booking(username.trim(), checkIn, checkOut, roomType.trim(), rate);
        allBookings.add(booking);
        return booking;
    }

    /**
     * Get all ACTIVE bookings for a specific customer.
     * @param username
     */
    public static synchronized List<Booking> getBookingsForCustomer(String username) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : allBookings) {
            if (b.getCustomerUsername().equals(username) && b.isActive()) {
                result.add(b);
            }
        }
        return result;
    }

    /**
     * Cancel a booking by its ID. Returns true if successful.
     * @param bookingId
     * @return 
     */
    public static synchronized boolean cancelBooking(int bookingId) {
        for (Booking b : allBookings) {
            if (b.getBookingId() == bookingId && b.isActive()) {
                b.cancel();
                return true;
            }
        }
        return false;
    }

    /**
     * Count how many rooms are booked for a given date (used for occupancy).
     */
    public static synchronized int countActiveBookingsForDate(LocalDate date) {
        int count = 0;
        for (Booking b : allBookings) {
            if (b.isActive() 
                && !date.isBefore(b.getCheckInDate()) 
                && date.isBefore(b.getCheckOutDate())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get all bookings (for staff view).
     */
    public static synchronized List<Booking> getAllBookings() {
        return new ArrayList<>(allBookings);
    }
}
