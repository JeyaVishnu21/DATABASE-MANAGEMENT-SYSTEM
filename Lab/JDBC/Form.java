import java.awt.*;
import java.sql.*;
import java.util.Scanner;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class EmployeeOracleApp extends JFrame {

    // Role -> salary per day mapping
    static final String[] ROLES = { "Manager", "Superviser", "Mechanic", "Shopkeeper", "Support Mechanic" };
    static final int[] SALARY_PER_DAY = { 1500, 1200, 1100, 1000, 900 };

    // Main window fields
    JTextField empIdField, salaryField;
    JButton calcSalaryBtn;
    JTable table;
    DefaultTableModel model;

    Connection con;

    // ----------------------------------------------------------------
    // Data holder
    // ----------------------------------------------------------------
    static class EmployeeRecord {
        final int empId;
        final String name;
        final double salary;
        final double salaryPerDay;
        final int workedDays;
        final String phoneNumber;
        final int age;

        EmployeeRecord(int empId, String name, double salary,
                double salaryPerDay, int workedDays, String phoneNumber, int age) {
            this.empId = empId;
            this.name = name;
            this.salary = salary;
            this.salaryPerDay = salaryPerDay;
            this.workedDays = workedDays;
            this.phoneNumber = phoneNumber;
            this.age = age;
        }
    }

    // ----------------------------------------------------------------
    // Validation helpers
    // ----------------------------------------------------------------
    static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("\\d{10}");
    }

    static boolean isValidAge(int age) {
        return age >= 18;
    }

    // ----------------------------------------------------------------
    // DB helpers
    // ----------------------------------------------------------------
    EmployeeRecord fetchEmployeeById(int empId) {
        if (con == null)
            return null;
        String sql = "SELECT employee_id, name, salary, salaryperday, worked_days, phone_number, age "
                + "FROM emp_payroll WHERE employee_id=?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, empId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return new EmployeeRecord(
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getDouble(3),
                            rs.getDouble(4),
                            rs.getInt(5),
                            rs.getString(6),
                            rs.getInt(7));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    void printEmployeeDetails(EmployeeRecord r, String actionLine) {
        System.out.println("employee_id  : " + r.empId);
        System.out.println("name         : " + r.name);
        System.out.println("age          : " + r.age);
        System.out.println("salary       : " + r.salary);
        System.out.println("salaryperday : " + r.salaryPerDay);
        System.out.println("worked_days  : " + r.workedDays);
        System.out.println("phone_number : " + r.phoneNumber);
        if (actionLine != null && !actionLine.isEmpty()) {
            System.out.println(actionLine);
        }
    }

    void printEmployeeById(int empId, String actionLineIfFound) {
        if (con == null)
            return;
        EmployeeRecord r = fetchEmployeeById(empId);
        if (r != null) {
            printEmployeeDetails(r, actionLineIfFound);
        } else {
            System.out.println("Employee with id " + empId + " not found.");
        }
    }

    void printAllEmployees() {
        if (con == null) return;
        String sql = "SELECT employee_id, name, salary, salaryperday, worked_days, phone_number, age "
                   + "FROM emp_payroll ORDER BY employee_id";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\n--- ALL EMPLOYEES ---------------------------------------------------------------------------------------");
            System.out.printf("%-6s | %-16s | %-4s | %-10s | %-10s | %-11s | %-12s%n", 
                              "ID", "Name", "Age", "Salary", "Salary/Day", "Worked Days", "Phone");
            System.out.println("---------------------------------------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-6d | %-16s | %-4d | %-10.1f | %-10.1f | %-11d | %-12s%n",
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getInt(7),
                    rs.getDouble(3),
                    rs.getDouble(4),
                    rs.getInt(5),
                    rs.getString(6));
            }
            System.out.println("---------------------------------------------------------------------------------------------------------\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static int getSalaryPerDayForIndex(int idx) {
        if (idx >= 0 && idx < SALARY_PER_DAY.length)
            return SALARY_PER_DAY[idx];
        return SALARY_PER_DAY[SALARY_PER_DAY.length - 1];
    }

    // ----------------------------------------------------------------
    // Constructor / main window
    // ----------------------------------------------------------------
    EmployeeOracleApp() {
        setTitle("Employee Salary Management - Oracle");
        setLayout(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu");
        JMenuItem insertItem = new JMenuItem("Insert");
        JMenuItem deleteItem = new JMenuItem("Delete");
        JMenuItem updateItem = new JMenuItem("Update");
        menu.add(insertItem);
        menu.add(deleteItem);
        menu.add(updateItem);
        menuBar.add(menu);

        menuBar.add(Box.createHorizontalGlue());
        
        JMenu viewMenu = new JMenu("View");
        JMenuItem showAllItem = new JMenuItem("Show All Records");
        viewMenu.add(showAllItem);
        menuBar.add(viewMenu);

        setJMenuBar(menuBar);

        // Salary lookup panel
        JLabel empIdLabel = new JLabel("Enter Employee ID:");
        empIdLabel.setBounds(50, 30, 180, 30);
        add(empIdLabel);

        empIdField = new JTextField();
        empIdField.setBounds(230, 30, 180, 30);
        add(empIdField);

        calcSalaryBtn = new JButton("Show Salary");
        calcSalaryBtn.setBounds(50, 80, 180, 35);
        add(calcSalaryBtn);

        salaryField = new JTextField();
        salaryField.setBounds(250, 80, 200, 35);
        salaryField.setEditable(false);
        add(salaryField);

        // Data table
        model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[] {
                "EMP_ID", "Name", "Age", "Salary (Rs.)", "Salary/Day", "Worked Days", "Phone"
        });

        table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setBounds(50, 200, 1200, 500);
        add(sp);

        // DB connect & load
        connectDB();
        if (con != null) {
            createTableIfNotExists();
            addAgeColumnIfNotExists();
            loadTable();
        } else {
            calcSalaryBtn.setEnabled(false);
            insertItem.setEnabled(false);
            deleteItem.setEnabled(false);
            updateItem.setEnabled(false);
        }

        // Action listeners
        calcSalaryBtn.addActionListener(e -> calculateSalary());
        insertItem.addActionListener(e -> new InsertForm());
        deleteItem.addActionListener(e -> deleteEmployee());
        updateItem.addActionListener(e -> new UpdateForm());
        showAllItem.addActionListener(e -> showAllRecordsDialog());

        setVisible(true);

        // Terminal menu in background thread
        Thread terminalThread = new Thread(this::runTerminalMenu, "terminal-menu-thread");
        terminalThread.setDaemon(true);
        terminalThread.start();
    }

    // ----------------------------------------------------------------
    // DB connection
    // ----------------------------------------------------------------
    void connectDB() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            String url = "jdbc:oracle:thin:@localhost:1521:xe";
            String user = "system";
            String pass = "oracle";
            con = DriverManager.getConnection(url, user, pass);
            System.out.println("Connected to Oracle DB");
        } catch (Exception e) {
            con = null;
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "DB Connection Failed:\n" + e,
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ----------------------------------------------------------------
    // Create emp_payroll table if it does not exist
    // ----------------------------------------------------------------
    void createTableIfNotExists() {
        String createSql = "CREATE TABLE emp_payroll ("
                + "  employee_id  NUMBER(38)    NOT NULL PRIMARY KEY,"
                + "  name         VARCHAR2(100),"
                + "  salary       FLOAT(126),"
                + "  salaryperday FLOAT(126)    NOT NULL,"
                + "  worked_days  NUMBER(38),"
                + "  phone_number VARCHAR2(15),"
                + "  age          NUMBER(3)"
                + ")";
        try (Statement st = con.createStatement()) {
            st.executeUpdate(createSql);
            System.out.println("Table emp_payroll created.");
        } catch (SQLException e) {
            if (e.getErrorCode() == 955) {
                System.out.println("Table emp_payroll already exists.");
            } else {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Could not create table emp_payroll:\n" + e.getMessage(),
                        "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ----------------------------------------------------------------
    // Add age column to existing table if it is missing
    // ----------------------------------------------------------------
    void addAgeColumnIfNotExists() {
        String checkSql = "SELECT COUNT(*) FROM user_tab_columns "
                + "WHERE table_name = 'EMP_PAYROLL' AND column_name = 'AGE'";
        try (Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(checkSql)) {
            rs.next();
            if (rs.getInt(1) == 0) {
                st.executeUpdate("ALTER TABLE emp_payroll ADD (age NUMBER(3))");
                System.out.println("Column age added to emp_payroll.");
            } else {
                System.out.println("Column age already exists.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------------------
    // Load rows into JTable
    // ----------------------------------------------------------------
    void loadTable() {
        if (con == null)
            return;
        try {
            model.setRowCount(0);
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT employee_id, name, age, salary, salaryperday, worked_days, phone_number "
                            + "FROM emp_payroll ORDER BY employee_id");
            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getInt(3),
                        rs.getDouble(4),
                        rs.getDouble(5),
                        rs.getInt(6),
                        rs.getString(7)
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------------------
    // Show salary for an employee ID
    // ----------------------------------------------------------------
    void calculateSalary() {
        if (con == null) {
            JOptionPane.showMessageDialog(this, "Not connected to DB.");
            return;
        }
        try {
            int id = Integer.parseInt(empIdField.getText().trim());
            PreparedStatement pst = con.prepareStatement(
                    "SELECT salary FROM emp_payroll WHERE employee_id=?");
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                salaryField.setText("Rs. " + rs.getDouble(1));
            } else {
                JOptionPane.showMessageDialog(this, "Employee not found!");
                salaryField.setText("");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid Input!");
            salaryField.setText("");
        }
    }

    // ----------------------------------------------------------------
    // Delete employee (GUI)
    // ----------------------------------------------------------------
    void deleteEmployee() {
        if (con == null) {
            JOptionPane.showMessageDialog(this, "Not connected to DB.");
            return;
        }
        String idStr = JOptionPane.showInputDialog("Enter Employee ID to delete:");
        if (idStr == null || idStr.trim().isEmpty())
            return;

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete?");
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int empId = Integer.parseInt(idStr.trim());
                printEmployeeById(empId, "will be deleted...");

                PreparedStatement pst = con.prepareStatement(
                        "DELETE FROM emp_payroll WHERE employee_id=?");
                pst.setInt(1, empId);
                int rows = pst.executeUpdate();

                if (rows > 0) {
                    System.out.println("deleted from DB.");
                    printAllEmployees();
                    JOptionPane.showMessageDialog(this, "Employee deleted successfully.");
                } else {
                    JOptionPane.showMessageDialog(this, "Employee not found!");
                }
                loadTable();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Delete Error!");
            }
        }
    }

    // ----------------------------------------------------------------
    // Show all records Dialog
    // ----------------------------------------------------------------
    void showAllRecordsDialog() {
        JFrame allRecordsFrame = new JFrame("All Inserted Records");
        allRecordsFrame.setSize(1000, 600);
        allRecordsFrame.setLocationRelativeTo(this);

        DefaultTableModel allRecordsModel = new DefaultTableModel();
        allRecordsModel.setColumnIdentifiers(new String[] {
                "EMP_ID", "Name", "Age", "Salary (Rs.)", "Salary/Day", "Worked Days", "Phone"
        });
        JTable allRecordsTable = new JTable(allRecordsModel);
        JScrollPane scrollPane = new JScrollPane(allRecordsTable);
        allRecordsFrame.add(scrollPane);

        if (con != null) {
            try {
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT employee_id, name, age, salary, salaryperday, worked_days, phone_number "
                                + "FROM emp_payroll ORDER BY employee_id");
                while (rs.next()) {
                    allRecordsModel.addRow(new Object[] {
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getInt(3),
                            rs.getDouble(4),
                            rs.getDouble(5),
                            rs.getInt(6),
                            rs.getString(7)
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Not connected to DB.");
        }
        allRecordsFrame.setVisible(true);
    }

    // ----------------------------------------------------------------
    // Terminal menu
    // ----------------------------------------------------------------
    void runTerminalMenu() {
        if (con == null) {
            System.out.println("DB not connected. Terminal menu disabled.");
            return;
        }
        Scanner sc = new Scanner(System.in);
        while (true) {
            try {
                System.out.println("\n--- TERMINAL MENU ---");
                System.out.println("1. Insert Employee");
                System.out.println("2. Update Employee");
                System.out.println("3. Delete Employee");
                System.out.println("4. Show All Employees");
                System.out.println("5. Exit");
                System.out.print("Enter choice: ");

                String choice = sc.nextLine().trim();
                if ("1".equals(choice)) {
                    terminalInsert(sc);
                } else if ("2".equals(choice)) {
                    terminalUpdate(sc);
                } else if ("3".equals(choice)) {
                    terminalDelete(sc);
                } else if ("4".equals(choice)) {
                    printAllEmployees();
                } else if ("5".equals(choice)) {
                    System.out.println("Exiting terminal menu.");
                    break;
                } else {
                    System.out.println("Invalid choice. Enter 1, 2, 3, 4, or 5.");
                }
            } catch (Exception ex) {
                System.out.println("Operation failed: " + ex.getMessage());
            }
        }
    }

    void terminalInsert(Scanner sc) {
        try {
            System.out.print("Enter Employee ID: ");
            int empId = Integer.parseInt(sc.nextLine().trim());

            System.out.print("Enter Name: ");
            String name = sc.nextLine().trim();

            System.out.print("Enter Age: ");
            int age;
            try {
                age = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Enter your age correctly");
                return;
            }
            if (!isValidAge(age)) {
                if (age >= 0 && age < 18) {
                    System.out.println("you are not eligeble for working in the shop");
                } else {
                    System.out.println("Enter your age correctly");
                }
                return;
            }

            System.out.print("Enter Phone Number (10 digits): ");
            String phone = sc.nextLine().trim();
            if (!isValidPhone(phone)) {
                System.out.println("Invalid phone number. Must be exactly 10 digits.");
                return;
            }

            System.out.print("Enter Worked Days: ");
            int workedDays = Integer.parseInt(sc.nextLine().trim());

            System.out.println("Select Role:");
            for (int i = 0; i < ROLES.length; i++) {
                System.out.println("  " + (i + 1) + ". " + ROLES[i]
                        + "  (Rs. " + SALARY_PER_DAY[i] + "/day)");
            }
            System.out.print("Enter role number: ");
            int roleIdx = Integer.parseInt(sc.nextLine().trim()) - 1;
            if (roleIdx < 0 || roleIdx >= ROLES.length) {
                System.out.println("Invalid role number.");
                return;
            }
            double salaryPerDay = getSalaryPerDayForIndex(roleIdx);
            double salary = salaryPerDay * workedDays;

            PreparedStatement pst = con.prepareStatement(
                    "INSERT INTO emp_payroll(employee_id, name, salary, salaryperday, worked_days, phone_number, age) "
                            + "VALUES(?,?,?,?,?,?,?)");
            pst.setInt(1, empId);
            pst.setString(2, name);
            pst.setDouble(3, salary);
            pst.setDouble(4, salaryPerDay);
            pst.setInt(5, workedDays);
            pst.setString(6, phone);
            pst.setInt(7, age);
            pst.executeUpdate();

            System.out.println();
            printEmployeeDetails(
                    new EmployeeRecord(empId, name, salary, salaryPerDay, workedDays, phone, age),
                    "inserted into DB.");
            printAllEmployees();
            SwingUtilities.invokeLater(this::loadTable);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid input. ID and days must be integers.");
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1) {
                System.out.println("Insert failed: Employee ID already exists.");
            } else {
                System.out.println("Insert failed: " + ex.getMessage());
            }
        }
    }

    void terminalUpdate(Scanner sc) {
        try {
            System.out.print("Enter Employee ID to update: ");
            int empId = Integer.parseInt(sc.nextLine().trim());

            EmployeeRecord loaded = fetchEmployeeById(empId);
            if (loaded == null) {
                System.out.println("Employee not found.");
                return;
            }
            printEmployeeDetails(loaded, "current data.");

            System.out.print("Enter New Name (leave empty to keep same): ");
            String rawName = sc.nextLine().trim();
            String newName = rawName.isEmpty() ? loaded.name : rawName;

            System.out.print("Enter New Age (leave empty to keep same): ");
            String rawAge = sc.nextLine().trim();
            int newAge = loaded.age;
            if (!rawAge.isEmpty()) {
                try {
                    newAge = Integer.parseInt(rawAge);
                } catch (NumberFormatException e) {
                    System.out.println("Enter your age correctly");
                    return;
                }
                if (!isValidAge(newAge)) {
                    if (newAge >= 0 && newAge < 18) {
                        System.out.println("you are not eligeble for workinh in the shop");
                    } else {
                        System.out.println("Enter your age correctly");
                    }
                    return;
                }
            }

            System.out.print("Enter New Phone (10 digits, leave empty to keep same): ");
            String rawPhone = sc.nextLine().trim();
            if (!rawPhone.isEmpty() && !isValidPhone(rawPhone)) {
                System.out.println("Invalid phone number. Must be exactly 10 digits.");
                return;
            }
            String newPhone = rawPhone.isEmpty() ? loaded.phoneNumber : rawPhone;

            System.out.print("Enter New Worked Days (leave empty to keep same): ");
            String rawDays = sc.nextLine().trim();
            int newDays = rawDays.isEmpty() ? loaded.workedDays : Integer.parseInt(rawDays);

            System.out.println("Select New Role (leave empty to keep same):");
            for (int i = 0; i < ROLES.length; i++) {
                System.out.println("  " + (i + 1) + ". " + ROLES[i]
                        + "  (Rs. " + SALARY_PER_DAY[i] + "/day)");
            }
            System.out.print("Enter role number (or press Enter to keep): ");
            String rawRole = sc.nextLine().trim();
            double newSalaryPerDay = loaded.salaryPerDay;
            if (!rawRole.isEmpty()) {
                int roleIdx = Integer.parseInt(rawRole) - 1;
                if (roleIdx < 0 || roleIdx >= ROLES.length) {
                    System.out.println("Invalid role, keeping old value.");
                } else {
                    newSalaryPerDay = getSalaryPerDayForIndex(roleIdx);
                }
            }
            double newSalary = newSalaryPerDay * newDays;

            PreparedStatement pst = con.prepareStatement(
                    "UPDATE emp_payroll SET name=?, salary=?, salaryperday=?, worked_days=?, phone_number=?, age=? "
                            + "WHERE employee_id=?");
            pst.setString(1, newName);
            pst.setDouble(2, newSalary);
            pst.setDouble(3, newSalaryPerDay);
            pst.setInt(4, newDays);
            pst.setString(5, newPhone);
            pst.setInt(6, newAge);
            pst.setInt(7, empId);

            int rows = pst.executeUpdate();
            if (rows > 0) {
                printEmployeeById(empId, "updated in DB.");
                printAllEmployees();
                SwingUtilities.invokeLater(this::loadTable);
            } else {
                System.out.println("Update failed: Employee not found.");
            }
        } catch (NumberFormatException ex) {
            System.out.println("Invalid input. ID and days must be integers.");
        } catch (SQLException ex) {
            System.out.println("Update failed: " + ex.getMessage());
        }
    }

    void terminalDelete(Scanner sc) {
        try {
            System.out.print("Enter Employee ID to delete: ");
            int empId = Integer.parseInt(sc.nextLine().trim());

            EmployeeRecord loaded = fetchEmployeeById(empId);
            if (loaded == null) {
                System.out.println("Employee not found.");
                return;
            }
            printEmployeeDetails(loaded, "will be deleted.");
            System.out.print("Confirm delete? (y/n): ");
            String confirm = sc.nextLine().trim();
            if (!"y".equalsIgnoreCase(confirm) && !"yes".equalsIgnoreCase(confirm)) {
                System.out.println("Delete cancelled.");
                return;
            }
            PreparedStatement pst = con.prepareStatement(
                    "DELETE FROM emp_payroll WHERE employee_id=?");
            pst.setInt(1, empId);
            int rows = pst.executeUpdate();
            if (rows > 0) {
                System.out.println("deleted from DB.");
                printAllEmployees();
                SwingUtilities.invokeLater(this::loadTable);
            } else {
                System.out.println("Delete failed: Employee not found.");
            }
        } catch (NumberFormatException ex) {
            System.out.println("Invalid input. ID must be an integer.");
        } catch (SQLException ex) {
            System.out.println("Delete failed: " + ex.getMessage());
        }
    }

    // ================================================================
    // Inner class: InsertForm
    // ================================================================
    class InsertForm extends JFrame {
        JTextField idField, nameField, ageField, phoneField, workedDaysField;
        JComboBox<String> roleCombo;
        JLabel salaryLabel;

        InsertForm() {
            setTitle("Insert Employee");
            setSize(460, 420);
            setLayout(new GridLayout(9, 2, 10, 10));
            setLocationRelativeTo(null);

            idField = new JTextField();
            nameField = new JTextField();
            ageField = new JTextField();
            phoneField = new JTextField();
            workedDaysField = new JTextField();

            String[] comboItems = new String[ROLES.length];
            for (int i = 0; i < ROLES.length; i++) {
                comboItems[i] = ROLES[i] + "  (Rs. " + SALARY_PER_DAY[i] + "/day)";
            }
            roleCombo = new JComboBox<>(comboItems);
            salaryLabel = new JLabel("Calculated Salary: --");

            add(new JLabel("Employee ID:"));
            add(idField);
            add(new JLabel("Name:"));
            add(nameField);
            add(new JLabel("Age:"));
            add(ageField);
            add(new JLabel("Phone Number:"));
            add(phoneField);
            add(new JLabel("Worked Days:"));
            add(workedDaysField);
            add(new JLabel("Role (Salary/Day):"));
            add(roleCombo);
            add(salaryLabel);
            add(new JLabel(""));

            JButton calcBtn = new JButton("Preview Salary");
            JButton saveBtn = new JButton("Save");
            add(calcBtn);
            add(saveBtn);

            calcBtn.addActionListener(e -> previewSalary());

            saveBtn.addActionListener(e -> {
                try {
                    if (con == null) {
                        JOptionPane.showMessageDialog(this, "Not connected to DB.");
                        return;
                    }

                    int empId = Integer.parseInt(idField.getText().trim());
                    String name = nameField.getText().trim();
                    String phone = phoneField.getText().trim();
                    int workedDays = Integer.parseInt(workedDaysField.getText().trim());

                    // Age validation
                    int age;
                    try {
                        age = Integer.parseInt(ageField.getText().trim());
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Enter your age correctly",
                                "Validation Error", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (!isValidAge(age)) {
                        if (age >= 0 && age < 18) {
                            JOptionPane.showMessageDialog(this, "you are not eligeble for workinh in the shop",
                                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this, "Enter your age correctly",
                                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                        }
                        return;
                    }

                    if (name.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Name cannot be empty.");
                        return;
                    }

                    // Phone validation
                    if (!isValidPhone(phone)) {
                        JOptionPane.showMessageDialog(this,
                                "Invalid phone number!\nPhone must be exactly 10 digits.",
                                "Validation Error", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    int roleIdx = roleCombo.getSelectedIndex();
                    double salaryPerDay = getSalaryPerDayForIndex(roleIdx);
                    double salary = salaryPerDay * workedDays;

                    PreparedStatement pst = con.prepareStatement(
                            "INSERT INTO emp_payroll(employee_id, name, salary, salaryperday, worked_days, phone_number, age) "
                                    + "VALUES(?,?,?,?,?,?,?)");
                    pst.setInt(1, empId);
                    pst.setString(2, name);
                    pst.setDouble(3, salary);
                    pst.setDouble(4, salaryPerDay);
                    pst.setInt(5, workedDays);
                    pst.setString(6, phone);
                    pst.setInt(7, age);
                    pst.executeUpdate();

                    printEmployeeDetails(
                            new EmployeeRecord(empId, name, salary, salaryPerDay, workedDays, phone, age),
                            "inserted into DB.");
                    loadTable();
                    printAllEmployees();
                    JOptionPane.showMessageDialog(this, "Employee inserted successfully!");
                    dispose();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "ID and Worked Days must be numbers.");
                } catch (SQLException ex) {
                    if (ex.getErrorCode() == 1) {
                        JOptionPane.showMessageDialog(this, "Insert failed: Employee ID already exists.");
                    } else {
                        JOptionPane.showMessageDialog(this, "Insert Error: " + ex.getMessage());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Insert Error!");
                }
            });

            setVisible(true);
        }

        void previewSalary() {
            try {
                int workedDays = Integer.parseInt(workedDaysField.getText().trim());
                int roleIdx = roleCombo.getSelectedIndex();
                double salaryPerDay = getSalaryPerDayForIndex(roleIdx);
                double salary = salaryPerDay * workedDays;
                salaryLabel.setText("Calculated Salary: Rs. " + salary);
            } catch (NumberFormatException ex) {
                salaryLabel.setText("Enter valid number of days.");
            }
        }
    }

    // ================================================================
    // Inner class: UpdateForm
    // ================================================================
    class UpdateForm extends JFrame {
        JTextField idField, nameField, ageField, phoneField, workedDaysField;
        JComboBox<String> roleCombo;
        JLabel salaryLabel;
        EmployeeRecord loaded;

        UpdateForm() {
            setTitle("Update Employee");
            setSize(460, 460);
            setLayout(new GridLayout(10, 2, 10, 10));
            setLocationRelativeTo(null);

            idField = new JTextField();
            nameField = new JTextField();
            ageField = new JTextField();
            phoneField = new JTextField();
            workedDaysField = new JTextField();

            String[] comboItems = new String[ROLES.length];
            for (int i = 0; i < ROLES.length; i++) {
                comboItems[i] = ROLES[i] + "  (Rs. " + SALARY_PER_DAY[i] + "/day)";
            }
            roleCombo = new JComboBox<>(comboItems);
            salaryLabel = new JLabel("Calculated Salary: --");

            nameField.setEnabled(false);
            ageField.setEnabled(false);
            phoneField.setEnabled(false);
            workedDaysField.setEnabled(false);
            roleCombo.setEnabled(false);

            add(new JLabel("Employee ID:"));
            add(idField);

            JButton fetchBtn = new JButton("Fetch");
            add(new JLabel(""));
            add(fetchBtn);

            add(new JLabel("Name:"));
            add(nameField);
            add(new JLabel("Age:"));
            add(ageField);
            add(new JLabel("Phone Number:"));
            add(phoneField);
            add(new JLabel("Worked Days:"));
            add(workedDaysField);
            add(new JLabel("Role (Salary/Day):"));
            add(roleCombo);
            add(salaryLabel);
            add(new JLabel(""));

            JButton calcBtn = new JButton("Preview Salary");
            JButton updateBtn = new JButton("Update");
            calcBtn.setEnabled(false);
            updateBtn.setEnabled(false);
            add(calcBtn);
            add(updateBtn);

            // Fetch
            fetchBtn.addActionListener(e -> {
                try {
                    if (con == null) {
                        JOptionPane.showMessageDialog(this, "Not connected to DB.");
                        return;
                    }
                    String rawId = idField.getText();
                    if (rawId == null || rawId.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Enter an Employee ID.");
                        return;
                    }
                    int empId = Integer.parseInt(rawId.trim());
                    loaded = fetchEmployeeById(empId);
                    if (loaded == null) {
                        JOptionPane.showMessageDialog(this, "Employee not found!");
                        nameField.setText("");
                        ageField.setText("");
                        phoneField.setText("");
                        workedDaysField.setText("");
                        salaryLabel.setText("Calculated Salary: --");
                        nameField.setEnabled(false);
                        ageField.setEnabled(false);
                        phoneField.setEnabled(false);
                        workedDaysField.setEnabled(false);
                        roleCombo.setEnabled(false);
                        updateBtn.setEnabled(false);
                        calcBtn.setEnabled(false);
                        return;
                    }
                    idField.setEnabled(false);
                    nameField.setEnabled(true);
                    ageField.setEnabled(true);
                    phoneField.setEnabled(true);
                    workedDaysField.setEnabled(true);
                    roleCombo.setEnabled(true);
                    updateBtn.setEnabled(true);
                    calcBtn.setEnabled(true);

                    nameField.setText(loaded.name);
                    ageField.setText(String.valueOf(loaded.age));
                    phoneField.setText(loaded.phoneNumber);
                    workedDaysField.setText(String.valueOf(loaded.workedDays));
                    salaryLabel.setText("Calculated Salary: Rs. " + loaded.salary);

                    // Match role by salary per day
                    int matchIdx = 0;
                    for (int i = 0; i < SALARY_PER_DAY.length; i++) {
                        if (SALARY_PER_DAY[i] == (int) loaded.salaryPerDay) {
                            matchIdx = i;
                            break;
                        }
                    }
                    roleCombo.setSelectedIndex(matchIdx);
                    printEmployeeDetails(loaded, "loaded for update...");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid ID!");
                }
            });

            // Preview
            calcBtn.addActionListener(e -> {
                try {
                    int wd = Integer.parseInt(workedDaysField.getText().trim());
                    double spd = getSalaryPerDayForIndex(roleCombo.getSelectedIndex());
                    salaryLabel.setText("Calculated Salary: Rs. " + (spd * wd));
                } catch (NumberFormatException ex) {
                    salaryLabel.setText("Enter valid number of days.");
                }
            });

            // Update
            updateBtn.addActionListener(e -> {
                try {
                    if (con == null) {
                        JOptionPane.showMessageDialog(this, "Not connected to DB.");
                        return;
                    }
                    if (loaded == null) {
                        JOptionPane.showMessageDialog(this, "Fetch an employee by ID first.");
                        return;
                    }
                    int empId = loaded.empId;

                    String newName = nameField.getText().trim();
                    if (newName.isEmpty())
                        newName = loaded.name;

                    // Age validation
                    String rawAge = ageField.getText().trim();
                    int newAge = loaded.age;
                    if (!rawAge.isEmpty()) {
                        try {
                            newAge = Integer.parseInt(rawAge);
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(this, "Enter your age correctly",
                                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        if (!isValidAge(newAge)) {
                            if (newAge >= 0 && newAge < 18) {
                                JOptionPane.showMessageDialog(this, "you are not eligeble for workinh in the shop",
                                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(this, "Enter your age correctly",
                                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                            }
                            return;
                        }
                    }

                    // Phone validation
                    String newPhone = phoneField.getText().trim();
                    if (!newPhone.isEmpty() && !isValidPhone(newPhone)) {
                        JOptionPane.showMessageDialog(this,
                                "Invalid phone number!\nPhone must be exactly 10 digits.",
                                "Validation Error", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (newPhone.isEmpty())
                        newPhone = loaded.phoneNumber;

                    String rawDays = workedDaysField.getText().trim();
                    int newDays = rawDays.isEmpty() ? loaded.workedDays : Integer.parseInt(rawDays);

                    double newSpd = getSalaryPerDayForIndex(roleCombo.getSelectedIndex());
                    double newSalary = newSpd * newDays;

                    PreparedStatement pst = con.prepareStatement(
                            "UPDATE emp_payroll SET name=?, salary=?, salaryperday=?, worked_days=?, phone_number=?, age=? "
                                    + "WHERE employee_id=?");
                    pst.setString(1, newName);
                    pst.setDouble(2, newSalary);
                    pst.setDouble(3, newSpd);
                    pst.setInt(4, newDays);
                    pst.setString(5, newPhone);
                    pst.setInt(6, newAge);
                    pst.setInt(7, empId);

                    int rows = pst.executeUpdate();
                    if (rows > 0) {
                        printEmployeeById(empId, "updated in DB.");
                        printAllEmployees();
                        JOptionPane.showMessageDialog(this, "Employee updated successfully!");
                    } else {
                        JOptionPane.showMessageDialog(this, "Employee not found!");
                    }
                    loadTable();
                    dispose();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Worked Days must be a number.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Update Error!");
                }
            });

            setVisible(true);
        }
    }

    // ----------------------------------------------------------------
    // Main
    // ----------------------------------------------------------------
    public static void main(String[] args) {
        new EmployeeOracleApp();
    }
}
