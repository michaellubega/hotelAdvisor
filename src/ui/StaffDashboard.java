package ui;

import models.Booking;
import models.HotelData;
import services.BookingService;
import services.RateEngine;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class StaffDashboard {
    private final JFrame frame;
    private final RateEngine engine = new RateEngine();
    private final JLabel rate = new JLabel("UGX 0",SwingConstants.CENTER);
    private final JTextArea breakdown = new JTextArea();
    private final JSpinner date;
    private final JCheckBox competitors = new JCheckBox("Nearby competitors are fully booked");

    public StaffDashboard() {
        UiKit.setup(); frame=UiKit.frame("Starview Hotel | Management",1180,760);
        JPanel root=UiKit.page(); root.add(UiKit.topBar("Management dashboard","DYNAMIC PRICING & OCCUPANCY ADVISOR","STAFF WORKSPACE"),BorderLayout.NORTH);
        JPanel body=new JPanel(new BorderLayout(22,22));body.setOpaque(false);body.setBorder(new EmptyBorder(26,28,26,28));
        JPanel welcome=new JPanel(new BorderLayout());welcome.setOpaque(false);
        JPanel welcomeText=new JPanel();welcomeText.setOpaque(false);welcomeText.setLayout(new BoxLayout(welcomeText,BoxLayout.Y_AXIS));
        JLabel h=UiKit.title("Good day, Administrator");JLabel s=UiKit.muted("Review hotel demand and calculate a recommended room rate.");welcomeText.add(h);welcomeText.add(Box.createVerticalStrut(6));welcomeText.add(s);welcome.add(welcomeText,BorderLayout.WEST);
        JPanel stats=new JPanel(new GridLayout(1,3,14,0));stats.setOpaque(false);
        stats.add(statCard("TOTAL ROOMS",String.valueOf(HotelData.TOTAL_ROOMS),"Hotel inventory"));
        stats.add(statCard("ACTIVE BOOKINGS",String.valueOf(BookingService.getAllBookings().stream().filter(Booking::isActive).count()),"Current sample records"));
        stats.add(statCard("BASE NIGHTLY RATE",String.format("UGX %,.0f",HotelData.BASE_RATE),"Before demand adjustments"));
        JPanel upper=new JPanel(new BorderLayout(0,18));upper.setOpaque(false);upper.add(welcome,BorderLayout.NORTH);upper.add(stats,BorderLayout.CENTER);
        JPanel work=new JPanel(new GridLayout(1,2,18,0));work.setOpaque(false);
        JPanel inputs=UiKit.card();inputs.setLayout(new BoxLayout(inputs,BoxLayout.Y_AXIS));
        JLabel ih=new JLabel("Rate calculation");ih.setFont(new Font("Segoe UI",Font.BOLD,19));ih.setForeground(UiKit.INK);
        JLabel id=UiKit.muted("Choose a date and competitor conditions.");
        date=new JSpinner(new SpinnerDateModel());date.setEditor(new JSpinner.DateEditor(date,"dd MMM yyyy"));date.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        competitors.setOpaque(false);competitors.setFont(UiKit.BODY);competitors.setForeground(UiKit.INK);
        JButton calculate=UiKit.button("Calculate recommended rate",true);calculate.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton bookings=UiKit.button("View all bookings",false);bookings.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputs.add(ih);inputs.add(Box.createVerticalStrut(7));inputs.add(id);inputs.add(Box.createVerticalStrut(25));inputs.add(fieldLabel("TARGET DATE"));inputs.add(Box.createVerticalStrut(8));inputs.add(date);inputs.add(Box.createVerticalStrut(20));inputs.add(competitors);inputs.add(Box.createVerticalStrut(24));inputs.add(calculate);inputs.add(Box.createVerticalStrut(12));inputs.add(bookings);inputs.add(Box.createVerticalGlue());
        JPanel result=UiKit.card();result.setLayout(new BorderLayout(0,12));
        JPanel rt=new JPanel();rt.setOpaque(false);rt.setLayout(new BoxLayout(rt,BoxLayout.Y_AXIS));JLabel rh=new JLabel("Recommended nightly rate");rh.setForeground(UiKit.INK);rh.setFont(new Font("Segoe UI",Font.BOLD,19));JLabel rs=UiKit.muted("Calculated from season, local events, occupancy and competitor availability.");rt.add(rh);rt.add(Box.createVerticalStrut(6));rt.add(rs);
        rate.setFont(new Font("Segoe UI",Font.BOLD,48));rate.setForeground(UiKit.GOLD_DARK);rate.setBorder(new EmptyBorder(20,0,12,0));
        breakdown.setEditable(false);breakdown.setFont(new Font("Consolas",Font.PLAIN,13));breakdown.setForeground(UiKit.INK);breakdown.setBackground(new Color(249,250,251));breakdown.setBorder(new EmptyBorder(16,16,16,16));breakdown.setText("Select a target date and calculate to view the pricing breakdown.");
        result.add(rt,BorderLayout.NORTH);result.add(rate,BorderLayout.CENTER);result.add(new JScrollPane(breakdown),BorderLayout.SOUTH);
        work.add(inputs);work.add(result);body.add(upper,BorderLayout.NORTH);body.add(work,BorderLayout.CENTER);root.add(body,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout());footer.setBackground(Color.WHITE);footer.setBorder(new EmptyBorder(12,28,12,28));JLabel foot=UiKit.muted("STARVIEW HOTEL  •  KANSANGA");JButton logout=UiKit.button("Sign out",false);footer.add(foot,BorderLayout.WEST);footer.add(logout,BorderLayout.EAST);root.add(footer,BorderLayout.SOUTH);
        calculate.addActionListener(e->calculate());bookings.addActionListener(e->showAllBookings());logout.addActionListener(e->{frame.dispose();new RoleSelectionScreen();});
        frame.setContentPane(root);frame.setVisible(true);calculate.doClick();
    }
    private JPanel statCard(String label,String value,String note){JPanel p=UiKit.card();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));JLabel l=new JLabel(label);l.setForeground(UiKit.MUTED);l.setFont(new Font("Segoe UI",Font.BOLD,10));JLabel v=new JLabel(value);v.setForeground(UiKit.INK);v.setFont(new Font("Segoe UI",Font.BOLD,25));JLabel n=UiKit.muted(note);p.add(l);p.add(Box.createVerticalStrut(7));p.add(v);p.add(Box.createVerticalStrut(3));p.add(n);return p;}
    private JLabel fieldLabel(String s){JLabel l=new JLabel(s);l.setForeground(UiKit.MUTED);l.setFont(new Font("Segoe UI",Font.BOLD,11));return l;}
    private void calculate(){Date selected=(Date)date.getValue();LocalDate day=selected.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();RateEngine.RateResult r=engine.calculateRecommendedRate(day,competitors.isSelected());rate.setText(String.format("UGX %,.0f",r.finalRate));StringBuilder b=new StringBuilder();b.append("PRICING SUMMARY\n").append("────────────────────────────────────\n");b.append("Target date              ").append(day).append("\n");b.append(String.format("Base nightly rate         UGX %,.0f%n",HotelData.BASE_RATE));b.append(String.format("Seasonality (%s)       x%.2f%n",r.seasonName,r.seasonMultiplier));b.append(r.eventName==null?"Local event               None (x1.00)\n":String.format("Local event               %s (x%.2f)%n",r.eventName,r.eventMultiplier));b.append(String.format("Occupancy demand          x%.2f%n",r.occupancyMultiplier));b.append(String.format("Competitor availability   x%.2f%n",r.competitorMultiplier));b.append("────────────────────────────────────\n");b.append("RECOMMENDATION\n");b.append(r.finalRate>=HotelData.BASE_RATE*1.3?"High demand — consider maximizing revenue.":r.finalRate<=HotelData.BASE_RATE*.9?"Lower demand — consider an occupancy promotion.":"Normal demand — maintain standard pricing.");breakdown.setText(b.toString());}
    private void showAllBookings(){List<Booking> list=BookingService.getAllBookings();String[] cols={"Booking ID","Customer","Room","Check-in","Check-out","Total","Status"};Object[][] rows=new Object[list.size()][7];for(int i=0;i<list.size();i++){Booking b=list.get(i);rows[i]=new Object[]{b.getBookingId(),b.getCustomerUsername(),b.getRoomType(),b.getCheckInDate(),b.getCheckOutDate(),String.format("UGX %,.0f",b.getTotalCost()),b.isActive()?"Active":"Cancelled"};}JTable table=new JTable(rows,cols);table.setRowHeight(30);table.setAutoCreateRowSorter(true);JScrollPane sp=new JScrollPane(table);sp.setPreferredSize(new Dimension(820,300));JOptionPane.showMessageDialog(frame,sp,"Hotel booking records",JOptionPane.PLAIN_MESSAGE);}
}
