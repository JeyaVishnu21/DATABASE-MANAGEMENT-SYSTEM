package Mypack;

import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ShoppingCartForm extends JFrame {

    CardLayout card = new CardLayout();
    JPanel mainPanel = new JPanel(card);
    Connection con;
    int customerId = -1;

    public ShoppingCartForm() {
        setTitle("Online Shopping System");
        setSize(900,600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        connectDB();

        mainPanel.add(new LoginPanel(this),"Login");
        mainPanel.add(new RegisterPanel(this),"Register");
        mainPanel.add(new AdminPanel(this),"Admin");
        mainPanel.add(new CustomerPanel(this),"Customer");

        add(mainPanel);
        card.show(mainPanel,"Login");
        setVisible(true);
    }

    void connectDB(){
        try{
            Class.forName("oracle.jdbc.OracleDriver");
            con=DriverManager.getConnection(
                "jdbc:oracle:thin:@localhost:1521:xe","system","oracle");
        }catch(Exception e){
            JOptionPane.showMessageDialog(this,e.getMessage());
        }
    }

    void showPanel(String name){
        card.show(mainPanel,name);
    }

    public static void main(String[] args){
        new ShoppingCartForm();
    }
}

//////////////// LOGIN //////////////////
class LoginPanel extends JPanel {
    LoginPanel(ShoppingCartForm app){

        setLayout(new GridLayout(5,2,10,10));

        JComboBox<String> type=new JComboBox<>(new String[]{"Customer","Admin"});
        JTextField email=new JTextField();
        JPasswordField pass=new JPasswordField();

        JButton login=new JButton("Login");
        JButton reg=new JButton("Register");

        add(new JLabel("User Type")); add(type);
        add(new JLabel("Email")); add(email);
        add(new JLabel("Password")); add(pass);
        add(login); add(reg);

        login.addActionListener(e->{
            try{
                if(type.getSelectedItem().equals("Admin")){
                    if(email.getText().equals("admin") &&
                       new String(pass.getPassword()).equals("admin")){
                        app.showPanel("Admin");
                    }else{
                        JOptionPane.showMessageDialog(this,"Invalid Admin");
                    }
                    return;
                }

                PreparedStatement pst=app.con.prepareStatement(
                    "SELECT customer_id FROM customers WHERE email=? AND password=?");

                pst.setString(1,email.getText());
                pst.setString(2,new String(pass.getPassword()));

                ResultSet rs=pst.executeQuery();

                if(rs.next()){
                    app.customerId=rs.getInt(1);
                    app.showPanel("Customer");
                }else{
                    JOptionPane.showMessageDialog(this,"Invalid Login");
                }

            }catch(Exception ex){
                JOptionPane.showMessageDialog(this,"Error");
            }
        });

        reg.addActionListener(e->app.showPanel("Register"));
    }
}

//////////////// REGISTER //////////////////
class RegisterPanel extends JPanel {

    RegisterPanel(ShoppingCartForm app){

        setLayout(new GridLayout(6,2,10,10));

        JTextField id=new JTextField();
        JTextField name=new JTextField();
        JTextField email=new JTextField();
        JTextField phone=new JTextField();
        JPasswordField pass=new JPasswordField();

        JButton reg=new JButton("Register");
        JButton back=new JButton("Back");

        add(new JLabel("ID")); add(id);
        add(new JLabel("Name")); add(name);
        add(new JLabel("Email")); add(email);
        add(new JLabel("Phone")); add(phone);
        add(new JLabel("Password")); add(pass);
        add(reg); add(back);

        back.addActionListener(e->app.showPanel("Login"));

        reg.addActionListener(e->{
            try{
                PreparedStatement pst=app.con.prepareStatement(
                    "INSERT INTO customers VALUES(?,?,?,?,?, '',SYSDATE)");

                pst.setInt(1,Integer.parseInt(id.getText()));
                pst.setString(2,name.getText());
                pst.setString(3,email.getText());
                pst.setString(4,phone.getText());
                pst.setString(5,new String(pass.getPassword()));

                pst.executeUpdate();
                JOptionPane.showMessageDialog(this,"Registered");

            }catch(Exception ex){
                JOptionPane.showMessageDialog(this,"Error");
            }
        });
    }
}

//////////////// ADMIN //////////////////
class AdminPanel extends JPanel {

    DefaultTableModel model=new DefaultTableModel();
    JTable table=new JTable(model);

    AdminPanel(ShoppingCartForm app){

        setLayout(new BorderLayout());

        model.addColumn("ID");
        model.addColumn("Name");
        model.addColumn("Category");
        model.addColumn("Price");
        model.addColumn("Stock");

        load(app);

        add(new JScrollPane(table),BorderLayout.CENTER);

        JButton back=new JButton("Back");
        back.addActionListener(e->app.showPanel("Login"));
        add(back,BorderLayout.SOUTH);
    }

    void load(ShoppingCartForm app){
        try{
            ResultSet rs=app.con.createStatement().executeQuery("SELECT * FROM products");
            while(rs.next()){
                model.addRow(new Object[]{
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDouble(4),
                    rs.getInt(5)
                });
            }
        }catch(Exception e){}
    }
}

//////////////// CUSTOMER //////////////////
class CustomerPanel extends JPanel {

    DefaultTableModel model=new DefaultTableModel();
    DefaultTableModel cartModel=new DefaultTableModel();

    JTable table=new JTable(model);
    JTable cart=new JTable(cartModel);

    CustomerPanel(ShoppingCartForm app){

        setLayout(new BorderLayout());

        model.addColumn("ID");
        model.addColumn("Name");
        model.addColumn("Price");
        model.addColumn("Stock");

        cartModel.addColumn("ID");
        cartModel.addColumn("Name");
        cartModel.addColumn("Qty");

        load(app);

        JButton add=new JButton("Add to Cart");
        JButton review=new JButton("Review Cart");

        JPanel top=new JPanel();
        top.add(add); top.add(review);

        add(top,BorderLayout.NORTH);
        add(new JScrollPane(table),BorderLayout.CENTER);

        // ADD
        add.addActionListener(e->{
            int r=table.getSelectedRow();
            if(r==-1) return;

            int stock=(int)model.getValueAt(r,3);
            int qty=Integer.parseInt(JOptionPane.showInputDialog("Qty"));

            if(qty>stock){
                JOptionPane.showMessageDialog(this,"Exceeds stock");
                return;
            }

            cartModel.addRow(new Object[]{
                model.getValueAt(r,0),
                model.getValueAt(r,1),
                qty
            });
        });

        // REVIEW
        review.addActionListener(e->{
            JFrame f=new JFrame("Cart");
            f.setSize(500,400);

            JButton update=new JButton("Update");
            JButton delete=new JButton("Delete");
            JButton order=new JButton("Order");

            JPanel p=new JPanel();
            p.add(update); p.add(delete); p.add(order);

            delete.addActionListener(x->{
                int r=cart.getSelectedRow();
                if(r!=-1) cartModel.removeRow(r);
            });

            update.addActionListener(x->{
                int r=cart.getSelectedRow();
                if(r==-1) return;

                int pid=(int)cartModel.getValueAt(r,0);
                int qty=Integer.parseInt(JOptionPane.showInputDialog("New Qty"));

                try{
                    PreparedStatement pst=app.con.prepareStatement(
                        "SELECT stock_quantity FROM products WHERE product_id=?");
                    pst.setInt(1,pid);
                    ResultSet rs=pst.executeQuery();

                    if(rs.next() && qty>rs.getInt(1)){
                        JOptionPane.showMessageDialog(f,"Exceeds stock");
                        return;
                    }

                    cartModel.setValueAt(qty,r,2);

                }catch(Exception ex){}
            });

            order.addActionListener(x->{
                try{
                    for(int i=0;i<cartModel.getRowCount();i++){
                        int pid=(int)cartModel.getValueAt(i,0);
                        int qty=(int)cartModel.getValueAt(i,2);

                        PreparedStatement pst=app.con.prepareStatement(
                            "UPDATE products SET stock_quantity=stock_quantity-? WHERE product_id=?");
                        pst.setInt(1,qty);
                        pst.setInt(2,pid);
                        pst.executeUpdate();
                    }

                    JOptionPane.showMessageDialog(f,"Order placed");
                    cartModel.setRowCount(0);

                }catch(Exception ex){}
            });

            f.add(new JScrollPane(cart));
            f.add(p,BorderLayout.SOUTH);
            f.setVisible(true);
        });
    }

    void load(ShoppingCartForm app){
        try{
            ResultSet rs=app.con.createStatement().executeQuery("SELECT * FROM products");
            while(rs.next()){
                model.addRow(new Object[]{
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getDouble(4),
                    rs.getInt(5)
                });
            }
        }catch(Exception e){}
    }
}
