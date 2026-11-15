package ui;

import models.Booking;
import models.User;
import services.BookingService;
import services.RateEngine;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class CustomerDashboard {

    private JFrame frame;
    private final User customer;
    private final RateEngine rateEngine;

    public CustomerDashboard(User customer) {
        UiKit.setup();
        this.customer = customer;
        this.rateEngine = new RateEngine();
        frame = UiKit.frame("Starview Hotel | Guest dashboard", 1180, 760);
        JPanel root = UiKit.page();
        root.add(UiKit.topBar("Guest dashboard", "YOUR STARVIEW STAY, ALL IN ONE PLACE", "WELCOME, " + customer.getFirstName().toUpperCase()), BorderLayout.NORTH);

        JPanel profile = UiKit.card();
        profile.setLayout(new GridLayout(1, 4, 16, 0));
        profile.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(18, 24, 8, 24), BorderFactory.createLineBorder(UiKit.LINE)));
        profile.add(profileItem("GUEST NAME", customer.getFullName()));
        profile.add(profileItem("EMAIL ADDRESS", customer.getEmail()));
        profile.add(profileItem("PHONE NUMBER", customer.getPhone()));
        profile.add(profileItem("LOCATION", customer.getLocation()));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Color.WHITE);
        tabs.setBorder(BorderFactory.createEmptyBorder(10, 18, 12, 18));
        tabs.addTab("  Available rooms  ", createRoomsTab());
        tabs.addTab("  My bookings  ", createBookingsTab());
        tabs.addTab("  Loyalty rewards  ", createLoyaltyTab());

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(18, 22, 12, 22));
        center.add(profile, BorderLayout.NORTH);
        center.add(tabs, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 26, 12, 26));
        footer.add(UiKit.muted("STARVIEW HOTEL  •  KANSANGA"), BorderLayout.WEST);
        JButton logoutBtn = UiKit.button("Sign out", false);
        footer.add(logoutBtn, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        logoutBtn.addActionListener(e -> { frame.dispose(); new RoleSelectionScreen(); });
        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private JPanel profileItem(String label, String value) {
        JPanel p = new JPanel(); p.setOpaque(false); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(label); l.setForeground(UiKit.MUTED); l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        JLabel v = new JLabel(value); v.setForeground(UiKit.INK); v.setFont(new Font("Segoe UI", Font.BOLD, 14));
        p.add(l); p.add(Box.createVerticalStrut(7)); p.add(v); return p;
    }

    // ================================================================
    // TAB 1: AVAILABLE ROOMS (with live prices)
    // ================================================================
    private JPanel createRoomsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(UiKit.BG);
        panel.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

        LocalDate today = LocalDate.now();

        JLabel heading = new JLabel("Available Rooms — Rates for " + today);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 18));
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
        grid.setBackground(UiKit.BG);

        RateEngine.RateResult baseRate = rateEngine.calculateRecommendedRate(today, false);
        double base = baseRate.finalRate;

        grid.add(createRoomCard("Standard Room",
            "Cozy room with queen bed",
            base * 1.0, "🛏"));

        grid.add(createRoomCard("Deluxe Suite",
            "Spacious suite with king bed",
            base * 1.4, "✨"));

        grid.add(createRoomCard("Executive Room",
            "Premium with lounge access",
            base * 1.8, "👑"));

        grid.add(createRoomCard("Family Room",
            "Two connected rooms",
            base * 2.2, "👨‍👩‍👧"));

        panel.add(heading, BorderLayout.NORTH);
        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRoomCard(String name, String desc, double price, String icon) {
        JPanel card = new JPanel(new BorderLayout(14, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiKit.LINE, 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel iconLbl = new JLabel(icon, SwingConstants.CENTER);
        iconLbl.setFont(new Font("Arial", Font.PLAIN, 40));

        JPanel info = new JPanel(new GridLayout(3, 1));
        info.setBackground(Color.WHITE);

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 17));
        nameLbl.setForeground(new Color(44, 62, 80));

        JLabel descLbl = new JLabel(desc);
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLbl.setForeground(Color.GRAY);

        JLabel priceLbl = new JLabel("UGX " + String.format("%,.0f", price) + " / night");
        priceLbl.setFont(new Font("Segoe UI", Font.BOLD, 19));
        priceLbl.setForeground(UiKit.GOLD_DARK);

        info.add(nameLbl);
        info.add(descLbl);
        info.add(priceLbl);

        JButton bookBtn = UiKit.button("Reserve this room", true);

        bookBtn.addActionListener(e -> bookRoom(name, price));

        card.add(iconLbl, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(bookBtn, BorderLayout.SOUTH);
        return card;
    }

    // ================================================================
    // BOOK A ROOM
    // ================================================================
    private void bookRoom(String roomName, double price) {
        // Simple dialog to pick dates
        JSpinner inSpinner = new JSpinner(new SpinnerDateModel());
        inSpinner.setEditor(new JSpinner.DateEditor(inSpinner, "yyyy-MM-dd"));
        JSpinner outSpinner = new JSpinner(new SpinnerDateModel());
        outSpinner.setEditor(new JSpinner.DateEditor(outSpinner, "yyyy-MM-dd"));

        JPanel dialog = new JPanel(new GridLayout(2, 2, 8, 8));
        dialog.add(new JLabel("Check-in:"));
        dialog.add(inSpinner);
        dialog.add(new JLabel("Check-out:"));
        dialog.add(outSpinner);

        int result = JOptionPane.showConfirmDialog(frame, dialog,
            "Book " + roomName + " at UGX " + String.format("%,.0f", price) + "/night",
            JOptionPane.OK_CANCEL_OPTION);

        if (result != JOptionPane.OK_OPTION) return;

        LocalDate checkIn = toLocalDate((java.util.Date) inSpinner.getValue());
        LocalDate checkOut = toLocalDate((java.util.Date) outSpinner.getValue());

        if (!checkOut.isAfter(checkIn)) {
            JOptionPane.showMessageDialog(frame,
                "Check-out must be after check-in.",
                "Invalid Dates", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Booking b = BookingService.createBooking(
            customer.getUsername(), checkIn, checkOut, roomName, price);

        if (b == null) {
            JOptionPane.showMessageDialog(frame,
                "Hotel is fully booked for those dates.",
                "Booking Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(frame,
            "Booking Confirmed!\n\n" +
            "Booking #" + b.getBookingId() + "\n" +
            "Room: " + b.getRoomType() + "\n" +
            "Nights: " + b.getNights() + "\n" +
            "Total: UGX " + String.format("%,.0f", b.getTotalCost()),
            "Success", JOptionPane.INFORMATION_MESSAGE);

        // Refresh the dashboard so the new booking shows up
        frame.dispose();
        new CustomerDashboard(customer);
    }

    private LocalDate toLocalDate(java.util.Date d) {
        return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    // ================================================================
    // TAB 2: MY BOOKINGS
    // ================================================================
    private JPanel createBookingsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel heading = new JLabel("Your Active Bookings");
        heading.setFont(new Font("Arial", Font.BOLD, 15));
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        List<Booking> bookings = BookingService.getBookingsForCustomer(customer.getUsername());

        if (bookings.isEmpty()) {
            JLabel empty = new JLabel("No active bookings yet.");
            empty.setFont(new Font("Arial", Font.ITALIC, 13));
            empty.setForeground(Color.GRAY);
            list.add(empty);
        } else {
            for (Booking b : bookings) {
                list.add(createBookingCard(b));
                list.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBookingCard(Booking b) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JLabel info = new JLabel("<html>" +
            "<b>Booking #" + b.getBookingId() + "</b> — " + b.getRoomType() + "<br>" +
            "Check-in: <b>" + b.getCheckInDate() + "</b> &nbsp;|&nbsp; " +
            "Check-out: <b>" + b.getCheckOutDate() + "</b> (" + b.getNights() + " nights)<br>" +
            "Rate: UGX " + String.format("%,.0f", b.getNightlyRate()) + "/night &nbsp;|&nbsp; " +
            "<b style='color:#2980b9;'>Total: UGX " + String.format("%,.0f", b.getTotalCost()) + "</b>" +
            "</html>");
        info.setFont(new Font("Arial", Font.PLAIN, 13));

        JButton cancelBtn = UiKit.button("Cancel booking", false);
        cancelBtn.setForeground(new Color(170, 65, 65));

        cancelBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(frame,
                "Cancel Booking #" + b.getBookingId() + "?",
                "Confirm", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                if (BookingService.cancelBooking(b.getBookingId())) {
                    JOptionPane.showMessageDialog(frame,
                        "Booking cancelled.",
                        "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                    frame.dispose();
                    new CustomerDashboard(customer);
                }
            }
        });

        card.add(info, BorderLayout.CENTER);
        card.add(cancelBtn, BorderLayout.EAST);
        return card;
    }

    // ================================================================
    // TAB 3: LOYALTY
    // ================================================================
    private JPanel createLoyaltyTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel title = new JLabel("⭐ Loyalty Program");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title, gbc);

        gbc.gridy++;
        JLabel points = new JLabel("Current Points: " + customer.getLoyaltyPoints());
        points.setFont(new Font("Arial", Font.BOLD, 16));
        points.setForeground(new Color(39, 174, 96));
        panel.add(points, gbc);

        gbc.gridy++;
        JProgressBar bar = new JProgressBar(0, 10000);
        bar.setValue(customer.getLoyaltyPoints());
        bar.setStringPainted(true);
        bar.setPreferredSize(new Dimension(320, 25));
        panel.add(bar, gbc);

        gbc.gridy++;
        JLabel rewards = new JLabel("<html><br><b>Rewards:</b><br>" +
            "• 500 points → Free breakfast<br>" +
            "• 2,000 points → Room upgrade<br>" +
            "• 5,000 points → Free night stay<br>" +
            "• 10,000 points → VIP membership</html>");
        rewards.setFont(new Font("Arial", Font.PLAIN, 13));
        panel.add(rewards, gbc);

        return panel;
    }
}