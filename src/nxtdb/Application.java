package nxtdb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import blackyblack.INxtApi;
import blackyblack.NxtApi;
import nrs.util.Convert;
import nrs.util.Logger;

public class Application
{
  public static final boolean isTestnet = true;
  public static final int defaultNrsPort = 7876;
  public static final int defaultTestnetNrsPort = 6876;
  public static final String defaultNrsHost = "127.0.0.1";
  public static final String version;
  static
  {
    if(isTestnet)
    {
      version = "-test";
    }
    else
    {
      version = "";
    }
  }
  
  public static INxtApi api = new NxtApi();
  public static Boolean terminated = false;
  public static String nrsVersion = null;

  public static void main(String[] args)
  {
    try
    {
      Connection db = createDB();
      copyBlock(db, 0);
      closeDB(db);
    }
    catch(Exception e)
    {
      Logger.logMessage("App failed", e);
    }
  }
  
  public static void copyBlock(Connection db, long height) throws SQLException
  {
    PreparedStatement stmt = null;
    NxtApi api = new NxtApi();
    
    try
    {
      JSONObject fullBlock = api.getBlock(height);
      JSONArray transactions = (JSONArray)fullBlock.get("transactions");
      
      for(Object a : transactions)
      {
        JSONObject tx = (JSONObject) a;
        if(tx == null) continue;
        
        Long type = (Long) tx.get("type");
        Long subtype = (Long) tx.get("subtype");
        
        //process only payments and messages
        if(!(type == 0 && subtype == 0) && !(type == 1)) continue;
          
        String transactionId = (String) tx.get("transaction");
        String senderRS = (String) tx.get("senderRS");
        String recipientRS = (String) tx.get("recipientRS");
        Long payment = 0L;
        
        //process payments
        if(type == 0 && subtype == 0)
        {
          String paymentStr = (String) tx.get("amountNQT");
          try
          {
            payment = Convert.parseLong(paymentStr);
          }
          catch(Exception e)
          {
          }
        }
        
        try
        {
          String sql = "INSERT INTO TRANSACTIONS (ID, TYPE, PAYMENT, SENDER, RECIPIENT) VALUES (?, ?, ?, ?, ?)";
          stmt = db.prepareStatement(sql);
          int paramNum = 1;
          stmt.setString(paramNum++, transactionId);
          stmt.setInt(paramNum++, type.intValue());
          stmt.setLong(paramNum++, payment);
          stmt.setString(paramNum++, senderRS);
          stmt.setString(paramNum++, recipientRS);
          stmt.executeUpdate();
        }
        catch(SQLException e)
        {
          Logger.logMessage("SQL insert failed for tx " + transactionId, e);
        }
      }
    }
    catch(Exception e)
    {
      Logger.logMessage("Block processing failed", e);
    }
    
    if(stmt != null)
    {
      stmt.close();
    }
  }

  public static Connection createDB() throws Exception
  {
    Connection c = null;
    Statement stmt = null;
    Class.forName("org.sqlite.JDBC");
    c = DriverManager.getConnection("jdbc:sqlite:test.db");
    Logger.logMessage("Opened database successfully");

    String sql = "";
    
    sql = "DROP TABLE IF EXISTS TRANSACTIONS";
    stmt = c.createStatement();
    stmt.executeUpdate(sql);
    stmt.close();
    
    sql = "CREATE TABLE TRANSACTIONS " +
        "(ID TEXT PRIMARY KEY    NOT NULL," +
        " TYPE           INTEGER NOT NULL, " +
        " PAYMENT        INTEGER NOT NULL, " + 
        " SENDER         TEXT    NOT NULL, " + 
        " RECIPIENT      TEXT    NOT NULL)";
    
    stmt = c.createStatement();
    stmt.executeUpdate(sql);
    stmt.close();
    return c;
  }
  
  public static void closeDB(Connection c) throws SQLException
  {
    c.close();
    Logger.logMessage("Closed database successfully");
  }

}
