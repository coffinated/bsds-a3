import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@WebServlet(name = "TextServer", value = "/TextServer")
public class TextServer extends HttpServlet {
  String qName = "bsds-words";
  String qUrl;
  SqsClient sqsClient;
  TextDao textDao;

  public void init() throws ServletException {

    sqsClient = SqsClient.builder()
            .region(Region.US_EAST_1)
            .build();
    try {
      CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
              .queueName(qName)
              .build();

      sqsClient.createQueue(createQueueRequest);

      GetQueueUrlResponse getQueueUrlResponse =
              sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(qName).build());
      qUrl = getQueueUrlResponse.queueUrl();
    } catch (SqsException e) {
      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1);
    }

    textDao = new TextDao();
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
          throws IOException {

    res.setContentType("application/json");

    String reqPath = req.getPathInfo();

    if (reqPath == null || reqPath.isEmpty()) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
      return;
    }

    if (reqPath.equals("/wordcount") &&
            req.getContentType().startsWith("application/json")) {

      Gson gson = new Gson();
      BufferedReader reqBody = req.getReader();
      Map<String, String> msg = gson.fromJson(reqBody,
              new TypeToken<Map<String, String>>(){}.getType());

      if (!msg.get("message").isEmpty()) {

        String[] words = msg.get("message").split("\\s");
        int count = words.length;
        List<SendMessageBatchRequestEntry> msgs = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
          // create SQS entry for each word in the line
          if (words[i].trim().isEmpty()) {
            count--;
            continue;
          }
          SendMessageBatchRequestEntry wordReq = SendMessageBatchRequestEntry.builder()
                  .id(UUID.randomUUID().toString())
                  .messageBody("{" + words[i] + ", 1}")
                  .build();
          msgs.add(wordReq);
          if (msgs.size() == 10 || i == words.length-1) {
            // send msgs to SQS queue in batches of 10 (or fewer if end of line is reached)
            SendMessageBatchRequest sendMessageBatchRequest = SendMessageBatchRequest.builder()
                    .queueUrl(qUrl)
                    .entries(msgs)
                    .build();
            sqsClient.sendMessageBatch(sendMessageBatchRequest);
            msgs.clear();
          }
        }

        // compile and return result
        ResultVal resVal = new ResultVal(count);
        String jsonRes = gson.toJson(resVal);
        res.setStatus(HttpServletResponse.SC_OK);
        res.getWriter().write(jsonRes);
        return;
      }
    }

    res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
          throws IOException {
    String paramWord = "";
    res.setContentType("application/json");

    // check for correct path and word parameter
    String reqPath = req.getPathInfo();
    if (reqPath == null || reqPath.isEmpty()) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
      return;
    }
    if (reqPath.equals("/wordcount")) {
      paramWord = req.getParameter("word");
    }
    // send back 400 if incorrect path/no word param
    if (paramWord == null || paramWord.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getWriter().write("No word supplied - word parameter is required!");
      return;
    }

    // query database for word count
    int wordCount = textDao.getWordCount(paramWord);
    // send back 400 if word not in database
    if (wordCount == 0) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write("Invalid word supplied - found 0 in the database");
      return;
    }

    // compile and return result
    Gson gson = new Gson();
    ResultVal resVal = new ResultVal(wordCount);
    String jsonRes = gson.toJson(resVal);
    res.setStatus(HttpServletResponse.SC_OK);
    res.getWriter().write(jsonRes);
  }

  // TODO: change load balancer to redirect requests based on method?

  public void destroy() {
    sqsClient.close();
    System.out.println("SQS client connection closed");
  }
}
