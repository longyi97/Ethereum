package com.ruiec.utils;

import java.util.List;

import net.sf.json.JSONObject;

/**
 * 提供钱包返回json操作
 * @author 贺云<br>
 * Version: 1.0<br>
 * Date: 2017年8月18日
 */
public class ResponseJSONObject {

	/**
	 * 钱包json返回方法
	 * @author 贺云<br>
	 * Version: 1.0<br>
	 * Date: 2017年8月18日
	 * @param code 返回码
	 * @param flag 操作标示
	 * @param msg 操作提示语
	 * @param jsonData 数据结果集
	 */
	public static String ResponseJSON(int code,boolean flag, String msg,JSONObject jsonData){
		JSONObject json = new JSONObject();
		json.put("statusCode", code);
		json.put("returnResult", msg);
		json.put("returnFlag", flag);
		json.put("data",jsonData);
		return json.toString();
	}
	
	/**
	 * 钱包jsonList返回方法
	 * @author 贺云<br>
	 * Version: 1.0<br>
	 * Date: 2017年8月18日
	 * @param code 返回码
	 * @param flag 操作标示
	 * @param msg 操作提示语
	 * @param jsonData 数据结果集
	 */
	public static String ResponseJSONList(int code,boolean flag, String msg,List<JSONObject> jsonList){
		JSONObject json = new JSONObject();
		json.put("statusCode", code);
		json.put("returnResult", msg);
		json.put("returnFlag", flag);
		json.put("data",jsonList);
		return json.toString();
	}
}
