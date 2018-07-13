package com.ruiec.schedule;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruiec.service.UtilService;

/**
 * 切换Geth客户端定时任务
 * 
 * @author bingo
 * @date 2018年4月27日 下午3:19:11
 */
public class SwitchGeth {

	private static final Logger LOGGER = LoggerFactory.getLogger(SwitchGeth.class);

	/** Geth连接异常次数 */
	private int failedTimes = 0;

	/**
	 * 切换Geth客户端
	 * 
	 * @param times
	 *            次数，异常累计次数达times切换Geth客户端
	 * @author bingo
	 * @date 2018年4月27日 下午3:54:43
	 */
	public void switchGeth(Integer times) {
		ScheduledExecutorService schedule = Executors.newSingleThreadScheduledExecutor();
		schedule.scheduleWithFixedDelay(new SwitchThread(times), 0, 1, TimeUnit.MINUTES);

	}

	/**
	 * 切换Geth客户端线程
	 * 
	 * @author bingo
	 * @date 2018年4月27日 下午3:51:10
	 */
	class SwitchThread extends Thread {

		/** 切换累计次数 */
		private int times;

		public SwitchThread(Integer times) {
			this.times = times;
		}

		@Override
		public void run() {
			if (!UtilService.gethStatus()) {
				if (++failedTimes > times) {
					LOGGER.info("以太坊客户端异常，切换连接客户端");
					UtilService.nextGethClient();
					failedTimes = 0;
				} else {
					LOGGER.info("以太坊客户端异常，连续累计次数：{}", failedTimes);
				}
			}
		}

	}
}
