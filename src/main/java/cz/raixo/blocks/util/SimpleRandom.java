package cz.raixo.blocks.util;

import java.util.*;

public class SimpleRandom<E> {

    private final NavigableMap<Double, E> map = new TreeMap<>();
    private double total = 0;

    public SimpleRandom<E> add(double weight, E result) {
        if (weight <= 0) return this;
        total += weight;
        map.put(total, result);
        return this;
    }

    public E next(Random random) {
        double value = random.nextDouble() * total;
        return Optional.ofNullable(map.higherEntry(value)).map(Map.Entry::getValue).orElse(null);
    }

    public void clear() {
        map.clear();
        // The running total is part of the table; leaving it behind made every refresh skew the
        // weights and start returning null for a growing share of rolls.
        total = 0;
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

}
