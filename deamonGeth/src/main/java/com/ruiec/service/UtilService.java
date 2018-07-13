package com.ruiec.service;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.admin.methods.response.PersonalUnlockAccount;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Files;

import com.ruiec.util.PropertiesUtils;

/**
 * 基础功能服务
 * 
 * @author bingo
 * @date 2018年3月6日 下午1:48:37
 */
public class UtilService {
	
	protected final static Logger LOGGER = LoggerFactory.getLogger(UtilService.class);
	
	/** 以太坊节点http-rpc地址（默认） */
	public static String URL = "http://127.0.0.1:8545";
	/** gas价格 */
	public static BigInteger GASPRICE = BigInteger.valueOf(18_000_000_000L);
	/** gas上限 */
	public static BigInteger GASLIMIT = BigInteger.valueOf(6_700_000L);
	
	static {
		URL = PropertiesUtils.getProperty("rpcUrl", URL);
		GASPRICE = BigInteger.valueOf(Long.valueOf(PropertiesUtils.getProperty("GASPRICE", GASPRICE.toString())));
		GASLIMIT = BigInteger.valueOf(Long.valueOf(PropertiesUtils.getProperty("GASLIMIT", GASLIMIT.toString())));
	}
	
	public static Web3j web3j = Web3j.build(new HttpService(URL));
	public static Admin admin = Admin.build(new HttpService(URL));
	
	/**
	 * 解锁账户
	 * 
	 * @author bingo
	 * @date 2018年3月7日 下午3:18:45
	 */
	public static Boolean unlockAccount(String address, String password) throws Exception {
		PersonalUnlockAccount personalUnlockAccount = admin.personalUnlockAccount(address, password).send();
		if (null != personalUnlockAccount.getResult()) {
			return personalUnlockAccount.accountUnlocked();
		} else {
			return false;
		}
	}
	
	/**
	 * 获取NONCE
	 * 
	 * @author bingo
	 * @date 2018年3月6日 下午1:52:29
	 */
	public static BigInteger getNonce(String address) throws Exception {
		return web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).sendAsync().get().getTransactionCount();
	}

	/**
	 * 获取智能合约编译后的二进制代码
	 * 
	 * @author bingo
	 * @param filePath
	 *            solidity编译后的bin文件
	 * @throws IOException
	 * @date 2018年3月6日 下午2:11:03
	 */
	public static String getSolidityBinary(String filePath) throws IOException {
		return Files.readString(new File(filePath));
	}
	
	/**
	 * 获取智能合约编译后的二进制代码
	 * 
	 * @author bingo
	 * @param filePath
	 *            solidity编译后的json文件
	 * @throws IOException 
	 * @date 2018年3月6日 下午2:11:03
	 */
//	public static String getSolidityBinary(String filePath) throws IOException {
//		String jsonContent = Files.readString(new File(filePath));
//		LOGGER.info("jsonContent: {}", jsonContent);
//		JSONObject json = JSONObject.fromObject(jsonContent);
//		LOGGER.info("json: {}", json.toString());
//		return json.getString("bytecode");
//	}
	
	/**
	 * 发布智能合约
	 * 
	 * @author bingo
	 * @date 2018年3月26日 下午5:20:05
	 */
	public static String deployContract(String address, String password, String filePath) {
		EthSendTransaction transactionResponse;
		try {
			// 解锁账户
			if (unlockAccount(address, password)) {
				// solicity编译后的字节码
				String solidityBinary = getSolidityBinary(filePath);
				BigInteger nonce = getNonce(address);
				Transaction transaction = Transaction.createContractTransaction(address, nonce, GASPRICE, GASLIMIT, BigInteger.ZERO, solidityBinary);
				transactionResponse = web3j.ethSendTransaction(transaction).sendAsync().get();
				return transactionResponse.getTransactionHash();
			}
		} catch (Exception e) {
			LOGGER.error("Exception", e);
		}
		return null;
	}
	
	/**
	 * 根据账户地址查询账户
	 * 
	 * @author bingo
	 * @date 2018年4月16日 下午4:01:50
	 */
	public static Boolean findAccount(String address) throws InterruptedException, ExecutionException {
		List<String> accounts = web3j.ethAccounts().sendAsync().get().getResult();
		for (String account : accounts) {
			if (account.equalsIgnoreCase(address)) {
				return true;
			}
		}
		return false;
	}
	
}
