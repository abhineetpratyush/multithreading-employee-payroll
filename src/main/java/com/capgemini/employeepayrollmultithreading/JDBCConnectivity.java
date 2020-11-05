package com.capgemini.employeepayrollmultithreading;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.Enumeration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JDBCConnectivity {
	private static final Logger log = LogManager.getLogger(JDBCConnectivity.class);
	public static void main(String[] args) throws CustomJDBCException {
		String jdbcURL = "jdbc:mysql://localhost:3306/payroll_service?useSSL=false";
		String userName = "root";
		String password = "abcd4321";
		Connection connection;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			log.info("Driver loaded!");
		} catch(ClassNotFoundException e) {
			throw new CustomJDBCException(ExceptionType.CLASS_NOT_FOUND);
		}

		listDrivers();


		try {
			log.info("Connecting to database: " + jdbcURL);
			connection = DriverManager.getConnection(jdbcURL, userName, password);
			log.info("Connection is successful!! " + connection);
		} catch(SQLException e) {
			throw new CustomJDBCException(ExceptionType.SQL_EXCEPTION);
		}
	}

	private static void listDrivers() {
		Enumeration<Driver> driverList = DriverManager.getDrivers();
		while(driverList.hasMoreElements()) {
			Driver driverClass = (Driver) driverList.nextElement();
			log.info(driverClass.getClass().getName());
		}
	}
}