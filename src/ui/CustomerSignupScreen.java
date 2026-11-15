package ui;

import models.User;
import services.UserRepository;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CustomerSignupScreen {
    private final JFrame frame;
    private JTextField first,last,email,phone,location;private JPasswordField pass,confirm;
    public CustomerSignupScreen(){
        UiKit.setup();frame=UiKit.frame("Starview Hotel | Create guest account",1050,760);JPanel root=UiKit.page();root.add(UiKit.topBar("Create your guest account","A FEW DETAILS TO GET YOUR STAY STARTED","GUEST REGISTRATION"),BorderLayout.NORTH);
        JPanel center=new JPanel(new GridBagLayout());center.setOpaque(false);center.setBorder(new EmptyBorder(25,25,25,25));JPanel card=UiKit.card();card.setLayout(new BorderLayout(0,20));card.setPreferredSize(new Dimension(760,570));
        JPanel intro=new JPanel();intro.setOpaque(false);intro.setLayout(new BoxLayout(intro,BoxLayout.Y_AXIS));JLabel h=UiKit.title("Join Starview");JLabel s=UiKit.muted("Create an account to explore rooms and keep your bookings in one place.");intro.add(h);intro.add(Box.createVerticalStrut(6));intro.add(s);card.add(intro,BorderLayout.NORTH);
        JPanel fields=new JPanel(new GridLayout(4,2,16,15));fields.setOpaque(false);first=UiKit.field("First name");last=UiKit.field("Last name");email=UiKit.field("Email address");phone=UiKit.field("Phone number");location=UiKit.field("City / location");pass=UiKit.passwordField("At least 6 characters");confirm=UiKit.passwordField("Re-enter password");
        fields.add(group("FIRST NAME",first));fields.add(group("LAST NAME",last));fields.add(group("EMAIL ADDRESS",email));fields.add(group("PHONE NUMBER",phone));fields.add(group("LOCATION",location));fields.add(group("PASSWORD",pass));fields.add(group("CONFIRM PASSWORD",confirm));JPanel empty=new JPanel();empty.setOpaque(false);fields.add(empty);card.add(fields,BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));actions.setOpaque(false);JButton back=UiKit.button("Back to sign in",false);JButton create=UiKit.button("Create account",true);actions.add(back);actions.add(create);card.add(actions,BorderLayout.SOUTH);
        back.addActionListener(e->{frame.dispose();new CustomerLoginScreen();});create.addActionListener(e->createAccount());center.add(card);root.add(center,BorderLayout.CENTER);frame.setContentPane(root);frame.setVisible(true);
    }
    private JPanel group(String label,JComponent c){JPanel p=new JPanel();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));JLabel l=new JLabel(label);l.setForeground(UiKit.MUTED);l.setFont(new Font("Segoe UI",Font.BOLD,10));p.add(l);p.add(Box.createVerticalStrut(7));p.add(c);return p;}
    private void createAccount(){String fn=first.getText().trim(),ln=last.getText().trim(),em=email.getText().trim(),ph=phone.getText().trim(),loc=location.getText().trim(),pw=new String(pass.getPassword()),cf=new String(confirm.getPassword());if(fn.isEmpty()||ln.isEmpty()||em.isEmpty()||ph.isEmpty()||loc.isEmpty()||pw.isEmpty()){JOptionPane.showMessageDialog(frame,"Please complete all fields.","Missing information",JOptionPane.WARNING_MESSAGE);return;}if(!em.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")){JOptionPane.showMessageDialog(frame,"Enter a valid email address.","Invalid email",JOptionPane.WARNING_MESSAGE);return;}if(pw.length()<6){JOptionPane.showMessageDialog(frame,"Choose a password with at least six characters.","Password too short",JOptionPane.WARNING_MESSAGE);return;}if(!pw.equals(cf)){JOptionPane.showMessageDialog(frame,"The passwords do not match.","Check password",JOptionPane.WARNING_MESSAGE);return;}String username=em.substring(0,em.indexOf('@'));User user=new User(username,pw,fn,ln,em,ph,loc,User.Role.CUSTOMER,500);if(!UserRepository.addUser(user)){JOptionPane.showMessageDialog(frame,"An account with that email or username already exists.","Account already exists",JOptionPane.ERROR_MESSAGE);return;}JOptionPane.showMessageDialog(frame,"Your guest account has been created. Welcome to Starview, "+fn+"!","Account created",JOptionPane.INFORMATION_MESSAGE);frame.dispose();new CustomerDashboard(user);}
}
