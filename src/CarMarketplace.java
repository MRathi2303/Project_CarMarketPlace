import java.sql.*;
import java.util.Scanner;

public class CarMarketplace {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/CarMarketDB";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Mr@230305";

    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Connected to the database.");

            Scanner scanner = new Scanner(System.in);
            System.out.println("Are you a customer or developer? (Enter 'customer' or 'developer')");
            String role = scanner.nextLine().toLowerCase();

            if (role.equals("customer")) {
                handleCustomer(connection, scanner);
            } else if (role.equals("developer")) {
                handleDeveloper(connection, scanner);
            } else {
                System.out.println("Invalid input. Exiting...");
            }
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }
    }

    private static void handleCustomer(Connection connection, Scanner scanner) throws SQLException {
        System.out.println("Enter minimum price:");
        double minPrice = scanner.nextDouble();
        System.out.println("Enter maximum price:");
        double maxPrice = scanner.nextDouble();

        scanner.nextLine(); // Consume leftover newline
        System.out.println("Enter fuel type (optional, leave blank to skip):");
        String fuelType = scanner.nextLine();
        System.out.println("Enter brand (optional, leave blank to skip):");
        String brand = scanner.nextLine();

        String query = "SELECT c.car_id, c.brand, c.model, c.price, ft.fuel_type " +
                "FROM Cars c " +
                "JOIN CarFuelOptions cfo ON c.car_id = cfo.car_id " +
                "JOIN FuelTypes ft ON cfo.fuel_id = ft.fuel_id " +
                "WHERE c.price BETWEEN ? AND ?";

        if (!fuelType.isEmpty()) query += " AND ft.fuel_type = ?";
        if (!brand.isEmpty()) query += " AND c.brand = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setDouble(1, minPrice);
            pstmt.setDouble(2, maxPrice);
            int paramIndex = 3;

            if (!fuelType.isEmpty()) pstmt.setString(paramIndex++, fuelType);
            if (!brand.isEmpty()) pstmt.setString(paramIndex, brand);

            ResultSet rs = pstmt.executeQuery();

            System.out.println("Available Cars:");
            while (rs.next()) {
                System.out.printf("ID: %d, Brand: %s, Model: %s, Price: %.2f, Fuel Type: %s%n",
                        rs.getInt("car_id"), rs.getString("brand"), rs.getString("model"),
                        rs.getDouble("price"), rs.getString("fuel_type"));
            }

            System.out.println("Enter the ID of the car you are interested in for on-road price calculation (or 0 to exit):");
            int carId = scanner.nextInt();

            if (carId != 0) {
                System.out.println("Enter your state (Delhi, Bangalore, UP, Haryana, Mumbai, Punjab, Gujarat):");
                scanner.nextLine(); // Consume newline
                String state = scanner.nextLine();

                calculateOnRoadPrice(connection, carId, state);
            }
        }
    }

    private static void calculateOnRoadPrice(Connection connection, int carId, String state) throws SQLException {
        String query = "SELECT price FROM Cars WHERE car_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, carId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                double price = rs.getDouble("price");
                double taxPercentage = getStateTaxPercentage(state);
                double insurancePercentage = 5.0; // Flat insurance percentage
                double tax = price * taxPercentage / 100;
                double insurance = price * insurancePercentage / 100;
                double onRoadPrice = price + tax + insurance;

                System.out.printf("Base Price: %.2f, Tax: %.2f, Insurance: %.2f, On-Road Price: %.2f%n",
                        price, tax, insurance, onRoadPrice);
            } else {
                System.out.println("Car not found.");
            }
        }
    }

    private static double getStateTaxPercentage(String state) {
        switch (state.toLowerCase()) {
            case "delhi": return 10.0;
            case "bangalore": return 12.0;
            case "up": return 8.0;
            case "haryana": return 9.0;
            case "mumbai": return 13.0;
            case "punjab": return 7.0;
            case "gujarat": return 6.0;
            default: return 10.0; // Default tax percentage
        }
    }

    private static void handleDeveloper(Connection connection, Scanner scanner) throws SQLException {
        while (true) {
            System.out.println("Developer Options:");
            System.out.println("1. Add Car");
            System.out.println("2. Update Car");
            System.out.println("3. Delete Car");
            System.out.println("4. View All Cars");
            System.out.println("5. Exit");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1 -> addCar(connection, scanner);
                case 2 -> updateCar(connection, scanner);
                case 3 -> deleteCar(connection, scanner);
                case 4 -> viewAllCars(connection);
                case 5 -> {
                    System.out.println("Exiting developer mode.");
                    return;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private static void addCar(Connection connection, Scanner scanner) throws SQLException {
        System.out.println("Enter car brand:");
        scanner.nextLine(); // Consume newline
        String brand = scanner.nextLine();
        System.out.println("Enter car model:");
        String model = scanner.nextLine();
        System.out.println("Enter car price:");
        double price = scanner.nextDouble();
        System.out.println("Enter fuel economy (km/l):");
        double economy = scanner.nextDouble();

        String query = "INSERT INTO Cars (brand, model, price, economy) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, brand);
            pstmt.setString(2, model);
            pstmt.setDouble(3, price);
            pstmt.setDouble(4, economy);
            pstmt.executeUpdate();
            System.out.println("Car added successfully.");
        }
    }

    private static void updateCar(Connection connection, Scanner scanner) throws SQLException {
        System.out.println("Enter car ID to update:");
        int carId = scanner.nextInt();
        System.out.println("Enter new brand:");
        scanner.nextLine(); // Consume newline
        String brand = scanner.nextLine();
        System.out.println("Enter new model:");
        String model = scanner.nextLine();
        System.out.println("Enter new price:");
        double price = scanner.nextDouble();
        System.out.println("Enter new fuel economy:");
        double economy = scanner.nextDouble();

        String query = "UPDATE Cars SET brand = ?, model = ?, price = ?, economy = ? WHERE car_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, brand);
            pstmt.setString(2, model);
            pstmt.setDouble(3, price);
            pstmt.setDouble(4, economy);
            pstmt.setInt(5, carId);
            pstmt.executeUpdate();
            System.out.println("Car updated successfully.");
        }
    }

    private static void deleteCar(Connection connection, Scanner scanner) throws SQLException {
        System.out.println("Enter car ID to delete:");
        int carId = scanner.nextInt();

        String query = "DELETE FROM Cars WHERE car_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, carId);
            pstmt.executeUpdate();
            System.out.println("Car deleted successfully.");
        }
    }

    private static void viewAllCars(Connection connection) throws SQLException {
        String query = "SELECT car_id, brand, model, price FROM Cars";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            System.out.println("All Cars:");
            while (rs.next()) {
                System.out.printf("ID: %d, Brand: %s, Model: %s, Price: %.2f%n",
                        rs.getInt("car_id"), rs.getString("brand"), rs.getString("model"),
                        rs.getDouble("price"));
            }
        }
    }
}
