package com.airport;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Scanner INPUT = new Scanner(System.in);
    private static final AirportSystem AIRPORT = new AirportSystem();

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("H:mm");

    public static void main(String[] args) {

        System.out.println("====================================================");
        System.out.println("       AIRPORT FLIGHT AND GATE SCHEDULING SYSTEM");
        System.out.println("====================================================");

        boolean running = true;

        while (running) {
            printMenu();
            int choice = readInt("Choose an option: ");

            try {
                switch (choice) {
                    case 1 -> registerFlight();
                    case 2 -> searchFlight();
                    case 3 -> updateFlight();
                    case 4 -> deleteFlight();
                    case 5 -> listFlights(
                            AIRPORT.getAllFlights(), "ALL FLIGHTS");
                    case 6 -> allocateGate();
                    case 7 -> releaseGate();
                    case 8 -> rescheduleFlight();
                    case 9 -> AIRPORT.getGateSchedule().displaySchedule();
                    case 10 -> showAvailableSlots();
                    case 11 -> showOccupiedSlots();
                    case 12 -> AIRPORT.displayOperationalReport();
                    case 13 -> listFlights(
                            AIRPORT.sortFlightsById(), "FLIGHTS SORTED BY ID");
                    case 14 -> listFlights(
                            AIRPORT.sortFlightsByDepartureTime(),
                            "FLIGHTS SORTED BY DEPARTURE TIME");
                    case 0 -> {
                        running = false;
                        System.out.println("Thank you for using the system.");
                    }
                    default -> System.out.println("Invalid menu option.");
                }

            } catch (RuntimeException ex) {
                System.out.println("Error: " + ex.getMessage());
            }

            if (running) {
                System.out.print("\nPress Enter to continue...");
                INPUT.nextLine();
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n-------------------- MAIN MENU --------------------");
        System.out.println("1. Register a flight");
        System.out.println("2. Search for a flight");
        System.out.println("3. Update a flight");
        System.out.println("4. Delete a flight");
        System.out.println("5. Display all flights");
        System.out.println("6. Allocate a gate");
        System.out.println("7. Release a gate");
        System.out.println("8. Reschedule a flight");
        System.out.println("9. Display gate schedule");
        System.out.println("10. Display available slots");
        System.out.println("11. Display occupied slots");
        System.out.println("12. Generate operational report");
        System.out.println("13. Sort flights by ID");
        System.out.println("14. Sort flights by departure time");
        System.out.println("0. Exit");
        System.out.println("---------------------------------------------------");
    }

    private static void registerFlight() {
        System.out.println("\nREGISTER FLIGHT");

        String id = readText("Flight ID: ");
        String airline = readText("Airline name: ");
        String destination = readText("Destination: ");

        FlightCategory category = readEnum(
                "Category (DOMESTIC, INTERNATIONAL, CARGO): ",
                FlightCategory.class);

        LocalTime time = readTime("Departure time (HH:mm): ");

        int capacity = category == FlightCategory.CARGO
                ? 0 : readNonNegativeInt("Passenger capacity: ");

        int booked = category == FlightCategory.CARGO
                ? 0 : readBoundedInt(
                        "Booked passengers: ", 0, capacity);

        FlightStatus status = readEnum(
                "Status (SCHEDULED, BOARDING, DELAYED, DEPARTED, CANCELLED): ",
                FlightStatus.class);

        Flight flight;

        if (category == FlightCategory.INTERNATIONAL) {
            String terminal = readText("Departure terminal: ");
            String boardingGate = readText("Boarding gate: ");
            boolean customs = readYesNo(
                    "Customs clearance required? (Y/N): ");

            flight = new InternationalFlight(
                    id, airline, destination, time,
                    capacity, booked, status,
                    terminal, boardingGate, customs);

        } else {
            flight = new Flight(
                    id, airline, destination, time,
                    capacity, booked, category, status);
        }

        AIRPORT.registerFlight(flight);

        System.out.println("Flight registered successfully.");
        System.out.println(flight.displayDetails());
    }

    private static void searchFlight() {
        String id = readText("Enter Flight ID: ");

        Flight flight = AIRPORT.searchFlight(id)
                .orElseThrow(() ->
                        new java.util.NoSuchElementException(
                                "Flight not found: " + id));

        System.out.println(flight.displayDetails());

        AIRPORT.getGateSchedule().findAllocation(id)
                .ifPresent(a ->
                        System.out.println("Gate allocation: " + a));
    }

    private static void updateFlight() {
        String id = readText("Enter Flight ID to update: ");

        Flight flight = AIRPORT.searchFlight(id)
                .orElseThrow(() ->
                        new java.util.NoSuchElementException(
                                "Flight not found: " + id));

        System.out.println("Current details: " + flight.displayDetails());

        String airline = readText("New airline name: ");
        String destination = readText("New destination: ");
        LocalTime time = readTime("New departure time (HH:mm): ");

        int capacity = flight.getFlightCategory() == FlightCategory.CARGO
                ? 0 : readNonNegativeInt("New passenger capacity: ");

        int booked = flight.getFlightCategory() == FlightCategory.CARGO
                ? 0 : readBoundedInt(
                        "New booked passengers: ", 0, capacity);

        FlightStatus status = readEnum(
                "New status: ", FlightStatus.class);

        AIRPORT.updateFlight(
                id, airline, destination, time,
                capacity, booked, status);

        System.out.println("Flight updated successfully.");
        System.out.println(
                "Any existing gate allocation has been handled automatically.");
    }

    private static void deleteFlight() {
        String id = readText("Enter Flight ID to delete: ");

        if (AIRPORT.deleteFlight(id)) {
            System.out.println(
                    "Flight deleted and gate allocation released.");
        } else {
            System.out.println("Flight not found.");
        }
    }

    private static void allocateGate() {
        String id = readText("Flight ID: ");
        String gate = readText("Gate (G01-G05): ");
        LocalTime time = readTime("Departure time (HH:mm): ");

        AIRPORT.allocateGate(id, gate, time);

        System.out.println("Gate allocated successfully.");
    }

    private static void releaseGate() {
        String id = readText("Flight ID: ");

        if (AIRPORT.releaseGate(id)) {
            System.out.println("Gate allocation released.");
        } else {
            System.out.println("This flight has no gate allocation.");
        }
    }

    private static void rescheduleFlight() {
        String id = readText("Flight ID: ");
        String gate = readText("New gate (G01-G05): ");
        LocalTime time = readTime("New departure time (HH:mm): ");

        AIRPORT.rescheduleFlight(id, gate, time);

        System.out.println("Flight rescheduled successfully.");
    }

    private static void showAvailableSlots() {
        System.out.println("\nAVAILABLE GATE/TIME SLOTS");

        AIRPORT.getGateSchedule().getAvailableSlots()
                .forEach(System.out::println);
    }

    private static void showOccupiedSlots() {
        System.out.println("\nOCCUPIED GATE/TIME SLOTS");

        List<GateSchedule.Allocation> occupied =
                AIRPORT.getGateSchedule().getOccupiedSlots();

        if (occupied.isEmpty()) {
            System.out.println("No gate allocations.");
        } else {
            occupied.forEach(System.out::println);
        }
    }

    private static void listFlights(
            List<Flight> flights, String heading) {

        System.out.println("\n========== " + heading + " ==========");

        if (flights.isEmpty()) {
            System.out.println("No flights registered.");
        } else {
            flights.forEach(f -> System.out.println(f.displayDetails()));
        }
    }

    private static String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = INPUT.nextLine().trim();

            if (!value.isEmpty()) {
                return value;
            }

            System.out.println("This field cannot be blank.");
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);

            try {
                return Integer.parseInt(INPUT.nextLine().trim());
            } catch (NumberFormatException ex) {
                System.out.println("Enter a whole number.");
            }
        }
    }

    private static int readNonNegativeInt(String prompt) {
        while (true) {
            int value = readInt(prompt);

            if (value >= 0) {
                return value;
            }

            System.out.println("Enter zero or a positive number.");
        }
    }

    private static int readBoundedInt(
            String prompt, int min, int max) {

        while (true) {
            int value = readInt(prompt);

            if (value >= min && value <= max) {
                return value;
            }

            System.out.println(
                    "Enter a number from " + min + " to " + max + ".");
        }
    }

    private static LocalTime readTime(String prompt) {
        while (true) {
            System.out.print(prompt);

            try {
                LocalTime time = LocalTime.parse(
                        INPUT.nextLine().trim(), TIME_FORMAT);

                if (GateSchedule.TIME_SLOTS.contains(time)) {
                    return time;
                }

                System.out.println(
                        "Available times: " + GateSchedule.TIME_LABELS);

            } catch (DateTimeParseException ex) {
                System.out.println(
                        "Invalid time. Use 24-hour format, e.g. 14:00.");
            }
        }
    }

    private static boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = INPUT.nextLine().trim();

            if (value.equalsIgnoreCase("Y")
                    || value.equalsIgnoreCase("YES")) {
                return true;
            }

            if (value.equalsIgnoreCase("N")
                    || value.equalsIgnoreCase("NO")) {
                return false;
            }

            System.out.println("Enter Y or N.");
        }
    }

    private static <E extends Enum<E>> E readEnum(
            String prompt, Class<E> type) {

        while (true) {
            System.out.print(prompt);

            String value = INPUT.nextLine()
                    .trim().toUpperCase().replace(' ', '_');

            try {
                return Enum.valueOf(type, value);
            } catch (IllegalArgumentException ex) {
                System.out.println(
                        "Options: " + String.join(", ",
                                java.util.Arrays.stream(
                                        type.getEnumConstants())
                                        .map(Enum::name)
                                        .toList()));
            }
        }
    }
}