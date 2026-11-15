package services;

import models.HotelData;
import java.time.LocalDate;
import java.time.Month;

public class RateEngine {

    /**
     * Container for the result of a rate calculation.
     * These field names match exactly what WebServer.java sends to the browser.
     */
    public static class RateResult {
        public double finalRate;
        public double seasonMultiplier;
        public double eventMultiplier;
        public double occupancyMultiplier;
        public double competitorMultiplier;
        public String eventName;    // null if no event
        public String seasonName;

        public RateResult(double finalRate,
                          double seasonMultiplier,
                          double eventMultiplier,
                          double occupancyMultiplier,
                          double competitorMultiplier,
                          String eventName,
                          String seasonName) {
            this.finalRate = finalRate;
            this.seasonMultiplier = seasonMultiplier;
            this.eventMultiplier = eventMultiplier;
            this.occupancyMultiplier = occupancyMultiplier;
            this.competitorMultiplier = competitorMultiplier;
            this.eventName = eventName;
            this.seasonName = seasonName;
        }
    }

    /**
     * Calculate the recommended daily rate for a given date.
     *
     * @param date                The target date
     * @param competitorsSoldOut  True if local competitors are sold out
     * @return                    A RateResult with the final rate and all multipliers
     */
    public RateResult calculateRecommendedRate(LocalDate date, boolean competitorsSoldOut) {

        // 1. LIVE OCCUPANCY — pulled from BookingService
        int currentOccupancy = BookingService.countActiveBookingsForDate(date);

        // 2. SEASONALITY
        Month month = date.getMonth();
        double seasonMult = getSeasonalityFactor(month);
        String seasonName = getSeasonName(month);

        // 3. LOCAL EVENTS — looked up in HotelData.LOCAL_EVENTS
        String eventName = HotelData.LOCAL_EVENTS.get(date);
        double eventMult = (eventName != null) ? 1.4 : 1.0;

        // 4. OCCUPANCY DEMAND
        double occMult = getOccupancyFactor(currentOccupancy);

        // 5. COMPETITOR AVAILABILITY
        double compMult = competitorsSoldOut ? 1.2 : 1.0;

        // 6. FINAL CALCULATION
        double finalRate = HotelData.BASE_RATE
                         * seasonMult
                         * eventMult
                         * occMult
                         * compMult;

        // Enforce minimum rate
        finalRate = Math.max(finalRate, HotelData.MIN_RATE);

        // Round to 2 decimals for consistency with currency
        finalRate = Math.round(finalRate * 100.0) / 100.0;

        return new RateResult(
            finalRate,
            seasonMult,
            eventMult,
            occMult,
            compMult,
            eventName,
            seasonName
        );
    }

    // ---------- SEASONALITY HELPERS ----------

    private double getSeasonalityFactor(Month month) {
        switch (month) {
            case JUNE:
            case JULY:
            case AUGUST:
                return 1.5;   // Peak (Summer)

            case DECEMBER:
                return 1.3;   // High (Holidays)

            case MAY:
            case SEPTEMBER:
            case OCTOBER:
                return 1.1;   // Shoulder

            default:
                return 0.8;   // Low (discount to fill rooms)
        }
    }

    private String getSeasonName(Month month) {
        switch (month) {
            case JUNE:
            case JULY:
            case AUGUST:
                return "Peak Season";

            case DECEMBER:
                return "High Season (Holidays)";

            case MAY:
            case SEPTEMBER:
            case OCTOBER:
                return "Shoulder Season";

            default:
                return "Low Season";
        }
    }

    // ---------- OCCUPANCY HELPERS ----------

    private double getOccupancyFactor(int occupancy) {
        double rate = (double) occupancy / HotelData.TOTAL_ROOMS;

        if (rate >= 0.9) return 1.3;   // 90%+ full  → high demand
        if (rate >= 0.7) return 1.1;   // 70%+ full  → healthy demand
        if (rate >= 0.4) return 1.0;   // 40%+ full  → normal
        return 0.9;                     // Below 40%  → discount
    }
}