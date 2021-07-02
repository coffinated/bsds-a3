package io.swagger.client;

public class RequestStat {
  static String EOF = "EOF";

  long startTime;
  String method;
  long latency;
  int code;

  public RequestStat(long startTime, String method, long latency, int code) {
    this.startTime = startTime;
    this.method = method;
    this.latency = latency;
    this.code = code;
  }

  public static RequestStat newEndOfStats() {
    return new RequestStat(0, RequestStat.EOF, 0, 0);
  }

  public boolean isEndOfStats() {
    return this.method == RequestStat.EOF;
  }

  @Override
  public String toString() {
    return this.startTime + "," + this.method + "," + this.latency + "," + this.code + "\n";
  }
}
