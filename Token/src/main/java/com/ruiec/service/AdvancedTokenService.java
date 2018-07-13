package com.ruiec.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

/**
 * ERC20代币高级功能服务
 *
 * @author bingo
 * @date 2018年3月16日 下午2:33:55
 */
public class AdvancedTokenService extends ERC20TokenService {
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(AdvancedTokenService.class);
	
	/** solidity编译后的文件 */
	private static final String FILEPATH = System.getProperty("user.dir") + File.separatorChar + "src" + File.separatorChar + "AdvancedToken_sol_MyAdvancedToken.bin";

	/**
	 * 新建带高级功能的ERC20代币
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
	 * @date 2018年3月16日 下午2:46:12
	 */
	public String newAdvancedToken(String filePath, String address, String password, BigInteger initialSupply, String name, String symbol) {
		LOGGER.info("newAdvancedToken ------------------------------------------------------------");
		EthSendTransaction transactionResponse;
		try {
			// 解锁账户
			if (unlockAccount(address, password)) {
				// solidity构造函数字节码
				String encodeConstructor = FunctionEncoder.encodeConstructor(Arrays.asList(new Uint256(initialSupply), new Utf8String(name), new Utf8String(symbol)));
				// solidity编译后字节码
				String solidityBinary = getSolidityBinary(filePath);
				BigInteger nonce = getNonce(address);
				Transaction transaction = Transaction.createContractTransaction(address, nonce, GASPRICE, GASLIMIT, BigInteger.valueOf(0), solidityBinary + encodeConstructor);
				transactionResponse = UtilService.getWeb3j().ethSendTransaction(transaction).sendAsync().get();
				LOGGER.info("TransactionHash: {}", transactionResponse.getTransactionHash());
				return transactionResponse.getTransactionHash();
			}
		} catch (Exception e) {
			LOGGER.error("Exception", e);
		}
		return null;
	}

	/**
	 * 增发代币
	 *
	 * @param from
	 *            发送方账户地址
	 * @param password
	 *            账户密码
	 * @param contractAddress
	 *            智能合约地址
	 * @param targetAddress
	 *            接收方账户地址
	 * @param mintedAmount
	 *            增发数量
	 * @author bingo
	 * @date 2018年3月16日 下午3:13:11
	 */
	public String mintToken(String from, String password, String contractAddress, String targetAddress, BigInteger mintedAmount) {
		LOGGER.info("mintToken ------------------------------------------------------------");
		try {
			if (unlockAccount(from, password)) {
				Function function = new Function("mintToken",
						Arrays.asList(new Address(targetAddress), new Uint256(mintedAmount)),
						Collections.singletonList(new TypeReference<Bool>() {}));
				String encodedFunction = FunctionEncoder.encode(function);
				String transactionHash = execute(from, contractAddress, encodedFunction);
				LOGGER.info("TransactionHash: {}", transactionHash);
				return transactionHash;
			}
		} catch (Exception e) {
			LOGGER.error("Exception", e);
		}
		return null;
	}

	/**
	 * 冻结账户
	 *
	 * @param from
	 *            发送方账户地址
	 * @param password
	 *            账户密码
	 * @param contractAddress
	 *            智能合约地址
	 * @param targetAddress
	 *            接收方账户地址
	 * @param freeze
	 *            冻结状态
	 * @author bingo
	 * @date 2018年3月16日 下午3:13:11
	 */
	public String freezeAccount(String from, String password, String contractAddress, String targetAddress, Boolean freeze) {
		LOGGER.info("freezeAccount ------------------------------------------------------------");
		try {
			if (unlockAccount(from, password)) {
				Function function = new Function("freezeAccount",
						Arrays.asList(new Address(targetAddress), new Bool(freeze)),
						Collections.singletonList(new TypeReference<Bool>() {}));
				String encodedFunction = FunctionEncoder.encode(function);
				String transactionHash = execute(from, contractAddress, encodedFunction);
				LOGGER.info("TransactionHash: {}", transactionHash);
				return transactionHash;
			}
		} catch (Exception e) {
			LOGGER.error("Exception", e);
		}
		return null;
	}

	/**
	 * 查询代币账户冻结状态
	 *
	 * @param ownerAddress
	 *            账户地址
	 * @param contractAddress
	 *            智能合约账户地址
	 * @author bingo
	 * @date 2018年3月16日 下午7:11:35
	 */
	public Boolean frozenAccountOf(String ownerAddress, String contractAddress) {
		LOGGER.info("frozenAccountOf: {} ------------------------------------------------------------", ownerAddress);
		try {
			Function function = new Function("frozenAccount",
					Collections.singletonList(new Address(ownerAddress)),
					Collections.singletonList(new TypeReference<Bool>() {}));
			String responseValue = callSmartContractFunction(function, contractAddress);
			Boolean frozen = (Boolean) FunctionReturnDecoder.decode(responseValue, function.getOutputParameters()).get(0).getValue();
			LOGGER.info("frozen: {}", frozen.toString());
			return frozen;
		} catch (Exception e) {
			LOGGER.error("Exception", e);
		}
		return null;
	}

	/**
	 * 查询代币属性(数字)
	 *
	 * @author bingo
	 * @date 2018年05月19日 下午01:53:16
	 */
	public Object getUintProp(String contractAddress, String name) {
		LOGGER.info("getUintProp ------------------------------------------------------------");
		try {
			Function function = new Function(name,
					Collections.emptyList(),
					Collections.singletonList(new TypeReference<Uint>() {}));
			String responseValue = callSmartContractFunction(function, contractAddress);
			Object result = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters()).get(0).getValue();
			LOGGER.info("{}: {}", name, result);
			return result;
		} catch (Exception e) {
			LOGGER.error("getUintProp Exception", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * 查询代币属性(字符串)
	 *
	 * @author bingo
	 * @date 2018年05月19日 下午01:53:16
	 */
	public Object getStringProp(String contractAddress, String name) {
		LOGGER.info("getUintProp ------------------------------------------------------------");
		try {
			Function function = new Function(name,
					Collections.emptyList(),
					Collections.singletonList(new TypeReference<Utf8String>() {}));
			String responseValue = callSmartContractFunction(function, contractAddress);
			Object result = FunctionReturnDecoder.decode(responseValue, function.getOutputParameters()).get(0).getValue();
			LOGGER.info("{}: {}", name, result);
			return result;
		} catch (Exception e) {
			LOGGER.error("getStringProp Exception", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * 设置代币买卖价格
	 *
	 * @author bingo
	 * @date 2018年05月19日 下午03:36:51
	 */
	public String setPrices(String from, String password, String contractAddress, BigInteger newSellPrice, BigInteger newBuyPrice) {
		LOGGER.info("setPrice ------------------------------------------------------------");
		try {
			if (unlockAccount(from, password)) {
				Function function = new Function("setPrices",
						Arrays.asList(new Uint256(newSellPrice), new Uint256(newBuyPrice)),
						Collections.emptyList());
				String encodedFunction = FunctionEncoder.encode(function);
				String transactionHash = execute(from, contractAddress, encodedFunction);
				LOGGER.info("TransactionHash: {}", transactionHash);
				return transactionHash;
			}
		} catch (Exception e) {
			LOGGER.error("Exception", e);
		}
		return null;
	}

	/**
	 * 买入代币
	 *
	 * @author bingo
	 * @date 2018年05月19日 下午03:36:51
	 */
	public String buy(String from, String password, String contractAddress, BigInteger value) {
		LOGGER.info("buy ------------------------------------------------------------");
		try {
			if (unlockAccount(from, password)) {
				Function function = new Function("buy",
						Collections.emptyList(),
						Collections.emptyList());
				String encodedFunction = FunctionEncoder.encode(function);
				String transactionHash = execute(from, contractAddress, encodedFunction, null, null, null, value);
				LOGGER.info("TransactionHash: {}", transactionHash);
				return transactionHash;
			}
		} catch (Exception e) {
			LOGGER.error("buy Exception", e);
		}
		return null;
	}

	/**
	 * 卖出代币
	 *
	 * @author bingo
	 * @date 2018年05月19日 下午03:36:51
	 */
	public String sell(String from, String password, String contractAddress, BigInteger value) {
		LOGGER.info("sell ------------------------------------------------------------");
		try {
			if (unlockAccount(from, password)) {
				Function function = new Function("sell",
						Arrays.asList(new Uint256(value)),
						Collections.emptyList());
				String encodedFunction = FunctionEncoder.encode(function);
				String transactionHash = execute(from, contractAddress, encodedFunction, null, null);
				LOGGER.info("TransactionHash: {}", transactionHash);
				return transactionHash;
			}
		} catch (Exception e) {
			LOGGER.error("sell Exception", e);
		}
		return null;
	}

	/**
	 * 设置代币自动充值gas阀值
	 *
	 * @author bingo
	 * @date 2018年05月21日 下午03:20:54
	 */
	public String setMinBalance(String from, String password, String contractAddress, BigInteger minBalance) {
		LOGGER.info("setMinBalance ------------------------------------------------------------");
		try {
			if (unlockAccount(from, password)) {
				Function function = new Function("setMinBalance",
						Arrays.asList(new Uint256(minBalance)),
						Collections.emptyList());
				String encodedFunction = FunctionEncoder.encode(function);
				String transactionHash = execute(from, contractAddress, encodedFunction);
				LOGGER.info("TransactionHash: {}", transactionHash);
				return transactionHash;
			}
		} catch (Exception e) {
			LOGGER.error("setMinBalance Exception", e);
		}
		return null;
	}

}
