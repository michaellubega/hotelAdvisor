package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;
import javax.imageio.ImageIO;

/** Shared visual system for the Starview Hotel desktop application. */
public final class UiKit {
    public static final Color NAVY = new Color(20, 35, 55);
    public static final Color NAVY_LIGHT = new Color(31, 53, 78);
    public static final Color GOLD = new Color(201, 163, 91);
    public static final Color GOLD_DARK = new Color(157, 119, 54);
    public static final Color INK = new Color(39, 49, 61);
    public static final Color MUTED = new Color(112, 123, 136);
    public static final Color BG = new Color(246, 247, 249);
    public static final Color LINE = new Color(226, 230, 235);
    public static final Color WHITE = Color.WHITE;
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font MEDIUM = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font HEADING = new Font("Segoe UI", Font.BOLD, 27);

    private UiKit() {}

    public static void setup() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        UIManager.put("Button.font", MEDIUM);
        UIManager.put("Label.font", BODY);
        UIManager.put("TextField.font", BODY);
        UIManager.put("PasswordField.font", BODY);
        UIManager.put("TabbedPane.font", MEDIUM);
        UIManager.put("Table.font", BODY);
        UIManager.put("TableHeader.font", MEDIUM);
        UIManager.put("OptionPane.messageFont", BODY);
        UIManager.put("OptionPane.buttonFont", MEDIUM);
    }

    public static JFrame frame(String title, int width, int height) {
        JFrame f = new JFrame(title);
        f.setSize(width, height);
        f.setMinimumSize(new Dimension(900, 620));
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLocationRelativeTo(null);
        f.getContentPane().setBackground(BG);
        return f;
    }

    public static JPanel page() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        return p;
    }

    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LINE), new EmptyBorder(22, 24, 22, 24)));
        return p;
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text); l.setFont(HEADING); l.setForeground(INK); return l;
    }
    public static JLabel muted(String text) {
        JLabel l = new JLabel(text); l.setForeground(MUTED); l.setFont(new Font("Segoe UI", Font.PLAIN, 13)); return l;
    }
    public static JButton button(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setFont(MEDIUM); b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(12, 20, 12, 20));
        b.setBackground(primary ? NAVY : WHITE); b.setForeground(primary ? WHITE : NAVY);
        b.setOpaque(true); b.setBorderPainted(!primary);
        if (!primary) b.setBorder(BorderFactory.createLineBorder(LINE));
        return b;
    }
    public static JTextField field(String hint) {
        JTextField f = new JTextField(); f.setFont(BODY); f.setForeground(INK);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), new EmptyBorder(11, 12, 11, 12)));
        f.setPreferredSize(new Dimension(300, 44)); f.setToolTipText(hint); return f;
    }
    public static JPasswordField passwordField(String hint) {
        JPasswordField f = new JPasswordField(); f.setFont(BODY);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), new EmptyBorder(11, 12, 11, 12)));
        f.setPreferredSize(new Dimension(300, 44)); f.setToolTipText(hint); return f;
    }
    public static JLabel brand() {
        JLabel l = new JLabel("STARVIEW  /  HOTEL", SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 17)); l.setForeground(WHITE); return l;
    }
    public static ImageIcon hotelImage(int width, int height) {
        try {
            URL url = UiKit.class.getResource("/starview-hotel.jpg");
            if (url == null) url = UiKit.class.getResource("/web/starview-hotel.jpg");
            if (url != null) return new ImageIcon(ImageIO.read(url).getScaledInstance(width, height, Image.SCALE_SMOOTH));
        } catch (Exception ignored) {}
        return null;
    }
    public static JPanel imagePanel(int width, int height) {
        ImageIcon icon = hotelImage(width, height);
        return new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (icon != null) g.drawImage(icon.getImage(), 0, 0, getWidth(), getHeight(), this);
                else { g.setColor(NAVY_LIGHT); g.fillRect(0,0,getWidth(),getHeight()); }
                Graphics2D g2=(Graphics2D)g.create();
                g2.setPaint(new GradientPaint(0,0,new Color(10,20,35,35),0,getHeight(),new Color(10,20,35,220)));
                g2.fillRect(0,0,getWidth(),getHeight()); g2.dispose();
            }
        };
    }
    public static JPanel topBar(String heading, String detail, String rightText) {
        JPanel bar = new JPanel(new BorderLayout()); bar.setBackground(NAVY); bar.setBorder(new EmptyBorder(18,28,18,28));
        JPanel labels = new JPanel(); labels.setOpaque(false); labels.setLayout(new BoxLayout(labels,BoxLayout.Y_AXIS));
        JLabel h = new JLabel(heading); h.setForeground(WHITE); h.setFont(new Font("Segoe UI",Font.BOLD,20));
        JLabel d = new JLabel(detail); d.setForeground(new Color(205,214,224)); d.setFont(new Font("Segoe UI",Font.PLAIN,12));
        labels.add(h); labels.add(Box.createVerticalStrut(4)); labels.add(d); bar.add(labels,BorderLayout.WEST);
        JLabel r = new JLabel(rightText); r.setForeground(GOLD); r.setFont(MEDIUM); bar.add(r,BorderLayout.EAST); return bar;
    }
}
