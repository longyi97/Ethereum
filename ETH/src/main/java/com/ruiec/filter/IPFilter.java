package com.ruiec.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ruiec.utils.PropertiesUtils;

/**
 * IP过滤器
 * 
 * @author bingo
 * @date 2018年4月20日 下午2:14:15
 */
@WebFilter("/*")
public class IPFilter implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(IPFilter.class);

	public void destroy() {
		LOGGER.info("destroy ip filter------------------------------------------------------------");
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		LOGGER.info("request url {}", req.getRequestURL());
		String ipsString = PropertiesUtils.getProperty("allowedIPs", "*");
		if (ipsString.equals("*")) {
			chain.doFilter(request, response);
			return;
		}
		String[] ips = ipsString.split(",");
		for (int i = 0; i < ips.length; i++) {
			if (request.getRemoteAddr().equals(ips[i])) {
				chain.doFilter(request, response);
				return;
			}
		}
	}

	public void init(FilterConfig fConfig) throws ServletException {
		LOGGER.info("init ip filter------------------------------------------------------------");
	}

}
