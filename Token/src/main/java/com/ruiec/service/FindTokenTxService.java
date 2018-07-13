package com.ruiec.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.ConnectException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock.TransactionResult;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.ruiec.utils.EthUtils;
import com.ruiec.utils.JdbcUtil;
import com.ruiec.utils.PropertiesUtils;

import net.sf.json.JSONObject;

/**
 * 搜索代币交易服务
 * 
 * @author bingo
 * @date 2018年3月26日 下午5:32:27
 */
public class FindTokenTxService extends UtilService implements Runnable {

	protected static final Logger LOGGER = LoggerFactory.getLogger(FindTokenTxService.class);
	
	/** 智能合约地址（代币） */
	private String contractAddresses;

	/**
	 * 根据智能合约地址构造搜索代币交易服务
	 * 
	 * @param contractAddress
	 *            智能合约地址
	 * @author bingo
	 * @date 2018年4月2日 下午7:06:27
	 */
	public FindTokenTxService(String contractAddresses) {
		this.contractAddresses = contractAddresses;
	}

	/**
	 * 搜索代币交易记录
	 * 
	 * @author bingo
	 * @date 2018年3月27日 下午3:59:28
	 */
	@Override
	public void run() {
		// 当前搜索的区块号
		BigInteger currentNumber = new BigInteger(PropertiesUtils.getProperty("findTokenTx.startBlockNumber", "1"));
		while (true) {
			try {
				// 当前节点区块号
				BigInteger blockNumber = UtilService.getWeb3j().ethBlockNumber().send().getBlockNumber();
				// 当前搜索的区块号小于当前节点区块号时开始搜索
				if (blockNumber.compareTo(currentNumber) != 1) {
					Thread.sleep(Integer.parseInt(PropertiesUtils.getProperty("findTokenTx.sleep", "5000")));
					continue;
				}
				// 根据区块号搜索代币交易记录
				searchTxByBlockNumber(currentNumber);
				currentNumber = currentNumber.add(BigInteger.ONE);
				// 记录当前查询区块数
				try {
					PropertiesUtils.updateProperty("findTokenTx.startBlockNumber", currentNumber.toString());
				} catch (Exception e) {
					LOGGER.error("记录当前查询区块数操作异常", e);
				}
			} catch (Exception e) {
				if (e.getCause() instanceof ConnectException) {
					LOGGER.error("客户端连接异常，请稍后重试 [{}]", e.getMessage());
				} else {
					LOGGER.error("搜索代币交易记录出现异常", e);
				}
				try {
					Thread.sleep(Integer.parseInt(PropertiesUtils.getProperty("findTokenTx.sleep", "5000")));
				} catch (Exception e1) {
					LOGGER.error("sleep Exception", e1);
				}
			}
		}
	}
	
	/**
	 * 根据区块号搜索代币交易记录
	 * 
	 * @author bingo
	 * @date 2018年4月16日 下午3:14:18
	 */
	public void searchTxByBlockNumber(BigInteger blockNumber) throws Exception {
		LOGGER.info("当前查找区块号：{}", blockNumber);
		List<TransactionResult> transactionResults = UtilService.getWeb3j().ethGetBlockByNumber(DefaultBlockParameter.valueOf(blockNumber), true).send().getResult().getTransactions();
		// 当前区块交易数为零时跳过
		if (transactionResults.size() == 0) {
			return;
		}
		LOGGER.info("开始搜索-------------------------------------");
		// 当前钱包所有账户地址
		List<String> accounts = UtilService.getWeb3j().ethAccounts().send().getAccounts();
		for (TransactionResult<Transaction> transactionResult : transactionResults) {
			Transaction transaction = transactionResult.get();
			if (null == transaction.getTo()) {
				continue;
			}
			// 交易是否为调用代币智能合约
			for (String contractAddress : contractAddresses.split(",")) {
				if (transaction.getTo().equalsIgnoreCase(contractAddress)) {
					// 是否为调用transfer方法
					List<Log> logs = UtilService.getWeb3j().ethGetTransactionReceipt(transaction.getHash()).send().getResult().getLogs();
					for (Log log : logs) {
						JSONObject jsonTransferEvent = JSONObject.fromObject(ERC20TokenService.getTransferEvent(log));
						if (!jsonTransferEvent.isNullObject()) {
							String fromAddress = jsonTransferEvent.optString("fromAddress");
							String toAddress = jsonTransferEvent.optString("toAddress");
							for (String account : accounts) {
								// transfer发送方（from）比对
								// transfer接收方（to）比对
								if (account.equals(fromAddress) || account.equals(toAddress)) {
									// 转账类型：0为充值；1为提现；2为转账
									int type = -1;
									if (account.equals(fromAddress)) {
										type = 0;
										for (String a : accounts) {
											if (a.equals(toAddress)) {
												type = 2;
											}
										}
									} else {
										type = 1;
										for (String a : accounts) {
											if (a.equals(fromAddress)) {
												type = 2;
											}
										}
									}
									BigDecimal value = new BigDecimal(jsonTransferEvent.optString("value")).divide(BigDecimal.valueOf(1000000000000000000L));
									String txInfo = EthUtils.getTransaction(UtilService.getWeb3j(), transaction.getHash());
									// 插入记录
									insertRecord(contractAddress, transaction, fromAddress, toAddress, value, txInfo, type);
									break;
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * 插入记录
	 * 
	 * @author bingo
	 * @date 2018年3月27日 下午7:21:31
	 */
	private void insertRecord(String contractAddress, Transaction transaction, String fromAddress, String toAddress, BigDecimal value, String txInfo, Integer type) throws Exception {
		String sql = "insert into ruiec_token_transfer_record (create_time,modify_time,contract_address,tx_hash,from_address,to_address,value,timestamp,tx_info,type) values(?,?,?,?,?,?,?,?,?,?)";
		List<Object> list = new ArrayList<Object>();
		list.add(new Date());
		list.add(new Date());
		list.add(contractAddress);
		list.add(transaction.getHash());
		list.add(fromAddress);
		list.add(toAddress);
		list.add(value);
		BigInteger timestamp = UtilService.getWeb3j().ethGetBlockByHash(transaction.getBlockHash(), false).send().getResult().getTimestamp();
		list.add(new Date(timestamp.longValue() * 1000));
		list.add(txInfo);
		list.add(type);
		JdbcUtil jdbcUtil = new JdbcUtil();
		Connection conn = jdbcUtil.getConnection();
		try {
			jdbcUtil.updateByPreparedStatement(conn, sql, list);
		} catch (Exception e) {
			if (e instanceof MySQLIntegrityConstraintViolationException){
				LOGGER.error("交易记录已存在，txHash:{}", transaction.getHash());
			} else {
				LOGGER.error("插入代币转账记录出现异常", e);
			}
		} finally {
			jdbcUtil.releaseConn(conn);
		}
	}
	
}
