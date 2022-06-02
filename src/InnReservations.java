import java.sql.*;
import java.util.Scanner;


public class InnReservations {

    public static void RoomsAndRates() throws SQLException{
        System.out.println("Room and Rates");

        // Step 0: Load MySQL JDBC Driver
        // No longer required as of JDBC 2.0  / Java 6
        try{
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("MySQL JDBC Driver loaded");
        } catch (ClassNotFoundException ex) {
            System.err.println("Unable to load JDBC Driver");
            System.exit(-1);
        }

        // Step 1: Establish connection to RDBMS
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {

            // Step 2: Construct SQL statement
            String sql = "select RoomName from rbeltr01.lab7_rooms";

            // Step 3: (omitted in this example) Start transaction
            try (Statement stmt = conn.createStatement()) {

                // Step 4: Send SQL statement to DBMS
                boolean exRes = stmt.execute(sql);
                ResultSet res = stmt.executeQuery(sql);


                while(res.next()){
                    System.out.println(res.getString("RoomName"));
                }



                // Step 5: Handle results
                System.out.format("Result from ALTER: %b %n", exRes);
            }

            // Step 6: (omitted in this example) Commit or rollback transaction
        }
        // Step 7: Close connection (handled by try-with-resources syntax)
    }

    public static void Reservations() {
        System.out.println("2");
    }

    public static void ReservationChange() {
        System.out.println("3");
    }

    public static void ReservationCancellation() {
        System.out.println("4");
    }

    public static void DetailedReservationInformation() {
        System.out.println("5");
    }

    public static void Revenue() {
        System.out.println("6");
    }

    public static void printIntro() {
        System.out.println("Welcome to our database!");
        System.out.println("To select an option from the list below, type the number to its right and press Enter\n");
        System.out.println("Rooms and Rates: 1");
        System.out.println("Reservations: 2");
        System.out.println("Reservation Change: 3");
        System.out.println("Reservation Cancellation: 4");
        System.out.println("Detailed Reservation Information: 5");
        System.out.println("Revenue: 6");
        System.out.println("Quit Application: 7\n");
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int response = 0;
        InnReservations ir = new InnReservations();
        printIntro();

        while(true){
            try {
                response = in.nextInt();
                if(response == 1){
                    RoomsAndRates();
                } else if(response == 2){
                    Reservations();
                } else if(response == 3){
                    ReservationChange();
                } else if(response == 4){
                    ReservationCancellation();
                } else if(response == 5){
                    DetailedReservationInformation();
                } else if(response == 6){
                    Revenue();
                } else if(response == 7){
                    break;
                }else{
                    System.out.println("Number not in range of options");
                }
            }
            catch (SQLException e) {
                System.err.println("SQLException: " + e.getMessage());
            }
            catch (Exception e2) {
//                System.out.println("Wrong format: Please input a digit from 1-7 inclusive and press Enter");
                System.err.println("Exception: " + e2.getMessage());
            }


        }
        System.out.println("Exiting Application :)");
    }

}