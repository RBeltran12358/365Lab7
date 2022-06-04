import java.sql.*;
import java.util.Scanner;


//1. Connection to the database
//2. Construct SQL statement (as a String)
//3. Start a transaction (implicitly or explicitly)
//4. Send SQL statement to the DBMS
//5. Receive result
//6. Commit (or rollback) transaction (sometimes implicit)
//7. Close connection

public class InnReservations {

    private static void loadDriver() throws SQLException{
        // Step 0: Load MySQL JDBC Driver
        try{
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("MySQL JDBC Driver loaded");
        } catch (ClassNotFoundException ex) {
            System.err.println("Unable to load JDBC Driver");
            System.exit(-1);
        }
    }


//    Output a list of rooms to  the  user  sorted  by  popularity  (highest  to  lowest)
//    Include in your output all columns from the roomstable, as well as the following:
//       Room popularity score:  number of days the room has been occupied during the previous 180 days divided by 180 (round to two decimal places)
//       Next available check-in date.
//       Length in days and check out date of the most recent (completed) stay in the room.

    public static void RoomsAndRates() throws SQLException{
        System.out.println("Room and Rates");
        loadDriver();

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
        System.out.println("1:\tRooms and Rates");
        System.out.println("2:\tReservations");
        System.out.println("3:\tReservation Change");
        System.out.println("4:\tReservation Cancellation");
        System.out.println("5:\tDetailed Reservation Information");
        System.out.println("6:\tRevenue");
        System.out.println("7:\tQuit Application\n");
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