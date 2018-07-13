package com.ruiec.util;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

/**
 * 配置工具类
 * 
 * @author bingo
 * @date 2018年4月3日 下午5:01:25
 */
public class PropertiesUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesUtils.class);

	private static Properties properties = new Properties();

	/**
	 * 初始化配置信息，加载并存入静态变量中
	 * 
	 * @author bingo
	 * @date 2018年4月3日 下午5:01:14
	 */
	static {
		LOGGER.info("init properties ------------------------------------------------------------");
		init();
		LOGGER.info("init properties finished");
	}

	/**
	 * 初始化配置信息
	 *
	 * @author bingo
	 * @date 2018年05月24日 上午09:52:18
	 */
	public static void init() {
		InputStreamReader inputStreamReader = null;
		try {
			inputStreamReader = new InputStreamReader(PropertiesUtils.class.getResourceAsStream("/applicationConfig.properties"), "UTF-8");
			properties.load(inputStreamReader);
		} catch (IOException e) {
			LOGGER.error("load properties file error", e);
			throw new RuntimeException(e);
		} finally {
			try {
				if (null != inputStreamReader) {
					inputStreamReader.close();
				}
			} catch (IOException e) {
				LOGGER.error("InputStreamReader close Exception", e);
			}
		}
	}

	/**
	 * 根据键获取配置信息对应的值
	 * 
	 * @author bingo
	 * @date 2018年3月27日 下午4:11:47
	 */
	public static String getProperty(String propertyKey) {
		return properties.getProperty(propertyKey);
	}

	/**
	 * 根据键获取配置信息对应的值
	 * 
	 * @author bingo
	 * @date 2018年3月27日 下午4:11:54
	 */
	public static String getProperty(String propertyKey, String defaultValue) {
		return properties.getProperty(propertyKey, defaultValue);
	}

	/**
	 * 根据键获取当前配置信息对应的值
	 *
	 * @author bingo
	 * @date 2018年3月27日 下午4:11:47
	 */
	public static String getCurrentProperty(String propertyKey) {
		init();
		return properties.getProperty(propertyKey);
	}

	/**
	 * 根据键获取当前配置信息对应的值
	 *
	 * @author bingo
	 * @date 2018年3月27日 下午4:11:54
	 */
	public static String getCurrentProperty(String propertyKey, String defaultValue) {
		init();
		return properties.getProperty(propertyKey, defaultValue);
	}

	/**
	 * 修改配置文件
	 * 
	 * @author bingo
	 * @date 2018年4月25日 下午5:57:19
	 */
	public static void updateProperty(String key, String value) {
		try {
			File file = new File(PropertiesUtils.class.getClassLoader().getResource("applicationConfig.properties").getFile());
			List<String> lines = FileUtils.readLines(file, "UTF-8");
			// 查找配置并修改
			for (int i = 0 ; i < lines.size() ; i++) {
				String line = lines.get(i);
				if (line.startsWith(key + "=")) {
					String[] strings = line.split("=");
					if (strings.length <= 2) {
						String newLine = line.substring(0, line.indexOf("=") + 1) + value;
						lines.remove(i);
						lines.add(i, newLine);
					}
				}
			}
			// 保存修改后内容到文件
			FileUtils.writeLines(file, "UTF-8", lines);
		} catch (IOException e) {
			throw new RuntimeException("file read error", e);
		}
	}

}
