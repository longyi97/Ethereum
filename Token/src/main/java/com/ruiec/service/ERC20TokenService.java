package com.ruiec.service;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Log;

import net.sf.json.JSONObject;

/**
 * ERC20标准代币服务
 * (notice:代币智能合约源码 https://ethereum.org/token)
 * 
 * @author bingo
 * @date 2018年3月15日 下午4:24:54
 */
public class ERC20TokenService extends UtilService {
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(ERC20TokenService.class);
	
	/** solidity编译后的文件 */
	private static final String FILEPATH = System.getProperty("user.dir") + File.separatorChar + "src" + File.separatorChar + "TokenERC20_sol_TokenERC20.bin";
	
	/** 主账户地址 */
	protected static String ADDRESS;
	
	static {
		try {
			ADDRESS = UtilService.getWeb3j().ethCoinbase().sendAsync().get().getAddress();
		} catch (Exception e) {
			LOGGER.error("Exception", e);
		}
	}

	/**
	 * 新建ERC20代币
	 * 
	 * @param address
	 *            创建者账户地址
	 * @param password
	 *            账户密码
	 * @param initialSupply
	 *            代币初始发行量
	 * @param name
	 *            代币名称
	 * @param symbol
	 * @author bingo
	 * @date 2018年3月15日 下午4:25:09
	 */
	public String newTokenERC20(String address, String password, BigInteger initialSupply, String name, String symbol) {
		LOGGER.info("newToken ------------------------------------------------------------");
		EthSendTransaction transactionResponse;
		try {
			// 解锁账户
			if (unlockAccount(address, password)) {
				// solidity构造函数字节码
				String encodeConstructor = FunctionEncoder.encodeConstructor(Arrays.asList(new Uint256(initialSupply), new Utf8String(name), new Utf8String(symbol)));
				// solidity编译后字节码
				String solidityBinary = getSolidityBinary(FILEPATH);
				BigInteger nonce = getNonce(address);
				Transaction transaction = Transaction.createContractTransaction(address, nonce, GASPRICE, GASLIMIT, BigInteger.ZERO, solidityBinary + encodeConstructor);
				transactionResponse = UtilService.getWeb3j().ethSendTransaction(transaction).sendAsync().get();
				LOGGER.info("TransactionHash: {}", transactionResponse.getTransactionHash());
				return transactionResponse.getTransactionHash();
			}
		} catch (Exception e) {
			LOGGER.error("Exception", e);
			throw new RuntimeException(e);
		}
		return null;
	}
	
	/**
	 * 发送代币
	 * 
	 * @param from
	 *            发送方账户地址
	 * @param password
	 *            账户密码
	 * @param contractAddress
	 *            智能合约地址
	 * @param to
	 *            接收方账户地址
	 * @param value
	 *            发送数量
	 * @author bingo
	 * @date 2018年3月15日 下午4:25:47
	 */
	public String sendTokenTransaction(String from, String password, String contractAddress, String to, BigInteger value) {
		LOGGER.info("sendTokenTransaction ------------------------------------------------------------");
		try {
			// 解锁账户
			if (unlockAccount(from, password)) {
				Function function = new Function("transfer",
						Arrays.asList(new Address(to), new Uint256(value)),
						Collections.singletonList(new TypeReference<Bool>() {}));
				String encodedFunction = FunctionEncoder.encode(function);
				String transactionHash = execute(from, contractAddress, encodedFunction);
				LOGGER.info("TransactionHash: {}", transactionHash);
				return transactionHash;
			}
		} catch (Exception e) {
			LOGGER.error("Exception", e);
			throw new RuntimeException(e);
		}
		return null;
	}
	
	/**
	 * 发送代币
	 * 
	 * @param from
	 *            发送方账户地址
	 * @param password
	 *            账户密码
	 * @param contractAddress
	 *            智能合约地址
	 * @param to
	 *            接收方账户地址
	 * @param value
	 *            发送数量
	 * @param gasLimit
	 *            gas上限
	 * @author bingo
	 * @date 2018年3月15日 下午4:25:47
	 */
	public String sendTokenTransaction(String from, String password, String contractAddress, String to, BigInteger value, BigInteger gasLimit) {
		LOGGER.info("sendTokenTransaction ------------------------------------------------------------");
		try {
			// 解锁账户
			if (unlockAccount(from, password)) {
				Function function = new Function("transfer",
						Arrays.asList(new Address(to), new Uint256(value)),
						Collections.singletonList(new TypeReference<Bool>() {}));
				String encodedFunction = FunctionEncoder.encode(function);
				String transactionHash = execute(from, contractAddress, encodedFunction, GASPRICE, gasLimit);
				LOGGER.info("TransactionHash: {}", transactionHash);
				return transactionHash;
			}
		} catch (Exception e) {
			LOGGER.error("Exception", e);
			throw new RuntimeException(e);
		}
		return null;
	}
	
	/**
	 * 查询代币余额
	 * 
	 * @param ownerAddress
	 *            账户地址
	 * @param contractAddress
	 *            智能合约账户地址
	 * @author bingo
	 * @date 2018年3月15日 下午4:26:14
	 */
	public BigInteger getBalanceOf(String ownerAddress, String contractAddress) {
		LOGGER.info("getBalanceOf: {} ------------------------------------------------------------", ownerAddress);
		try {
			Function function = new Function("balanceOf",
					Collections.singletonList(new Address(ownerAddress)),
					Collections.singletonList(new TypeReference<Uint256>() {}));
			String responseValue = callSmartContractFunction(function, contractAddress);
			BigInteger balance = (BigInteger) FunctionReturnDecoder.decode(responseValue, function.getOutputParameters()).get(0).getValue();
			LOGGER.info("{} balance: {}", contractAddress, balance.toString());
			return balance;
		} catch (Exception e) {
			LOGGER.error("Exception", e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 查询代币发行量
	 * 
	 * @author bingo
	 * @date 2018年3月15日 下午4:26:47
	 */
	public BigInteger getTotalSupply(String contractAddress) {
		LOGGER.info("getTotalSupply ------------------------------------------------------------");
		try {
			Function function = new Function("totalSupply", 
					Collections.emptyList(), 
					Collections.singletonList(new TypeReference<Uint256>() {}));
			String responseValue = callSmartContractFunction(function, contractAddress);
			BigInteger totalSupply = (BigInteger) FunctionReturnDecoder.decode(responseValue, function.getOutputParameters()).get(0).getValue();
			LOGGER.info("totalSupply: {}", totalSupply.toString());
			return totalSupply;
		} catch (Exception e) {
			LOGGER.error("Exception", e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 查询代币名称
	 * 
	 * @author bingo
	 * @date 2018年3月15日 下午4:27:39
	 */
	public String getName(String contractAddress) {
		LOGGER.info("getName ------------------------------------------------------------");
		try {
			Function function = new Function("name", 
					Collections.emptyList(), 
					Collections.singletonList(new TypeReference<Utf8String>() {}));
			String responseValue = callSmartContractFunction(function, contractAddress);
			String name = (String) FunctionReturnDecoder.decode(responseValue, function.getOutputParameters()).get(0).getValue();
			LOGGER.info("name: {}", name);
			return name;
		} catch (Exception e) {
			LOGGER.error("Exception", e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 查询代币符号
	 * 
	 * @author bingo
	 * @date 2018年3月15日 下午4:27:49
	 */
	public String getSymbol(String contractAddress) {
		LOGGER.info("getSymbol ------------------------------------------------------------");
		try {
			Function function = new Function("symbol", 
					Collections.emptyList(), 
					Collections.singletonList(new TypeReference<Utf8String>() {}));
			String responseValue = callSmartContractFunction(function, contractAddress);
			String symbol = (String) FunctionReturnDecoder.decode(responseValue, function.getOutputParameters()).get(0).getValue();
			LOGGER.info("symbol: {}", symbol);
			return symbol;
		} catch (Exception e) {
			LOGGER.error("Exception", e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 调用智能合约中的方法
	 * 
	 * @author bingo
	 * @date 2018年3月7日 上午9:07:29
	 */
	protected String callSmartContractFunction(Function function, String contractAddress) throws Exception {
		String functionEncode = FunctionEncoder.encode(function);
		return UtilService.getWeb3j().ethCall(Transaction.createEthCallTransaction(ADDRESS, contractAddress, functionEncode), DefaultBlockParameterName.LATEST).sendAsync().get().getValue();
	}
	
	/**
	 * 执行智能合约交易
	 * 
	 * @author bingo
	 * @date 2018年3月7日 下午2:01:47
	 */
	protected String execute(String from, String contractAddress, String encodedFunction) throws Exception {
		BigInteger nonce = getNonce(from);
		Transaction transaction = Transaction.createFunctionCallTransaction(from, nonce, GASPRICE, GASLIMIT, contractAddress, encodedFunction);
		EthSendTransaction ethSendTransaction = UtilService.getWeb3j().ethSendTransaction(transaction).sendAsync().get();
		return ethSendTransaction.getTransactionHash();
	}
	
	/**
	 * 执行智能合约交易
	 * 
	 * @author bingo
	 * @date 2018年3月7日 下午2:01:47
	 */
	protected String execute(String from, String contractAddress, String encodedFunction,BigInteger gasPrice, BigInteger gasLimit) throws Exception {
		BigInteger nonce = getNonce(from);
		Transaction transaction = Transaction.createFunctionCallTransaction(from, nonce, gasPrice, gasLimit, contractAddress, encodedFunction);
		EthSendTransaction ethSendTransaction = UtilService.getWeb3j().ethSendTransaction(transaction).sendAsync().get();
		return ethSendTransaction.getTransactionHash();
	}

	/**
	 * 执行智能合约交易
	 *
	 * @author bingo
	 * @date 2018年05月19日 下午04:29:48
	 */
	protected String execute(String from, String contractAddress, String encodedFunction,BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, BigInteger value) throws Exception {
		Transaction transaction = Transaction.createFunctionCallTransaction(from, nonce, gasPrice, gasLimit, contractAddress, value, encodedFunction);
		EthSendTransaction ethSendTransaction = UtilService.getWeb3j().ethSendTransaction(transaction).sendAsync().get();
		return ethSendTransaction.getTransactionHash();
	}

	/**
	 * 获取转币事件日志
	 * 
	 * @author bingo
	 * @date 2018年3月27日 上午11:08:59
	 */
	public static String getTransferEvent(Log log) {
		Event transferEvent = new Event("Transfer", 
				Arrays.asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}), 
				Arrays.asList(new TypeReference<Uint256>() {}));
		List<String> topics = log.getTopics();
		// Event签名
		String eventSignature = EventEncoder.encode(transferEvent);
		if (topics.get(0).equals(eventSignature)) {
			// 发送方地址
			String formAddress = new Address(topics.get(1)).getValue();
			// 接收方地址
			String toAddress = new Address(topics.get(2)).getValue();
			// 转账数量
			BigInteger value = (BigInteger) FunctionReturnDecoder.decode(log.getData(), transferEvent.getNonIndexedParameters()).get(0).getValue();
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name", eventSignature);
			jsonObject.put("fromAddress", formAddress);
			jsonObject.put("toAddress", toAddress);
			jsonObject.put("value", value);
			return jsonObject.toString();
		}
		return null;
	}
	
}
