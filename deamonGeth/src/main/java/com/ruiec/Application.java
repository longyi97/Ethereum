package com.ruiec;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruiec.service.DeamonService;
import com.ruiec.util.PropertiesUtils;

/**
 * 主程序
 * 
 * @author bingo
 * @date 2018年4月17日 下午4:16:19
 */
public class Application {

	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

	/**
	 * 保持Geth客户端连接线程
	 * 
	 * @author bingo
	 * @date 2018年4月17日 下午4:57:23
	 */
	class KeepConn extends Thread {

		@Override
		public void run() {
			LOGGER.info("保持Geth客户端连接线程开启 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			try {
				DeamonService.keepConn(PropertiesUtils.getProperty("bat.restartGeth"));
			} catch (Exception e) {
				LOGGER.error("保持Geth客户端连接线程异常", e);
			}
		}
		
	}
	
	/**
	 * 保持Geth客户端同步区块线程
	 * 
	 * @author bingo
	 * @date 2018年4月17日 下午5:14:02
	 */
	class KeepSyncing extends Thread {
		
		@Override
		public void run() {
			LOGGER.info("保持Geth客户端同步区块线程开启 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			try {
				DeamonService.deamonGeth(PropertiesUtils.getProperty("bat.restartGeth"));
			} catch (Exception e) {
				LOGGER.error("保持Geth客户端同步区块线程异常", e);
			}
		}
		
	}
	
	/**
	 * 主程序入口
	 * 
	 * @author bingo
	 * @date 2018年4月18日 下午2:12:42
	 */
	public static void main(String[] args) {
		Application application = new Application();
		long keepConnDelay = Long.valueOf(PropertiesUtils.getProperty("Thread.KeepConn.duration", "10"));
		long keepSyncingDelay = Long.valueOf(PropertiesUtils.getProperty("Thread.KeepSyncing.duration", "60"));
		ScheduledExecutorService schedule = Executors.newScheduledThreadPool(3);
		// 保持Geth客户端连接定时任务
		schedule.scheduleWithFixedDelay(application.new KeepConn(), 0, keepConnDelay, TimeUnit.MINUTES);
		// 保持Geth客户端同步区块定时任务
		schedule.scheduleWithFixedDelay(application.new KeepSyncing(), 0, keepSyncingDelay, TimeUnit.MINUTES);
	}
	
}
