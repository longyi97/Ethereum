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
 * 根据账户地址查询账户servlet
 * 
 * @author bingo
 * @date 2018年4月16日 下午4:47:23
 */
@WebServlet("/GetAccount")
public class GetAccount extends HttpServlet {
	
	private static final long serialVersionUID = -57837183609138345L;

	private static final Logger LOGGER = LoggerFactory.getLogger(GetAccount.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8"); 
		String accountId=request.getParameter("accountId");
		try {
			JSONObject jsonData = new JSONObject();
			boolean isExist = UtilService.findAccount(accountId);
			jsonData.put("isExist", isExist);
			LOGGER.info("{} is exist: {}", accountId, isExist);
			response.getWriter().println(ResponseJSONObject.ResponseJSON(200, true, "查询账户是否存在操作成功", jsonData));
		} catch (Exception e) {
			if (e.getCause() instanceof ConnectException) {
				LOGGER.error("客户端连接异常，请稍后重试 [{}]", e.getMessage());
				response.getWriter().println(ResponseJSONObject.ResponseJSON(501, false, "客户端连接异常，请稍后重试", null));
				return;
			}
			LOGGER.error("查询账户是否存在操作异常",e);
			response.getWriter().println(ResponseJSONObject.ResponseJSON(500, false, "查询账户是否存在操作异常", null));
		}
	}

}
