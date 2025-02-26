public class Plant implements Runnable {
    // How long do we want to run the juice processing
    public static final long PROCESSING_TIME = 5 * 1000;

    private static final int NUM_PLANTS = 2;

    public static void main(String[] args) {
        // Startup the plants
        Plant[] plants = new Plant[NUM_PLANTS];
        for (int i = 0; i < NUM_PLANTS; i++) {
            plants[i] = new Plant(i);
            plants[i].startPlant();
        }

        // Give the plants time to do work
        delay(PROCESSING_TIME, "Plant malfunction");

        // Stop the plant, and wait for it to shutdown
        for (Plant p : plants) {
            p.stopPlant();
        }
        for (Plant p : plants) {
            p.waitToStop();
        }

        // Summarize the results
        int totalProvided = 0;
        int totalProcessed = 0;
        int totalBottles = 0;
        int totalWasted = 0;
        for (Plant p : plants) {
            totalProvided += p.getProvidedOranges();
            totalProcessed += p.getProcessedOranges();
            totalBottles += p.getBottles();
            totalWasted += p.getWaste();
        }
        System.out.println("Total provided/processed = " + totalProvided + "/" + totalProcessed);
        System.out.println("Created " + totalBottles +
                ", wasted " + totalWasted + " oranges");
    }

    private static void delay(long time, String errMsg) {
        long sleepTime = Math.max(1, time);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignored) {
        }
    }

    public final int ORANGES_PER_BOTTLE = 3;

    private final Thread thread;
    private int orangesProvided;
    private int orangesProcessed;
    private volatile boolean timeToWork;
    private volatile BlockingMailbox mailbox = new BlockingMailbox();

    Plant(int threadNum) {
        orangesProvided = 0;
        orangesProcessed = 0;
        thread = new Thread(this, "Plant[" + threadNum + "]");
    }

    public void startPlant() {
        timeToWork = true;
        thread.start();
    }

    public void stopPlant() {
        timeToWork = false;
    }

    public void waitToStop() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            System.err.println(thread.getName() + " stop malfunction");
        }
    }

    public void run() {
        System.out.print(Thread.currentThread().getName() + " Processing oranges");
        
        // Start one worker to fetch and peel oranges
        Thread fetchAndPeeler;
        fetchAndPeeler = new Thread(this::fetchAndPeelOranges, "Fetcher");
        fetchAndPeeler.setDaemon(true);
        fetchAndPeeler.start();
        
        // Start one worker to squeeze, bottle, and inspect oranges
        Thread squeezerBottlerAndInspector;
        squeezerBottlerAndInspector = new Thread(this::squeezeBottleAndInspectOranges, "Squeezer");
        squeezerBottlerAndInspector.setDaemon(true);
        squeezerBottlerAndInspector.start();
        
        // Wait until it's time to stop
        while (timeToWork) {
        }

        System.out.println("");
        System.out.println(Thread.currentThread().getName() + " Done");
    }

    public int getProvidedOranges() {
        return orangesProvided;
    }

    public int getProcessedOranges() {
        return orangesProcessed;
    }

    public int getBottles() {
        return orangesProcessed / ORANGES_PER_BOTTLE;
    }

    public int getWaste() {
        return orangesProcessed % ORANGES_PER_BOTTLE;
    }

    public void fetchAndPeelOranges() {
        while (timeToWork) {
            Orange o = new Orange();
            System.out.print(".");
            orangesProvided++;
            while (o.getState() != Orange.State.Fetched) {
                o.runProcess();
            }
            mailbox.put(o);
        }
    }

    public void squeezeBottleAndInspectOranges() {
        while (timeToWork) {
            Orange o = mailbox.get();
            while (o.getState() != Orange.State.Bottled) {
                o.runProcess();
            }
        }
        orangesProcessed++;
    }
}
