package Mypack;
import java.sql.*;
import java.util.Scanner;

public class EmployeeConsoleApp {

    static Scanner sc = new Scanner(System.in);

    public static Connection getConnection() throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        return DriverManager.getConnection(
                "jdbc:oracle:thin:@localhost:1521:xe", "system", "oracle");
    }

    public static boolean isValidName(String name) {
        return name.matches("[a-zA-Z ]+");
    }

    public static boolean isValidPhone(String phone) {
        return phone.matches("\\d{10}");
    }

    public static boolean validate(String name, String phone, int age, int salary, int days) {

        if (!isValidName(name)) {
            System.out.println("❌ Name must contain only alphabets!");
            return false;
        }

        if (!isValidPhone(phone)) {
            System.out.println("❌ Phone must be exactly 10 digits!");
            return false;
        }

        if (age < 18) {
            System.out.println("❌ Not Allowed! Age must be 18+");
            return false;
        }

        if (age < 0 || salary < 0 || days < 0) {
            System.out.println("❌ Negative values not allowed!");
            return false;
        }

        if (salary <= 10) {
            System.out.println("❌ Salary must be greater than 10!");
            return false;
        }

        if (days <= 0) {
            System.out.println("❌ Days must be greater than 0!");
            return false;
        }

        return true;
    }

    // 🔹 Safe Integer Input
    public static Integer getIntInput(String message) {
        System.out.print(message);
        String input = sc.nextLine();

        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid number! Please enter a valid integer.");
            return null;
        }
    }

    // 🔹 INSERT
    public static void insert() {
        try {
            System.out.print("Enter Name: ");
            String name = sc.nextLine();

            System.out.print("Enter Phone: ");
            String phone = sc.nextLine();

            Integer age = getIntInput("Enter Age: ");
            if (age == null) return;

            Integer salary = getIntInput("Enter Salary per Day: ");
            if (salary == null) return;

            Integer days = getIntInput("Enter Days Worked: ");
            if (days == null) return;

            if (!validate(name, phone, age, salary, days)) return;

            int total = salary * days;

            Connection con = getConnection();

            String query = "INSERT INTO employee_reg(name, phone, age, salary_per_day, days_worked, total_salary) VALUES (?, ?, ?, ?, ?, ?)";

            PreparedStatement pst = con.prepareStatement(query);

            pst.setString(1, name);
            pst.setString(2, phone);
            pst.setInt(3, age);
            pst.setInt(4, salary);
            pst.setInt(5, days);
            pst.setInt(6, total);

            pst.executeUpdate();

            System.out.println("✅ Employee Inserted Successfully!");

            con.close();

        } catch (Exception e) {
            System.out.println("❌ Database Error: " + e.getMessage());
        }
    }

    // 🔹 UPDATE
    public static void update() {
        try {
            Integer id = getIntInput("Enter Employee ID: ");
            if (id == null) return;

            System.out.print("Enter Name: ");
            String name = sc.nextLine();

            System.out.print("Enter Phone: ");
            String phone = sc.nextLine();

            Integer age = getIntInput("Enter Age: ");
            if (age == null) return;

            Integer salary = getIntInput("Enter Salary per Day: ");
            if (salary == null) return;

            Integer days = getIntInput("Enter Days Worked: ");
            if (days == null) return;

            if (!validate(name, phone, age, salary, days)) return;

            int total = salary * days;

            Connection con = getConnection();

            String query = "UPDATE employee_reg SET name=?, phone=?, age=?, salary_per_day=?, days_worked=?, total_salary=? WHERE emp_id=?";

            PreparedStatement pst = con.prepareStatement(query);

            pst.setString(1, name);
            pst.setString(2, phone);
            pst.setInt(3, age);
            pst.setInt(4, salary);
            pst.setInt(5, days);
            pst.setInt(6, total);
            pst.setInt(7, id);

            int result = pst.executeUpdate();

            if (result > 0)
                System.out.println("✅ Updated Successfully!");
            else
                System.out.println("❌ Employee Not Found!");

            con.close();

        } catch (Exception e) {
            System.out.println("❌ Database Error: " + e.getMessage());
        }
    }

    // 🔹 DELETE
    public static void delete() {
        try {
            Integer id = getIntInput("Enter Employee ID: ");
            if (id == null) return;

            Connection con = getConnection();

            String query = "DELETE FROM employee_reg WHERE emp_id=?";
            PreparedStatement pst = con.prepareStatement(query);

            pst.setInt(1, id);

            int result = pst.executeUpdate();

            if (result > 0)
                System.out.println("✅ Deleted Successfully!");
            else
                System.out.println("❌ Employee Not Found!");

            con.close();

        } catch (Exception e) {
            System.out.println("❌ Database Error: " + e.getMessage());
        }
    }

    // 🔹 VIEW
    public static void view() {
        try {
            Connection con = getConnection();

            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM employee_reg");

            System.out.println("\n======================================================");
            System.out.println("ID | Name | Phone | Age | Salary | Days | Total Salary");
            System.out.println("======================================================");

            while (rs.next()) {
                System.out.println(
                        rs.getInt("emp_id") + " | " +
                        rs.getString("name") + " | " +
                        rs.getString("phone") + " | " +
                        rs.getInt("age") + " | " +
                        rs.getInt("salary_per_day") + " | " +
                        rs.getInt("days_worked") + " | " +
                        rs.getInt("total_salary")
                );
            }

            System.out.println("======================================================");

            con.close();

        } catch (Exception e) {
            System.out.println("❌ Database Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        while (true) {
            System.out.println("\n===== EMPLOYEE MANAGEMENT SYSTEM =====");
            System.out.println("1. Insert");
            System.out.println("2. Update");
            System.out.println("3. Delete");
            System.out.println("4. View");
            System.out.println("5. Exit");
            System.out.print("Enter choice: ");

            String input = sc.nextLine();

            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid choice!");
                continue;
            }

            switch (choice) {
                case 1: insert(); break;
                case 2: update(); break;
                case 3: delete(); break;
                case 4: view(); break;
                case 5: System.out.println("Exiting..."); System.exit(0);
                default: System.out.println("❌ Invalid choice!");
            }
        }
    }
}
