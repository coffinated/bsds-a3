import com.zaxxer.hikari.*;

public class DataSource {
  private static final HikariConfig config = new HikariConfig();
  private static final HikariDataSource dataSrc;

  private static final String HOST_NAME = System.getenv("MySQL_IP_ADDRESS");
  private static final String PORT = System.getenv("MySQL_PORT");
  private static final String DATABASE = "bsds";
  private static final String USERNAME = System.getenv("DB_USERNAME");
  private static final String PASSWORD = System.getenv("DB_PASSWORD");

  static {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC&rewriteBatchedStatements=true", HOST_NAME, PORT, DATABASE);
    // https://github.com/openbouquet/HikariCP
    config.setJdbcUrl(url);
    config.setUsername(USERNAME);
    config.setPassword(PASSWORD);
    config.setMaximumPoolSize(800);
    config.setConnectionTimeout(8000);
    config.addDataSourceProperty("cachePrepStmts", true);
    config.addDataSourceProperty("prepStmtCacheSize", 250);
    config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
    config.addDataSourceProperty("useServerPrepStmts", false);

    dataSrc = new HikariDataSource(config);
  }

  public static HikariDataSource getDataSource() {
    return dataSrc;
  }
}
