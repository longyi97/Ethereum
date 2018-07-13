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
import com.ruiec.utils.ResponseJSONObject;

import net.sf.json.JSONObject;

/**
 * 创建账户servlet
 * 
 * @author bingo
 * @date 2018年4月3日 上午11:15:03
 */
@WebServlet("/CreateAccount")
public class CreateAccount extends HttpServlet {

	private static final long serialVersionUID = 1371146656629461180L;

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateAccount.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		String password = request.getParameter("password");
		JSONObject jsonData = new JSONObject();
		try {
			String accountId = UtilService.getAdmin().personalNewAccount(password).send().getAccountId();
			if (null != accountId) {
				jsonData.put("accountId", accountId);
				LOGGER.info("创建账户成功，账户地址是:[{}]!", accountId);
				response.getWriter().println(ResponseJSONObject.ResponseJSON(200, true, "创建账户成功", jsonData));
			} else {
				LOGGER.info("password:[{}]创建账户失败", password);
				response.getWriter().println(ResponseJSONObject.ResponseJSON(400, false, "创建账户失败", null));
			}
		} catch (Exception e) {
			if (e.getCause() instanceof ConnectException) {
				LOGGER.error("客户端连接异常，请稍后重试 [{}]", e.getMessage());
				response.getWriter().println(ResponseJSONObject.ResponseJSON(501, false, "客户端连接异常，请稍后重试", null));
				return;
			}
			LOGGER.error("password:[{}]创建账户异常", password, e);
			response.getWriter().println(ResponseJSONObject.ResponseJSON(500, false, "创建账户异常", null));
		}
	}

}
