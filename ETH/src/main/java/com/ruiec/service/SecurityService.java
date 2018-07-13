package com.ruiec.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruiec.sms.YPWSendMsgService;
import com.ruiec.utils.JdbcUtil;
import com.ruiec.utils.PropertiesUtils;

/**
 * 安全服务
 * 
 * @author bingo
 * @date 2018年4月23日 下午7:01:21
 */
public class SecurityService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityService.class);
	
	/**
	 * 每日转账限额
	 * 
	 * @author bingo
	 * @date 2018年4月23日 下午8:15:56
	 */
	public Boolean dailyAmountLimit(BigDecimal amount) {
		try {
			BigDecimal todayAmount = findTodayAmount();
			if (todayAmount.compareTo(amount) > 0) {
				String msg = "服务器异常，原因[ 转账金额<"+ todayAmount +">达每日上限<"+ amount +"> ]，建议操作[ - ]";
				sendSMS(msg);
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("Exception", e);
		}
		return false;
	}
	
	/**
	 * 查询今日转账数量
	 * 
	 * @author bingo
	 * @date 2018年4月23日 下午8:16:18
	 */
	public BigDecimal findTodayAmount() {
		String sql = "SELECT SUM(`value`) amount FROM ruiec_eth_transaction_request WHERE TO_DAYS(create_time) = TO_DAYS(NOW())";
		JdbcUtil jdbcUtil = new JdbcUtil();
		Connection connection = null;
		BigDecimal todayAmount = BigDecimal.ZERO;
		try {
			connection = jdbcUtil.getConnection();
			List<Object> params = new ArrayList<Object>();
			List<Map<String,Object>> results = jdbcUtil.findResult(connection, sql, params);
			if (results.size() > 0) {
				Object object = results.get(0).get("amount");
				if (object instanceof BigDecimal) {
					todayAmount = (BigDecimal) results.get(0).get("amount");
				}
			}
		} catch (Exception e) {
			LOGGER.error("findTodayAmount Exception", e);
			throw new RuntimeException(e);
		}
		return todayAmount;
	}
	
	/**
	 * 发送短信
	 * 
	 * @author bingo
	 * @date 2018年4月19日 下午2:06:45
	 */
	public static void sendSMS(String msg) {
		Map<String, String> map = YPWSendMsgService.sendMsg(PropertiesUtils.getProperty("error.MobilePhone"),PropertiesUtils.getProperty("msg.name") + msg);
		LOGGER.info("code:{}, mag:{}", map.get("code"), map.get("msg"));
	}
}
