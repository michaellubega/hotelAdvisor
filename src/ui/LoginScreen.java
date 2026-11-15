package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import models.User;
import services.UserRepository;

public class LoginScreen {
    private final JFrame frame;
    public LoginScreen() {
        UiKit.setup(); frame=UiKit.frame("Starview Hotel | Staff sign in",1080,700);
        JPanel root=new JPanel(new GridLayout(1,2));
        JPanel hero=UiKit.imagePanel(540,700); hero.setLayout(new BorderLayout());
        JPanel heroText=new JPanel(); heroText.setOpaque(false); heroText.setLayout(new BoxLayout(heroText,BoxLayout.Y_AXIS)); heroText.setBorder(new EmptyBorder(0,42,55,35));
        JLabel brand=UiKit.brand(); brand.setHorizontalAlignment(SwingConstants.LEFT);
        JLabel line=new JLabel("The view is better\nfrom here."); line.setForeground(Color.WHITE); line.setFont(new Font("Segoe UI",Font.BOLD,36));
        JLabel small=new JLabel("STARVIEW HOTEL  •  KANSANGA"); small.setForeground(new Color(240,240,240)); small.setFont(new Font("Segoe UI",Font.PLAIN,13));
        heroText.add(Box.createVerticalGlue()); heroText.add(brand); heroText.add(Box.createVerticalStrut(24)); heroText.add(line); heroText.add(Box.createVerticalStrut(12)); heroText.add(small); hero.add(heroText);
        JPanel right=new JPanel(new GridBagLayout()); right.setBackground(UiKit.BG);
        JPanel form=new JPanel(); form.setOpaque(false); form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS)); form.setBorder(new EmptyBorder(45,65,45,65));
        JLabel eyebrow=new JLabel("TEAM ACCESS"); eyebrow.setForeground(UiKit.GOLD_DARK); eyebrow.setFont(new Font("Segoe UI",Font.BOLD,12));
        JLabel heading=UiKit.title("Staff sign in"); JLabel sub=UiKit.muted("Use your staff credentials to open the management dashboard.");
        JTextField username=UiKit.field("Username"); JPasswordField password=UiKit.passwordField("Password");
        form.add(eyebrow); form.add(Box.createVerticalStrut(12)); form.add(heading); form.add(Box.createVerticalStrut(8)); form.add(sub); form.add(Box.createVerticalStrut(35));
        form.add(fieldGroup("USERNAME",username)); form.add(Box.createVerticalStrut(18)); form.add(fieldGroup("PASSWORD",password)); form.add(Box.createVerticalStrut(26));
        JButton login=UiKit.button("Sign in to dashboard",true); login.setAlignmentX(Component.LEFT_ALIGNMENT); login.setMaximumSize(new Dimension(420,48));
        JButton back=UiKit.button("←  Back to team access",false); back.setAlignmentX(Component.LEFT_ALIGNMENT); back.setMaximumSize(new Dimension(420,46));
        form.add(login); form.add(Box.createVerticalStrut(12)); form.add(back); form.add(Box.createVerticalStrut(30));
        JPanel demo=UiKit.card(); demo.setLayout(new BoxLayout(demo,BoxLayout.Y_AXIS)); demo.setMaximumSize(new Dimension(420,82));
        JLabel dt=new JLabel("DEMONSTRATION ACCOUNT"); dt.setForeground(UiKit.GOLD_DARK); dt.setFont(new Font("Segoe UI",Font.BOLD,11));
        JLabel dc=new JLabel("Username: admin     Password: 1234"); dc.setForeground(UiKit.INK); dc.setFont(new Font("Segoe UI",Font.PLAIN,13)); demo.add(dt); demo.add(Box.createVerticalStrut(8)); demo.add(dc); form.add(demo);
        login.addActionListener(e->{String id=username.getText().trim(); String pw=new String(password.getPassword()); User staff=UserRepository.authenticate(id,pw,User.Role.STAFF); if(staff!=null){frame.dispose();new StaffDashboard();}else JOptionPane.showMessageDialog(frame,"The username or password is incorrect. Please try again.","Sign in unsuccessful",JOptionPane.ERROR_MESSAGE);});
        password.addActionListener(e->login.doClick()); back.addActionListener(e->{frame.dispose();new RoleSelectionScreen();});
        right.add(form); root.add(hero);root.add(right);frame.setContentPane(root);frame.setVisible(true);
    }
    private JPanel fieldGroup(String label,JComponent field){JPanel p=new JPanel();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));JLabel l=new JLabel(label);l.setForeground(UiKit.MUTED);l.setFont(new Font("Segoe UI",Font.BOLD,11));p.add(l);p.add(Box.createVerticalStrut(8));field.setMaximumSize(new Dimension(420,46));p.add(field);return p;}
}
