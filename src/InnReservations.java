import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Arrays;


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

    private static void Reservations() throws SQLException {

        System.out.println("Reservations\r\n");
        loadDriver();

        // Step 1: Establish connection to RDBMS
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {
            // Step 2: Construct SQL statement
            Scanner scanner = new Scanner(System.in);

            System.out.print("What's the first name? ");
            String f_name = scanner.nextLine();

            System.out.print("What's the last name? ");
            String l_name = scanner.nextLine();

            System.out.print("What's the room code? ");
            String room_code = scanner.nextLine();

            System.out.print("What's the bed type? ");
            String bed_type = scanner.nextLine();

            System.out.print("What will be the begin date of stay (YYYY-MM-DD)? ");
            LocalDate checkIn = LocalDate.parse(scanner.nextLine());

            System.out.print("What will be the end date of stay (YYYY-MM-DD)? ");
            LocalDate checkOut = LocalDate.parse(scanner.nextLine());

            System.out.print("For how many children? ");
            String num_children = scanner.nextLine();

            System.out.print("For how many adults? ");
            String num_adults = scanner.nextLine();

//            System.out.format("Until what date will %s be available (YYYY-MM-DD)? ", flavor);


//            String updateSql = "UPDATE hp_goods SET AvailUntil = ? WHERE Flavor = ?";
//
//            // Step 3: Start transaction
//            conn.setAutoCommit(false);
//
//            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
//
//                // Step 4: Send SQL statement to DBMS
//                pstmt.setDate(1, java.sql.Date.valueOf(availDt));
//                pstmt.setString(2, flavor);
//                int rowCount = pstmt.executeUpdate();
//
//                // Step 5: Handle results
//                System.out.format("Updated %d records for %s pastries%n", rowCount, flavor);
//
//                // Step 6: Commit or rollback transaction
//                conn.commit();
//            } catch (SQLException e) {
//                conn.rollback();
//            }

        }
        // Step 7: Close connection (handled implcitly by try-with-resources syntax)
    }

    private static void ReservationChange() throws SQLException {
        System.out.println("Reservation Change");
//        If there is a conflict with the begin date and or end date, do we cancel the change
//        or change the other values still? I'd assume no change is made to anything
        loadDriver();

        // Step 1: Establish connection to RDBMS
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {

            // Prompt User for Reservation Code
            Scanner scanner = new Scanner(System.in);
            System.out.print("What's the reservation code? ");
            String r_code = scanner.nextLine();

            // Prompt User for confirmation to continue
            System.out.println("\nFor the following fields, type the new value or type 'none' for zero changes to that field");
            System.out.print("What's the updated First Name? ");
            String firstName = scanner.nextLine();
            System.out.print("What's the updated Last Name? ");
            String lastName = scanner.nextLine();

            System.out.print("What's the updated begin date (YYYY-MM-DD)? ");
            String checkInStr = scanner.nextLine();

            System.out.print("What's the updated end date (YYYY-MM-DD)? ");
            String checkOutStr = scanner.nextLine();

            System.out.print("What's the updated number of children? ");
            String numChildren = scanner.nextLine();
            System.out.print("What's the updated number of adults? ");
            String numAdults = scanner.nextLine();


            // Step 2: Construct SQL statement
            String sqlStmtLeft = "UPDATE rbeltr01.lab7_reservations SET";
            String sqlStmtRight = " WHERE CODE = ?";
            // Modify setConditions based on what was given, and what works with existing data
            if(!firstName.equals("none"))
                sqlStmtLeft += " FirstName = ?," ;

            if (!lastName.equals("none"))
                sqlStmtLeft += " LastName = ?,";

            if (!numChildren.equals("none"))
                sqlStmtLeft += " Kids = ?,";

            if (!numAdults.equals("none"))
                sqlStmtLeft += " Adults = ?,";

            if (!checkInStr.equals("none")){
                // Have more logic for checking conflicts ......
                LocalDate checkIn = LocalDate.parse(checkInStr);
                sqlStmtLeft += " CheckIn = ?,";
            }

            if (!checkOutStr.equals("none")){
                // Have more logic for checking conflicts ......
                LocalDate checkOut = LocalDate.parse(checkOutStr);
                sqlStmtLeft += " Checkout = ?,";
            }

            // Step 3: Start transaction
            conn.setAutoCommit(false);

            String sqlStmt = sqlStmtLeft.substring(0,sqlStmtLeft.length() - 1) + sqlStmtRight;

            try (PreparedStatement pstmt = conn.prepareStatement(sqlStmt)) {

                // Step 4: Send SQL statement to DBMS
                int pos = 1;

                if(!firstName.equals("none")){
                    pstmt.setString(pos, firstName);
                    pos++;
                }

                if (!lastName.equals("none")){
                    pstmt.setString(pos, lastName);
                    pos++;
                }

                if (!numChildren.equals("none")){
                    pstmt.setInt(pos, Integer.parseInt(numChildren));
                    pos++;
                }

                if (!numAdults.equals("none")){
                    pstmt.setInt(pos, Integer.parseInt(numAdults));
                    pos++;
                }

                // My mortal enemies, Cannot resolve method 'setDate(java.sql.Date)' HUHHHHHH
//                if (!checkInStr.equals("none")){
//                    // Have more logic for checking conflicts ......
//                    LocalDate checkIn = LocalDate.parse(checkInStr);
//                    pstmt.setDate(java.sql.Date.valueOf(checkIn));
//                    pstmt.set
//                    pos++;
//                    java.sql.Date.valueOf(checkIn);
//                }
//
//                if (!checkOutStr.equals("none")){
//                    // Have more logic for checking conflicts ......
//                    LocalDate checkOut = LocalDate.parse(checkOutStr);
//                    pstmt.setString(checkOut);
//                    pos++;
//                }

                pstmt.setInt(2, Integer.parseInt(r_code));

                System.out.println(pstmt);
//                int rowCount = pstmt.executeUpdate();
//
//                // Step 5: Handle results
//                if (rowCount == 0)
//                    System.out.println("\nReservation Code not found in our records.");
//                System.out.format("Updated %d records for reservations", rowCount);
//
//                // Step 6: Commit or rollback transaction
//                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            }


        }
    }

    private static void ReservationCancellation() throws SQLException {
        System.out.println("Reservation Cancellation");
        loadDriver();

        // Step 1: Establish connection to RDBMS
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {

            // Prompt User for Reservation Code
            Scanner scanner = new Scanner(System.in);
            System.out.print("What's the reservation code? ");
            String r_code = scanner.nextLine();

            // Prompt User for confirmation to continue
            System.out.print("Are you sure you want to cancel the reservation? ('yes' or 'no') ");
            Boolean confirmation = scanner.nextLine().equals("yes");
            if (confirmation) {
                // Step 2: Construct SQL statement
                String sqlStmt = "DELETE FROM rbeltr01.lab7_reservations\n" +
                        "WHERE CODE = ?";

                // Step 3: Start transaction
                conn.setAutoCommit(false);

                try (PreparedStatement pstmt = conn.prepareStatement(sqlStmt)) {

                    // Step 4: Send SQL statement to DBMS
                    pstmt.setString(1, r_code);
                    int rowCount = pstmt.executeUpdate();

                    // Step 5: Handle results
                    if (rowCount == 0)
                        System.out.println("\nReservation Code not found in our records.");
                    System.out.format("Updated %d records for reservations", rowCount);

                    // Step 6: Commit or rollback transaction
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                }
            }


        }
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
                    System.out.println("Before func");
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
                System.out.print("\n\nChoose another option: ");
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