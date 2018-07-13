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
 * 查询账户转账记录servlet
 * 
 * @author 陈靖原/bingo
 * @date 2018年4月2日 上午10:56:37
 */
@WebServlet("/GetRecordByPage")
public class GetRecordByPage extends HttpServlet {

	private static final long serialVersionUID = -2141879775108514342L;

	private static final Logger LOGGER = LoggerFactory.getLogger(GetRecordByPage.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			response.setCharacterEncoding("UTF-8");
			int id = Integer.valueOf(request.getParameter("id"));
			int pageSize = Integer.valueOf(request.getParameter("pageSize"));
			List<Map<String, Object>> result = findRecordByPage(id, pageSize);
			List<JSONObject> transactionRecord = new ArrayList<JSONObject>();
			if (result.size() > 0) {
				for (Map<String, Object> obj : result) {
					JSONObject jsonObject = JSONObject.fromObject(obj.get("tx_info"));
					jsonObject.put("dbid", Integer.valueOf(obj.get("id").toString()));
					transactionRecord.add(jsonObject);
				}
				LOGGER.info("查询成功", transactionRecord);
				response.getWriter().println(ResponseJSONObject.ResponseJSONList(200, true, "查询成功", transactionRecord));
			} else {
				response.getWriter().println(ResponseJSONObject.ResponseJSON(200, true, "暂无数据", null));
			}
		} catch (Exception e) {
			if (e.getCause() instanceof ConnectException) {
				LOGGER.error("客户端连接异常，请稍后重试 [{}]", e.getMessage());
				response.getWriter().println(ResponseJSONObject.ResponseJSON(501, false, "客户端连接异常，请稍后重试", null));
				return;
			}
			LOGGER.error("查询交易记录异常", e);
			response.getWriter().println(ResponseJSONObject.ResponseJSON(500, false, "查询交易记录异常", null));
		}
	}
	
	/**
	 * 分页查询交易记录
	 * 
	 * @param id
	 *            开始查询的id位置（id自增）
	 * @param pageSize
	 *            查询记录数量
	 * @author bingo
	 * @date 2018年4月2日 上午10:50:48
	 */
	private List<Map<String, Object>> findRecordByPage(Integer id, Integer pageSize) {
		JdbcUtil jdbcUtil = new JdbcUtil();
		Connection conn = null;
		try {
			conn = jdbcUtil.getConnection();
			String sql = "SELECT * from ruiec_etc_transaction_record WHERE id > ? LIMIT 0,?";
			List<Object> selectParams = new ArrayList<Object>();
			selectParams.add(id);
			selectParams.add(pageSize);
			return jdbcUtil.findResult(conn, sql, selectParams);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			jdbcUtil.releaseConn(conn);
		}
	}
}
