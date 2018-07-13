package com.ruiec.servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.ConnectException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;

import com.ruiec.service.UtilService;
import com.ruiec.utils.ResponseJSONObject;

import net.sf.json.JSONObject;

/**
 * 查询账户余额servlet
 * 
 * @author bingo
 * @date 2018年4月2日 下午2:17:56
 */
@WebServlet("/GetBalance")
public class GetBalance extends HttpServlet {

	private static final long serialVersionUID = 1222703744187289165L;

	private static final Logger LOGGER = LoggerFactory.getLogger(GetBalance.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		String accountId = request.getParameter("accountId");
		try {
			JSONObject jsonData = new JSONObject();
			BigInteger wei = UtilService.getWeb3j().ethGetBalance(accountId, DefaultBlockParameterName.LATEST).sendAsync().get().getBalance();
			BigDecimal balance = new BigDecimal(wei).divide(new BigDecimal("1000000000000000000"));
			jsonData.put("balance", balance.toPlainString());
			LOGGER.info("balance of {}: {}", accountId, balance.toPlainString());
			response.getWriter().println(ResponseJSONObject.ResponseJSON(200, true, "查询余额成功", jsonData));
		} catch (Exception e) {
			if (e.getCause() instanceof ConnectException) {
				LOGGER.error("客户端连接异常，请稍后重试 [{}]", e.getMessage());
				response.getWriter().println(ResponseJSONObject.ResponseJSON(501, false, "客户端连接异常，请稍后重试", null));
				return;
			}
			LOGGER.error("查询余额异常", e);
			response.getWriter().println(ResponseJSONObject.ResponseJSON(500, false, "查询余额异常", null));
		}
	}

}
