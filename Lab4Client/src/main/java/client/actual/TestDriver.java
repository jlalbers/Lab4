package client.actual;

public class TestDriver {
    public static void main(String[] args) {
        System.out.println("Testing Little's Law baseline");
        String basePath = "http://44.203.37.114:8080/Lab4_war/";
        Lab4Api clientBaseline = new Lab4Api(basePath, 1, 1, 1, 1);
        clientBaseline.littlesTest(10000);
        /*
        System.out.println("Baseline complete\n");
        for (int i = 16; i < 1025; i += 16) {
            System.out.println("Testing client with " + i + " threads");
            Lab4Api clientTest = new Lab4Api(basePath, i, 10, 10000,40);
            clientTest.test();
            System.out.println("Test complete\n");
        }
        */
    }
}
