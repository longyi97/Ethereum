package com.ruiec.servlet;

import java.io.IOException;
import java.net.ConnectException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruiec.service.UtilService;
import com.ruiec.utils.EthUtils;
import com.ruiec.utils.ResponseJSONObject;

import net.sf.json.JSONObject;

/**
 * 根据Hash查询交易记录
 * 
 * @author bingo
 * @date 2018年3月30日 下午5:56:16
 */
@WebServlet("/GetTransaction")
public class GetTransaction extends HttpServlet {

	private static final long serialVersionUID = -8115973805369796598L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GetTransaction.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		String txHash = request.getParameter("txHash");
		if (null == UtilService.getWeb3j().ethGetTransactionByHash(txHash).send().getResult()) {
			LOGGER.info("txHash:[{}] Hash不存在", txHash);
			response.getWriter().println(ResponseJSONObject.ResponseJSON(400, false, "Hash不存在", null));
			return;
		}
		if (null == UtilService.getWeb3j().ethGetTransactionReceipt(txHash).send().getResult()) {
			LOGGER.info("txHash:[{}] 暂未找到Hash对应信息，请稍后重试", txHash);
			response.getWriter().println(ResponseJSONObject.ResponseJSON(200, false, "暂未找到Hash对应信息，请稍后重试", null));
			return;
		}
		try {
			String result = EthUtils.getTransaction(UtilService.getWeb3j(), txHash);
			JSONObject jsonData = JSONObject.fromObject(result);
			LOGGER.info("txHash:[{}] 查询交易成功", jsonData);
			response.getWriter().println(ResponseJSONObject.ResponseJSON(200, true, "查询交易成功", jsonData));
		} catch (Throwable e) {
			if (e.getCause() instanceof ConnectException) {
				LOGGER.error("客户端连接异常，请稍后重试 [{}]", e.getMessage());
				response.getWriter().println(ResponseJSONObject.ResponseJSON(501, false, "客户端连接异常，请稍后重试", null));
				return;
			}
			LOGGER.error("txHash:[{}] 查询交易异常", txHash, e);
			response.getWriter().println(ResponseJSONObject.ResponseJSON(500, false, "查询交易异常", null));
		}
	}

}
