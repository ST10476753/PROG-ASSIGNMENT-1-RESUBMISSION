package com.airport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class AirportSystemTest {

    private AirportSystem airport;

    private Flight createFlight(
            String id, LocalTime time,
            int capacity, int booked, FlightStatus status) {

        return new Flight(
                id, "Aerolux", "Cape Town",
                time, capacity, booked,
                FlightCategory.DOMESTIC, status);
    }

    @BeforeEach
    void setUp() {
        airport = new AirportSystem();
    }

    @Test
    void registerAndSearchFlight() {
        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 75, FlightStatus.SCHEDULED));

        assertTrue(airport.searchFlight("fl101").isPresent());
        assertEquals("FL101",
                airport.searchFlight("FL101")
                        .orElseThrow().getFlightId());
    }

    @Test
    void duplicateFlightIdsAreRejected() {
        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 75, FlightStatus.SCHEDULED));

        assertThrows(IllegalArgumentException.class,
                () -> airport.registerFlight(createFlight(
                        "fl101", LocalTime.of(8, 0),
                        100, 50, FlightStatus.SCHEDULED)));
    }

    @Test
    void blankFlightIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> createFlight(
                        " ", LocalTime.of(6, 0),
                        100, 50, FlightStatus.SCHEDULED));
    }

    @Test
    void invalidDepartureTimeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> createFlight(
                        "FL101", LocalTime.of(7, 0),
                        100, 50, FlightStatus.SCHEDULED));
    }

    @Test
    void bookedPassengersCannotExceedCapacity() {
        assertThrows(IllegalArgumentException.class,
                () -> createFlight(
                        "FL101", LocalTime.of(6, 0),
                        50, 51, FlightStatus.SCHEDULED));
    }

    @Test
    void negativePassengerCapacityIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> createFlight(
                        "FL101", LocalTime.of(6, 0),
                        -1, 0, FlightStatus.SCHEDULED));
    }

    @Test
    void cargoFlightMustHaveZeroPassengerCapacity() {
        assertThrows(IllegalArgumentException.class,
                () -> new Flight(
                        "CG101", "Aerolux", "Durban",
                        LocalTime.of(8, 0), 100, 0,
                        FlightCategory.CARGO,
                        FlightStatus.SCHEDULED));
    }

    @Test
    void updateFlightChangesDetails() {
        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 75, FlightStatus.SCHEDULED));

        airport.updateFlight(
                "FL101", "New Airline", "Durban",
                LocalTime.of(8, 0), 80, 60,
                FlightStatus.DELAYED);

        Flight updated = airport.searchFlight("FL101").orElseThrow();

        assertEquals("New Airline", updated.getAirlineName());
        assertEquals("Durban", updated.getDestination());
        assertEquals(LocalTime.of(8, 0), updated.getDepartureTime());
        assertEquals(80, updated.getPassengerCapacity());
        assertEquals(60, updated.getBookedPassengers());
        assertEquals(FlightStatus.DELAYED, updated.getFlightStatus());
    }

    @Test
    void invalidUpdateDoesNotChangeOriginalFlight() {
        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 75, FlightStatus.SCHEDULED));

        assertThrows(IllegalArgumentException.class,
                () -> airport.updateFlight(
                        "FL101", "New Airline", "Durban",
                        LocalTime.of(8, 0), 50, 60,
                        FlightStatus.DELAYED));

        Flight unchanged = airport.searchFlight("FL101").orElseThrow();

        assertEquals("Aerolux", unchanged.getAirlineName());
        assertEquals("Cape Town", unchanged.getDestination());
        assertEquals(LocalTime.of(6, 0), unchanged.getDepartureTime());
        assertEquals(100, unchanged.getPassengerCapacity());
        assertEquals(75, unchanged.getBookedPassengers());
    }

    @Test
    void gateAndTimeCannotBeAllocatedTwice() {
        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 50, FlightStatus.SCHEDULED));

        airport.registerFlight(createFlight(
                "FL102", LocalTime.of(6, 0),
                100, 50, FlightStatus.SCHEDULED));

        airport.allocateGate("FL101", "G01", LocalTime.of(6, 0));

        assertThrows(IllegalStateException.class,
                () -> airport.allocateGate(
                        "FL102", "G01", LocalTime.of(6, 0)));
    }

    @Test
    void oneFlightCannotHaveTwoGateAllocations() {
        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 50, FlightStatus.SCHEDULED));

        airport.allocateGate("FL101", "G01", LocalTime.of(6, 0));

        assertThrows(IllegalStateException.class,
                () -> airport.getGateSchedule().allocateGate(
                        "FL101", "G02", LocalTime.of(6, 0)));
    }

    @Test
    void gateTimeMustMatchFlightDepartureTime() {
        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 50, FlightStatus.SCHEDULED));

        assertThrows(IllegalArgumentException.class,
                () -> airport.allocateGate(
                        "FL101", "G01", LocalTime.of(8, 0)));
    }

    @Test
    void cancelledFlightReleasesGate() {
        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 50, FlightStatus.SCHEDULED));

        airport.allocateGate("FL101", "G01", LocalTime.of(6, 0));

        airport.updateFlight(
                "FL101", "Aerolux", "Cape Town",
                LocalTime.of(6, 0), 100, 50,
                FlightStatus.CANCELLED);

        assertTrue(airport.getGateSchedule()
                .getOccupiedSlots().isEmpty());
    }

    @Test
    void deletingFlightReleasesGate() {
        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 50, FlightStatus.SCHEDULED));

        airport.allocateGate("FL101", "G01", LocalTime.of(6, 0));

        assertTrue(airport.deleteFlight("FL101"));
        assertTrue(airport.searchFlight("FL101").isEmpty());
        assertTrue(airport.getGateSchedule()
                .getOccupiedSlots().isEmpty());
    }

    @Test
    void reschedulingChangesGateAndTime() {
        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 50, FlightStatus.SCHEDULED));

        airport.allocateGate("FL101", "G01", LocalTime.of(6, 0));

        airport.rescheduleFlight(
                "FL101", "G02", LocalTime.of(8, 0));

        GateSchedule.Allocation allocation =
                airport.getGateSchedule()
                        .findAllocation("FL101").orElseThrow();

        assertEquals("G02", allocation.gate());
        assertEquals(LocalTime.of(8, 0), allocation.time());
        assertEquals(LocalTime.of(8, 0),
                airport.searchFlight("FL101").orElseThrow()
                        .getDepartureTime());
    }

    @Test
    void failedReschedulingRestoresOriginalAllocation() {
        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 50, FlightStatus.SCHEDULED));

        airport.registerFlight(createFlight(
                "FL102", LocalTime.of(8, 0),
                100, 50, FlightStatus.SCHEDULED));

        airport.allocateGate("FL101", "G01", LocalTime.of(6, 0));
        airport.allocateGate("FL102", "G02", LocalTime.of(8, 0));

        assertThrows(IllegalStateException.class,
                () -> airport.rescheduleFlight(
                        "FL101", "G02", LocalTime.of(8, 0)));

        assertEquals(LocalTime.of(6, 0),
                airport.searchFlight("FL101").orElseThrow()
                        .getDepartureTime());

        GateSchedule.Allocation original =
                airport.getGateSchedule()
                        .findAllocation("FL101").orElseThrow();

        assertEquals("G01", original.gate());
        assertEquals(LocalTime.of(6, 0), original.time());
    }

    @Test
    void updatingDepartureTimeMovesExistingGateAllocation() {
        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 50, FlightStatus.SCHEDULED));

        airport.allocateGate("FL101", "G01", LocalTime.of(6, 0));

        airport.updateFlight(
                "FL101", "Aerolux", "Cape Town",
                LocalTime.of(8, 0), 100, 50,
                FlightStatus.SCHEDULED);

        GateSchedule.Allocation allocation =
                airport.getGateSchedule()
                        .findAllocation("FL101").orElseThrow();

        assertEquals("G01", allocation.gate());
        assertEquals(LocalTime.of(8, 0), allocation.time());
    }

    @Test
    void passengerLoadPercentageIsCorrect() {
        Flight flight = createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 75, FlightStatus.SCHEDULED);

        assertEquals(75.0, flight.getPassengerLoadPercentage(), 0.001);
    }

    @Test
    void averagePassengerLoadIsCorrect() {
        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 50, FlightStatus.SCHEDULED));

        airport.registerFlight(createFlight(
                "FL102", LocalTime.of(8, 0),
                200, 100, FlightStatus.SCHEDULED));

        assertEquals(50.0,
                airport.getAveragePassengerLoadPercentage(), 0.001);
    }

    @Test
    void busiestGateIsIdentified() {
        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 50, FlightStatus.SCHEDULED));

        airport.registerFlight(createFlight(
                "FL102", LocalTime.of(8, 0),
                100, 50, FlightStatus.SCHEDULED));

        airport.allocateGate("FL101", "G01", LocalTime.of(6, 0));
        airport.allocateGate("FL102", "G01", LocalTime.of(8, 0));

        assertEquals("G01", airport.getBusiestGate().orElseThrow());
    }

    @Test
    void scheduledAndDelayedFlightsAreSeparated() {
        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(6, 0),
                100, 50, FlightStatus.SCHEDULED));

        airport.registerFlight(createFlight(
                "FL102", LocalTime.of(8, 0),
                100, 50, FlightStatus.DELAYED));

        assertEquals(1, airport.getScheduledFlights().size());
        assertEquals(1, airport.getDelayedFlights().size());
    }

    @Test
    void flightsAreSortedByDepartureTime() {
        airport.registerFlight(createFlight(
                "FL203", LocalTime.of(10, 0),
                100, 50, FlightStatus.SCHEDULED));

        airport.registerFlight(createFlight(
                "FL101", LocalTime.of(8, 0),
                100, 50, FlightStatus.SCHEDULED));

        assertEquals("FL101",
                airport.sortFlightsByDepartureTime()
                        .get(0).getFlightId());
    }

    @Test
    void missingFlightReturnsEmptySearchResult() {
        assertTrue(airport.searchFlight("UNKNOWN").isEmpty());
    }
}