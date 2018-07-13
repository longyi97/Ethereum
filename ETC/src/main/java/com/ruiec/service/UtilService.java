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

import com.ruiec.utils.PropertiesUtils;

/**
 * 基础功能服务
 * 
 * @author bingo
 * @date 2018年3月6日 下午1:48:37
 */
public class UtilService {

	protected final static Logger LOGGER = LoggerFactory.getLogger(UtilService.class);

	/** 以太坊节点http-rpc地址（默认"http://127.0.0.1:8545"） */
	public static final String URL;
	/** gas价格（默认"18_000_000_000L"） */
	public static final BigInteger GASPRICE;
	/** gas上限（默认"6_700_000L"） */
	public static final BigInteger GASLIMIT;
	private static Web3j web3j;
	private static Admin admin;
	/** Geth客户端编号 */
	private static int currentNum = 0;

	static {
		URL = PropertiesUtils.getProperty("walletServerUrl", "http://127.0.0.1:8545");
		GASPRICE = new BigInteger(PropertiesUtils.getProperty("GASPRICE", "18000000000"));
		GASLIMIT = new BigInteger(PropertiesUtils.getProperty("GASLIMIT", "6700000"));
		String[] urls = URL.split(",");
		web3j = Web3j.build(new HttpService(urls[0]));
		admin = Admin.build(new HttpService(urls[0]));
	}

	/** 获取 web3j */
	public static Web3j getWeb3j() {
		return web3j;
	}

	/** 设置web3j */
	public static void setWeb3j(Web3j web3j) {
		UtilService.web3j = web3j;
	}

	/** 获取 admin */
	public static Admin getAdmin() {
		return admin;
	}

	/** 设置admin */
	public static void setAdmin(Admin admin) {
		UtilService.admin = admin;
	}

	/**
	 * 切换Geth客户端
	 * 
	 * @author bingo
	 * @date 2018年4月28日 下午3:51:03
	 */
	public static Integer nextGethClient() {
		String[] urls = URL.split(",");
		if (urls.length > 1) {
			currentNum = ++currentNum % urls.length;
			web3j = Web3j.build(new HttpService(urls[currentNum]));
			admin = Admin.build(new HttpService(urls[currentNum]));
		}
		return currentNum;
	}

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
	 * @throws ExecutionException 
	 * @throws InterruptedException 
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

	/**
	 * Geth客户端状态
	 * 
	 * @author bingo
	 * @date 2018年4月27日 下午3:00:46
	 */
	public static Boolean gethStatus() {
		try {
			if (web3j.ethBlockNumber().send().getBlockNumber().compareTo(BigInteger.ZERO) > 0) {
				return true;
			}
		} catch (IOException e) {
			LOGGER.error("geth status exception: {}", e.getMessage());
		}
		return false;
	}
	
}
