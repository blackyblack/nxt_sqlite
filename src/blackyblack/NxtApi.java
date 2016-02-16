package blackyblack;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import nrs.NxtException.NxtApiException;
import nrs.crypto.Crypto;
import nrs.crypto.EncryptedData;
import nrs.util.Convert;
import nxtdb.Application;

public class NxtApi implements INxtApi
{
  //private static Long transactionFee = Constants.ONE_NXT;
  public static Object lock = new Object();
  public static String host;
  public static int port;
  
  static {
    host = Application.defaultNrsHost;
    if(Application.isTestnet)
    {      
      port = Application.defaultTestnetNrsPort;
    }
    else
    {
      port = Application.defaultNrsPort;
    }
  }
  
  public static String api()
  {
    return "http://" + host + ":" + port + "/nxt";
  }
  
  public Long now()
  {
    return (long) Convert.getEpochTime();
  }
  
  public String getPublicKey(String account) throws NxtApiException
  {
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "getAccountPublicKey"));
    fields.add(new BasicNameValuePair("account", account));
    
    CloseableHttpResponse response = null;
    String publicKey = null;
    try
    {
      try 
      {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost http = new HttpPost(api());
        http.setHeader("Origin", host);
        http.setEntity(entity);
        response = httpclient.execute(http);
        HttpEntity result = response.getEntity();
        String content = EntityUtils.toString(result);
        
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject)parser.parse(content);
  
        publicKey = Convert.emptyToNull((String)json.get("publicKey"));
        if(publicKey == null)
        {
          throw new NxtApiException("no publicKey from NRS");
        }
      }
      finally
      {
        if(response != null)
          response.close();
      }
    }
    catch (Exception e)
    {
      throw new NxtApiException(e.getMessage());
    }
    
    return publicKey;
  }
  
  public JSONObject create(String recipient,
      String secretPhrase) throws NxtApiException
  {    
    byte[] publicKey = Crypto.getPublicKey(secretPhrase);
    String publicString = Convert.toHexString(publicKey);
    
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "sendMessage"));
    fields.add(new BasicNameValuePair("recipient", recipient));
    fields.add(new BasicNameValuePair("publicKey", publicString));
    fields.add(new BasicNameValuePair("feeNQT", "0"));
    fields.add(new BasicNameValuePair("broadcast", "false"));
    fields.add(new BasicNameValuePair("deadline", "1440"));
    
    CloseableHttpResponse response = null;
    JSONObject tx = null;
    try
    {
      try 
      {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost http = new HttpPost(api());
        http.setHeader("Origin", host);
        http.setEntity(entity);
        response = httpclient.execute(http);
        HttpEntity result = response.getEntity();
        String content = EntityUtils.toString(result);
        
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject)parser.parse(content);
        tx = (JSONObject) json.get("transactionJSON");
        if(tx == null)
        {
          throw new NxtApiException("no transactionJSON from NRS");
        }
      }
      finally
      {
        if(response != null)
          response.close();
      }
    }
    catch (Exception e)
    {
      throw new NxtApiException(e.getMessage());
    }
    
    return tx;
  }
  
  public String broadcast(String message) throws NxtApiException
  {    
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "broadcastTransaction"));
    fields.add(new BasicNameValuePair("transactionBytes", message));
    
    CloseableHttpResponse response = null;
    String txid = null;
    try
    {
      try 
      {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost http = new HttpPost(api());
        http.setHeader("Origin", host);
        http.setEntity(entity);
        response = httpclient.execute(http);
        HttpEntity result = response.getEntity();
        String content = EntityUtils.toString(result);
        
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject)parser.parse(content);
  
        txid = Convert.emptyToNull((String)json.get("transaction"));
        if(txid == null)
        {
          throw new NxtApiException("no txid from NRS");
        }
      }
      finally
      {
        if(response != null)
          response.close();
      }
    }
    catch (Exception e)
    {
      throw new NxtApiException(e.getMessage());
    }
    
    return txid;
  }
  
  @SuppressWarnings("unchecked")
  public List<String> getTransactionIds(String account,
      Boolean pays, Boolean tells,
      int timelimit) throws Exception
  {
    String messageType = null;
    if(pays && !tells)
    {
      messageType = "0";
    }
    if(tells && !pays)
    {
      messageType = "1";
    }
    
    String timestamp = null;
    if(timelimit > 0)
    {
      if(now() > timelimit)
      {
        timestamp = "" + (now() - timelimit);
      }
    }
    
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "getAccountTransactionIds"));
    fields.add(new BasicNameValuePair("account", account));
    if(messageType != null)
    {
      fields.add(new BasicNameValuePair("type", messageType));
    }
    if(timestamp != null)
    {
      fields.add(new BasicNameValuePair("timestamp", timestamp));
    }
    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
    
    JSONObject json = null;
    CloseableHttpResponse response = null;
    CloseableHttpClient httpclient = HttpClients.createDefault();
    HttpPost http = new HttpPost(api());
    http.setHeader("Origin", host);
    http.setEntity(entity);
    response = httpclient.execute(http);
    HttpEntity result = response.getEntity();
    String content = EntityUtils.toString(result);
      
    JSONParser parser = new JSONParser();
    json = (JSONObject)parser.parse(content);
    JSONArray a = (JSONArray)json.get("transactionIds");
    return (List<String>)a;
  }
  
  public JSONObject getTransaction(String txid) throws Exception
  {
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "getTransaction"));
    fields.add(new BasicNameValuePair("transaction", txid));
    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
    
    JSONObject json = null;
    CloseableHttpResponse response = null;
    CloseableHttpClient httpclient = HttpClients.createDefault();
    HttpPost http = new HttpPost(api());
    http.setHeader("Origin", host);
    http.setEntity(entity);
    response = httpclient.execute(http);
    HttpEntity result = response.getEntity();
    String content = EntityUtils.toString(result);
      
    JSONParser parser = new JSONParser();
    json = (JSONObject)parser.parse(content);
    if(json == null) return null;
    if(Convert.emptyToNull((String)json.get("transaction")) == null) return null;
    return json;
  }
  
  public String readEncryptedMessageSafe(String txid, String secretPhrase) throws NxtApiException
  {
    JSONObject o = null;
    try
    {
      o = getTransaction(txid);
    }
    catch (Exception e)
    {
      throw new NxtApiException(e.getMessage());
    }
    
    if(o == null) return "";
    
    String sender = (String) o.get("senderRS");
    if(sender == null) return "";
    
    String senderPublicKey = getPublicKey(sender);
    
    JSONObject attach = (JSONObject) o.get("attachment");
    if(attach == null) return "";
    
    JSONObject encryptedMessage = (JSONObject) attach.get("encryptedMessage");
    if(encryptedMessage == null) return "";
    
    String data = (String) encryptedMessage.get("data");
    String nonce = (String) encryptedMessage.get("nonce");
    
    if(data == null || nonce == null) return "";
    
    EncryptedData enc = null;
    byte[] theirPublicKey = null;
    try
    {
      enc = new EncryptedData(Convert.parseHexString(data), Convert.parseHexString(nonce));
      theirPublicKey = Convert.parseHexString(senderPublicKey);
    }
    catch (Exception e)
    {
    }
    
    if(enc == null || theirPublicKey == null) return "";
    
    byte[] result = enc.decrypt(Crypto.getPrivateKey(secretPhrase), theirPublicKey);
    return Convert.toString(result);
  }
  
  public String readEncryptedMessage(String txid, String secretPhrase) throws NxtApiException
  {
    return readEncryptedMessageSafe(txid, secretPhrase);
  }
  
  public JSONObject getBlockchainStatus() throws NxtApiException
  {
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "getBlockchainStatus"));
    
    JSONObject answer = null;
    CloseableHttpResponse response = null;
    try
    {
      try 
      {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost http = new HttpPost(NxtApi.api());
        http.setHeader("Origin", NxtApi.host);
        http.setEntity(entity);
        response = httpclient.execute(http);
        HttpEntity result = response.getEntity();
        String content = EntityUtils.toString(result);
          
        JSONParser parser = new JSONParser();
        answer = (JSONObject)parser.parse(content);
      }
      catch(ConnectException e)
      {
      }
      catch(ClientProtocolException e)
      {
      }
      catch(IOException e)
      {
      }
      finally
      {
        if(response != null)
          response.close();
      }
    }
    catch (Exception e)
    {
      throw new NxtApiException(e.getMessage());
    }
    
    return answer;
  }
  
  public Long getCurrentBlock() throws NxtApiException
  {
    JSONObject status = getBlockchainStatus();
    
    Long blocksNow = Convert.nullToZero((Long) status.get("numberOfBlocks"));
    return blocksNow;
  }
  
  public JSONObject getAsset(String txid) throws NxtApiException
  {
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "getAsset"));
    fields.add(new BasicNameValuePair("asset", txid));
    fields.add(new BasicNameValuePair("includeCounts", "false"));
    
    CloseableHttpResponse response = null;
    JSONObject json = null;
    try
    {
      try 
      {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost http = new HttpPost(api());
        http.setHeader("Origin", host);
        http.setEntity(entity);
        response = httpclient.execute(http);
        HttpEntity result = response.getEntity();
        String content = EntityUtils.toString(result);
        
        JSONParser parser = new JSONParser();
        json = (JSONObject)parser.parse(content);
      }
      finally
      {
        if(response != null)
          response.close();
      }
    }
    catch (Exception e)
    {
      throw new NxtApiException(e.getMessage());
    }
    
    return json;
  }
  
  public JSONObject getBlock(long height) throws Exception
  {
    List<BasicNameValuePair> fields = new ArrayList<BasicNameValuePair>();
    fields.add(new BasicNameValuePair("requestType", "getBlock"));
    fields.add(new BasicNameValuePair("height", "" + height));
    fields.add(new BasicNameValuePair("includeTransactions", "true"));
    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(fields, "UTF-8");
    
    JSONObject json = null;
    CloseableHttpResponse response = null;
    CloseableHttpClient httpclient = HttpClients.createDefault();
    HttpPost http = new HttpPost(api());
    http.setHeader("Origin", host);
    http.setEntity(entity);
    response = httpclient.execute(http);
    HttpEntity result = response.getEntity();
    String content = EntityUtils.toString(result);
      
    JSONParser parser = new JSONParser();
    json = (JSONObject)parser.parse(content);
    return json;
  }
}
