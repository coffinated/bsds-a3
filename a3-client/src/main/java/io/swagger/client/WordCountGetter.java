package io.swagger.client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import io.swagger.client.api.TextbodyApi;
import io.swagger.client.model.ResultVal;

public class WordCountGetter implements Runnable {

  private final String[] words;
  public boolean done;
  final BlockingQueue<RequestStat> stats = new LinkedBlockingQueue<>();
  private final RequestStatCollector getStatCollector;
  private int numRequests;

  public WordCountGetter() {
    this.words = new String[]{"the", "Gorton", "system", "can",
            "is", "request", "about", "to", "are", "text"};
    this.done = false;
    this.getStatCollector = new RequestStatCollector(stats);
    this.numRequests = 0;
  }

  public void run() {
    Thread gsc = new Thread(getStatCollector);
    gsc.start();
    TextbodyApi apiInstance = new TextbodyApi();
    String function = "wordcount";
    while (!done) {
      try {
        Thread.sleep(1000);

        try {
          for (int i = 0; i < 10; i++) {
            // send GET request to servlet, track start and end time
            long start = System.currentTimeMillis();
            ApiResponse<ResultVal> result = apiInstance.getWordCount(function, words[i]);
            long end = System.currentTimeMillis();

            // track stats in getStatCollector
            long latency = end - start;
            int code = result.getStatusCode();
            RequestStat stat = new RequestStat(start, "GET", latency, code);
            stats.put(stat);
            numRequests++;
          }
        } catch (ApiException e) {
          System.err.format("API Exception%n code = %s%n message = %s%n",
                  e.getCode(), e.getResponseBody());
        }
      } catch (InterruptedException e) {
        System.out.println(e.getMessage());
      }
    }
    // tell getStateCollector to finish up
    RequestStat notify = RequestStat.newEndOfStats();
    try {
      stats.put(notify);
      gsc.join();
    } catch (InterruptedException e) {
      System.err.format("Interrupted exception after GET loop done");
    }

    System.out.println("++++++++++++++++ GET Stats ++++++++++++++++");
    System.out.println("Number of GET requests: " + numRequests);
    System.out.println("Mean response time: " + getStatCollector.getMean() + " ms");
    System.out.println("Max response time: " + getStatCollector.getMax() + " ms");
    System.out.println("+++++++++++++++++++++++++++++++++++++++++++");
  }
}
