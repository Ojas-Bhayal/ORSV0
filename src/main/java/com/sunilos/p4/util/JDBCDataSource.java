package com.sunilos.p4.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.sunilos.p4.exception.DatabaseException;

/**
 * JDBC DataSource is a Data Connection Pool (DCP) matching Experiment 5 Class Diagram.
 * Supports environment variable overrides (ORS_DB_DRIVER, ORS_DB_URL, ORS_DB_USER,
 * ORS_DB_PASSWORD) and an optional test connection supplier for disposable
 * integration test databases (Phase 7 Task 4).
 */
public class JDBCDataSource {

	private static JDBCDataSource datasource;
	private static volatile Callable<Connection> testConnectionSupplier;

	private JDBCDataSource() {
	}

	private ComboPooledDataSource cpds = null;

	public static void setTestConnectionSupplier(Callable<Connection> supplier) {
		testConnectionSupplier = supplier;
	}

	public static void clearTestConnectionSupplier() {
		testConnectionSupplier = null;
	}

	private static String resolveConfig(ResourceBundle rb, String key, String envVar, String fallback) {
		String env = System.getenv(envVar);
		if (env != null && !env.isEmpty()) {
			return env;
		}
		String sysProp = System.getProperty(envVar);
		if (sysProp != null && !sysProp.isEmpty()) {
			return sysProp;
		}
		try {
			String val = rb.getString(key);
			if (val != null && val.startsWith("${") && val.endsWith("}")) {
				int colon = val.indexOf(':');
				if (colon > 0) {
					return val.substring(colon + 1, val.length() - 1);
				}
				return fallback;
			}
			return val;
		} catch (Exception e) {
			return fallback;
		}
	}

	public static synchronized JDBCDataSource getInstance() {
		if (datasource == null) {
			ResourceBundle rb = ResourceBundle.getBundle("com.sunilos.p4.bundle.system");

			datasource = new JDBCDataSource();
			datasource.cpds = new ComboPooledDataSource();
			try {
				datasource.cpds.setDriverClass(resolveConfig(rb, "driver", "ORS_DB_DRIVER", "com.mysql.cj.jdbc.Driver"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			datasource.cpds.setJdbcUrl(resolveConfig(rb, "url", "ORS_DB_URL", "jdbc:mysql://localhost:3306/p04"));
			datasource.cpds.setUser(resolveConfig(rb, "username", "ORS_DB_USER", "root"));
			datasource.cpds.setPassword(resolveConfig(rb, "password", "ORS_DB_PASSWORD", ""));
			datasource.cpds.setInitialPoolSize(DataUtility.getInt((rb.getString("initialPoolSize"))));
			datasource.cpds.setAcquireIncrement(DataUtility.getInt(rb.getString("acquireIncrement")));
			datasource.cpds.setMaxPoolSize(DataUtility.getInt(rb.getString("maxPoolSize")));
			datasource.cpds.setMaxIdleTime(DataUtility.getInt(rb.getString("timeout")));
			datasource.cpds.setMinPoolSize(DataUtility.getInt(rb.getString("minPoolSize")));
		}
		return datasource;
	}

	public static Connection getConnection() throws SQLException {
		Callable<Connection> supplier = testConnectionSupplier;
		if (supplier != null) {
			try {
				return supplier.call();
			} catch (SQLException e) {
				throw e;
			} catch (Exception e) {
				throw new SQLException("Test connection supplier failed", e);
			}
		}
		return getInstance().cpds.getConnection();
	}

	public static void closeConnection(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				throw new DatabaseException("Connection close exception" + e.getMessage());
			}
		}
	}

	public static void trnRollback(Connection connection) {
		rollBack(connection);
	}

	public static void rollBack(Connection connection) {
		if (connection != null) {
			try {
				connection.rollback();
			} catch (SQLException e) {
				e.printStackTrace();
				throw new DatabaseException("Rollback exception" + e.getMessage());
			}
		}
	}
}
