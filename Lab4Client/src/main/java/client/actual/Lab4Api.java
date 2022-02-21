package client.actual;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Lab4Api {

    final private String basePath; //"http://localhost:8080/Lab3Server_war_exploded";
    final private int numThreads; //maxThread = 1024
    final private int numRuns; //up to 20
    final private int numSkiers; //up to 100,000 skiers
    final private int numLifts; //range from 5 to 60, by default 40
    final private Vector<String> responses = new Vector<>();
    private final Random rand = new Random();

    public Lab4Api(String basePath, int numThreads, int numRuns, int numSkiers, int numLifts) {
        this.basePath = basePath;
        this.numThreads = numThreads; //maxThread = 1024
        this.numRuns = numRuns; //up to 20
        this.numSkiers = numSkiers; //up to 100,000 skiers
        this.numLifts = numLifts; //range from 5 to 60, by default 40
    }

    public String getBasePath() {
        return this.basePath;
    }

    public int getNumThreads() {
        return this.numThreads;
    }

    public int getNumRuns() {
        return this.numRuns;
    }

    public int getNumSkiers() {
        return this.numSkiers;
    }

    public int getNumLifts() {
        return this.numLifts;
    }

    public Random getRand() {
        return this.rand;
    }

    public Thread instrumentedThread(int numSkiersStart, int numSkiersEnd, int startTime, int endTime, int numRequests,
                                     AtomicInteger successful, AtomicInteger unsuccessful,
                                     AtomicInteger failures, CountDownLatch[] latches) {
        Runnable runTest = () -> {
            // Set up client
            ApiClient client = new ApiClient();
            client.setBasePath(this.getBasePath());
            SkiersApi skiersApi = new SkiersApi();
            skiersApi.setApiClient(client);

            // Send requests
            for (int i = 0; i < numRequests; i++) {
                // Generate random record
                int skier = this.rand.nextInt(numSkiersEnd - numSkiersStart + 1) + numSkiersStart;
                int time = this.rand.nextInt(endTime - startTime + 1) + startTime;
                int lift = this.rand.nextInt(this.getNumLifts() - 4) + 5;
                int wait = this.rand.nextInt(11);

                // Configure LiftRide
                LiftRide ride = new LiftRide();
                ride.setLiftID(lift);
                ride.setTime(time);
                ride.setWaitTime(wait);

                // Request start time
                long requestStart = System.nanoTime();

                // Make 5 attempts at successful request
                boolean success = false;
                for (int j = 0; j < 5; j++) {
                    try {
                        skiersApi.writeNewLiftRide(ride, 1, "2022", "1", skier);
                        success = true;
                        break;
                    } catch (ApiException e) {
                        System.out.println("Request " + j + " failed");
                        failures.incrementAndGet();
                    }
                }

                // Calculate latency
                long latency = System.nanoTime() - requestStart;

                // Update successful/unsuccessful request counters
                if (success) {
                    successful.incrementAndGet();
                } else {
                    unsuccessful.incrementAndGet();
                }

                // Record latency to list
                this.responses.add(Long.toString(latency));

            }
            for (CountDownLatch latch : latches) {
                latch.countDown();
            }
        };

        // Return Thread object
        return new Thread(runTest);
    }

    public void test() {
        // Create file to store results
        File file = new File("res/" + this.getNumThreads() + " Threads.txt");
        if (file.exists()) {file.delete();}
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Create counters for successful and unsuccessful requests
        AtomicInteger successful = new AtomicInteger();
        AtomicInteger unsuccessful = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        //phase 1
        System.out.println("Phase One Started");
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Calculate number of threads for each phase
        int numOfThreadOne = (int) (this.getNumThreads() * 0.25);
        int numOfThreadTwo = this.getNumThreads();
        int numOfThreadThree = (int) (this.getNumThreads() * 0.1);

        // Countdown latches for phase gates
        CountDownLatch phaseGateOne = new CountDownLatch((int) Math.ceil(numOfThreadOne * 0.2));
        CountDownLatch phaseGateTwo = new CountDownLatch((int) Math.ceil(numOfThreadTwo * 0.2));

        // Countdown latch for all threads to complete
        CountDownLatch allComplete = new CountDownLatch(numOfThreadOne + numOfThreadTwo + numOfThreadThree);

        // Get test start time
        long start = System.nanoTime();

        // Phase 1
        // Start numThreads / 4 request threads
        int phase1Requests = (int) Math.ceil(this.getNumRuns() * 0.2) * (this.getNumSkiers() / (numOfThreadOne));
        for (int i = 0; i < numOfThreadOne; i++) {
            // Start of skierID range
            int skierIDStart = 1 + i * (this.getNumSkiers() / numOfThreadOne);
            // End of skierID range
            int skierIDEnd =  (i + 1) * (this.getNumSkiers() / numOfThreadOne);
            // Generate thread with info
            Thread thread = this.instrumentedThread(skierIDStart, skierIDEnd, 1, 90, phase1Requests, successful,
                    unsuccessful, failures, new CountDownLatch[] {phaseGateOne, allComplete});
            // Start thread
            thread.start();
        }
        // Wait for 20% of Phase 1 threads to complete
        try {
            phaseGateOne.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Phase 2
        System.out.println("Phase Two Started");
        int phase2Requests = (int) Math.ceil(this.getNumRuns() * 0.6) * (this.getNumSkiers() / (numOfThreadTwo));
        for (int i = 0; i < numOfThreadTwo; i++) {
            // Start of skierID range
            int skierIDStart = 1 + i * (this.getNumSkiers() / numOfThreadTwo);
            // End of skierID range
            int skierIDEnd =  (i + 1) * (this.getNumSkiers() / numOfThreadTwo);
            // Generate thread with info
            Thread thread = this.instrumentedThread(skierIDStart, skierIDEnd, 91, 360, phase2Requests, successful,
                    unsuccessful, failures, new CountDownLatch[] {phaseGateTwo, allComplete});
            // Start thread
            thread.start();
        }
        // Wait for 20% of Phase 2 threads to complete
        try {
            phaseGateTwo.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Phase 3
        System.out.println("Phase Three Started");
        int phase3Requests = (this.getNumRuns() / 10) * (this.getNumSkiers() / (numOfThreadThree));
        for (int i = 0; i < numOfThreadThree; i++) {
            // Start of skierID range
            int skierIDStart = 1 + i * (this.getNumSkiers() / numOfThreadThree);
            // End of skierID range
            int skierIDEnd =  (i + 1) * (this.getNumSkiers() / numOfThreadThree);
            // Generate thread with info
            Thread thread = this.instrumentedThread(skierIDStart, skierIDEnd, 361, 420, phase3Requests, successful,
                    unsuccessful, failures, new CountDownLatch[] {phaseGateTwo, allComplete});
            // Start thread
            thread.start();
        }
        // Wait for all threads to complete
        try {
            allComplete.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long end = System.nanoTime();
        System.out.println("There are " + this.getNumThreads() + " threads working and the total skier is " + this.getNumSkiers() +
                ": time spent is " + (end - start) / 1000000.0 + " ms");
        try {
            writer.write(String.valueOf(end - start) + "\n");
            writer.write(successful.get() + unsuccessful.get() + "\n");
            writer.write(successful.get() + "\n");
            writer.write(failures.get() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String latency: this.responses) {
            try {
                writer.write(latency + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void littlesTest(int n) {
        ApiClient client = new ApiClient();
        client.setBasePath(this.getBasePath());
        SkiersApi api = new SkiersApi();
        api.setApiClient(client);
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            try {
                api.writeNewLiftRide(new LiftRide(), 1, "2022", "1", 1);
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }
        double totalTime =
                (double) (TimeUnit.SECONDS.convert(System.nanoTime() - start,TimeUnit.NANOSECONDS));

        double throughput = n / totalTime;
        File file = new File("res/baseline.txt");
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(throughput + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}