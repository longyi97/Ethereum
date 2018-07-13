package com.ruiec.service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.ConnectException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSyncing.Result;

import com.ruiec.sms.YPWSendMsgService;
import com.ruiec.util.PropertiesUtils;

import net.sf.json.JSONObject;

/**
 * 守护服务
 *
 * @author bingo
 * @date 2018年4月17日 下午1:44:27
 */
public class DeamonService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeamonService.class);

	private static Web3j web3j = UtilService.web3j;

	/** 当前区块号 */
	private static volatile BigInteger currentBlock = BigInteger.ZERO;
	/** 区块高度 */
	private static volatile BigInteger highestBlock = BigInteger.ZERO;

	/** 客户端失去联接连续次数 */
	private static volatile int disconnConsTimes = 0;
	/** 客户端同步区块异常连续次数 */
	private static volatile int syncErrorConsTimes = 0;
	/** 客户端同步区块异常连续次数 */
	private static volatile int syncErrorConsTimes2 = 0;

	/**
	 * 启动Geth
	 *
	 * @param filePath
	 *            bat file location
	 * @author bingo
	 * @date 2018年4月17日 下午2:39:39
	 */
	public static void restartGeth(String filePath) {
		LOGGER.info("开始执行命令：{}", filePath);
		Process process = null;
		try {
			process = Runtime.getRuntime().exec("CMD /C start " + filePath);
			process.waitFor();
			LOGGER.info("执行完毕");
			Thread.sleep(Long.valueOf(PropertiesUtils.getProperty("startGeth.sleep")));
			Runtime.getRuntime().exec("TASKKILL /IM cmd.exe /F");
			LOGGER.info("关闭其它CMD窗口成功");
		} catch (Exception e) {
			LOGGER.error("启动Geth操作异常", e);
			throw new RuntimeException(e);
		} finally {
			if (null != process) {
				process.destroy();
			}
		}
	}

	/**
	 * Geth客户端是否连接
	 *
	 * @author bingo
	 * @date 2018年4月17日 下午2:59:08
	 */
	public static boolean isConnected() {
		try {
			web3j.ethBlockNumber().send();
			return true;
		} catch (IOException e) {
			if (e.getCause() instanceof ConnectException) {
				return false;
			} else {
				LOGGER.error("客户端连接异常", e);
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 保持以太坊区块同步服务
	 *
	 * @author bingo
	 * @date 2018年4月19日 下午1:45:14
	 */
	public static void deamonGeth(String filePath) {
		try {
			// 1. 判断当前区块是否改变
			BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
			if (blockNumber.compareTo(BigInteger.ZERO) <= 0) {
				return;
			}
			if (blockNumber.compareTo(currentBlock) <= 0) {
				LOGGER.error("当前区块[{} -> {}]未更新，即将重启Geth客户端", currentBlock, blockNumber);
				restartGeth(filePath);
				// 更新currentBlock
				new Thread(DeamonService::updateCurrentBlock).start();
				// 客户端失去联接连续次数，超过则发送提醒，大于0为开启
				int consTimes = Integer.valueOf(PropertiesUtils.getCurrentProperty("times.syncingError", "-1"));
				if (consTimes > 0 && consTimes < ++syncErrorConsTimes) {
					sendSMS(PropertiesUtils.getProperty("msg.syncingError"));
				}
				return;
			} else {
				currentBlock = blockNumber;
				syncErrorConsTimes = 0;
			}

			// 2. 判断最高区块是否改变
			Result result = web3j.ethSyncing().send().getResult();
			JSONObject jsonObject = JSONObject.fromObject(result);
			if (!result.isSyncing()) {
				return;
			}
			String highestBlockNumberString = jsonObject.optString("highestBlock");
			BigInteger highestBlockNumber = BigInteger.valueOf(Long.parseLong(highestBlockNumberString.substring(2), 16));
			if (highestBlockNumber.compareTo(highestBlock) <= 0) {
				LOGGER.error("区块高度[{} -> {}]未更新，即将重启Geth客户端", highestBlock, highestBlockNumber);
				restartGeth(filePath);
				// 更新highestBlock
				new Thread(DeamonService::updateHighestBlock).start();
				// 客户端失去联接连续次数，超过则发送提醒，大于0为开启
				int consTimes = Integer.valueOf(PropertiesUtils.getCurrentProperty("times.syncingError", "-1"));
				if (consTimes > 0 && consTimes < ++syncErrorConsTimes2) {
					sendSMS(PropertiesUtils.getProperty("msg.syncingError"));
				}
				return;
			} else {
				highestBlock = highestBlockNumber;
				syncErrorConsTimes2 = 0;
			}

			// 3. 其它判断

		} catch (Exception e) {
			if (e.getCause() instanceof ConnectException) {
				LOGGER.error("客户端连接异常，请稍后重试 [{}]", e.getMessage());
			} else {
				LOGGER.error("保持以太坊区块同步服务异常", e);
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 保持以太坊区块同步服务
	 *
	 * @author bingo
	 * @date 2018年4月19日 下午1:55:06
	 */
	public static void keepConn(String filePath) {
		Long sleep = Long.valueOf(PropertiesUtils.getCurrentProperty("isConnected.sleep", "10000"));
		int times = Integer.valueOf(PropertiesUtils.getProperty("isConnected.times", "6"));
		try {
			// n秒 * m次连接不上Geth则视为Geth进程关闭
			for (int i = 0; i < times; i++) {
				if (isConnected()) {
					disconnConsTimes = 0;
					return;
				}
				LOGGER.error("客户端失去连接，重试第{}次", i);
				Thread.sleep(sleep);
			}
			restartGeth(filePath);
			// 客户端失去联接连续次数，超过则发送提醒，大于0为开启
			int consTimes = Integer.valueOf(PropertiesUtils.getProperty("times.disconnected", "-1"));
			if (consTimes > 0 && consTimes < ++disconnConsTimes) {
				sendSMS(PropertiesUtils.getProperty("msg.disconnected"));
			}
		} catch (Exception e) {
			LOGGER.error("保持以太坊连接服务异常", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * 更新currentBlock
	 * 
	 * @author bingo
	 * @date 2018年06月06日 下午04:33:17
	 */
	private static void updateCurrentBlock() {
		BigInteger blockNumber = BigInteger.ZERO;
		do {
			try {
				// 1.update currentBlock number
				blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
				currentBlock = blockNumber.compareTo(currentBlock) > 0 ? blockNumber : currentBlock;
				LOGGER.info("当前区块更新成功 {}", currentBlock);
			} catch (IOException e) {
				// sleep 5s if get blockNumber error
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					throw new RuntimeException(e1);
				}
			}
		} while (blockNumber.compareTo(BigInteger.ZERO) == 0);
	}

	/**
	 * 更新highestBlock
	 * 
	 * @author bingo
	 * @date 2018年06月06日 下午04:33:38
	 */
	private static void updateHighestBlock() {
		BigInteger highestBlockNumber = BigInteger.ZERO;
		int circleTimes = 0;
		// 10s * 18 = 3min 内获取highestBlock失败循环
		do {
			try {
				// get highestBlock from eth.syncing
				Result result = web3j.ethSyncing().send().getResult();
				JSONObject jsonObject = JSONObject.fromObject(result);
				if (!result.isSyncing()) {
					Thread.sleep(10000);
					continue;
				}
				String highestBlockNumberString = jsonObject.optString("highestBlock");
				highestBlockNumber = BigInteger.valueOf(Long.parseLong(highestBlockNumberString.substring(2), 16));
				highestBlock = highestBlockNumber.compareTo(highestBlock) > 0 ? highestBlockNumber : highestBlock;
				LOGGER.info("区块高度更新成功 {}", highestBlockNumber);
			} catch (Exception e) {
				// sleep 5s if get highestBlock error
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
					throw new RuntimeException(e1);
				}
			}
		} while (highestBlockNumber.compareTo(BigInteger.ZERO) == 0 && circleTimes++ < 18);
	}

	/**
	 * 发送短信
	 *
	 * @author bingo
	 * @date 2018年4月19日 下午2:06:45
	 */
	private static void sendSMS(String msg) {
		Map<String, String> map = YPWSendMsgService.sendMsg(PropertiesUtils.getCurrentProperty("error.MobilePhone"),PropertiesUtils.getProperty("msg.name") + msg);
		LOGGER.info("code:{}, mag:{}", map.get("code"), map.get("msg"));
	}
}