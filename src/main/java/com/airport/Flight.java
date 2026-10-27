package com.airport;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Flight {

    private final String flightId;
    private String airlineName;
    private String destination;
    private LocalTime departureTime;
    private int passengerCapacity;
    private int bookedPassengers;
    private final FlightCategory flightCategory;
    private FlightStatus flightStatus;

    public Flight(String flightId, String airlineName,
                  String destination, LocalTime departureTime,
                  int passengerCapacity, int bookedPassengers,
                  FlightCategory flightCategory,
                  FlightStatus flightStatus) {

        if (flightId == null || flightId.isBlank()) {
            throw new IllegalArgumentException("Flight ID is required.");
        }

        this.flightId = flightId.trim().toUpperCase();
        this.flightCategory = Objects.requireNonNull(flightCategory);
        this.airlineName = validateText(airlineName, "Airline name");
        this.destination = validateText(destination, "Destination");
        this.departureTime = validateTime(departureTime);
        this.flightStatus = Objects.requireNonNull(flightStatus);

        validatePassengers(passengerCapacity, bookedPassengers);

        this.passengerCapacity = passengerCapacity;
        this.bookedPassengers = bookedPassengers;
    }

    private String validateText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    private LocalTime validateTime(LocalTime time) {
        Objects.requireNonNull(time, "Departure time is required.");

        if (!GateSchedule.TIME_SLOTS.contains(time)) {
            throw new IllegalArgumentException(
                    "Invalid departure time. Available times: "
                    + GateSchedule.TIME_LABELS);
        }

        return time;
    }

    private void validatePassengers(int capacity, int booked) {
        if (capacity < 0 || booked < 0) {
            throw new IllegalArgumentException(
                    "Passenger figures cannot be negative.");
        }

        if (flightCategory == FlightCategory.CARGO
                && (capacity != 0 || booked != 0)) {
            throw new IllegalArgumentException(
                    "Cargo flights must have zero passenger capacity "
                    + "and zero booked passengers.");
        }

        if (booked > capacity) {
            throw new IllegalArgumentException(
                    "Booked passengers cannot exceed capacity.");
        }
    }

    public String getFlightId() {
        return flightId;
    }

    public String getAirlineName() {
        return airlineName;
    }

    public String getDestination() {
        return destination;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public int getPassengerCapacity() {
        return passengerCapacity;
    }

    public int getBookedPassengers() {
        return bookedPassengers;
    }

    public FlightCategory getFlightCategory() {
        return flightCategory;
    }

    public FlightStatus getFlightStatus() {
        return flightStatus;
    }

    public void setAirlineName(String airlineName) {
        this.airlineName = validateText(airlineName, "Airline name");
    }

    public void setDestination(String destination) {
        this.destination = validateText(destination, "Destination");
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = validateTime(departureTime);
    }

    public void setPassengerCapacity(int capacity) {
        validatePassengers(capacity, bookedPassengers);
        this.passengerCapacity = capacity;
    }

    public void setBookedPassengers(int booked) {
        validatePassengers(passengerCapacity, booked);
        this.bookedPassengers = booked;
    }

    public void setFlightStatus(FlightStatus status) {
        this.flightStatus = Objects.requireNonNull(status);
    }

    public void updateDetails(
            String airline,
            String destination,
            LocalTime departureTime,
            int capacity,
            int booked,
            FlightStatus status) {

        String validAirline = validateText(airline, "Airline name");
        String validDestination = validateText(destination, "Destination");
        LocalTime validTime = validateTime(departureTime);
        FlightStatus validStatus = Objects.requireNonNull(status);

        validatePassengers(capacity, booked);

        this.airlineName = validAirline;
        this.destination = validDestination;
        this.departureTime = validTime;
        this.passengerCapacity = capacity;
        this.bookedPassengers = booked;
        this.flightStatus = validStatus;
    }

    public double getPassengerLoadPercentage() {
        if (passengerCapacity == 0) {
            return 0.0;
        }

        return bookedPassengers * 100.0 / passengerCapacity;
    }

    public String displayDetails() {
        return String.format(
                "ID: %s | Airline: %s | Destination: %s"
                + " | Departure: %s | Category: %s"
                + " | Status: %s | Passengers: %d/%d"
                + " | Load: %.2f%%",
                flightId,
                airlineName,
                destination,
                departureTime.format(
                        DateTimeFormatter.ofPattern("HH:mm")),
                flightCategory,
                flightStatus,
                bookedPassengers,
                passengerCapacity,
                getPassengerLoadPercentage());
    }
}