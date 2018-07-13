package com.ruiec.servlet;

import java.io.IOException;
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

import com.ruiec.service.UtilService;
import com.ruiec.utils.JdbcUtil;
import com.ruiec.utils.ResponseJSONObject;

import net.sf.json.JSONObject;

/**
 * 查询代币转账记录servlet
 * 
 * @author bingo
 * @date 2018年3月29日 下午10:24:54
 */
@WebServlet("/GetTokenRecordServlet")
public class GetTokenRecordServlet extends HttpServlet {

	private static final long serialVersionUID = 5308069168679761931L;

	private static final Logger LOGGER = LoggerFactory.getLogger(GetTokenRecordServlet.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		LOGGER.info("查询代币转账记录 ------------------------------------------------------------");
		response.setCharacterEncoding("UTF-8");
		String txHash = request.getParameter("txHash");
		String token = request.getParameter("token");
		
		try {
			if (null == UtilService.getWeb3j().ethGetTransactionByHash(txHash).send().getResult()) {
				LOGGER.info("txHash:[{}] Hash不存在", txHash);
				response.getWriter().println(ResponseJSONObject.ResponseJSON(400, false, "Hash不存在", null));
				return;
			}
			Map<String, Object> map = findTokenRecordByHash(txHash, token);
			if (null != map) {
				JSONObject result = JSONObject.fromObject(map);
				result.remove("create_time");
				result.remove("modify_time");
				result.remove("timestamp");
				LOGGER.info("查询代币转账记录成功 {}", result);
				response.getWriter().println(ResponseJSONObject.ResponseJSON(200, true, "查询代币转账记录成功", result));
			} else {
				LOGGER.info("暂未找到Hash对应信息，请稍后重试 , txHash:{}", txHash);
				response.getWriter().println(ResponseJSONObject.ResponseJSON(200, false, "暂未找到Hash对应信息，请稍后重试", null));
			}
		} catch (Exception e) {
			if (e.getCause() instanceof ConnectException) {
				LOGGER.error("客户端连接异常，请稍后重试 [{}]", e.getMessage());
				response.getWriter().println(ResponseJSONObject.ResponseJSON(501, false, "客户端连接异常，请稍后重试", null));
				return;
			}
			LOGGER.error("查询代币转账记录异常", e);
			response.getWriter().println(ResponseJSONObject.ResponseJSON(500, false, "查询代币转账记录异常", null));
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
