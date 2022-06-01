import java.util.Scanner;


public class InnReservations {

    public static void RoomsAndRates() {
        System.out.println("1");
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

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int response = 0;

        System.out.println("Welcome to our database!");
        System.out.println("To select an option from the list below, type the number to its right and press Enter\n");
        System.out.println("Rooms and Rates: 1");
        System.out.println("Reservations: 2");
        System.out.println("Reservation Change: 3");
        System.out.println("Reservation Cancellation: 4");
        System.out.println("Detailed Reservation Information: 5");
        System.out.println("Revenue: 6");
        System.out.println("Quit Application: 7\n");

        while(true){
            try {
                response = in.nextInt();
            }
            catch(Exception e) {
                System.out.println("Wrong format: Please input a digit from 1-7 inclusive and press Enter");
                break;
            }

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
        System.out.println("Exiting Application :)");
    }

}