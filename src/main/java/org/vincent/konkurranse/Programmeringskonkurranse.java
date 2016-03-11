package org.vincent.konkurranse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Let's give it a try!
 * A bit brute force, I must admit...
 *
 * @author Vincent Quaegebeur
 */
public class Programmeringskonkurranse {

    // A cache to store the factors of each number... yes, will be big at the end
    private static final SortedMap<Integer, SortedSet<Integer>> FACTORS_CACHE = new TreeMap<>();

    public static void main(String[] args) throws Exception {

        // A cache for the results of f(n) found so far
        final SortedMap<Integer, Integer> currentResults = new TreeMap<>();

        // A cache for the prime numbers
        final SortedSet<Integer> currentPrimes = new TreeSet<>();

        // And the values we 're looking for!
        final List<Integer> values = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(3);

        values.add(1); // let's not forget it!
        System.out.println("f(1) = 1");

        for (int current = 2; current < 1_000_000; current++) {

            int value = current;

            executorService.submit(
                    () -> {
                        SortedMap<Integer, Integer> primeFactors = primeFactors(value);
                        if (primeFactors.size() == 1 && primeFactors.firstKey() == value) {
                            // If it's prime, f(n) = 1, as the only sequence is [0 n]
                            currentPrimes.add(value);
                            currentResults.put(value, 1);
                        } else {
                            // All the factors
                            SortedSet<Integer> factors = factors(value, primeFactors);
                            // Calculate the number of sequences, given the current primes and current results as cache
                            int numberOfSequences = numberOfSequences(value, factors, currentPrimes, currentResults);
                            currentResults.put(value, numberOfSequences);
                            if (value == numberOfSequences) {
                                System.out.println(String.format("f(%s) = %s", value, numberOfSequences));
                                values.add(value);
                            }
                        }
                    }
            );

        }

        executorService.shutdown();
        while (!executorService.isTerminated()) {
            Thread.sleep(1000L);
        }

        System.out.println("Result");
        System.out.println("==================");
        System.out.println(values.stream().mapToInt(Integer::valueOf).sum());

    }

    /**
     * Returns the number of sequences (f(n)) for the specified value, with some caches to help a bit...
     *
     * @param value          value to calculate f(n) for
     * @param factors        factors of this value
     * @param currentPrimes  prime numbers currently identified
     * @param currentResults results already calculated
     *
     * @return f(n)
     */
    private static int numberOfSequences(Integer value, SortedSet<Integer> factors, SortedSet<Integer> currentPrimes, SortedMap<Integer, Integer> currentResults) {
        Integer result = currentResults.get(value);
        if (result != null) {
            // If the result is cache, return the result!
            return result;
        }
        if (currentPrimes.contains(value)) {
            // If it's a prime number, it's 1
            currentResults.put(value, 1);
            return 1;
        }
        if (factors.isEmpty()) {
            // Else
            currentPrimes.add(value);
            currentResults.put(value, 1);
            return 1;
        }
        // Here's the trick
        // A bit brute force but...
        // For all factors of the value, we calculate recursively the number of sequences that end with the factor
        // For example, for 12, we calculate all the sequences that end with 6, 4, 3, 2
        // We add 1: that's the sequence [1, n] itself
        return 1 + factors.stream().map(factor -> numberOfSequences(factor, factors(factor), currentPrimes, currentResults)).mapToInt(i -> i).sum();
    }

    /**
     * Gets all the factors of a value, others than 1 and the value itself.
     *
     * @param value value
     *
     * @return factors
     */
    public static SortedSet<Integer> factors(Integer value) {
        SortedSet<Integer> result = FACTORS_CACHE.get(value);
        if (result != null) {
            return result;
        }
        SortedSet<Integer> factors = factors(value, primeFactors(value));
        FACTORS_CACHE.put(value, factors);
        return factors;
    }

    /**
     * Gets all the factors of a value, others than 1 and the value itself, given the prime factors and their
     * multiplicity.
     *
     * @param value        value
     * @param primeFactors prime factor to multiplicity
     *
     * @return factors
     */
    public static SortedSet<Integer> factors(Integer value, Map<Integer, Integer> primeFactors) {
        SortedSet<Integer> result = new TreeSet<>();
        result.add(1);
        for (Map.Entry<Integer, Integer> primeFactor : primeFactors.entrySet()) {
            SortedSet<Integer> newResult = new TreeSet<>();
            for (int i = 0; i <= primeFactor.getValue(); i++) {
                int current = (int) Math.pow(primeFactor.getKey(), i);
                newResult.addAll(result.stream()
                        .map(currentResult -> current * currentResult).collect(Collectors.toList()));
            }
            result = newResult;
        }
        result.remove(1);
        result.remove(value);
        return result;
    }

    /**
     * Gives the prime factors and their multiplicity.
     *
     * @param value value
     *
     * @return prime factor to multiplicity
     */
    public static SortedMap<Integer, Integer> primeFactors(int value) {
        int currentValue = value;
        SortedMap<Integer, Integer> factors = new TreeMap<>();
        for (int potentialFactor = 2; potentialFactor <= value / potentialFactor; potentialFactor++) {
            int count = 0;
            while (currentValue % potentialFactor == 0) {
                count = count + 1;
                currentValue = currentValue / potentialFactor;
            }
            if (count > 0) {
                factors.put(potentialFactor, count);
            }
        }
        if (currentValue > 1) {
            factors.put(currentValue, 1);
        }
        return factors;
    }

}
