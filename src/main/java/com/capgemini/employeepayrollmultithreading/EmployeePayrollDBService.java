package com.capgemini.employeepayrollmultithreading;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class QueryResultStructure{
	public SQLFunctionType functionType;
	public double maleGroupOutput;
	public double femaleGroupOutput;

	public QueryResultStructure(SQLFunctionType functionType, double maleGroupOutput, double femaleGroupOutput) {
		this.functionType = functionType;
		this.maleGroupOutput = maleGroupOutput;
		this.femaleGroupOutput = femaleGroupOutput;
	}
}

enum SQLFunctionType{
	AVG, SUM, MIN, MAX, COUNT
}

public class EmployeePayrollDBService {
	private int connectionCounter = 0;
	private static final Logger log = LogManager.getLogger(EmployeePayrollDBService.class);
	private static EmployeePayrollDBService employeePayrollDBService;
	private PreparedStatement preparedStatement;
	private PreparedStatement preparedStatemetForRetrieval;
	private EmployeePayrollDBService() {}

	public static EmployeePayrollDBService getInstance() {
		if(employeePayrollDBService == null) {
			employeePayrollDBService = new EmployeePayrollDBService();
		}
		return employeePayrollDBService;
	}
	private synchronized Connection getConnection() throws CustomJDBCException {
		connectionCounter++;
		String jdbcURL = "jdbc:mysql://localhost:3306/payroll_service?useSSL=false";
		String userName = "root";
		String password = "abcd4321";
		Connection connection;
		log.info("Processing Thread: " + Thread.currentThread().getName()+
				" Connecting to database with Id:" + connectionCounter);
		try {
			connection = DriverManager.getConnection(jdbcURL, userName, password);
			log.info("Processing Thread: " + Thread.currentThread().getName()+
					" Id: " + connectionCounter + " Connection is successful!!" + connection);
			return connection;
		} catch (SQLException e) {
			throw new CustomJDBCException(ExceptionType.SQL_EXCEPTION);
		}
	}

	public List<EmployeePayrollData> readData() throws CustomJDBCException {
		String sql = "select * from employee_payroll inner join company_details on employee_payroll.company_id "
				+ "= company_details.company_id inner join department_employee on employee_payroll.id = "
				+ "department_employee.employee_id inner join department on department.department_id = "
				+ "department_employee.department_id;"; 
		try (Connection connection = this.getConnection()){
			Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			ResultSet resultSet = statement.executeQuery(sql);
			return this.getEmployeePayrollListFromResultSet(resultSet);
		} catch (SQLException e) {
			throw new CustomJDBCException(ExceptionType.SQL_EXCEPTION);
		}
	}

	private List<EmployeePayrollData> getEmployeePayrollListFromResultSet(ResultSet resultSet) throws CustomJDBCException {
		List<EmployeePayrollData> employeePayrollList = new ArrayList<>();
		try {
			List<String> departmentNames = new ArrayList<>();
			resultSet.next();
			int referenceEmployeeId = resultSet.getInt("id");
			resultSet.absolute(0);
			while (resultSet.next()) {
				int id = resultSet.getInt("id");
				if(id != referenceEmployeeId) {
					int rowNumber = resultSet.getRow() - 1;
					resultSet.absolute(rowNumber);
					int employeeId = resultSet.getInt("id");
					String employeeName = resultSet.getString("name");
					String gender = resultSet.getString("gender");
					double salary = resultSet.getDouble("salary");
					LocalDate start = resultSet.getDate("start").toLocalDate();
					String companyName = resultSet.getString("company_name");
					int companyId = resultSet.getInt("company_id");
					employeePayrollList.add(new EmployeePayrollData(employeeId, employeeName, gender, salary, start, companyName, companyId, departmentNames));
					referenceEmployeeId = id;
					departmentNames.clear();
				}
				else {
					departmentNames.add(resultSet.getString("department_name"));
				}
			}
			int rowNumber = resultSet.getRow() - 1;
			resultSet.absolute(rowNumber);
			int employeeId = resultSet.getInt("id");
			String employeeName = resultSet.getString("name");
			String gender = resultSet.getString("gender");
			double salary = resultSet.getDouble("salary");
			LocalDate start = resultSet.getDate("start").toLocalDate();
			String companyName = resultSet.getString("company_name");
			int companyId = resultSet.getInt("company_id");
			employeePayrollList.add(new EmployeePayrollData(employeeId, employeeName, gender, salary, start, companyName, companyId, departmentNames));
			return employeePayrollList;
		} catch (SQLException e) {
			throw new CustomJDBCException(ExceptionType.RESULT_SET_PROBLEM);
		}
	}

	public int updateEmployeeData(String name, double salary) throws CustomJDBCException {
		return this.updateEmployeeDetailsUsingStatement(name, salary);
	}

	private synchronized int updateEmployeePayrollDataUsingPreparedStatement(String name, double salary) throws CustomJDBCException {
		if(this.preparedStatement==null) {
			this.prepareStatementForEmployeePayroll();
		}
		try {
			preparedStatement.setDouble(1, salary);
			preparedStatement.setString(2, name);
			int rowsAffected=preparedStatement.executeUpdate();
			return rowsAffected;
		}catch(SQLException e) {
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_USE_PREPARED_STATEMENT);
		}
	}

	private void prepareStatementForEmployeePayroll() throws CustomJDBCException {
		try {
			Connection connection = this.getConnection();
			String sql = "update employee_payroll set salary = ? where name = ?";
			this.preparedStatement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		}catch (SQLException e) {
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_USE_PREPARED_STATEMENT);
		}

	}

	public synchronized List<EmployeePayrollData> getEmployeePayrollDataFromDB(String name) throws CustomJDBCException  {
		if (this.preparedStatemetForRetrieval == null) {
			this.prepareStatementForEmployeePayrollDataRetrieval();
		}
		try (Connection connection = this.getConnection()) {
			this.preparedStatemetForRetrieval.setString(1, name);
			ResultSet resultSet = preparedStatemetForRetrieval.executeQuery();
			return this.getEmployeePayrollListFromResultSet(resultSet);
		} catch (SQLException e) {
			throw new CustomJDBCException(ExceptionType.SQL_EXCEPTION);
		}
	}

	private void prepareStatementForEmployeePayrollDataRetrieval() throws CustomJDBCException {
		try {
			Connection connection = this.getConnection();
			String sql = "select * from employee_payroll inner join company_details on "
					+ "employee_payroll.company_id = company_details.company_id inner join "
					+ "department_employee on employee_payroll.id = department_employee.employee_id "
					+ "inner join department on department.department_id = "
					+ "department_employee.department_id where name = ?";
			this.preparedStatemetForRetrieval = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		} catch (SQLException e) {
			throw new CustomJDBCException(ExceptionType.SQL_EXCEPTION);
		}

	}

	private int updateEmployeeDetailsUsingStatement(String name, double salary) throws CustomJDBCException {

		int employeeId = 0, rowsAffected = 0;
		Connection connection = this.getConnection();
		try {
			connection.setAutoCommit(false);
		} catch (SQLException e2) {
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_SET_AUTO_COMMIT);
		}
		try (Statement statement = connection.createStatement();){
			String sql = String.format("update employee_payroll set salary = %.2f where name = '%s';", salary, name);
			statement.executeUpdate(sql);
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				throw new CustomJDBCException(ExceptionType.UNABLE_TO_ROLLBACK);
			}
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_USE_STATEMENT);
		}
		try(Statement statement = connection.createStatement()){
			String sql = String.format("select id from employee_payroll"
					+ " where name = %s", name);
			ResultSet resultSet = statement.executeQuery(sql);
			employeeId = resultSet.getInt("id");
		}
		catch(SQLException e)
		{
			try {
				connection.rollback();
			} catch (SQLException e1) {
				throw new CustomJDBCException(ExceptionType.UNABLE_TO_ROLLBACK);
			}
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_GET_ID);
		}
		try(Statement statement = connection.createStatement()){
			double deductions = salary * 0.2;
			double taxablePay = salary - deductions;
			double tax = taxablePay * 0.1;
			double netPay = salary - tax;
			String sql = String.format("update payroll_details set basic_pay = %s,"
					+ "deductions = %s, taxable_pay = %s, tax = %s, net_pay = %s where employee_id = %s", deductions, taxablePay, tax, netPay, employeeId);
			rowsAffected = statement.executeUpdate(sql);
		} catch(SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				throw new CustomJDBCException(ExceptionType.UNABLE_TO_ROLLBACK);
			}
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_ADD_RECORD_TO_DB);
		}
		try {
			connection.commit();
		} catch (SQLException e) {

			throw new CustomJDBCException(ExceptionType.UNABLE_TO_COMMIT);
		}
		return rowsAffected;
	}

	public List<EmployeePayrollData> getEmployeePayrollDataInDateRange(LocalDate startDate, LocalDate endDate) throws CustomJDBCException {
		String sql = String.format("select * from employee_payroll inner "
				+ "join company_details on employee_payroll.company_id = "
				+ "company_details.company_id inner join department_employee on "
				+ "employee_payroll.id = department_employee.employee_id inner join "
				+ "department on department.department_id = department_employee.department_id "
				+ "where start between cast('%s' as date) and cast('%s' as date);",
				startDate.toString(), endDate.toString());
		try (Connection connection = this.getConnection()) {
			Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			ResultSet resultSet = statement.executeQuery(sql);
			return this.getEmployeePayrollListFromResultSet(resultSet);
		} catch (SQLException e) {
			throw new CustomJDBCException(ExceptionType.SQL_EXCEPTION);
		}
	}

	public QueryResultStructure performSQLFunction(SQLFunctionType functionType) throws CustomJDBCException {
		String sql = String.format("select gender, %s(salary) from employee_payroll group by gender", functionType.toString());
		try(Connection connection = this.getConnection()){
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(sql);
			double maleGroupOutput = 0;
			double femaleGroupOutput = 0;
			while(resultSet.next()) {
				if(resultSet.getString("gender").equals("M")) 
					maleGroupOutput = resultSet.getDouble(String.format("%s(salary)", functionType.toString()));
				else femaleGroupOutput = resultSet.getDouble(String.format("%s(salary)", functionType.toString()));
			}
			return new QueryResultStructure(functionType, maleGroupOutput, femaleGroupOutput);
		} catch (SQLException e) {
			throw new CustomJDBCException(ExceptionType.SQL_EXCEPTION);
		}
	}

	public EmployeePayrollData addEmployeeToPayrollUC7(String name, double salary, LocalDate startDate, String gender) throws CustomJDBCException {
		int employeeId = -1;
		EmployeePayrollData employeePayrollData = null;
		String sql = String.format("insert into employee_payroll (name, gender, salary, start) " +
				"values ('%s', '%s', %s, '%s')", name, gender, salary, Date.valueOf(startDate));
		try(Connection connection = this.getConnection()){
			Statement statement = connection.createStatement();
			int rowAffected = statement.executeUpdate(sql, statement.RETURN_GENERATED_KEYS);
			if(rowAffected == 1) {
				ResultSet resultSet = statement.getGeneratedKeys();
				if(resultSet.next()) employeeId = resultSet.getInt(1);
			}
			employeePayrollData = new EmployeePayrollData(employeeId, name, gender, salary, startDate);
		} catch (SQLException e) {
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_ADD_RECORD_TO_DB);
		}
		return employeePayrollData;
	}

	public EmployeePayrollData addEmployeeToPayroll(String name, double salary, LocalDate startDate, String gender, int companyId, String companyName, int departmentId) throws CustomJDBCException  {
		int employeeId = -1;
		Connection connection = null;
		EmployeePayrollData employeePayrollData = null;
		connection = this.getConnection();
		try {
			connection.setAutoCommit(false);
		} catch (SQLException e1) {
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_SET_AUTO_COMMIT);
		}
		try(Statement statement = connection.createStatement()){
			String sql = String.format("insert into employee_payroll (name, gender, salary, start, company_id) " +
					"values ('%s', '%s', %s, '%s', %s)", name, gender, salary, Date.valueOf(startDate), companyId);
			int rowAffected = statement.executeUpdate(sql, statement.RETURN_GENERATED_KEYS);
			if(rowAffected == 1) {
				ResultSet resultSet = statement.getGeneratedKeys();
				if(resultSet.next()) employeeId = resultSet.getInt(1);
				resultSet.close();
			}

		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				throw new CustomJDBCException(ExceptionType.UNABLE_TO_ROLLBACK);
			}
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_ADD_RECORD_TO_DB);
		}
		try(Statement statement = connection.createStatement()){
			double deductions = salary * 0.2;
			double taxablePay = salary - deductions;
			double tax = taxablePay * 0.1;
			double netPay = salary - tax;
			String sqlOne = String.format("insert into payroll_details " + 
					"(employee_id, basic_pay, deductions, taxable_pay, tax, net_pay) values " +
					"(%s, %s, %s, %s, %s, %s)", employeeId, salary, deductions, taxablePay, tax, netPay);
			statement.executeUpdate(sqlOne);
		} catch(SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				throw new CustomJDBCException(ExceptionType.UNABLE_TO_ROLLBACK);
			}
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_ADD_RECORD_TO_DB);
		}
		try(Statement statement = connection.createStatement()){
			String sql = String.format("insert into department_employee " + 
					" values " +
					"(%s, %s)", departmentId, employeeId);
			int rowAffected = statement.executeUpdate(sql);
			if(rowAffected == 1) {
				String sqlTwo = String.format("select department_name from department where department_id = %s", departmentId);
				ResultSet resultSet = statement.executeQuery(sqlTwo);
				resultSet.next();
				List<String> departmentNames = new ArrayList<>();
				String departmentName = resultSet.getString("department_name");
				departmentNames.add(departmentName);
				employeePayrollData = new EmployeePayrollData(employeeId, name, gender, salary, startDate, companyName, companyId, departmentNames);
			}
		} catch(SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				throw new CustomJDBCException(ExceptionType.UNABLE_TO_ROLLBACK);
			}
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_ADD_RECORD_TO_DB);
		}
		try {
			connection.commit();
		} catch (SQLException e) {
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_COMMIT);
		} finally {
			if(connection != null)
				try {
					connection.close();
				} catch (SQLException e) {
					throw new CustomJDBCException(ExceptionType.UNABLE_TO_CLOSE_CONNECTION);
				}
		}
		return employeePayrollData;
	}

	public Object deleteEmployeeFromPayroll(String name) throws CustomJDBCException {
		Connection connection = null;
		EmployeePayrollData employeePayrollData = null;
		List<EmployeePayrollData> payrollList = new ArrayList<>();
		connection = this.getConnection();
		try {
			connection.setAutoCommit(false);
		} catch (SQLException e1) {
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_SET_AUTO_COMMIT);
		}
		try(Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)){
			String sqlOne = String.format("select * from employee_payroll inner join company_details on "
					+ "employee_payroll.company_id = company_details.company_id inner join "
					+ "department_employee on employee_payroll.id = department_employee.employee_id "
					+ "inner join department on department.department_id = "
					+ "department_employee.department_id where name = '%s'", name);
			ResultSet resultSetOne = statement.executeQuery(sqlOne);
			payrollList = getEmployeePayrollListFromResultSet(resultSetOne);
			String sqlTwo = String.format("delete from department_employee where employee_id = %s", payrollList.get(0).id);
			resultSetOne.close();
			statement.executeUpdate(sqlTwo);
		} catch (SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				throw new CustomJDBCException(ExceptionType.UNABLE_TO_ROLLBACK);
			}
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_DELETE_RECORD);
		}
		try(Statement statement = connection.createStatement()){
			String sql = String.format("update employee_payroll set is_active = false where id = %s", payrollList.get(0).id);
			int rowsAffected = statement.executeUpdate(sql);
			if(rowsAffected == 1) {
				employeePayrollData = new EmployeePayrollData(payrollList.get(0).id, payrollList.get(0).name, payrollList.get(0).gender,
						payrollList.get(0).salary, payrollList.get(0).startDate, payrollList.get(0).companyName, 
						payrollList.get(0).companyId, payrollList.get(0).departmentNames);
			}
		} catch(SQLException e) {
			try {
				connection.rollback();
			} catch (SQLException e1) {
				throw new CustomJDBCException(ExceptionType.UNABLE_TO_ROLLBACK);
			}
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_DELETE_RECORD);
		}

		try {
			connection.commit();
		} catch (SQLException e) {
			throw new CustomJDBCException(ExceptionType.UNABLE_TO_COMMIT);
		} finally {
			if(connection != null)
				try {
					connection.close();
				} catch (SQLException e) {
					throw new CustomJDBCException(ExceptionType.UNABLE_TO_CLOSE_CONNECTION);
				}
		}
		return employeePayrollData;
	}

	public boolean checkIfEmployeeActive(String employeeName) throws CustomJDBCException {
		Connection connection = this.getConnection();
		boolean status;
		try {
			Statement statement = connection.createStatement();
			String sql = String.format("select is_active from employee_payroll where name = '%s'", employeeName);
			ResultSet resultSet = statement.executeQuery(sql);
			resultSet.next();
			int isActive = resultSet.getInt("is_active");
			status = (isActive == 0) ? false  : true;  
		} catch (SQLException e) {
			throw new CustomJDBCException(ExceptionType.SQL_EXCEPTION);
		}
		return status;
	}	
}