package com.ruiec.servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.ConnectException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruiec.service.ERC20TokenService;
import com.ruiec.utils.JdbcUtil;
import com.ruiec.utils.ResponseJSONObject;

import net.sf.json.JSONObject;

/**
 * 查询代币余额servlet
 * 
 * @author bingo
 * @date 2018年4月23日 下午2:36:50
 */
@WebServlet("/GetBalance")
public class GetBalance extends HttpServlet {

	private static final long serialVersionUID = 8178677189464608764L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GetBalance.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		String accountId = request.getParameter("accountId");
		String token = request.getParameter("token");
		
		try {
			JSONObject jsonData = new JSONObject();
			ERC20TokenService erc20TokenService = new ERC20TokenService();
			BigInteger unitBalance = erc20TokenService.getBalanceOf(accountId, token);
			BigDecimal balance = new BigDecimal(unitBalance).divide(new BigDecimal("1000000000000000000"));
			jsonData.put("balance", balance.toPlainString());
			LOGGER.info("balance of {}: {}", accountId, balance.toPlainString());
			response.getWriter().println(ResponseJSONObject.ResponseJSON(200, true, "查询代币余额成功", jsonData));
		} catch (Exception e) {
			if (e.getCause() instanceof ConnectException) {
				LOGGER.error("客户端连接异常，请稍后重试 [{}]", e.getMessage());
				response.getWriter().println(ResponseJSONObject.ResponseJSON(501, false, "客户端连接异常，请稍后重试", null));
				return;
			}
			LOGGER.error("查询代币余额异常", e);
			response.getWriter().println(ResponseJSONObject.ResponseJSON(500, false, "查询代币余额异常", null));
		}
	}

	/**
	 * 根据交易Hash查询代币转账交易记录
	 * 
	 * @author bingo
	 * @date 2018年3月29日 下午10:05:10
	 */
	public Map<String, Object> findTokenRecordByHash(String txHash, String token) {
		JdbcUtil jdbcUtil = new JdbcUtil();
		Connection conn = null;
		try {
			conn = jdbcUtil.getConnection();
			String sql = "SELECT * from ruiec_token_transfer_record WHERE tx_hash = ? and contract_address = ?";
			List<Object> params = new ArrayList<Object>();
			params.add(txHash);
			params.add(token);
			List<Map<String, Object>> results = jdbcUtil.findResult(conn, sql, params);
			if (results.size() > 0) {
				return results.get(0);
			}
		} catch (SQLException e) {
			LOGGER.error("根据交易Hash查询代币转账记录异常", e);
			throw new RuntimeException(e);
		} finally {
			jdbcUtil.releaseConn(conn);
		}
		return null;
	}

}
