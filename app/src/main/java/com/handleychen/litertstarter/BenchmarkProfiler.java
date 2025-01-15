package com.handleychen.litertstarter;

public class BenchmarkProfiler {

    private long startT;
    private int count;
    private long minT = 1000000L, maxT, accuT;

    public void start() {
        startT = System.currentTimeMillis();
    }

    public void end() {
        long costT = System.currentTimeMillis() - startT;
        if (costT < 0) {
            throw new RuntimeException("costT < 0");
        }
        accuT += costT;
        ++count;
        if (costT > maxT) {
            maxT = costT;
        }
        if (costT < minT) {
            minT = costT;
        }
    }

    public long getMinT() {
        return minT;
    }

    public long getMaxT() {
        return maxT;
    }

    public long getAvgT() {
        if (count == 0) {
            return 0;
        }
        return accuT / count;
    }
}
