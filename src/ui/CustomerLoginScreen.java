package ui;

import models.User;
import services.UserRepository;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CustomerLoginScreen {
    private final JFrame frame;
    public CustomerLoginScreen(){
        UiKit.setup();frame=UiKit.frame("Starview Hotel | Guest portal",1080,700);JPanel root=new JPanel(new GridLayout(1,2));
        JPanel hero=UiKit.imagePanel(540,700);hero.setLayout(new BorderLayout());JPanel words=new JPanel();words.setOpaque(false);words.setLayout(new BoxLayout(words,BoxLayout.Y_AXIS));words.setBorder(new EmptyBorder(0,42,55,35));
        JLabel brand=UiKit.brand();brand.setHorizontalAlignment(SwingConstants.LEFT);JLabel statement=new JLabel("Make room for\nsomething special.");statement.setForeground(Color.WHITE);statement.setFont(new Font("Segoe UI",Font.BOLD,35));words.add(Box.createVerticalGlue());words.add(brand);words.add(Box.createVerticalStrut(24));words.add(statement);hero.add(words);
        JPanel right=new JPanel(new GridBagLayout());right.setBackground(UiKit.BG);JPanel form=new JPanel();form.setOpaque(false);form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));form.setBorder(new EmptyBorder(50,65,50,65));
        JLabel eyebrow=new JLabel("GUEST PORTAL");eyebrow.setForeground(UiKit.GOLD_DARK);eyebrow.setFont(new Font("Segoe UI",Font.BOLD,12));JLabel heading=UiKit.title("Welcome back");JLabel sub=UiKit.muted("Sign in to view room rates and manage your bookings.");
        JTextField id=UiKit.field("Email address or username");JPasswordField pw=UiKit.passwordField("Password");
        form.add(eyebrow);form.add(Box.createVerticalStrut(12));form.add(heading);form.add(Box.createVerticalStrut(8));form.add(sub);form.add(Box.createVerticalStrut(35));form.add(labelled("EMAIL OR USERNAME",id));form.add(Box.createVerticalStrut(18));form.add(labelled("PASSWORD",pw));form.add(Box.createVerticalStrut(25));
        JButton login=UiKit.button("Sign in",true);login.setAlignmentX(Component.LEFT_ALIGNMENT);login.setMaximumSize(new Dimension(420,48));JButton signup=UiKit.button("Create a guest account",false);signup.setAlignmentX(Component.LEFT_ALIGNMENT);signup.setMaximumSize(new Dimension(420,46));JButton back=UiKit.button("←  Back to portal selection",false);back.setAlignmentX(Component.LEFT_ALIGNMENT);back.setMaximumSize(new Dimension(420,46));
        form.add(login);form.add(Box.createVerticalStrut(12));form.add(signup);form.add(Box.createVerticalStrut(10));form.add(back);form.add(Box.createVerticalStrut(28));
        JPanel demo=UiKit.card();demo.setLayout(new BoxLayout(demo,BoxLayout.Y_AXIS));demo.setMaximumSize(new Dimension(420,78));JLabel d1=new JLabel("DEMO GUEST ACCOUNT");d1.setForeground(UiKit.GOLD_DARK);d1.setFont(new Font("Segoe UI",Font.BOLD,11));JLabel d2=new JLabel("john@example.com    /    pass123");d2.setForeground(UiKit.INK);demo.add(d1);demo.add(Box.createVerticalStrut(7));demo.add(d2);form.add(demo);
        login.addActionListener(e->{User u=UserRepository.authenticate(id.getText().trim(),new String(pw.getPassword()),User.Role.CUSTOMER);if(u==null)JOptionPane.showMessageDialog(frame,"We couldn't sign you in. Check your details and try again.","Sign in unsuccessful",JOptionPane.ERROR_MESSAGE);else{frame.dispose();new CustomerDashboard(u);}});
        pw.addActionListener(e->login.doClick());signup.addActionListener(e->{frame.dispose();new CustomerSignupScreen();});back.addActionListener(e->{frame.dispose();new RoleSelectionScreen();});
        right.add(form);root.add(hero);root.add(right);frame.setContentPane(root);frame.setVisible(true);
    }
    private JPanel labelled(String label,JComponent c){JPanel p=new JPanel();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));JLabel l=new JLabel(label);l.setForeground(UiKit.MUTED);l.setFont(new Font("Segoe UI",Font.BOLD,11));p.add(l);p.add(Box.createVerticalStrut(8));c.setMaximumSize(new Dimension(420,46));p.add(c);return p;}
}
