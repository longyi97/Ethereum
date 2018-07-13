package com.ruiec.service;

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
import org.web3j.protocol.core.methods.response.Transaction;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.ruiec.utils.EthUtils;
import com.ruiec.utils.JdbcUtil;
import com.ruiec.utils.PropertiesUtils;

/**
 * 搜索交易记录
 * 
 * @author bingo
 * @date 2018年4月2日 上午10:28:56
 */
public class GetEthTransactionService extends Thread {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GetEthTransactionService.class);
	
	@Override
	public void run() {
		// 开始查询的区块号
		BigInteger selectBlockNumber = new BigInteger(PropertiesUtils.getProperty("startBlockNumber", "1"));
		while (true) {
			try {
				// 目前同步到的最新区块号
				BigInteger blockNumber = UtilService.getWeb3j().ethBlockNumber().send().getBlockNumber();
				// 当要查询的区块号小于最新区块号时，开始查询
				if (blockNumber.compareTo(selectBlockNumber) != 1) {
					sleep(Integer.parseInt(PropertiesUtils.getProperty("getEthTransaction.sleep", "5000")));
					continue;
				}
				// 根据区块号搜索交易记录
				searchTxByBlockNumber(selectBlockNumber);
				selectBlockNumber = selectBlockNumber.add(BigInteger.ONE);
				// 记录当前查询区块数
				try {
					PropertiesUtils.updateProperty("startBlockNumber", selectBlockNumber.toString());
				} catch (Exception e) {
					LOGGER.error("记录当前查询区块数操作异常", e);
				}
			} catch (Throwable e) {
				if (e.getCause() instanceof ConnectException) {
					LOGGER.error("客户端连接异常，请稍后重试 [{}]", e.getMessage());
				} else {
					LOGGER.error("搜索交易记录出现异常", e);
				}
				try {
					sleep(Integer.parseInt(PropertiesUtils.getProperty("getEthTransaction.sleep", "5000")));
				} catch (Exception e1) {
					LOGGER.error("sleep Exception", e1);
				}
			}
		}
	}
	
	/**
	 * 根据区块号搜索交易记录
	 * 
	 * @author bingo
	 * @date 2018年4月2日 上午10:25:11
	 */
	public void searchTxByBlockNumber(BigInteger blockNumber) throws Exception {
		LOGGER.info("当前查找区块号：{}", blockNumber);
		List<TransactionResult> transactions = UtilService.getWeb3j().ethGetBlockByNumber(DefaultBlockParameter.valueOf(blockNumber), true).send().getResult().getTransactions();
		// 当前区块交易数为零时跳过
		if (transactions.size() == 0) {
			return;
		}
		LOGGER.info("开始搜索-------------------------------------");
		// 当前钱包所有账户地址
		List<String> accounts = UtilService.getWeb3j().ethAccounts().send().getAccounts();
		// 比对交易地址是否为当前钱包账户地址
		for (TransactionResult<Transaction> tx : transactions) {
			Transaction transaction = tx.get();
			if (null == transaction.getTo()) {
				continue;
			}
			for (String account : accounts) {
				if (account.equals(transaction.getTo())) {
					String txInfo = EthUtils.getTransaction(UtilService.getWeb3j(), transaction.getHash());
					insertRecord(transaction, txInfo);
				}
			}
		}
	}
	
	/**
	 * 插入记录
	 * 
	 * @author bingo
	 * @date 2018年4月2日 上午10:10:03
	 */
	private void insertRecord(Transaction transaction, String txInfo) {
		String sql = "insert into ruiec_eth_transaction_record (create_time,modify_time,wallet_address,hash_address,transaction_json,type) values(?,?,?,?,?,0)";
		List<Object> list = new ArrayList<Object>();
		list.add(new Date());
		list.add(new Date());
		list.add(transaction.getTo());
		list.add(transaction.getHash());
		list.add(txInfo);
		JdbcUtil jdbcUtil = new JdbcUtil();
		Connection conn = jdbcUtil.getConnection();
		try {
			jdbcUtil.updateByPreparedStatement(conn, sql, list);
		} catch (Exception e1) {
			if (e1 instanceof MySQLIntegrityConstraintViolationException){
				LOGGER.error("交易记录已存在，txHash:{}", transaction.getHash());
			} else {
				LOGGER.error("插入交易记录出现异常", e1);
			}
			// 此处出现的问题为插入相同数据（重启服务重复搜索），暂时吞了异常
//			throw new RuntimeException(e1);
		} finally {
			jdbcUtil.releaseConn(conn);
		}
	}

}
