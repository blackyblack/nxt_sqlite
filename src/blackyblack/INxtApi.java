package blackyblack;

import java.util.List;

import nrs.NxtException.NxtApiException;

import org.json.simple.JSONObject;

public interface INxtApi
{
  public Long now();
  
  public String readEncryptedMessage(String txid, String secretPhrase) throws NxtApiException;
  
  public List<String> getTransactionIds(String account,
      Boolean pays, Boolean tells,
      int timelimit) throws Exception;
  
  public JSONObject getTransaction(String txid) throws Exception;
  
  /*public String transactionSafe(String recipient, String secretPhrase,
      String message, String messageEncrypt, long amountNQT) throws NxtApiException;
  
  public String assetTransferSafe(String recipient, String secretPhrase,
      String message, String messageEncrypt, String assetId, Long assetNQT) throws NxtApiException;*/
  
  public String getPublicKey(String account) throws NxtApiException;
  
  public JSONObject getBlockchainStatus() throws NxtApiException;
  
  public Long getCurrentBlock() throws NxtApiException;
  
  public JSONObject getAsset(String txid) throws NxtApiException;
}
