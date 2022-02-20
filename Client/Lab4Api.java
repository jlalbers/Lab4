import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class Lab4Api {
    public static class Info {
        public int startTime;
        public int skierIDStart;
        public double runTimeIdx;
        public double percentageOfThread;
        String phase;

        public Info(int startTime, int skierIDStart, double runTimeIdx, double percentageOfThread, String phase) {
            this.startTime = startTime;
            this.skierIDStart = skierIDStart;
            this.runTimeIdx = runTimeIdx;
            this.percentageOfThread = percentageOfThread;
            this.phase = phase;
        }
    }

    final static private String basePath = "http://localhost:8080/Lab3Server_war_exploded";
    final static private int NUMTHREADS = 128; //maxThread = 1024
    final static private int NUMRUNS = 10; //up to 20
    final static private int NUMSKIERS = 10000; //up to 100,000 skiers
    final static private int NUMLIFTs = 40; //range from 5 to 60, by default 40
    static private Random rand = new Random();
    static private Map<String, Integer> statusCodeMap = new HashMap<>();
    static File file;
    static FileWriter writer;





    synchronized public  void incCall(SkiersApi apiInstance, ApiClient client, Info info) {
        int numOfThread = (int)(NUMTHREADS * info.percentageOfThread);
        int skierPerThread = NUMSKIERS / numOfThread;
        int currSkierStart = info.skierIDStart;
        info.skierIDStart += skierPerThread;

        for (int i = 0; i < info.runTimeIdx * NUMRUNS; i++) { // num run
            for (int j = currSkierStart; j < currSkierStart + skierPerThread; j++) { //skier in range
                Integer resortID = 56; // Integer | ID of the resort
                String seasonID = "56"; // String | ID of the season
                String dayID = "56"; // String | ID of the day
                try {
                    long threadStart = System.nanoTime();
                    ApiResponse res = apiInstance.getSkierDayVerticalWithHttpInfo(resortID, seasonID, dayID, j);
                    long threadEnd = System.nanoTime();
                    String key = info.phase + " " + res.getStatusCode();
                    statusCodeMap.put(key, statusCodeMap.getOrDefault(key, 0) + 1);

                    //System.out.println(res.getStatusCode());
                    //Integer verticalResult = apiInstance.getSkierDayVertical(resortID, seasonID, dayID, skierID);
                    //System.out.println(verticalResult);
                    writer.write(String.valueOf((threadEnd - threadStart) + "\n"));
                } catch (ApiException | IOException e) {
                    System.err.println("Exception when calling SkiersApi#getSkierDayVertical");
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        long start = System.currentTimeMillis();


        //phase 1
        System.out.println("Phase One Started");
        Lab4Api lab4Api = new Lab4Api();

            try {
                file = new File("Multi-Thread-Performance.txt");
                file.createNewFile();
                writer = new FileWriter(file);
            } catch (final IOException e) {
                throw new ExceptionInInitializerError(e.getMessage());
            }


        Info infoPhaseOne = new Info(0, 0, 0.2, 0.25, "PhaseOne");
        Info infoPhaseTwo = new Info(91, 0, 0.6, 1d, "PhaseTwo");
        Info infoPhaseThree = new Info(361, 0, 0.1, 0.1, "PhaseThree");

        int numOfThreadOne = (int) (NUMTHREADS * infoPhaseOne.percentageOfThread);
        int numOfThreadTwo = (int) (NUMTHREADS * infoPhaseTwo.percentageOfThread);
        int numOfThreadThree = (int) (NUMTHREADS * infoPhaseThree.percentageOfThread);;

        CountDownLatch completedOne = new CountDownLatch((int) Math.ceil(numOfThreadOne * 0.2));
        CountDownLatch completedTwo = new CountDownLatch((int) Math.ceil(numOfThreadTwo * 0.2));
        CountDownLatch completedTotal = new CountDownLatch(numOfThreadThree + numOfThreadTwo + numOfThreadOne);

        //Phase 1
        System.out.println("Phase 1 time is " + System.currentTimeMillis());

        for (int i = 0; i < numOfThreadOne; i++) {
            long threadStart = System.currentTimeMillis();
            SkiersApi apiInstance = new SkiersApi();
            ApiClient client = apiInstance.getApiClient();
            client.setBasePath(basePath);

            // lambda runnable creation - interface only has a single method so lambda works fine
            Runnable thread =  () -> { lab4Api.incCall(apiInstance, client, infoPhaseOne);
                completedOne.countDown();
                completedTotal.countDown();
            };
            new Thread(thread).start();
            long threadEnd = System.currentTimeMillis();
            //writer.write(String.valueOf((threadEnd - threadStart) + "\n"));
        }

        completedOne.await();



        //Phase 2
        System.out.println("Phase Two Started");
        System.out.println("Phase 2 time is " + System.currentTimeMillis());

        for (int i = 0; i < numOfThreadTwo; i++) {
            long threadStart = System.currentTimeMillis();
            SkiersApi apiInstance = new SkiersApi();
            ApiClient client = apiInstance.getApiClient();
            client.setBasePath(basePath);

            // lambda runnable creation - interface only has a single method so lambda works fine
            Runnable thread =  () -> { lab4Api.incCall(apiInstance, client, infoPhaseTwo);
                completedTwo.countDown();
                completedTotal.countDown();
            };
            new Thread(thread).start();
            long threadEnd = System.currentTimeMillis();
            //writer.write(String.valueOf((threadEnd - threadStart) + "\n"));
        }

        completedTwo.await();


        //Phase 3
        System.out.println("Phase Three Started");
        System.out.println("Phase 3 time is " + System.currentTimeMillis());

        for (int i = 0; i < numOfThreadThree; i++) {
            long threadStart = System.currentTimeMillis();
            SkiersApi apiInstance = new SkiersApi();
            ApiClient client = apiInstance.getApiClient();
            client.setBasePath(basePath);

            // lambda runnable creation - interface only has a single method so lambda works fine
            Runnable thread =  () -> { lab4Api.incCall(apiInstance, client, infoPhaseThree); completedTotal.countDown();
            };
            new Thread(thread).start();
            long threadEnd = System.currentTimeMillis();
            //writer.write(String.valueOf((threadEnd - threadStart) + "\n"));
        }

        completedTotal.await();

        long end = System.currentTimeMillis();
        System.out.println("There are " + NUMTHREADS + " threads working and the total skier is " + NUMSKIERS +
                ": time spent is " + (end - start) + " ms");
        writer.flush();
        writer.close();

        for (Map.Entry<String, Integer> entry: statusCodeMap.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
        return;
    }
}