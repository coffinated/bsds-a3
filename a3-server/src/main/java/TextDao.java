import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;

public class TextDao {

  private static HikariDataSource dataSrc;

  public TextDao() {
    dataSrc = DataSource.getDataSource();
  }

  public int getWordCount(String word) {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet result = null;
    int wordCount = 0;
    String query = "SELECT COUNT(*) as wordCount FROM words WHERE word=(?)";

    try {
      conn = dataSrc.getConnection();
      statement = conn.prepareStatement(query);
      statement.setString(1, word);
      result = statement.executeQuery();
      while (result.next()) {
        wordCount = result.getInt("wordCount");
      }
    } catch (SQLException se) {
      System.out.println("SQL error: " + se.getErrorCode());
      System.out.println("Error Message: " + se.getMessage());
      System.out.println("Word: " + word);
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (statement != null) {
          statement.close();
        }
        if (result != null) {
          result.close();
        }
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }

    return wordCount;
  }
}