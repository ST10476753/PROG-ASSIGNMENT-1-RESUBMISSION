package com.airport;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class AirportSystem {

    private final Map<String, Flight> flights = new LinkedHashMap<>();
    private final GateSchedule gateSchedule = new GateSchedule();

    public void registerFlight(Flight flight) {
        Objects.requireNonNull(flight, "Flight is required.");

        String id = normalizeId(flight.getFlightId());

        if (flights.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Duplicate Flight ID: " + id);
        }

        flights.put(id, flight);
    }

    public Optional<Flight> searchFlight(String id) {
        return Optional.ofNullable(flights.get(normalizeId(id)));
    }

    public List<Flight> getAllFlights() {
        return new ArrayList<>(flights.values());
    }

    public void updateFlight(
            String id,
            String airline,
            String destination,
            LocalTime newTime,
            int capacity,
            int booked,
            FlightStatus newStatus) {

        Flight flight = requireFlight(id);

        // Validate everything before changing the existing flight.
        new Flight(
                flight.getFlightId(),
                airline,
                destination,
                newTime,
                capacity,
                booked,
                flight.getFlightCategory(),
                newStatus);

        LocalTime oldTime = flight.getDepartureTime();
        String oldAirline = flight.getAirlineName();
        String oldDestination = flight.getDestination();
        int oldCapacity = flight.getPassengerCapacity();
        int oldBooked = flight.getBookedPassengers();
        FlightStatus oldStatus = flight.getFlightStatus();

        Optional<GateSchedule.Allocation> oldAllocation =
                gateSchedule.findAllocation(id);

        boolean mustRelease = oldAllocation.isPresent()
                && (!oldTime.equals(newTime)
                || newStatus == FlightStatus.CANCELLED);

        if (mustRelease) {
            gateSchedule.releaseGate(id);
        }

        try {
            flight.updateDetails(
                    airline, destination, newTime,
                    capacity, booked, newStatus);

            if (oldAllocation.isPresent()
                    && newStatus != FlightStatus.CANCELLED
                    && !oldTime.equals(newTime)) {

                gateSchedule.allocateGate(
                        id,
                        oldAllocation.get().gate(),
                        newTime);
            }

        } catch (RuntimeException ex) {
            flight.updateDetails(
                    oldAirline, oldDestination, oldTime,
                    oldCapacity, oldBooked, oldStatus);

            if (mustRelease && oldAllocation.isPresent()) {
                GateSchedule.Allocation old = oldAllocation.get();

                gateSchedule.allocateGate(
                        id, old.gate(), old.time());
            }

            throw ex;
        }
    }

    public boolean deleteFlight(String id) {
        String key = normalizeId(id);
        Flight removed = flights.remove(key);

        if (removed != null) {
            gateSchedule.releaseGate(key);
            return true;
        }

        return false;
    }

    public void allocateGate(
            String id, String gate, LocalTime time) {

        Flight flight = requireFlight(id);

        if (flight.getFlightStatus() == FlightStatus.CANCELLED
                || flight.getFlightStatus() == FlightStatus.DEPARTED) {
            throw new IllegalStateException(
                    "Cancelled or departed flights cannot be allocated.");
        }

        if (!flight.getDepartureTime().equals(time)) {
            throw new IllegalArgumentException(
                    "Gate time must match flight departure time: "
                    + flight.getDepartureTime());
        }

        gateSchedule.allocateGate(
                flight.getFlightId(), gate, time);
    }

    public boolean releaseGate(String id) {
        requireFlight(id);
        return gateSchedule.releaseGate(id);
    }

    public void rescheduleFlight(
            String id, String newGate, LocalTime newTime) {

        Flight flight = requireFlight(id);

        if (flight.getFlightStatus() == FlightStatus.CANCELLED
                || flight.getFlightStatus() == FlightStatus.DEPARTED) {
            throw new IllegalStateException(
                    "Cancelled or departed flights cannot be rescheduled.");
        }

        if (!GateSchedule.TIME_SLOTS.contains(newTime)) {
            throw new IllegalArgumentException(
                    "Invalid departure time slot.");
        }

        Optional<GateSchedule.Allocation> oldAllocation =
                gateSchedule.findAllocation(id);

        LocalTime oldTime = flight.getDepartureTime();

        if (oldAllocation.isPresent()
                && oldAllocation.get().gate().equalsIgnoreCase(newGate)
                && oldAllocation.get().time().equals(newTime)) {
            return;
        }

        if (oldAllocation.isPresent()) {
            gateSchedule.releaseGate(id);
        }

        try {
            flight.setDepartureTime(newTime);
            gateSchedule.allocateGate(id, newGate, newTime);

        } catch (RuntimeException ex) {
            flight.setDepartureTime(oldTime);

            if (oldAllocation.isPresent()) {
                GateSchedule.Allocation old = oldAllocation.get();

                gateSchedule.allocateGate(
                        id, old.gate(), old.time());
            }

            throw ex;
        }
    }

    public GateSchedule getGateSchedule() {
        return gateSchedule;
    }

    public List<Flight> getScheduledFlights() {
        return flights.values().stream()
                .filter(f -> f.getFlightStatus() == FlightStatus.SCHEDULED)
                .collect(Collectors.toList());
    }

    public List<Flight> getDelayedFlights() {
        return flights.values().stream()
                .filter(f -> f.getFlightStatus() == FlightStatus.DELAYED)
                .collect(Collectors.toList());
    }

    public Optional<String> getBusiestGate() {
        return GateSchedule.GATES.stream()
                .max(Comparator.comparingLong(
                        gateSchedule::countFlightsAtGate));
    }

    public Optional<LocalTime> getBusiestTimeSlot() {
        return gateSchedule.countFlightsByTimeSlot()
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    public Optional<Flight> getHighestPassengerLoadFlight() {
        return flights.values().stream()
                .filter(f -> f.getPassengerCapacity() > 0)
                .max(Comparator.comparingDouble(
                        Flight::getPassengerLoadPercentage));
    }

    public double getAveragePassengerLoadPercentage() {
        return flights.values().stream()
                .filter(f -> f.getPassengerCapacity() > 0)
                .mapToDouble(Flight::getPassengerLoadPercentage)
                .average()
                .orElse(0.0);
    }

    public List<Flight> sortFlightsById() {
        return flights.values().stream()
                .sorted(Comparator.comparing(Flight::getFlightId))
                .collect(Collectors.toList());
    }

    public List<Flight> sortFlightsByDepartureTime() {
        return flights.values().stream()
                .sorted(Comparator
                        .comparing(Flight::getDepartureTime)
                        .thenComparing(Flight::getFlightId))
                .collect(Collectors.toList());
    }

    public void displayOperationalReport() {
        System.out.println("\n========== AIRPORT OPERATIONAL REPORT ==========");

        List<Flight> scheduled = getScheduledFlights();
        List<Flight> delayed = getDelayedFlights();

        System.out.println("\nSCHEDULED FLIGHTS: " + scheduled.size());

        if (scheduled.isEmpty()) {
            System.out.println("No scheduled flights.");
        } else {
            scheduled.forEach(f -> System.out.println(f.displayDetails()));
        }

        System.out.println("\nDELAYED FLIGHTS: " + delayed.size());

        if (delayed.isEmpty()) {
            System.out.println("No delayed flights.");
        } else {
            delayed.forEach(f -> System.out.println(f.displayDetails()));
        }

        System.out.println("\nGATE UTILISATION:");

        for (String gate : GateSchedule.GATES) {
            System.out.println(
                    gate + ": "
                    + gateSchedule.countFlightsAtGate(gate)
                    + " allocated flight(s)");
        }

        getBusiestGate().ifPresent(gate ->
                System.out.println(
                        "Busiest gate: " + gate
                        + " (" + gateSchedule.countFlightsAtGate(gate)
                        + " flight(s))"));

        System.out.println("\nDEPARTURE TIME SLOT UTILISATION:");

        gateSchedule.countFlightsByTimeSlot().forEach(
                (time, count) ->
                        System.out.println(time + ": " + count + " flight(s)"));

        getBusiestTimeSlot().ifPresent(time ->
                System.out.println(
                        "Busiest departure time: " + time
                        + " (" + gateSchedule.countFlightsByTimeSlot()
                        .get(time) + " flight(s))"));

        System.out.println("\nPASSENGER LOAD:");

        getHighestPassengerLoadFlight().ifPresentOrElse(
                f -> System.out.printf(
                        "Highest passenger load: %s | %d/%d passengers"
                        + " | %.2f%%%n",
                        f.getFlightId(),
                        f.getBookedPassengers(),
                        f.getPassengerCapacity(),
                        f.getPassengerLoadPercentage()),
                () -> System.out.println(
                        "No passenger flights available for calculation."));

        System.out.printf(
                "Average passenger load percentage: %.2f%%%n",
                getAveragePassengerLoadPercentage());

        System.out.println("\nFLIGHTS SORTED BY FLIGHT ID:");

        sortFlightsById().forEach(
                f -> System.out.println(f.displayDetails()));

        System.out.println("\nFLIGHTS SORTED BY DEPARTURE TIME:");

        sortFlightsByDepartureTime().forEach(
                f -> System.out.println(f.displayDetails()));

        System.out.println(
                "\n================================================");
    }

    private Flight requireFlight(String id) {
        Flight flight = flights.get(normalizeId(id));

        if (flight == null) {
            throw new NoSuchElementException(
                    "Flight not found: " + id);
        }

        return flight;
    }

    private String normalizeId(String id) {
        return id == null ? "" : id.trim().toUpperCase();
    }
}
