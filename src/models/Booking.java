
package models;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Booking {

    private static int nextId = 1000;

    private int bookingId;
    private String customerUsername;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String roomType;
    private double nightlyRate;
    private double totalCost;
    private boolean isActive;

    public Booking(String customerUsername, LocalDate checkInDate, LocalDate checkOutDate,
                   String roomType, double nightlyRate) {
        this.bookingId = nextId++;
        this.customerUsername = customerUsername;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.roomType = roomType;
        this.nightlyRate = nightlyRate;
        this.isActive = true;

        // Calculate nights (returns long)
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (nights < 1) nights = 1;
        this.totalCost = nights * nightlyRate;
    }

    public int getBookingId() { return bookingId; }
    public String getCustomerUsername() { return customerUsername; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public String getRoomType() { return roomType; }
    public double getNightlyRate() { return nightlyRate; }
    public double getTotalCost() { return totalCost; }
    public boolean isActive() { return isActive; }

    // MUST be 'long' to avoid the LocalDate cannot be converted to int error
    public long getNights() {
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    public void cancel() {
        this.isActive = false;
    }

    @Override
    public String toString() {
        return "Booking #" + bookingId + " | " + roomType + " | "
             + checkInDate + " to " + checkOutDate + " | $" + totalCost;
    }
}