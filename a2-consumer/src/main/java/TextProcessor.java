import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

public class TextProcessor {
  private final String qUrl;
  private final SqsClient sqsClient;
  private final ConcurrentHashMap<String, Integer> textMap;

  public TextProcessor() {
    sqsClient = SqsClient.builder()
            .region(Region.US_EAST_1)
            .build();

    GetQueueUrlResponse getQueueUrlResponse =
            sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName("bsds-words").build());
    qUrl = getQueueUrlResponse.queueUrl();

    textMap = new ConcurrentHashMap<>();
  }

  public static void main(String[] args) throws InterruptedException {

    int numThreads = 20;
    if (args.length > 0) {
      numThreads = Integer.parseInt(args[0]);
    }
    TextProcessor tp = new TextProcessor();
    CountDownLatch done = new CountDownLatch(numThreads);

    Runnable consumerThread = () -> {
      System.out.println("Starting thread " + Thread.currentThread());
      try {
        while (true) {
          ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                  .queueUrl(tp.qUrl)
                  .maxNumberOfMessages(5) // TODO: does this have to be 1?
                  .waitTimeSeconds(5)
                  .build();
          List<Message> messages = tp.sqsClient.receiveMessage(receiveMessageRequest).messages();

          if (messages.size() == 0) {
            System.out.println("Thread " + Thread.currentThread() + " got 0 messages, exiting");
            done.countDown();
            break;
          }

          for (Message each : messages) {
            String word = each.body().split(",")[0].substring(1);
            tp.textMap.put(word, tp.textMap.getOrDefault(word, 0) + 1);
          }

          // TODO: determine if changing visibility timeout is necessary
          //      changeMessages(tc.sqsClient, tc.qUrl, messages);

          deleteMessages(tp.sqsClient, tp.qUrl, messages);
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

    done.await();
    tp.sqsClient.close();
  }

  public static void changeMessages(SqsClient sqsClient, String queueUrl, List<Message> messages) {
    try {
      for (Message message : messages) {
        ChangeMessageVisibilityRequest req = ChangeMessageVisibilityRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .visibilityTimeout(100)
                .build();
        sqsClient.changeMessageVisibility(req);
      }
    } catch (SqsException e) {
      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1);
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
