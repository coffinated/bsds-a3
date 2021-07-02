package io.swagger.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

public class TextProducer implements Runnable {

  private final BlockingQueue<String> bq;
  private final Path file;
  private final int maxThreads;

  public TextProducer(BlockingQueue<String> bq, Path file, int maxThreads) {
    this.bq = bq;
    this.file = file;
    this.maxThreads = maxThreads;
  }

  public void run() {
    try {
      BufferedReader reader = Files.newBufferedReader(this.file);
      String line;
      // using BufferedQueue means lines will be added until the limit is reached,
      // then the producer will wait until there is space to add more lines
      while ((line = reader.readLine()) != null) {
        if (line.trim().isEmpty()) {
          continue;
        }
        bq.put(line);
      }

      for (int i = 0; i < this.maxThreads; i++) {
        // add one 'EOF' to queue for each consumer before breaking out
        bq.put("EOF");
      }

      System.out.println("Producer reached EOF");
      reader.close();
    } catch (InterruptedException | IOException ex) {
      System.err.format("Exception: %s%n", ex);
    }
  }
}
