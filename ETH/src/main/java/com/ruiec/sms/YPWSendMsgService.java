package com.ruiec.sms;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.log4j.Logger;

import com.ruiec.utils.HttpClientUtils;
import com.ruiec.utils.PropertiesUtils;

/**
 * 发送短信服务（云片网）<br>
 * 		服务商: www.yunpian.com<br>
 * 		传输协议: HTTP<br>
 * 		API接口（POST）: http://sms.yunpian.com/v2/sms/single_send.json<br>
 * @author 刘立雯<br>
 * Version: 1.0<br>
 * Date: 2017年04月12日
 */
public class YPWSendMsgService {

	private static final Logger LOGGER = Logger.getLogger(YPWSendMsgService.class);
	private static final String LOGGER_PREFIX = "短信发送服务（云片网）: "; // 日志前缀
	
	/**
	 * 发送短信
	 * @param mobile 手机号
	 * @param text 短信内容
	 * @return
	 * @author 刘立雯
	 * Date: 2017年04月12日
	 */
	public static Map<String, String> sendMsg(String mobile, String text){
		Map<String, String> map=new HashMap<String, String>();
		try {
			Map<String, String> req=new HashMap<String, String>();
			req.put("apikey", PropertiesUtils.getProperty("YP.apikey"));
			req.put("mobile", "+"+URLEncoder.encode(mobile.replaceAll("\\+", ""), "gbk"));//格式必须是"+"号开头("+"号需要urlencode处理，否则会出现格式错误)
			req.put("text", PropertiesUtils.getProperty("YP.sign")+text);
			String jsonStr=HttpClientUtils.doPostQueryCmd(PropertiesUtils.getProperty("YP.url"), req);
			
			if(null != jsonStr && jsonStr.isEmpty()){
				LOGGER.error(LOGGER_PREFIX+"发送短信的请求失败...");
				map.put("code", "403");
				map.put("msg", "请求结果为空");
				return map;
			}
			
			String[] str=jsonStr.split(",");
			
			if(str==null||str.length==0){
				LOGGER.error(LOGGER_PREFIX+"发送短信的请求失败...");
				map.put("code", "403");
				map.put("msg", "请求结果为空");
				return map;
			}
			
			int code = 1;//发送短信返回的状态码
			String msg="";//发送短信返回的消息
			
			for (String string : str) {
				if(string.contains("code")){
					code=Integer.valueOf(string.split(":")[1].replaceAll("\"", ""));
				}
				if(string.contains("msg")){
					msg=string.split(":")[1].replaceAll("\"", "");
				}
			}
			
			//code = 0:	正确返回。可以从api返回的对应字段中取数据。
			if(code==0){
				LOGGER.info(LOGGER_PREFIX + mobile + "=|=" + text + "=|=" + msg);
				map.put("code", "200");
				map.put("msg", "短信发送成功");
				return map;
			}
			//code > 0:	调用API时发生错误，需要开发者进行相应的处理。
			else if (code>0) {
				LOGGER.error(LOGGER_PREFIX+ msg+"（调用API时发生错误，需要开发者进行相应的处理）");
				map.put("code", "400");
				map.put("msg", msg);
				return map;
			}
			//-50 < code <= -1:	权限验证失败，需要开发者进行相应的处理。
			else if (code<=-1&&code>-50) {
				LOGGER.error(LOGGER_PREFIX+ msg+"（权限验证失败，需要开发者进行相应的处理）");
				map.put("code", "403");
				map.put("msg", msg+"（权限验证失败，需要开发者进行相应的处理）");
				return map;
			}
			//code <= -50:	系统内部错误，请联系技术支持，调查问题原因并获得解决方案。
			else if (code<=-50) {
				LOGGER.error(LOGGER_PREFIX+ msg+"（系统内部错误，请联系技术支持，调查问题原因并获得解决方案）");
				map.put("code", "403");
				map.put("msg", msg+"（云片系统内部错误，请联系技术支持，调查问题原因并获得解决方案）");
				return map;
			}
			return map;
		} catch (Exception e) {
			LOGGER.error(LOGGER_PREFIX + "短信发送失败...", e);
			map.put("code", "500");
			map.put("msg", "短信发送失败");
			return map;
		}
	}
	
	public static void main(String[] args) {
		Map<String, String> map = YPWSendMsgService.sendMsg(PropertiesUtils.getProperty("error.MobilePhone"),
				PropertiesUtils.getProperty("msg.name") + PropertiesUtils.getProperty("msg.disconnected"));
		BiConsumer<String, String> action = new BiConsumer<String, String>() {
			
			@Override
			public void accept(String t, String u) {
				System.out.println(t + ":" + u);
				
			}
		};
		map.forEach(action);
	}
}
