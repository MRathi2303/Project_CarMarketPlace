import javax.swing.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;

public class CarMarketplaceGUI {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/CarMarketDB";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Mr@230305";

    public static void main(String[] args) {
        // Create the main frame
        JFrame frame = new JFrame("Car Marketplace");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600);

        // Labels and input fields for filters
        JLabel priceLabel = new JLabel("Price Range:");
        priceLabel.setBounds(50, 50, 100, 20);
        JTextField minPriceField = new JTextField("Min");
        minPriceField.setBounds(150, 50, 100, 20);
        JTextField maxPriceField = new JTextField("Max");
        maxPriceField.setBounds(260, 50, 100, 20);

        JLabel fuelLabel = new JLabel("Fuel Type:");
        fuelLabel.setBounds(50, 100, 100, 20);
        JComboBox<String> fuelCombo = new JComboBox<>(new String[]{"", "Petrol", "Diesel", "Electric"});
        fuelCombo.setBounds(150, 100, 150, 20);

        // City selection
        JLabel cityLabel = new JLabel("Select City:");
        cityLabel.setBounds(50, 150, 100, 20);
        JComboBox<String> cityCombo = new JComboBox<>(new String[]{"UP", "Delhi", "Bangalore"});
        cityCombo.setBounds(150, 150, 150, 20);

        // Search button
        JButton searchButton = new JButton("Search");
        searchButton.setBounds(200, 200, 100, 30);

        // JList for displaying cars
        JList<String> carList = new JList<>();
        carList.setBounds(50, 250, 500, 100);
        JScrollPane carListScrollPane = new JScrollPane(carList);
        carListScrollPane.setBounds(50, 250, 500, 100);

        // Result area to show on-road price
        JTextArea resultArea = new JTextArea();
        resultArea.setBounds(50, 400, 500, 100);
        resultArea.setEditable(false);
        JScrollPane resultScrollPane = new JScrollPane(resultArea);
        resultScrollPane.setBounds(50, 400, 500, 100);

        // Add components to frame
        frame.add(priceLabel);
        frame.add(minPriceField);
        frame.add(maxPriceField);
        frame.add(fuelLabel);
        frame.add(fuelCombo);
        frame.add(cityLabel);
        frame.add(cityCombo);
        frame.add(searchButton);
        frame.add(carListScrollPane);
        frame.add(resultScrollPane);
        frame.setLayout(null);
        frame.setVisible(true);

        // Action listener for the search button
        searchButton.addActionListener(e -> {
            try {
                // Fetch filter inputs
                double minPrice = Double.parseDouble(minPriceField.getText());
                double maxPrice = Double.parseDouble(maxPriceField.getText());
                String fuelType = (String) fuelCombo.getSelectedItem();

                // Connect to the database
                try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                    // Build the query dynamically
                    String query = "SELECT c.car_id, c.brand, c.model, c.price, ft.fuel_type " +
                            "FROM Cars c " +
                            "JOIN CarFuelOptions cfo ON c.car_id = cfo.car_id " +
                            "JOIN FuelTypes ft ON cfo.fuel_id = ft.fuel_id " +
                            "WHERE c.price BETWEEN ? AND ?";
                    if (!fuelType.isEmpty()) {
                        query += " AND ft.fuel_type = ?";
                    }

                    // Prepare the statement
                    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                        pstmt.setDouble(1, minPrice);
                        pstmt.setDouble(2, maxPrice);
                        if (!fuelType.isEmpty()) {
                            pstmt.setString(3, fuelType);
                        }

                        // Execute the query
                        ResultSet rs = pstmt.executeQuery();

                        // Create a list to display car models
                        DefaultListModel<String> carListModel = new DefaultListModel<>();
                        while (rs.next()) {
                            String carDetails = rs.getString("brand") + " " + rs.getString("model");
                            carListModel.addElement(carDetails);
                        }
                        carList.setModel(carListModel);
                    }
                }
            } catch (Exception ex) {
                resultArea.setText("Error: " + ex.getMessage());
            }
        });

        // Add a listener to handle car selection
        carList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedCar = carList.getSelectedValue();
                if (selectedCar != null) {
                    // Extract the selected car model from the list
                    String[] carDetails = selectedCar.split(" ");
                    String brand = carDetails[0];
                    String model = carDetails[1];

                    // Get the price from the database for the selected car
                    double basePrice = 0.0;
                    try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                        String priceQuery = "SELECT price FROM Cars WHERE brand = ? AND model = ?";
                        try (PreparedStatement pstmt = connection.prepareStatement(priceQuery)) {
                            pstmt.setString(1, brand);
                            pstmt.setString(2, model);
                            ResultSet rs = pstmt.executeQuery();
                            if (rs.next()) {
                                basePrice = rs.getDouble("price");
                            }
                        }

                        // Get the selected city from the combo box
                        String city = (String) cityCombo.getSelectedItem();

                        // Calculate the on-road price
                        double onRoadPrice = calculateOnRoadPrice(basePrice, city);

                        // Display the on-road price in the result area
                        resultArea.setText("Selected Car: " + selectedCar + "\n" +
                                "On-road Price in " + city.toUpperCase() + ": ₹" + onRoadPrice);
                    } catch (SQLException ex) {
                        resultArea.setText("Error: " + ex.getMessage());
                    }
                }
            }
        });
    }

    // Method to calculate on-road price based on city
    public static double calculateOnRoadPrice(double basePrice, String city) {
        double taxRate = 0;
        double insuranceRate = 0;

        // Tax and insurance percentages based on city
        switch (city.toLowerCase()) {
            case "delhi":
                taxRate = 0.12; // Example 12% tax
                insuranceRate = 0.05; // Example 5% insurance
                break;
            case "up":
                taxRate = 0.10; // 10% tax
                insuranceRate = 0.04; // 4% insurance
                break;
            case "bangalore":
                taxRate = 0.11; // 11% tax
                insuranceRate = 0.05; // 5% insurance
                break;
            // Add other cities as needed
            default:
                taxRate = 0.10; // Default 10% tax
                insuranceRate = 0.04; // Default 4% insurance
                break;
        }

        // Calculate the tax and insurance
        double taxAmount = basePrice * taxRate;
        double insuranceAmount = basePrice * insuranceRate;

        // Calculate and return the on-road price
        return basePrice + taxAmount + insuranceAmount;
    }
}
