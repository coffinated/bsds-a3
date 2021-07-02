import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;

public class TextDao {

  private static HikariDataSource dataSrc;

  public TextDao() {
    dataSrc = DataSource.getDataSource();
  }

  public void addWords(ArrayList<String> words) {
    Connection conn = null;
    PreparedStatement statement = null;
    String insertStatement = "INSERT INTO words (word) VALUES (?)";

    if (words.size() == 0) {
      System.out.println("Tried to insert 0 words?!");
    }

    try {
      conn = dataSrc.getConnection();
      conn.setAutoCommit(false);
      statement = conn.prepareStatement(insertStatement);

      for (int i = 0; i < words.size(); i++) {
        statement.setString(1, words.get(i));
        statement.addBatch();
      }
      statement.executeBatch();
      conn.commit();
    } catch (SQLException se) {
      System.out.println("SQL error: " + se.getErrorCode());
      System.out.println("Error Message: " + se.getMessage());
      System.out.println("Word batch: " + words);
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (statement != null) {
          statement.close();
        }
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }
  }
}
