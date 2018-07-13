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

import com.ruiec.utils.JdbcUtil;
import com.ruiec.utils.ResponseJSONObject;

import net.sf.json.JSONObject;

/**
 * 查询代币交易记录servlet
 * 
 * @author bingo
 * @date 2018年3月27日 下午10:06:10
 */
@WebServlet("/GetTokenRecordByPage")
public class GetTokenRecordByPage extends HttpServlet {

	private static final long serialVersionUID = -8084092007377330523L;

	private static final Logger LOGGER = LoggerFactory.getLogger(GetTokenRecordByPage.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		LOGGER.info("查询代币交易记录 ------------------------------------------------------------");
		response.setCharacterEncoding("UTF-8");
		int id = Integer.valueOf(request.getParameter("id"));
		int pageSize = Integer.valueOf(request.getParameter("pageSize"));
		String token = request.getParameter("token");
		try {
			List<Map<String, Object>> results = findTokenRecordByPage(id, pageSize, token);
			if (results.size() > 0) {
				List<JSONObject> transactionRecord = new ArrayList<JSONObject>();
				for (Map<String, Object> result : results) {
					transactionRecord.add(JSONObject.fromObject(result));
				}
				LOGGER.info("查询代币交易记录成功 {}", transactionRecord);
				response.getWriter().println(ResponseJSONObject.ResponseJSONList(200, true, "查询代币交易记录成功", transactionRecord));
			} else {
				response.getWriter().println(ResponseJSONObject.ResponseJSON(400, false, "暂无数据", null));
			}
		} catch (Exception e) {
			if (e.getCause() instanceof ConnectException) {
				LOGGER.error("客户端连接异常，请稍后重试 [{}]", e.getMessage());
				response.getWriter().println(ResponseJSONObject.ResponseJSON(501, false, "客户端连接异常，请稍后重试", null));
				return;
			}
			LOGGER.error("查询代币交易记录异常", e);
			response.getWriter().println(ResponseJSONObject.ResponseJSON(500, false, "查询代币交易记录异常", null));
		}
	}

	/**
	 * 分页查询代币交易记录
	 * 
	 * @param id
	 *            开始查询的id位置（id自增）
	 * @param pageSize
	 *            查询记录数量
	 * @author bingo
	 * @date 2018年3月27日 下午10:30:53
	 */
	private List<Map<String, Object>> findTokenRecordByPage(Integer id, Integer pageSize, String token) {
		JdbcUtil jdbcUtil = new JdbcUtil();
		Connection conn = null;
		try {
			conn = jdbcUtil.getConnection();
			String sql = "SELECT * from ruiec_token_transfer_record WHERE id > ? and contract_address = ? LIMIT 0,?";
			List<Object> params = new ArrayList<Object>();
			params.add(id);
			params.add(token);
			params.add(pageSize);
			return jdbcUtil.findResult(conn, sql, params);
		} catch (SQLException e) {
			LOGGER.error("查询代币交易记录异常", e);
			throw new RuntimeException(e);
		} finally {
			jdbcUtil.releaseConn(conn);
		}
	}

}
