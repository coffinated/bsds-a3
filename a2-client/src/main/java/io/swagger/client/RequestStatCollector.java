package io.swagger.client;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;

public class RequestStatCollector implements Runnable {
  BlockingQueue<RequestStat> stats;
  ArrayList<Long> latencies;
  long sum;

  public RequestStatCollector(BlockingQueue<RequestStat> stats) {
    this.stats = stats;
    this.sum = 0;
    this.latencies = new ArrayList<>();
  }

  public void run() {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter("output.csv"));
      while (true) {
        RequestStat stat = stats.take();
        if (stat.isEndOfStats()) {
          break;
        }

        sum += stat.latency;
        latencies.add(stat.latency);
        writer.write(stat.toString());
      }
      Collections.sort(latencies);
      writer.close();
    } catch (InterruptedException | IOException e) {
      System.err.format("Exception on RequestStateCollector Runnable: %s%n", e);
    }
  }

  public long getPercentile(int percentile) {
    int total = latencies.size();
    int index = (int) (total * (percentile / 100.0));
    return latencies.get(index);
  }

  public long getMedian() {
    return this.getPercentile(50);
  }

  public double getMean() {
    return sum / (double)latencies.size();
  }

  public long getMax() {
    return Collections.max(latencies);
  }
}
