package com.airport;

import java.time.LocalTime;

public class InternationalFlight extends Flight {

    private String departureTerminal;
    private String boardingGate;
    private boolean customsClearanceRequired;

    public InternationalFlight(
            String flightId,
            String airlineName,
            String destination,
            LocalTime departureTime,
            int passengerCapacity,
            int bookedPassengers,
            FlightStatus flightStatus,
            String departureTerminal,
            String boardingGate,
            boolean customsClearanceRequired) {

        super(
                flightId,
                airlineName,
                destination,
                departureTime,
                passengerCapacity,
                bookedPassengers,
                FlightCategory.INTERNATIONAL,
                flightStatus);

        setDepartureTerminal(departureTerminal);
        setBoardingGate(boardingGate);
        this.customsClearanceRequired = customsClearanceRequired;
    }

    public String getDepartureTerminal() {
        return departureTerminal;
    }

    public String getBoardingGate() {
        return boardingGate;
    }

    public boolean isCustomsClearanceRequired() {
        return customsClearanceRequired;
    }

    public void setDepartureTerminal(String terminal) {
        if (terminal == null || terminal.isBlank()) {
            throw new IllegalArgumentException(
                    "Departure terminal is required.");
        }

        this.departureTerminal = terminal.trim();
    }

    public void setBoardingGate(String gate) {
        if (gate == null || gate.isBlank()) {
            throw new IllegalArgumentException(
                    "Boarding gate is required.");
        }

        this.boardingGate = gate.trim().toUpperCase();
    }

    public void setCustomsClearanceRequired(boolean required) {
        this.customsClearanceRequired = required;
    }

    @Override
    public String displayDetails() {
        return super.displayDetails()
                + String.format(
                        " | Terminal: %s | Boarding gate: %s"
                        + " | Customs clearance: %s",
                        departureTerminal,
                        boardingGate,
                        customsClearanceRequired
                                ? "Required" : "Not required");
    }
}
