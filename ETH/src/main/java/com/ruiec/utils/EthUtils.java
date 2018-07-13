package com.ruiec.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import net.sf.json.JSONObject;

/**
 * 心态坊工具类
 * 
 * @author bingo
 * @date 2018年3月15日 下午4:45:41
 */
public class EthUtils {

	protected final static Logger LOGGER = LoggerFactory.getLogger(EthUtils.class);

	/**
	 * GetTransactionReceipt
	 * 
	 * @author bingo
	 * @date 2018年3月15日 下午5:47:46
	 */
	public static String ethGetTransactionReceipt(Web3j web3j, String transactionHash) throws Exception {
		TransactionReceipt transactionReceipt = web3j.ethGetTransactionReceipt(transactionHash).sendAsync().get().getResult();
		if (null != transactionReceipt) {
			JSONObject jsonObject = JSONObject.fromObject(transactionReceipt);
			jsonObject.remove("cumulativeGasUsedRaw");
			jsonObject.remove("blockNumberRaw");
			jsonObject.remove("logIndexRaw");
			jsonObject.remove("transactionIndexRaw");
			jsonObject.remove("gasUsedRaw");
			return jsonObject.toString();
		}
		return null;
	}

	/**
	 * 查询余额
	 * 
	 * @author bingo
	 * @date 2018年3月16日 下午7:59:38
	 */
	public static BigInteger ethGetBalance(Web3j web3j, String ownerAddress) throws Exception {
		return web3j.ethGetBalance(ownerAddress, DefaultBlockParameterName.LATEST).sendAsync().get().getBalance();
	}

	/**
	 * 交易信息（JSON）
	 * 
	 * @author bingo
	 * @date 2018年3月29日 下午5:13:32
	 */
	public static String getTransaction(Web3j web3j, String txHash) throws Exception {
		Transaction transaction = web3j.ethGetTransactionByHash(txHash).send().getResult();
		TransactionReceipt transactionReceipt = web3j.ethGetTransactionReceipt(txHash).send().getResult();
		JSONObject jsonData = new JSONObject();
		jsonData.put("transactionHash", transaction.getHash());
		Boolean txReceiptStatus = (transactionReceipt.getStatus() != null && transactionReceipt.getStatus().equals("0x1")) ? true : false;
		jsonData.put("TxReceiptStatus", txReceiptStatus);
		// 已确认区块数 = 区块高度 - 交易所在区块数
		BigInteger blockHeight = web3j.ethBlockNumber().send().getBlockNumber();
		BigInteger blockConfirmations = blockHeight.subtract(transaction.getBlockNumber());
		jsonData.put("blockHeight", transaction.getBlockNumber());
		jsonData.put("blockConfirmations", blockConfirmations);
		BigInteger timeStamp = web3j.ethGetBlockByHash(transaction.getBlockHash(), false).send().getResult().getTimestamp();
		jsonData.put("timeStamp", new Date(timeStamp.longValue() * 1000).toString());
		jsonData.put("from", transaction.getFrom());
		jsonData.put("to", transaction.getTo());
		BigDecimal bigDecimal = new BigDecimal(transaction.getValue()).divide(new BigDecimal("1000000000000000000"));
		jsonData.put("value", bigDecimal.toPlainString());
		jsonData.put("gasLimit", transaction.getGas());
		jsonData.put("cumulativeGasUsed", transactionReceipt.getCumulativeGasUsed());
		jsonData.put("gasUsedByTxn", transactionReceipt.getGasUsed());
		jsonData.put("gasPrice", transaction.getGasPrice());
		BigDecimal actualTxCost = new BigDecimal(transaction.getGasPrice().multiply(transactionReceipt.getGasUsed())).divide(BigDecimal.valueOf(1000000000000000000L));
		jsonData.put("actualTxCost", actualTxCost);
		jsonData.put("nonce", transaction.getNonce());
		jsonData.put("inputData", transaction.getInput());
		return jsonData.toString();
	}

}
