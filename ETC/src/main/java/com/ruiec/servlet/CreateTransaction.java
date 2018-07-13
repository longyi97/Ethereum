package com.ruiec.servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.ConnectException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.core.methods.request.Transaction;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.ruiec.service.SecurityService;
import com.ruiec.service.UtilService;
import com.ruiec.utils.JdbcUtil;
import com.ruiec.utils.PropertiesUtils;
import com.ruiec.utils.ResponseJSONObject;

import net.sf.json.JSONObject;

/**
 * 创建转账交易servlet
 * 
 * @author bingo
 * @date 2018年4月2日 下午2:36:11
 */
@WebServlet("/CreateTransaction")
public class CreateTransaction extends HttpServlet {

	private static final long serialVersionUID = 7952369350069116346L;

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateTransaction.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		String accountId = request.getParameter("accountId");
		String password = request.getParameter("password");
		String toAccountId = request.getParameter("toAccountId");
		String amountString = request.getParameter("amount");

		BigDecimal amountDecimal = new BigDecimal(amountString);
		BigDecimal amountWei = amountDecimal.multiply(new BigDecimal("1000000000000000000"));
		BigInteger amount = amountWei.toBigInteger();

		try {
			if (UtilService.unlockAccount(accountId, password)) {
				JSONObject jsonData = new JSONObject();
				Transaction transaction = Transaction.createEtherTransaction(accountId, null, UtilService.GASPRICE,BigInteger.valueOf(21000L), toAccountId, amount);
				String txHash = UtilService.getWeb3j().ethSendTransaction(transaction).send().getResult();
				if (null != txHash) {
					jsonData.put("txHash", txHash);
					LOGGER.info("转账交易成功，账户:[{}]转账到账户:[{}],资金amount:[{}],txHash:[{}]", accountId, toAccountId, amountString, txHash);
					response.getWriter().println(ResponseJSONObject.ResponseJSON(200, true, "转账交易成功", jsonData));
					// 插入交易记录
					insertRecord(txHash, accountId, toAccountId, amountDecimal, request.getRemoteAddr());
					// 每日限额
					BigDecimal dailyAmountLimit = new BigDecimal(PropertiesUtils.getProperty("dailyAmountLimit", "-1"));
					// dailyAmountLimit配置的值大于0生效
					if (dailyAmountLimit.compareTo(BigDecimal.ZERO) > 0) {
						SecurityService securityService = new SecurityService();
						securityService.dailyAmountLimit(dailyAmountLimit);
					}
				} else {
					LOGGER.info("转账交易失败，账户:[{}]转账到账户:[{}],资金amount:[{}]", accountId, toAccountId, amountString);
					response.getWriter().println(ResponseJSONObject.ResponseJSON(400, false, "转账交易失败", null));
				}
			}
		} catch (Exception e) {
			if (e.getCause() instanceof ConnectException) {
				LOGGER.error("客户端连接异常，请稍后重试 [{}]", e.getMessage());
				response.getWriter().println(ResponseJSONObject.ResponseJSON(501, false, "客户端连接异常，请稍后重试", null));
				return;
			}
			LOGGER.error("转账交易异常", e);
			response.getWriter().println(ResponseJSONObject.ResponseJSON(500, false, "转账交易异常", null));
		}
	}

	/**
	 * 插入记录
	 * 
	 * @author bingo
	 * @date 2018年4月20日 下午4:38:06
	 */
	public void insertRecord(String txHash, String fromAddress, String toAddress, BigDecimal value, String ipAddress) {
		String sql = "insert into ruiec_etc_transaction_request (create_time, tx_hash, from_address, to_address, value, ip_address) values(?, ?, ?, ?, ?, ?)";
		List<Object> params = new ArrayList<Object>();
		params.add(new Date());
		params.add(txHash);
		params.add(fromAddress);
		params.add(toAddress);
		params.add(value);
		params.add(ipAddress);
		JdbcUtil jdbcUtil = new JdbcUtil();
		Connection connection = null;
		try {
			connection = jdbcUtil.getConnection();
			jdbcUtil.updateByPreparedStatement(connection, sql, params);
		} catch (Exception e) {
			if (e instanceof MySQLIntegrityConstraintViolationException){
				LOGGER.error("交易记录已存在，txHash:{}", txHash);
			} else {
				LOGGER.error("插入交易请求记录出现异常", e);
			}
		} finally {
			if (null != connection) {
				jdbcUtil.releaseConn(connection);
			}
		}
	}
	
}
