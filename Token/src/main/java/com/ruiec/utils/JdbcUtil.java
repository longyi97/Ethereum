package com.ruiec.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库链接工具类
 * 
 * @author 贺云<br>
 *         Version: 1.0<br>
 *         Date: 2017年8月21日
 */
public class JdbcUtil {

	// 表示定义数据库的用户名
	private static String USERNAME;
	// 定义数据库的密码
	private static String PASSWORD;
	// 定义数据库的驱动信息
	private static String DRIVER;
	// 定义访问数据库的地址
	private static String URL;

	static {
		// 加载数据库配置信息，并给相关的属性赋值
		try {
			USERNAME = PropertiesUtils.getProperty("jdbc.username");
			PASSWORD = PropertiesUtils.getProperty("jdbc.password");
			DRIVER = PropertiesUtils.getProperty("jdbc.driver");
			URL = PropertiesUtils.getProperty("jdbc.url");
		} catch (Exception e) {
			throw new RuntimeException("读取数据库配置文件异常！", e);
		}
	}

	/**
	 * 获取数据库连接
	 * 
	 * @return 数据库连接
	 */
	public Connection getConnection() {
		try {
			// 注册驱动
			Class.forName(DRIVER);
			// 获取连接
			Connection connection = (Connection) DriverManager.getConnection(URL + "?useUnicode=true&characterEncoding=utf-8&useSSL=false", USERNAME, PASSWORD);
			return connection;
		} catch (Exception e) {
			throw new RuntimeException("get connection error!", e);
		}
	}

	/**
	 * 执行更新操作
	 * 
	 * @param sql
	 *            sql语句
	 * @param params
	 *            执行参数
	 * @return 执行结果
	 * @throws SQLException
	 */
	public boolean updateByPreparedStatement(Connection connection, String sql, List<?> params) throws SQLException {
		boolean flag = false;
		// 表示当用户执行添加删除和修改的时候所影响数据库的行数
		int result = -1;
		PreparedStatement pstmt = null;
		try {
			pstmt = (PreparedStatement) connection.prepareStatement(sql);
			int index = 1;
			// 填充sql语句中的占位符
			if (params != null && !params.isEmpty()) {
				for (int i = 0; i < params.size(); i++) {
					pstmt.setObject(index++, params.get(i));
				}
			}
			result = pstmt.executeUpdate();
			flag = result > 0 ? true : false;
		} catch (Exception e) {
			throw e;
		} finally {
			if (null != pstmt) {
				pstmt.close();
			}
		}
		return flag;
	}

	/**
	 * 执行查询操作
	 * 
	 * @param sql
	 *            sql语句
	 * @param params
	 *            执行参数
	 * @return
	 * @throws SQLException
	 */
	public List<Map<String, Object>> findResult(Connection connection, String sql, List<?> params) throws SQLException {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		int index = 1;
		PreparedStatement pstmt = null;
		ResultSet resultSet = null;
		try {
			pstmt = (PreparedStatement) connection.prepareStatement(sql);
			if (params != null && !params.isEmpty()) {
				for (int i = 0; i < params.size(); i++) {
					pstmt.setObject(index++, params.get(i));
				}
			}
			resultSet = pstmt.executeQuery();
			ResultSetMetaData metaData = (ResultSetMetaData) resultSet.getMetaData();
			int cols_len = metaData.getColumnCount();
			while (resultSet.next()) {
				Map<String, Object> map = new HashMap<String, Object>();
				for (int i = 0; i < cols_len; i++) {
					String cols_name = metaData.getColumnName(i + 1);
					Object cols_value = resultSet.getObject(cols_name);
					if (cols_value == null) {
						cols_value = "";
					}
					map.put(cols_name, cols_value);
				}
				list.add(map);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (null != resultSet) {
				resultSet.close();
			}
			if (null != pstmt) {
				pstmt.close();
			}
		}
		return list;
	}

	/**
	 * 释放资源
	 */
	public void releaseConn(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
