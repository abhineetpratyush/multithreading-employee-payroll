package com.capgemini.employeepayrollmultithreading;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.capgemini.employeepayrollmultithreading.EmployeePayrollService.IOService;


public class JDBCEmployeePayrollTest {
	private static final Logger log = LogManager.getLogger(JDBCEmployeePayrollTest.class);
	public EmployeePayrollService employeePayrollService;

	@Before
	public void initialise() {
		this.employeePayrollService = new EmployeePayrollService();
	}


	@Test
	public void givenEmployeePayrollInDB_WhenRetrieved_ShouldMatchEmployeeCount() throws CustomJDBCException {
		List<EmployeePayrollData> employeePayrollData = employeePayrollService.readEmployeePayrollData(IOService.DB_IO);
		Assert.assertEquals(3, employeePayrollData.size());
	}

	@Test
	public void givenNewSalaryForEmployee_WhenUpdated_ShouldSyncWithDB() throws CustomJDBCException {
		employeePayrollService.readEmployeePayrollData(IOService.DB_IO);
		employeePayrollService.updateEmployeeSalary("Terissa", 3000000.00);
		boolean result = employeePayrollService.checkEmployeePayrollInSyncWithDB("Terissa");
		Assert.assertTrue(result);
	}

	@Test
	public void givenNewSalaryForEmployee_WhenUpdatedUsingPreparedStatement_ShouldSyncWithDB() throws CustomJDBCException {
		employeePayrollService.readEmployeePayrollData(IOService.DB_IO);
		employeePayrollService.updateEmployeeSalary("Terissa", 3000000.00);
		boolean result = employeePayrollService.checkEmployeePayrollInSyncWithDB("Terissa");
		Assert.assertTrue(result);
	}

	@Test
	public void givenEmployeePayrollInDB_WhenRetrievedOnDateRange_ShouldPassTheTest() throws CustomJDBCException {
		employeePayrollService.readEmployeePayrollData(IOService.DB_IO);
		LocalDate startDate = LocalDate.parse("2018-01-31");
		LocalDate endDate = LocalDate.parse("2020-02-02");
		List<EmployeePayrollData> employeeList = this.employeePayrollService.getEmployeePayrollDataInDateRange(startDate, endDate);
		Assert.assertEquals(1, employeeList.size());
	}

	@Test
	public void givenEmployeePayrollInDB_WhenSQLFunctionsPerformed_ShouldPassTheTest() throws CustomJDBCException {
		employeePayrollService.readEmployeePayrollData(IOService.DB_IO);
		QueryResultStructure result = this.employeePayrollService.performSQLFunction(SQLFunctionType.SUM);
		Assert.assertEquals(9000000.00, result.maleGroupOutput, 0);
		Assert.assertEquals(3000000.00, result.femaleGroupOutput, 0);
	}

	@Test
	public void givenNewEmployee_WhenAdded_ShouldSyncWithDB() throws CustomJDBCException {
		employeePayrollService.readEmployeePayrollData(IOService.DB_IO);
		employeePayrollService.addEmployeeToPayroll("Mark", 5000000.00, LocalDate.now(), "M", 1, "Capgemini", 3);
		boolean result = employeePayrollService.checkEmployeePayrollInSyncWithDB("Mark");
		Assert.assertTrue(result);
	}

	@Test
	public void givenEmployee_WhenDeleted_ShouldSyncWithDB() throws CustomJDBCException {
		employeePayrollService.readEmployeePayrollData(IOService.DB_IO);
		employeePayrollService.deleteEmployeeFromPayroll("Mark");
		boolean result = employeePayrollService.checkEmployeeDeleted("Mark");
		Assert.assertFalse(result);
	}
	
	@Test
	public void given6Employees_WhenAddedToDB_ShouldMatchNoOfEntries() throws CustomJDBCException {
		EmployeePayrollDataStructrureForThread[] arrayOfEmps = {
												new EmployeePayrollDataStructrureForThread("Jeff Bezos", 100000.00, LocalDate.now(), "M", "Capgemini", 1, 2),
												new EmployeePayrollDataStructrureForThread("Bill Gates", 200000.00, LocalDate.now(), "M", "Cisco", 2, 1),
												new EmployeePayrollDataStructrureForThread("Anil", 300000.00, LocalDate.now(), "M", "Capgemini", 1, 4),
												new EmployeePayrollDataStructrureForThread("Sundar", 400000.00, LocalDate.now(), "M", "Cisco", 2, 3),
												new EmployeePayrollDataStructrureForThread("Melinda", 500000.00, LocalDate.now(), "F", "Cisco", 1, 2),
												new EmployeePayrollDataStructrureForThread("Kim Jon", 600000.00, LocalDate.now(), "M", "Capgemini", 2, 1)};
												
		employeePayrollService.readEmployeePayrollData(IOService.DB_IO);
		Instant start = Instant.now();
		employeePayrollService.addEmployeesToPayroll(Arrays.asList(arrayOfEmps));
		Instant end = Instant.now();
		log.info("Duration without thread: "+ Duration.between(start, end));
		Assert.assertEquals(12, employeePayrollService.countEntries());
	}
}
