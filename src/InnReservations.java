import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Scanner;

public class InnReservations {

    private static void loadDriver() throws SQLException{
        // Step 0: Load MySQL JDBC Driver
        try{
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            System.err.println("Unable to load JDBC Driver");
            System.exit(-1);
        }
    }

    private static void RoomsAndRates() throws SQLException{
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

                for (int i = 1; i <= colCount; i++)
                    System.out.printf("%-30s", rsmd.getColumnName(i));

                System.out.println("");

                while (res.next()) {
                    System.out.println("");
                    for (int i = 1; i <= colCount; i++)
                        System.out.printf("%-30s", res.getString(i));
                }

                System.out.println("");

                // Step 6: Commit or rollback transaction
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            }
        }
    }

    private static double[] getCostOfStay(LocalDate checkIn, LocalDate checkOut, double baseprice) throws SQLException {
        LocalDate temp = checkIn;
        int numWeekendDays = 0;
        int numWeekDays = 0;
        double []ret = new double[2];

        while(!temp.isEqual(checkOut)){
            DayOfWeek day = DayOfWeek.of(temp.get(ChronoField.DAY_OF_WEEK));
            boolean isWeekend = day == DayOfWeek.SUNDAY || day == DayOfWeek.SATURDAY;
            // check if weekend
            if(isWeekend){
                numWeekendDays++;
            }else{
                numWeekDays++;
            }
            temp = temp.plusDays(1);
        }
        ret[0] = (numWeekDays * baseprice) + (numWeekendDays * (1.1 * baseprice));
        ret[1] = numWeekDays+numWeekendDays;
        return ret;
    }

    private static void Reservations() throws SQLException {
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

            System.out.print("What's the room code? (Type ???Any??? to if no preference) ");
            String room_code = scanner.nextLine();

            System.out.print("What's the bed type? (Type ???Any??? to if no preference) ");
            String bed_type = scanner.nextLine();

            System.out.print("What is the desired check in date (YYYY-MM-DD)? ");
            LocalDate checkIn = LocalDate.parse(scanner.nextLine());

            System.out.print("What is the desired check out date of stay (YYYY-MM-DD)? ");
            LocalDate checkOut = LocalDate.parse(scanner.nextLine());

            System.out.print("For how many children? ");
            String num_children = scanner.nextLine();

            System.out.print("For how many adults? ");
            String num_adults = scanner.nextLine();

            int desired_ocp = Integer.parseInt(num_children) + Integer.parseInt(num_adults);

            int maxOcc = -1;
            String[][] results = new String[5][10];

            conn.setAutoCommit(false);

            String sqlMaxOccQuery = "select max(maxOcc)\nfrom \n    rbeltr01.lab7_rooms;";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlMaxOccQuery)) {
                ResultSet res = pstmt.executeQuery();
                ResultSetMetaData rsmd = res.getMetaData();
                int colCount = rsmd.getColumnCount();
                // Step 5: Handle results
                while (res.next()) {
                    for (int i = 1; i < colCount + 1; i++)
                        maxOcc = Integer.parseInt(res.getString(1));
                }
                // Step 6: Commit or rollback transaction
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            }

            // Check for dates where checkin is after or same time as checkout
            boolean dateConflict = checkOut.isBefore(checkIn) || checkIn.isEqual(checkOut);

            if(maxOcc >= desired_ocp && !dateConflict ) {
                // Step 2: Construct SQL statement
                String sqlMatchQuery = "SELECT room, RoomName, Beds, BedType, MaxOcc, BasePrice, Decor, NextAvailableCheckInDate, SuggestedCheckOut, Priority\n" +
                        "from \n" +
                        "    ((select room, RoomName, Beds, BedType, MaxOcc, BasePrice, Decor, ? as NextAvailableCheckInDate, ? as SuggestedCheckOut,\n" +
                        "        case roomname in (\n" +
                        "                select roomname\n" +
                        "                from \n" +
                        "                    rbeltr01.lab7_reservations r1 join rbeltr01.lab7_rooms on Room = RoomCode\n" +
                        "                where\n" +
                        "                    (Checkout > ? and Checkout <= ?) OR\n" +
                        "                   (CheckIn >= ? and CheckIn < ?)\n" +
                        "                )\n" +
                        "            when true then 'OCCUPIED'\n" +
                        "            else 'Available'\n" +
                        "            end 'AvailableStatus',\n" +
                        "        '1' as Priority\n" +
                        "    from \n" +
                        "        rbeltr01.lab7_reservations r1 join rbeltr01.lab7_rooms on Room = RoomCode\n" +
                        "    where\n" +
                        "        RoomCode LIKE ? AND\n" +
                        "        bedType LIKE ? AND\n" +
                        "        maxOcc >= ?\n" +
                        "    group by room\n" +
                        "    having AvailableStatus = 'Available'\n" +
                        "    order by room) \n" +
                        "    \n" +
                        "    UNION\n" +
                        "    \n" +
                        "    (select room, RoomName, Beds, BedType, MaxOcc, BasePrice, Decor, max(checkout) as NextAvailableCheckInDate,\n" +
                        "    ADDDATE(max(checkout), datediff( ? , ? )) as SuggestedCheckOut, 'Available' as AvailableStatus, '2' as Priority\n" +
                        "    from \n" +
                        "        rbeltr01.lab7_reservations r1 join rbeltr01.lab7_rooms on Room = RoomCode\n" +
                        "    where\n" +
                        "        maxOcc >= ?\n" +
                        "    group by room)) t\n" +
                        "order by priority\n" +
                        "limit 5;";

                // Step 3: Start transaction
                conn.setAutoCommit(false);

                try (PreparedStatement pstmt = conn.prepareStatement(sqlMatchQuery)) {
                    // Inject field values
                    pstmt.setDate(1, java.sql.Date.valueOf(checkIn));
                    pstmt.setDate(2, java.sql.Date.valueOf(checkOut));
                    pstmt.setDate(3, java.sql.Date.valueOf(checkIn));
                    pstmt.setDate(4, java.sql.Date.valueOf(checkOut));
                    pstmt.setDate(5, java.sql.Date.valueOf(checkIn));
                    pstmt.setDate(6, java.sql.Date.valueOf(checkOut));

                    if(room_code.equals("any"))
                        pstmt.setString(7, "%");
                    else
                        pstmt.setString(7, "%" + room_code+ "%");

                    if(bed_type.equals("any"))
                        pstmt.setString(8, "%");
                    else
                        pstmt.setString(8, "%" + bed_type + "%");

                    pstmt.setInt(9, desired_ocp);
                    pstmt.setDate(10, java.sql.Date.valueOf(checkOut));
                    pstmt.setDate(11, java.sql.Date.valueOf(checkIn));
                    pstmt.setInt(12, desired_ocp);

                    // Step 4: Send SQL statement to DBMS
                    ResultSet res = pstmt.executeQuery();
                    ResultSetMetaData rsmd = res.getMetaData();
                    int count = rsmd.getColumnCount();

                    System.out.println("");
                    System.out.print("  ");
                    for (int i = 1; i <= count; i++)
                        System.out.printf("%-30s", rsmd.getColumnName(i));
                    System.out.println("");

                    int y = 0;
                    while (res.next()) {
                        System.out.println();
                        System.out.print((y + 1) + " ");
                        for (int i = 1; i <= count; i++) {
                            String val = res.getString(i);
                            results[y][i - 1] = val;
                            System.out.printf("%-30s", val);
                        }
                        y++;
                    }

                    // Step 6: Commit or rollback transaction
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                }

                System.out.print("\n\nIf you are interested in booking one of the above rooms, select the number (1-5). " +
                        "To cancel request, enter cancel : ");
                String answer = scanner.nextLine();

                if(answer.equals("1") | answer.equals("2") | answer.equals("3") | answer.equals("4") | answer.equals("5")) {

                    int chosenRoom = Integer.parseInt(answer) - 1;
                    double totalPrice = getCostOfStay(LocalDate.parse(results[chosenRoom][7]), LocalDate.parse(results[chosenRoom][8]), Double.parseDouble(results[chosenRoom][5]))[0];
                    double lengthOfRes = getCostOfStay(LocalDate.parse(results[chosenRoom][7]), LocalDate.parse(results[chosenRoom][8]), Double.parseDouble(results[chosenRoom][5]))[1];

                    System.out.println("\nConfirmation Page for selected option: " + (chosenRoom + 1) + "\n");
                    System.out.println("First Name: " + f_name);
                    System.out.println("Last Name: " + l_name);
                    System.out.println("Room Code: " + results[chosenRoom][0]);
                    System.out.println("Room Name: " + results[chosenRoom][1]);
                    System.out.println("Bed Type: " + results[chosenRoom][3]);
                    System.out.println("Check In Date: " + results[chosenRoom][7]);
                    System.out.println("Check Out Date: " + results[chosenRoom][8]);
                    System.out.println("Number of adults: " + num_adults);
                    System.out.println("Number of children: " + num_children);
                    System.out.println("Total cost of stay: " + totalPrice);

                    System.out.print("\n\nTo confirm booking reservation, enter confirm. " +
                            "To cancel request, enter cancel : ");
                    String confirm = scanner.nextLine();

                    if(confirm.equals("confirm") | confirm.equals("Y") | confirm.equals("Yes")) {
                        String sqlInsertQuery = "INSERT into rbeltr01.lab7_reservations (CODE, Room, CheckIn, Checkout, Rate, LastName, FirstName, Adults, Kids) values  \n" +
                                "    (?, ?, ?, ?, ?, ?, ?, ?, ?);";

                        String sqlReservationCodeQuery = "select max(code)\n" +
                                "from rbeltr01.lab7_reservations";

                        int newReservationCode = 0;

                        try (PreparedStatement pstmt = conn.prepareStatement(sqlReservationCodeQuery)) {
                            ResultSet res = pstmt.executeQuery();
                            ResultSetMetaData rsmd = res.getMetaData();
                            int colCount = rsmd.getColumnCount();

                            while (res.next()) {
                                for (int i = 1; i < colCount + 1; i++)
                                    newReservationCode = Integer.parseInt(res.getString(i)) + 1;
                            }

                            conn.commit();
                        } catch (SQLException e) {
                            conn.rollback();
                        }

                        System.out.println("Your confirmation code is: " + newReservationCode);

                        try (PreparedStatement pstmt = conn.prepareStatement(sqlInsertQuery)) {

                            //Inject values to insert
                            pstmt.setInt(1, newReservationCode);
                            pstmt.setString(2, results[chosenRoom][0]);
                            pstmt.setString(3, results[chosenRoom][7]);
                            pstmt.setString(4, results[chosenRoom][8]);
                            pstmt.setString(5, String.valueOf(totalPrice / lengthOfRes));
                            pstmt.setString(6, l_name);
                            pstmt.setString(7, f_name);
                            pstmt.setString(8, num_adults);
                            pstmt.setString(9, num_children);

                            int rowCount = pstmt.executeUpdate();

                            // Step 5: Handle results
                            if (rowCount > 0) {
                                System.out.println("Reservation was successfully made! Thank you!");
                            }
                            // Step 6: Commit or rollback transaction
                            conn.commit();
                        } catch (SQLException e) {
                            conn.rollback();
                        }
                    }
                    else {
                        System.out.println("Thank you for your inquiry, no reservations were made at this time.");
                    }
                }
                else {
                    System.out.println("Thank you for your inquiry, no reservations were made at this time.");
                }
            } else if(maxOcc >= desired_ocp){
                System.out.println("The requested CheckIn and Checkout dates conflict with each other(Checkout before CheckIn or same date)\nPlease try again. ");
            } else {
                System.out.println("The requested person count (children plus adults) exceeds the maximum capacity of any one room at the Inn.\nThere are no suitable rooms are available. ");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Step 7: Close connection (handled implcitly by try-with-resources syntax)
    }

    private static void ReservationChange() throws SQLException {
        loadDriver();
        ArrayList<String> vals = new ArrayList<>();

        // Step 1: Establish connection to RDBMS
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {

            // Prompt User for Reservation Code
            Scanner scanner = new Scanner(System.in);
            System.out.print("What's the reservation code? ");
            String res_code = scanner.nextLine();

            // Prompt User for confirmation to continue
            System.out.println("\nFor the following fields, type the new value or type 'none' for zero changes to that field");
            System.out.print("What's the updated first name? ");
            String firstName = scanner.nextLine();
            System.out.print("What's the updated last name? ");
            String lastName = scanner.nextLine();

            System.out.print("What's the updated begin date (YYYY-MM-DD)? ");
            String checkInStr = scanner.nextLine();

            System.out.print("What's the updated end date (YYYY-MM-DD)? ");
            String checkOutStr = scanner.nextLine();

            System.out.print("What's the updated number of children? ");
            String numChildren = scanner.nextLine();
            System.out.print("What's the updated number of adults? ");
            String numAdults = scanner.nextLine();

            int desired_ocp = Integer.parseInt(numChildren) + Integer.parseInt(numAdults);

            int maxOcc = -1;

            conn.setAutoCommit(false);

            String sqlMaxOccQuery = "select maxOcc from lab7_rooms \n" +
                    "where RoomCode = (select Room from rbeltr01.lab7_reservations \n" +
                    "                WHERE CODE = ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sqlMaxOccQuery)) {
                pstmt.setInt(1, Integer.parseInt(res_code));
                ResultSet res = pstmt.executeQuery();
                ResultSetMetaData rsmd = res.getMetaData();
                int colCount = rsmd.getColumnCount();
                // Step 5: Handle results
                while (res.next()) {
                    for (int i = 1; i < colCount + 1; i++){
                        maxOcc = Integer.parseInt(res.getString(1));
                    }
                }
                // Step 6: Commit or rollback transaction
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            }

            if (maxOcc >= desired_ocp){
                // Step 2: Construct SQL statement
                String sqlStmt = "UPDATE rbeltr01.lab7_reservations SET FirstName = ?, LastName = ?, Kids = ?, Adults = ?, CheckIn = ?, Checkout = ? WHERE CODE = ?";

                // Step 2: Construct SQL statement for
                String sqlStmtBase = "select * from rbeltr01.lab7_reservations WHERE CODE = ?";

                // Step 3: Start transaction
                conn.setAutoCommit(false);

                // Run query to get the current values of reservation
                try (PreparedStatement base_pstmt = conn.prepareStatement(sqlStmtBase)) {
                    // Step 4: Send SQL statement to DBMS
                    base_pstmt.setInt(1, Integer.parseInt(res_code));
                    ResultSet res = base_pstmt.executeQuery();
                    ResultSetMetaData rsmd = res.getMetaData();
                    int colCount = rsmd.getColumnCount();

                    // Step 5: Handle results
                    while (res.next()) {
                        for (int i = 1; i <= colCount + 1; i++)
                            vals.add(res.getString(i));
                    }

                    // Step 6: Commit or rollback transaction
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                }

                // Step 3: Start transaction
                conn.setAutoCommit(false);

                try (PreparedStatement pstmt = conn.prepareStatement(sqlStmt)) {
                    // Step 4: Send SQL statement to DBMS

                    if(firstName.equals("none"))
                        firstName = vals.get(6);
                    pstmt.setString(1, firstName);

                    if(lastName.equals("none"))
                        firstName = vals.get(5);
                    pstmt.setString(2, lastName);

                    if (numChildren.equals("none"))
                        numChildren = vals.get(8);
                    pstmt.setInt(3, Integer.parseInt(numChildren));

                    if (numAdults.equals("none"))
                        numAdults = vals.get(7);
                    pstmt.setInt(4, Integer.parseInt(numAdults));

                    String conflicts_sqlStmt = "select * from rbeltr01.lab7_reservations where ";

                    if (checkInStr.equals("none")) {
                        checkInStr = vals.get(2);
                        conflicts_sqlStmt += "((CheckIn > ? and CheckIn < ?)";
                    } else{
                        conflicts_sqlStmt += "((CheckIn >= ? and CheckIn < ?)";
                    }
                    pstmt.setDate(5, java.sql.Date.valueOf(checkInStr));

                    if (checkOutStr.equals("none")){
                        checkOutStr = vals.get(3);
                        conflicts_sqlStmt += " or (Checkout > ? and Checkout < ?)) ";
                    } else {
                        conflicts_sqlStmt +=  " or (Checkout > ? and Checkout <= ?)) ";
                    }

                    conflicts_sqlStmt += "and Room = (select Room from rbeltr01.lab7_reservations where CODE = ?) and CODE <> ?";
                    pstmt.setDate(6, java.sql.Date.valueOf(checkOutStr));

                    pstmt.setInt(7, Integer.parseInt(res_code));

                    // Check for dates where checkin is after or same time as checkout
                    boolean dateConflict = LocalDate.parse(checkOutStr).isBefore(LocalDate.parse(checkInStr)) || LocalDate.parse(checkInStr).isEqual(LocalDate.parse(checkOutStr));
                    if(!dateConflict){
                        try (PreparedStatement conflicts_pstmt = conn.prepareStatement(conflicts_sqlStmt)) {
                            conflicts_pstmt.setDate(1, java.sql.Date.valueOf(checkInStr));
                            conflicts_pstmt.setDate(2, java.sql.Date.valueOf(checkOutStr));
                            conflicts_pstmt.setDate(3, java.sql.Date.valueOf(checkInStr));
                            conflicts_pstmt.setDate(4, java.sql.Date.valueOf(checkOutStr));
                            conflicts_pstmt.setInt(5, Integer.parseInt(res_code));
                            conflicts_pstmt.setInt(6, Integer.parseInt(res_code));

                            ResultSet res = conflicts_pstmt.executeQuery();
                            int count = 0;
                            while (res.next())
                                count++;

                            // Step 5: Handle results
                            if (count > 0){
                                System.out.println("Error: Dates conflicting with existing reservations");
                            } else {
                                // Step 4: Send SQL statement to DBMS
                                int rowCount = pstmt.executeUpdate();

                                // Step 5: Handle results
                                if (rowCount == 0)
                                    System.out.println("\nReservation Code not found in our records. Please try again");
                                else{
                                    System.out.format("Successfully updated reservation");
                                }
                                // Step 6: Commit or rollback transaction
                                conn.commit();
                            }
                        } catch (SQLException e) {
                            conn.rollback();
                        }
                    } else {
                        System.out.println("The requested CheckIn and Checkout dates conflict with each other(Checkout before CheckIn or same date)\nPlease try again. ");
                    }
                } catch (SQLException e) {
                    conn.rollback();
                }
            } else {
                System.out.println("The requested person count (children plus adults) exceeds the maximum capacity of the reservation's room.");
            }
        }
    }

    private static void ReservationCancellation() throws SQLException {
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
                    else
                        System.out.println("\nSuccessfully updated reservation.");

                    // Step 6: Commit or rollback transaction
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                }
            }
        }
    }

    private static void DetailedReservationInformation() throws SQLException {
        loadDriver();

        // Step 1: Establish connection to RDBMS
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {

            // Prompt User for Reservation Code
            System.out.println("\nFor the following fields, type the values you want to be included in search or type 'any'");
            Scanner scanner = new Scanner(System.in);
            System.out.print("What's the first name? ");
            String f_name = scanner.nextLine();

            System.out.print("What's the last name? ");
            String l_name = scanner.nextLine();

            System.out.print("What is the check in date (YYYY-MM-DD)? ");
            String checkInStr = scanner.nextLine();

            System.out.print("What is the checkout date (YYYY-MM-DD)? ");
            String checkOutStr = scanner.nextLine();

            System.out.print("What's the room code? ");
            String room_code = scanner.nextLine();

            System.out.print("What's the reservation code? ");
            String res_code = scanner.nextLine();

            // Step 2: Construct SQL statement
            String sqlStmt = "select *\n" +
                    "from \n" +
                    "    rbeltr01.lab7_reservations r1 join rbeltr01.lab7_rooms on Room = RoomCode\n" +
                    "where\n" +
                    "    FirstName LIKE ? AND\n" +
                    "    LastName LIKE ? AND\n" +
                    "    CheckIn LIKE ? AND\n" +
                    "    Checkout LIKE ? AND\n" +
                    "    Room LIKE ? AND\n" +
                    "    Code LIKE ?;";

            // Step 3: Start transaction
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sqlStmt)) {
                // Inject field values
                if(!f_name.equals("any"))
                    pstmt.setString(1, "%" + f_name + "%");
                else
                    pstmt.setString(1, "%");

                if(!l_name.equals("any"))
                    pstmt.setString(2, "%" + l_name + "%");
                else
                    pstmt.setString(2, "%");

                if(!checkInStr.equals("any"))
                    pstmt.setDate(3, java.sql.Date.valueOf(checkInStr));
                else
                    pstmt.setString(3, "%");

                if(!checkOutStr.equals("any"))
                    pstmt.setDate(4, java.sql.Date.valueOf(checkOutStr));
                else
                    pstmt.setString(4, "%");

                if(!room_code.equals("any"))
                    pstmt.setString(5, "%" + room_code + "%");
                else
                    pstmt.setString(5, "%");

                if(!room_code.equals("any"))
                    pstmt.setString(6, "%" + res_code + "%");
                else
                    pstmt.setString(6, "%");

                // Step 4: Send SQL statement to DBMS
                ResultSet res = pstmt.executeQuery();
                ResultSetMetaData rsmd = res.getMetaData();
                int count = rsmd.getColumnCount();

                for (int i = 1; i < count; i++)
                    System.out.printf("%-30s", rsmd.getColumnName(i));
                System.out.println("");


                while (res.next()) {
                    System.out.println("");
                    for (int i = 1; i < count; i++)
                        System.out.printf("%-30s", res.getString(i));
                }

                // Step 6: Commit or rollback transaction
                System.out.println();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void Revenue() throws SQLException {
        loadDriver();

        // Step 1: Establish connection to RDBMS
        try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                System.getenv("HP_JDBC_USER"),
                System.getenv("HP_JDBC_PW"))) {

            // Step 2: Construct SQL statement
            String sqlStmt = "select r.Room, COALESCE(Jan,0) Jan, COALESCE(Feb,0) Feb, COALESCE(March,0) March, COALESCE(April,0) April, COALESCE(May,0) May, COALESCE(June,0) June, COALESCE(July,0) July, COALESCE(Aug,0) Aug, COALESCE(Sep,0) Sep, COALESCE(Oct,0) Oct, COALESCE(Nov,0) Nov, COALESCE(Decem,0) 'Dec', \n" +
                    "    (COALESCE(Jan,0) + COALESCE(Feb,0) + COALESCE(March,0) + COALESCE(April,0) + COALESCE(May,0) + COALESCE(June,0) + COALESCE(July,0) + COALESCE(Aug,0) + COALESCE(Sep,0) + COALESCE(Oct,0) + COALESCE(Nov,0) + COALESCE(Decem,0)) as Total\n" +
                    "from \n" +
                    "    (select Room \n" +
                    "    from rbeltr01.lab7_reservations\n" +
                    "    group by Room) r\n" +
                    "    \n" +
                    "    LEFT JOIN \n" +
                    "    \n" +
                    "    (select r1.room, sum(least(datediff(checkout, checkin), 31) * rate) as Jan\n" +
                    "    from \n" +
                    "        rbeltr01.lab7_reservations r1\n" +
                    "    where\n" +
                    "        month(r1.checkIn) <= '1' AND month(r1.checkout) >= '1'\n" +
                    "        AND year(r1.checkIn) = YEAR(DATE(CURRENT_TIMESTAMP))\n" +
                    "    group by room) jan\n" +
                    "\n" +
                    "    on r.Room = jan.room\n" +
                    "    \n" +
                    "    LEFT JOIN \n" +
                    "    \n" +
                    "    (select r1.room, sum(least(datediff(checkout, checkin), 28) * rate) as Feb\n" +
                    "    from \n" +
                    "        rbeltr01.lab7_reservations r1\n" +
                    "    where\n" +
                    "        month(r1.checkIn) <= '2' AND month(r1.checkout) >= '2'\n" +
                    "        AND year(r1.checkIn) = YEAR(DATE(CURRENT_TIMESTAMP))\n" +
                    "    group by room) feb\n" +
                    "    \n" +
                    "    on r.Room = feb.room\n" +
                    "    \n" +
                    "    LEFT JOIN \n" +
                    "    \n" +
                    "    (select r1.room, sum(least(datediff(checkout, checkin), 31) * rate) as March\n" +
                    "    from \n" +
                    "        rbeltr01.lab7_reservations r1\n" +
                    "    where\n" +
                    "        month(r1.checkIn) <= '3' AND month(r1.checkout) >= '3'\n" +
                    "        AND year(r1.checkIn) = YEAR(DATE(CURRENT_TIMESTAMP))\n" +
                    "    group by room) march\n" +
                    "    \n" +
                    "    on r.Room = march.room\n" +
                    "    \n" +
                    "    LEFT JOIN \n" +
                    "    \n" +
                    "    (select r1.room, sum(least(datediff(checkout, checkin), 30) * rate) as April\n" +
                    "    from \n" +
                    "        rbeltr01.lab7_reservations r1\n" +
                    "    where\n" +
                    "        month(r1.checkIn) <= '4' AND month(r1.checkout) >= '4'\n" +
                    "        AND year(r1.checkIn) = YEAR(DATE(CURRENT_TIMESTAMP))\n" +
                    "    group by room) april\n" +
                    "    \n" +
                    "    on r.Room = april.room\n" +
                    "    \n" +
                    "    LEFT JOIN \n" +
                    "    \n" +
                    "    (select r1.room, sum(least(datediff(checkout, checkin), 31) * rate) as May\n" +
                    "    from \n" +
                    "        rbeltr01.lab7_reservations r1\n" +
                    "    where\n" +
                    "        month(r1.checkIn) <= '5' AND month(r1.checkout) >= '5'\n" +
                    "        AND year(r1.checkIn) = YEAR(DATE(CURRENT_TIMESTAMP))\n" +
                    "    group by room) may\n" +
                    "    \n" +
                    "    on r.Room = may.room\n" +
                    "    \n" +
                    "    LEFT JOIN \n" +
                    "    \n" +
                    "    (select r1.room, sum(least(datediff(checkout, checkin), 30) * rate) as June\n" +
                    "    from \n" +
                    "        rbeltr01.lab7_reservations r1\n" +
                    "    where\n" +
                    "        month(r1.checkIn) <= '6' AND month(r1.checkout) >= '6'\n" +
                    "        AND year(r1.checkIn) = YEAR(DATE(CURRENT_TIMESTAMP))\n" +
                    "    group by room) june\n" +
                    "    \n" +
                    "    on r.Room = june.room\n" +
                    "    \n" +
                    "    LEFT join\n" +
                    "    \n" +
                    "    (select r1.room, sum(least(datediff(checkout, checkin), 31) * rate) as July\n" +
                    "    from \n" +
                    "        rbeltr01.lab7_reservations r1\n" +
                    "    where\n" +
                    "        month(r1.checkIn) <= '7' AND month(r1.checkout) >= '7'\n" +
                    "        AND year(r1.checkIn) = YEAR(DATE(CURRENT_TIMESTAMP))\n" +
                    "    group by room) july \n" +
                    "    \n" +
                    "    on r.Room = july.room\n" +
                    "    \n" +
                    "    LEFT join\n" +
                    "    \n" +
                    "    (select r1.room, sum(least(datediff(checkout, checkin), 31) * rate) as Aug\n" +
                    "    from \n" +
                    "        rbeltr01.lab7_reservations r1\n" +
                    "    where\n" +
                    "        month(r1.checkIn) <= '8' AND month(r1.checkout) >= '8'\n" +
                    "        AND year(r1.checkIn) = YEAR(DATE(CURRENT_TIMESTAMP))\n" +
                    "    group by room) aug\n" +
                    "    \n" +
                    "    on r.Room = aug.room\n" +
                    "    \n" +
                    "    LEFT join\n" +
                    "    \n" +
                    "    (select r1.room, sum(least(datediff(checkout, checkin), 30) * rate) as Sep\n" +
                    "    from \n" +
                    "        rbeltr01.lab7_reservations r1\n" +
                    "    where\n" +
                    "        month(r1.checkIn) <= '9' AND month(r1.checkout) >= '9'\n" +
                    "        AND year(r1.checkIn) = YEAR(DATE(CURRENT_TIMESTAMP))\n" +
                    "    group by room) sep \n" +
                    "    \n" +
                    "    on r.Room = sep.room\n" +
                    "    \n" +
                    "    \n" +
                    "    LEFT join\n" +
                    "    \n" +
                    "    (select r1.room, sum(least(datediff(checkout, checkin), 31) * rate) as Oct\n" +
                    "    from \n" +
                    "        rbeltr01.lab7_reservations r1\n" +
                    "    where\n" +
                    "        month(r1.checkIn) <= '10' AND month(r1.checkout) >= '10'\n" +
                    "        AND year(r1.checkIn) = YEAR(DATE(CURRENT_TIMESTAMP))\n" +
                    "    group by room) oct\n" +
                    "    \n" +
                    "    on r.Room = oct.room\n" +
                    "    \n" +
                    "    LEFT join\n" +
                    "    \n" +
                    "    (select r1.room, sum(least(datediff(checkout, checkin), 30) * rate) as Nov\n" +
                    "    from \n" +
                    "        rbeltr01.lab7_reservations r1\n" +
                    "    where\n" +
                    "        month(r1.checkIn) <= '11' AND month(r1.checkout) >= '11'\n" +
                    "        AND year(r1.checkIn) = YEAR(DATE(CURRENT_TIMESTAMP))\n" +
                    "    group by room) nov\n" +
                    "    \n" +
                    "    on r.Room = nov.room\n" +
                    "    \n" +
                    "    LEFT join\n" +
                    "    \n" +
                    "    (select r1.room, sum(least(datediff(checkout, checkin), 31) * rate) as Decem\n" +
                    "    from \n" +
                    "        rbeltr01.lab7_reservations r1\n" +
                    "    where\n" +
                    "        month(r1.checkIn) <= '12' AND month(r1.checkout) >= '12'\n" +
                    "        AND year(r1.checkIn) = YEAR(DATE(CURRENT_TIMESTAMP))\n" +
                    "    group by room) decem\n" +
                    "    \n" +
                    "    on r.Room = decem.room;";

            // Step 3: Start transaction
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sqlStmt)) {

                // Step 4: Send SQL statement to DBMS
                ResultSet res = pstmt.executeQuery(sqlStmt);
                ResultSetMetaData rsmd = res.getMetaData();
                int colCount = rsmd.getColumnCount();

                for (int i = 1; i <= colCount; i++) {
                    System.out.printf("%-30s", rsmd.getColumnName(i));
                }

                System.out.println("");

                while (res.next()) {
                    System.out.println();
                    for (int i = 1; i <= colCount; i++) {
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

    private static void printIntro() {
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
        int response = 7;
        InnReservations ir = new InnReservations();
        System.out.println("Welcome to our database!");
        System.out.println("To select an option from the list below, type the number to its right and press Enter\n");


        while(true){
            System.out.println("\nSelect a command (1-7): ");
            printIntro();
            System.out.print("> ");
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
                System.err.println("Exception: " + e2.getMessage());
            }
        }
        System.out.println("Exiting Application :)");
    }
}