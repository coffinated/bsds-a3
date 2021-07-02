import java.util.ArrayList;
import java.util.List;

import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

public class TextProcessor {
  private final String qUrl;
  private final SqsClient sqsClient;
  private static TextDao textDao;
  private static int emptyReceives;
  private static boolean startedReceivingMessages;

  public TextProcessor(int numThreads) {
    sqsClient = SqsClient.builder()
            .region(Region.US_EAST_1)
            .httpClientBuilder(ApacheHttpClient.builder()
                    .maxConnections(numThreads))
            .build();

    GetQueueUrlResponse getQueueUrlResponse =
            sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName("bsds-words").build());
    qUrl = getQueueUrlResponse.queueUrl();
    textDao = new TextDao();
    emptyReceives = 0;
  }

  public static void main(String[] args) {
    int numThreads = 800;
    if (args.length > 0) {
      numThreads = Integer.parseInt(args[0]);
    }
    TextProcessor tp = new TextProcessor(numThreads);

    Runnable consumerThread = () -> {
      System.out.println("Starting thread " + Thread.currentThread());
      try {
        while (true) {
          ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                  .queueUrl(tp.qUrl)
                  .maxNumberOfMessages(10)
                  .waitTimeSeconds(5)
                  .build();
          List<Message> messages = tp.sqsClient.receiveMessage(receiveMessageRequest).messages();
          ArrayList<String> words = new ArrayList<>();

          for (Message each : messages) {
            int length = each.body().length();
            String word = each.body().substring(1, length-4);

            if (word.isEmpty()) {
              System.out.println("Empty payload:");
              System.out.println(each.body());
            } else {
              words.add(word);
            }
          }

          if (words.size() > 0) {
            textDao.addWords(words);
            deleteMessages(tp.sqsClient, tp.qUrl, messages);
            startedReceivingMessages = true;
          } else if (startedReceivingMessages && emptyReceives++ > 500) {
            tp.sqsClient.close();
            System.out.println("Received 500 empties from queue, exiting");
            System.exit(0);
          }
        }

      } catch (SqsException e) {
        System.err.println(e.awsErrorDetails().errorMessage());
        System.exit(1);
      }
    };

    for (int i = 0; i < numThreads; i++) {
      Thread ct = new Thread(consumerThread);
      ct.start();
    }
  }

  public static void deleteMessages(SqsClient sqsClient, String queueUrl, List<Message> messages) {
    try {
      List<DeleteMessageBatchRequestEntry> deleteMessages = new ArrayList<>();
      for (Message message : messages) {
        DeleteMessageBatchRequestEntry entry = DeleteMessageBatchRequestEntry.builder()
                .id(message.messageId())
                .receiptHandle(message.receiptHandle())
                .build();
        deleteMessages.add(entry);
      }
      DeleteMessageBatchRequest deleteMessageRequest = DeleteMessageBatchRequest.builder()
              .queueUrl(queueUrl)
              .entries(deleteMessages)
              .build();
      sqsClient.deleteMessageBatch(deleteMessageRequest);
    } catch (SqsException e) {
      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1);
    }
  }
}
