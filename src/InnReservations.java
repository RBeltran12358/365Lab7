import java.sql.*;
import java.time.LocalDate;
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

    private static void RoomsAndRates() throws SQLException{
        System.out.println("Room and Rates");
        loadDriver();

        // Step 1: Establish connection to RDBMS
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {

            // Step 2: Construct SQL statement
            String sqlStmt = "select RoomCode, RoomName, Beds, BedType, MaxOcc, BasePrice, Decor, \n" +
                    "PopularityScore, NextAvailCheckIn, datediff(checkout, checkin) as LengthOfMostRecentStay\n" +
                    "from \n" +
                    "    rbeltr01.lab7_reservations r1 join rbeltr01.lab7_rooms on Room = RoomCode\n" +
                    "    join (select Room,\n" +
                    "        round( \n" +
                    "            sum(datediff(least(checkout,  DATE(CURRENT_TIMESTAMP)), \n" +
                    "            greatest(checkin,  ADDDATE(DATE(CURRENT_TIMESTAMP), INTERVAL -180 DAY))))\n" +
                    "            / 180, 2) PopularityScore, \n" +
                    "        max(checkout) as NextAvailCheckIn\n" +
                    "    from \n" +
                    "        rbeltr01.lab7_reservations \n" +
                    "    where\n" +
                    "        checkin <= DATE(CURRENT_TIMESTAMP) AND\n" +
                    "        checkout >=  ADDDATE(DATE(CURRENT_TIMESTAMP), INTERVAL -180 DAY)\n" +
                    "    group by Room) t\n" +
                    "    on r1.Room = t.Room and NextAvailCheckIn = Checkout \n" +
                    "where\n" +
                    "    checkout < DATE(CURRENT_TIMESTAMP)\n" +
                    "order by PopularityScore desc; ";

            // Step 3: Start transaction
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sqlStmt)) {

                // Step 4: Send SQL statement to DBMS
                ResultSet res = pstmt.executeQuery(sqlStmt);
                ResultSetMetaData rsmd = res.getMetaData();
                int colCount = rsmd.getColumnCount();

                for (int i = 1; i < colCount; i++) {
                    System.out.printf("%-30s", rsmd.getColumnName(i));
                }

                System.out.println("");

                while (res.next()) {
                    System.out.println("");
                    for (int i = 1; i < colCount; i++) {
                        System.out.printf("%-30s", res.getString(i));
                    }
                }

                System.out.println("");

                // Step 6: Commit or rollback transaction
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            }
        }
    }

    private static void Reservations() {
        System.out.println("2");
    }

    private static void ReservationChange() {
        System.out.println("3");
    }

    private static void ReservationCancellation() {
        System.out.println("4");
    }

    private static void DetailedReservationInformation() {
        System.out.println("5");
    }

    private static void Revenue() {
        System.out.println("6");
    }

    private static void printIntro() {
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