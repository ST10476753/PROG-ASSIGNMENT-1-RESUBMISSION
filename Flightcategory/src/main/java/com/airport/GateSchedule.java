package com.airport;

import java.time.LocalTime;
import java.util.*;

public class GateSchedule {

    public static final List<String> GATES = List.of(
            "G01", "G02", "G03", "G04", "G05");

    public static final List<LocalTime> TIME_SLOTS = List.of(
            LocalTime.of(6, 0),
            LocalTime.of(8, 0),
            LocalTime.of(10, 0),
            LocalTime.of(12, 0),
            LocalTime.of(14, 0),
            LocalTime.of(16, 0),
            LocalTime.of(18, 0),
            LocalTime.of(20, 0));

    public static final String TIME_LABELS =
            "06:00, 08:00, 10:00, 12:00, "
            + "14:00, 16:00, 18:00, 20:00";

    private final String[][] schedule =
            new String[GATES.size()][TIME_SLOTS.size()];

    public void allocateGate(
            String flightId, String gate, LocalTime time) {

        if (flightId == null || flightId.isBlank()) {
            throw new IllegalArgumentException("Flight ID is required.");
        }

        int row = GATES.indexOf(normalizeGate(gate));
        int col = TIME_SLOTS.indexOf(time);

        if (row < 0) {
            throw new IllegalArgumentException(
                    "Unknown gate. Choose G01 to G05.");
        }

        if (col < 0) {
            throw new IllegalArgumentException(
                    "Invalid time slot. Available times: "
                    + TIME_LABELS);
        }

        if (findAllocation(flightId).isPresent()) {
            throw new IllegalStateException(
                    "This flight already has a gate allocation.");
        }

        if (schedule[row][col] != null) {
            throw new IllegalStateException(
                    "That gate and time slot are already occupied.");
        }

        schedule[row][col] = flightId.trim().toUpperCase();
    }

    public boolean releaseGate(String flightId) {
        if (flightId == null) {
            return false;
        }

        boolean released = false;

        for (int r = 0; r < schedule.length; r++) {
            for (int c = 0; c < schedule[r].length; c++) {
                if (schedule[r][c] != null
                        && schedule[r][c].equalsIgnoreCase(flightId)) {
                    schedule[r][c] = null;
                    released = true;
                }
            }
        }

        return released;
    }

    public Optional<Allocation> findAllocation(String flightId) {
        if (flightId == null) {
            return Optional.empty();
        }

        for (int r = 0; r < schedule.length; r++) {
            for (int c = 0; c < schedule[r].length; c++) {
                if (schedule[r][c] != null
                        && schedule[r][c].equalsIgnoreCase(flightId)) {
                    return Optional.of(new Allocation(
                            GATES.get(r),
                            TIME_SLOTS.get(c),
                            schedule[r][c]));
                }
            }
        }

        return Optional.empty();
    }

    public List<Allocation> getAllocations() {
        List<Allocation> result = new ArrayList<>();

        for (int r = 0; r < schedule.length; r++) {
            for (int c = 0; c < schedule[r].length; c++) {
                if (schedule[r][c] != null) {
                    result.add(new Allocation(
                            GATES.get(r),
                            TIME_SLOTS.get(c),
                            schedule[r][c]));
                }
            }
        }

        return result;
    }

    public List<Allocation> getAvailableSlots() {
        List<Allocation> result = new ArrayList<>();

        for (int r = 0; r < schedule.length; r++) {
            for (int c = 0; c < schedule[r].length; c++) {
                if (schedule[r][c] == null) {
                    result.add(new Allocation(
                            GATES.get(r),
                            TIME_SLOTS.get(c),
                            null));
                }
            }
        }

        return result;
    }

    public List<Allocation> getOccupiedSlots() {
        return getAllocations();
    }

    public long countFlightsAtGate(String gate) {
        int row = GATES.indexOf(normalizeGate(gate));

        if (row < 0) {
            throw new IllegalArgumentException("Unknown gate: " + gate);
        }

        return Arrays.stream(schedule[row])
                .filter(Objects::nonNull)
                .count();
    }

    public Map<LocalTime, Long> countFlightsByTimeSlot() {
        Map<LocalTime, Long> counts = new LinkedHashMap<>();

        for (int c = 0; c < TIME_SLOTS.size(); c++) {
            long count = 0;

            for (String[] row : schedule) {
                if (row[c] != null) {
                    count++;
                }
            }

            counts.put(TIME_SLOTS.get(c), count);
        }

        return counts;
    }

    public void displaySchedule() {
        System.out.println("\n========== AIRPORT GATE SCHEDULE ==========");

        System.out.printf("%-8s", "Gate");

        for (LocalTime time : TIME_SLOTS) {
            System.out.printf("| %-7s", time);
        }

        System.out.println();

        for (int r = 0; r < GATES.size(); r++) {
            System.out.printf("%-8s", GATES.get(r));

            for (int c = 0; c < TIME_SLOTS.size(); c++) {
                System.out.printf(
                        "| %-7s",
                        schedule[r][c] == null
                                ? "—" : schedule[r][c]);
            }

            System.out.println();
        }

        System.out.println("============================================");
    }

    private String normalizeGate(String gate) {
        return gate == null ? "" : gate.trim().toUpperCase();
    }

    public record Allocation(
            String gate, LocalTime time, String flightId) {

        @Override
        public String toString() {
            return gate + " at " + time
                    + (flightId == null
                    ? " (available)"
                    : " - Flight " + flightId);
        }
    }
}
