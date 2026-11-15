package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;

/** Desktop landing page for authorised hotel team members. Customers use the public website. */
public class RoleSelectionScreen {
    private final JFrame frame;
    private static final String CUSTOMER_URL = "http://localhost:8080";

    public RoleSelectionScreen() {
        UiKit.setup();
        frame = UiKit.frame("Starview Hotel | Team access", 1120, 720);
        frame.setMinimumSize(new Dimension(900, 620));
        JPanel root = new JPanel(new GridLayout(1, 2));
        JPanel visual = UiKit.imagePanel(560, 720);
        visual.setLayout(new BorderLayout());
        JPanel visualText = new JPanel(); visualText.setOpaque(false);
        visualText.setLayout(new BoxLayout(visualText, BoxLayout.Y_AXIS));
        visualText.setBorder(new EmptyBorder(0, 42, 55, 35));
        JLabel brand = UiKit.brand(); brand.setHorizontalAlignment(SwingConstants.LEFT);
        JLabel statement = new JLabel("A stay worth\nremembering.");
        statement.setForeground(Color.WHITE); statement.setFont(new Font("Segoe UI", Font.BOLD, 38));
        JLabel caption = new JLabel("Thoughtful hospitality. Smarter hotel operations.");
        caption.setForeground(new Color(240,240,240)); caption.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        visualText.add(Box.createVerticalGlue()); visualText.add(brand);
        visualText.add(Box.createVerticalStrut(22)); visualText.add(statement);
        visualText.add(Box.createVerticalStrut(14)); visualText.add(caption);
        visual.add(visualText, BorderLayout.CENTER);

        JPanel right = new JPanel(new GridBagLayout()); right.setBackground(UiKit.BG);
        JPanel content = new JPanel(); content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(42, 48, 42, 48));
        JLabel eyebrow = new JLabel("AUTHORISED TEAM ACCESS");
        eyebrow.setForeground(UiKit.GOLD_DARK); eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 12));
        content.add(eyebrow); content.add(Box.createVerticalStrut(12));
        content.add(UiKit.title("Staff & management"));
        content.add(Box.createVerticalStrut(9));
        content.add(UiKit.muted("Choose the workspace you need. Customer bookings are handled on the hotel website."));
        content.add(Box.createVerticalStrut(30));
        content.add(roleCard("Staff workspace", "Review daily bookings, occupancy and room-rate guidance.", "STAFF SIGN IN", () -> openStaff()));
        content.add(Box.createVerticalStrut(16));
        content.add(roleCard("Management workspace", "Review hotel-wide booking and pricing information.", "MANAGEMENT SIGN IN", () -> openStaff()));
        content.add(Box.createVerticalStrut(24));
        JButton customer = UiKit.button("Open customer website  ↗", false);
        customer.setAlignmentX(Component.LEFT_ALIGNMENT); customer.setMaximumSize(new Dimension(460, 46));
        customer.addActionListener(e -> openCustomerWebsite());
        content.add(customer); content.add(Box.createVerticalStrut(12));
        JLabel hint = UiKit.muted("The management option currently uses the shared staff demonstration account; separate manager permissions are a future enhancement.");
        hint.setAlignmentX(Component.LEFT_ALIGNMENT); content.add(hint);
        content.add(Box.createVerticalGlue());
        JLabel foot = UiKit.muted("STARVIEW HOTEL  •  KANSANGA, KAMPALA");
        foot.setAlignmentX(Component.LEFT_ALIGNMENT); content.add(foot);
        right.add(content); root.add(visual); root.add(right);
        frame.setContentPane(root); frame.setVisible(true);
    }

    private void openStaff() { frame.dispose(); new LoginScreen(); }

    private void openCustomerWebsite() {
        try {
            if (!Desktop.isDesktopSupported()) throw new IllegalStateException("Desktop browser integration is unavailable.");
            Desktop.getDesktop().browse(new URI(CUSTOMER_URL));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Start the web server first, then open " + CUSTOMER_URL + " in your browser.", "Customer website", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private JPanel roleCard(String title, String desc, String action, Runnable run) {
        JPanel p = UiKit.card(); p.setLayout(new BorderLayout(15, 10));
        p.setMaximumSize(new Dimension(470, 132)); p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel txt = new JPanel(); txt.setOpaque(false); txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title); t.setFont(new Font("Segoe UI", Font.BOLD, 18)); t.setForeground(UiKit.INK);
        JLabel d = new JLabel("<html><div style='width:300px'>" + desc + "</div></html>");
        d.setForeground(UiKit.MUTED); d.setFont(UiKit.BODY);
        txt.add(t); txt.add(Box.createVerticalStrut(8)); txt.add(d);
        JButton b = UiKit.button(action, true); b.addActionListener(e -> run.run());
        p.add(txt, BorderLayout.CENTER); p.add(b, BorderLayout.SOUTH); return p;
    }
}
