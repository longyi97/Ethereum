package com.ruiec.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruiec.schedule.SwitchGeth;
import com.ruiec.service.FindTokenTxService;
import com.ruiec.utils.PropertiesUtils;

/**
 * 初始化配置、开启线程
 * 
 * @author bingo
 * @date 2018年3月29日 下午2:05:48
 */
@WebServlet(value = "/InitServlet", loadOnStartup = 1)
public class InitServlet extends HttpServlet {

	private static final long serialVersionUID = -6432362863224438705L;

	protected static final Logger LOGGER = LoggerFactory.getLogger(InitServlet.class);

	public void init() throws ServletException {

		// 开启搜索代币交易记录线程
		String contractAddresses = PropertiesUtils.getProperty("contractAddress", "");
		if (!contractAddresses.equals("")) {
			LOGGER.info("启动搜索代币交易记录线程>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			new Thread(new FindTokenTxService(contractAddresses)).start();
		}

		// 开启切换以太坊客户端线程
		int switchGethTimes = Integer.valueOf(PropertiesUtils.getProperty("switchGeth.times", "-1"));
		if (switchGethTimes > 0) {
			LOGGER.info("启动切换以太坊客户端线程 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			new SwitchGeth().switchGeth(switchGethTimes);
		}
	}
}
