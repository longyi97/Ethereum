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

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.ruiec.service.ERC20TokenService;
import com.ruiec.service.SecurityService;
import com.ruiec.utils.JdbcUtil;
import com.ruiec.utils.PropertiesUtils;
import com.ruiec.utils.ResponseJSONObject;

import net.sf.json.JSONObject;

/**
 * 代币转账servlet
 * 
 * @author bingo
 * @date 2018年3月28日 上午9:33:36
 */
@WebServlet("/TokenTransferServlet")
public class TokenTransferServlet extends HttpServlet {

	private static final long serialVersionUID = -1163931291555908733L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TokenTransferServlet.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		LOGGER.info("代币转账请求 ------------------------------------------------------------");
		response.setCharacterEncoding("UTF-8");
		String fromAddress = request.getParameter("fromAddress");
		String toAddress = request.getParameter("toAddress");
		String password = request.getParameter("password");
		BigDecimal value = new BigDecimal(request.getParameter("value"));
		String token = request.getParameter("token");

		try {
			String txHash = transfer(fromAddress, password, toAddress, value, token);
			if (null != txHash) {
				LOGGER.info("转币请求成功，txHash: {}", txHash);
				JSONObject result = new JSONObject();
				result.put("txHash", txHash);
				response.getWriter().println(ResponseJSONObject.ResponseJSON(200, true, "转币请求成功", result));
				// 插入交易记录
				insertRecord(token, txHash, fromAddress, toAddress, value, request.getRemoteAddr());
				// 每日限额
				BigDecimal dailyAmountLimit = new BigDecimal(PropertiesUtils.getProperty("dailyAmountLimit", "-1"));
				// dailyAmountLimit配置的值大于0生效
				if (dailyAmountLimit.compareTo(BigDecimal.ZERO) > 0) {
					SecurityService securityService = new SecurityService();
					securityService.dailyAmountLimit(dailyAmountLimit, token);
				}
			} else {
				LOGGER.info("转币请求失败");
				response.getWriter().println(ResponseJSONObject.ResponseJSON(400, false, "转币请求失败", null));
			}
		} catch (Exception e) {
			if (e.getCause() instanceof ConnectException) {
				LOGGER.error("客户端连接异常，请稍后重试 [{}]", e.getMessage());
				response.getWriter().println(ResponseJSONObject.ResponseJSON(501, false, "客户端连接异常，请稍后重试", null));
				return;
			}
			LOGGER.error("转币请求失败操作异常", e);
			response.getWriter().println(ResponseJSONObject.ResponseJSON(500, false, "转币请求失败操作异常", null));
		}
	}

	/**
	 * 代币转账
	 * 
	 * @author bingo
	 * @date 2018年3月28日 上午10:15:21
	 */
	private String transfer(String fromAddress, String password, String toAddress, BigDecimal value, String token) {
		BigInteger amount = value.multiply(BigDecimal.valueOf(1000000000000000000L)).toBigInteger();
		return new ERC20TokenService().sendTokenTransaction(fromAddress, password, token, toAddress, amount, BigInteger.valueOf(60000));
	}

	/**
	 * 插入记录
	 * 
	 * @author bingo
	 * @date 2018年4月20日 下午4:38:06
	 */
	public void insertRecord(String contractAddress, String txHash, String fromAddress, String toAddress, BigDecimal value, String ipAddress) {
		String sql = "insert into ruiec_token_transfer_request (create_time, contract_address, tx_hash, from_address, to_address, value, ip_address) values(?, ?, ?, ?, ?, ?, ?)";
		List<Object> params = new ArrayList<Object>();
		params.add(new Date());
		params.add(contractAddress);
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
				LOGGER.error("插入代币转账请求记录出现异常", e);
			}
		} finally {
			if (null != connection) {
				jdbcUtil.releaseConn(connection);
			}
		}
	}

}
