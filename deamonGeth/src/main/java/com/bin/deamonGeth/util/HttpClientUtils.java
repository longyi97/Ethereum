package com.bin.deamonGeth.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class HttpClientUtils{

	/**
	 * 模拟请求
	 * 
	 * @param url
	 * @param params
	 * @return
	 */
	public static String doGet(String url, Map<String, String> params) {
		CloseableHttpClient httpclient = createSSLClientDefault();
//		url = url + "?";
//		if (params != null)
//			for (Map.Entry<String, String> entry : params.entrySet()) {
//				String name = entry.getKey();
//				String value = entry.getValue();
//				if (name != null && value != null) {
//					url = url + name + "=" + value + "&";
//				}
//			}
		HttpGet httpGet = new HttpGet(url);
		CloseableHttpResponse response = null;
		try {
			response = httpclient.execute(httpGet);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				HttpEntity httpEntity = response.getEntity();
				if (httpEntity == null) {
					return null;
				}
				return EntityUtils.toString(httpEntity);

			}
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (response != null) {
				try {
					response.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (httpclient != null) {
				try {
					httpclient.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public static CloseableHttpClient createSSLClientDefault() {
		try {
			SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
				// 信任所有
				public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					return true;
				}
			}).build();
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
			return HttpClients.custom().setSSLSocketFactory(sslsf).build();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		return HttpClients.createDefault();
	}

	/**
	 * 发送post请求
	 * 
	 * @param strURL
	 * @param req
	 * @return
	 */
	public static String doPostQueryCmd(String strURL, Map<String, String> req)
	{
		CloseableHttpResponse response = null;
		try
		{
			CloseableHttpClient httpClient = HttpClients.createDefault();
			  HttpPost post = new HttpPost(strURL);
			  RequestConfig config = RequestConfig.custom()
			    .setConnectionRequestTimeout(10000).setConnectTimeout(10000)
			    .setSocketTimeout(10000).build();
			  
			   List formparams = new ArrayList();
			   
			   for (Map.Entry<String, String> entry : req.entrySet()){
					formparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
				}
			   post.setEntity(new UrlEncodedFormEntity(formparams, "UTF-8"));
			   post.setConfig(config);
			   response = httpClient.execute(post);
			   HttpEntity entity = response.getEntity();
			   String content = EntityUtils.toString(entity);
			   EntityUtils.consume(entity);
			
			
			return content;
			
		}catch(Exception e){
			
		}finally{
			try {
			    response.close();
			   } catch (IOException e) {
			    e.printStackTrace();
			   }
		}
			
			return null;
			
	}
	
	/**
	 * 拼接链接和参数
	 * @param baseUrl
	 * @param params
	 * @return
	 */
	public static String joinUrlAndParams(String baseUrl, Map<String, String> params){
		if(params.size()>0){
			int flag = 1;
			StringBuffer sb = new StringBuffer(baseUrl + "?");
			for(Map.Entry<String, String> entry : params.entrySet()){
				String key = entry.getKey();
				String value = entry.getValue();
				sb.append(key).append("=").append(value);
				if(flag < params.size()){
					sb.append("&");
				}
				flag ++;
			}
			return sb.toString();
		}
		return baseUrl;
	}
	
	/**
	 * 屏蔽异常
	 * @param client
	 * @param httpGet
	 * @return
	 */
	public static CloseableHttpResponse getResponse(CloseableHttpClient client, HttpGet httpGet){
		CloseableHttpResponse response = null;
		try {
			response = client.execute(httpGet);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return response;
	}
	
	/**
	 * 屏蔽异常
	 * @param client
	 * @param httpRequest
	 * @return
	 */
	public static CloseableHttpResponse getResponse(CloseableHttpClient client, HttpUriRequest httpRequest){
		CloseableHttpResponse response = null;
		try {
			response = client.execute(httpRequest);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return response;
	}
	
	/**
	 * 获取响应流
	 * @param response
	 * @return
	 */
	public static InputStream getInputStream(CloseableHttpResponse response){
		InputStream localInputStream = null;
		try {
			localInputStream = response.getEntity().getContent();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return localInputStream;
	}
	/**
	 * 状态码检查
	 * @param response
	 * @param statusCode
	 * @return
	 */
	public static boolean validStatus(CloseableHttpResponse response, int statusCode){
		return response.getStatusLine().getStatusCode() == statusCode;
	}
	/**
	 * 获取响应体并屏蔽异常
	 * @param response
	 * @return
	 */
	public static String getContent(CloseableHttpResponse response){
		HttpEntity entity = response.getEntity();
		String content = null;
		try {
			content = EntityUtils.toString(entity);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return content;
	}
	/**
	 * 获取指定头的值
	 * @param response
	 * @param headKey
	 * @return
	 */
	public static String getHead(CloseableHttpResponse response, String headKey){
		Header[] headers = response.getHeaders(headKey);
		if(headers.length<1)
			return null;
		Header headerCaptcha = headers[0];	
		String headValue = headerCaptcha.getValue();
		return headValue;
	}
	
	/**
	 * 获取UrlEncodedFormEntity对象并屏蔽异常
	 * @param params 参数对集合
	 * @return
	 */
	public static UrlEncodedFormEntity getFormEntity(List<NameValuePair> params){
		try {
			UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(params);
			return formEntity;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
