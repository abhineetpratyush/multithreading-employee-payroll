package com.capgemini.employeepayrollmultithreading;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.apache.groovy.json.internal.IO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.capgemini.employeepayrollmultithreading.EmployeePayrollService.IOService;
import com.google.gson.Gson;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;


public class JDBCEmployeePayrollTest {
	private static final Logger log = LogManager.getLogger(JDBCEmployeePayrollTest.class);
	public EmployeePayrollService employeePayrollService;

	@Before
	public void initialise() {
		this.employeePayrollService = new EmployeePayrollService();
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = 3000;
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
		Instant threadStart = Instant.now();
		employeePayrollService.addEmployeesToPayrollWithThreads(Arrays.asList(arrayOfEmps));
		Instant threadEnd = Instant.now();
		log.info("Duration with thread: "+ Duration.between(threadStart, threadEnd));
		Assert.assertEquals(21, employeePayrollService.countEntries());
		
	}
	
	@Test
	public void givenNewSalaryForMultipleEmployees_WhenUpdatedUsingMultithreading_ShouldSyncWithDB() throws CustomJDBCException {
		employeePayrollService.readEmployeePayrollData(IOService.DB_IO);
		EmployeeSalaryNameStructure[] arrayOfNameAndSalary = {
													new EmployeeSalaryNameStructure("Terissa", 4000000.00),
													new EmployeeSalaryNameStructure("Bill", 2100000.00),
													new EmployeeSalaryNameStructure("Charlie", 3400000.00)};
		employeePayrollService.updateEmployeesSalary(Arrays.asList(arrayOfNameAndSalary));
		boolean resultOne = employeePayrollService.checkEmployeePayrollInSyncWithDB("Terissa");
		boolean resultTwo = employeePayrollService.checkEmployeePayrollInSyncWithDB("Bill");
		boolean resultThree = employeePayrollService.checkEmployeePayrollInSyncWithDB("Charlie");
		Assert.assertTrue(resultOne && resultTwo && resultThree);
	}
	
	@Test
	public void givenEmployeeDataInJsonServer_WhenRetrieved_ShouldMatchCount() {
		EmployeePayrollDataForRest[] arraysOfEmps = getEmployeeList();
		employeePayrollService = new EmployeePayrollService(Arrays.asList(arraysOfEmps));
		int entries = employeePayrollService.countEntries(IOService.REST_IO);
		Assert.assertEquals(2, entries);
	}

	private EmployeePayrollDataForRest[] getEmployeeList() {
		Response response = RestAssured.get("/employee_payroll");
		System.out.println("EMPLOYEE PAYROLL ENTRIES IN JSON SERVER\n" + response.asString());
		EmployeePayrollDataForRest[] arrayOfEmps = new Gson().fromJson(response.asString(), EmployeePayrollDataForRest[].class);
		return arrayOfEmps;
	}
	
	private Response addEmployeeToJsonServer(EmployeePayrollDataForRest employeePayrollData) {
		String empJson = new Gson().toJson(employeePayrollData);
		RequestSpecification request = RestAssured.given();
		request.header("Content-Type", "application/json");
		request.body(empJson);
		return request.post("/employee_payroll");
	}
	@Test
	public void givenNewEmployee_WhenAdded_ShouldMatch201ResponseAndCount() {
		EmployeePayrollDataForRest[] arraysOfEmps = getEmployeeList();
		employeePayrollService = new EmployeePayrollService(Arrays.asList(arraysOfEmps));
		EmployeePayrollDataForRest employeePayrollData = new EmployeePayrollDataForRest(3, "Shruti Sharma", 3500.00, "2020-01-05", "F");
		Response response = addEmployeeToJsonServer(employeePayrollData);
		int statusCode = response.getStatusCode();
		Assert.assertEquals(201, statusCode);
		
		employeePayrollData = new Gson().fromJson(response.asString(), EmployeePayrollDataForRest.class);
		employeePayrollService.addEmployeeToPayroll(employeePayrollData, IOService.REST_IO);
		int entries = employeePayrollService.countEntries(IOService.REST_IO);
		Assert.assertEquals(3, entries);
	}
	
	@Test
	public void givenMuiltipleNewEmployees_WhenAdded_ShouldMatch201ResponseAndCount() {
		EmployeePayrollDataForRest[] arraysOfEmps = getEmployeeList();
		employeePayrollService = new EmployeePayrollService(Arrays.asList(arraysOfEmps));
		EmployeePayrollDataForRest[] arrayOfemployeePayrollData = {
				new EmployeePayrollDataForRest(4, "Rohan Sharma", 3400.00, "2019-01-05", "F"),
				new EmployeePayrollDataForRest(5, "Batman Kumar", 2500.00, "2017-01-04", "M"),
				new EmployeePayrollDataForRest(6, "Spiderman Singh", 9000.00, "2018-01-23", "M"),
		};
		for(EmployeePayrollDataForRest employeePayrollData : arrayOfemployeePayrollData) {
		Response response = addEmployeeToJsonServer(employeePayrollData);
		int statusCode = response.getStatusCode();
		Assert.assertEquals(201, statusCode);
		
		employeePayrollData = new Gson().fromJson(response.asString(), EmployeePayrollDataForRest.class);
		employeePayrollService.addEmployeeToPayroll(employeePayrollData, IOService.REST_IO);
		}
		int entries = employeePayrollService.countEntries(IOService.REST_IO);
		Assert.assertEquals(6, entries);
	}

	@Test
	public void givenNewSalaryForEmployee_WhenUpdated_ShouldMatch200Response() {
		EmployeePayrollDataForRest[] arraysOfEmps = getEmployeeList();
		employeePayrollService = new EmployeePayrollService(Arrays.asList(arraysOfEmps));
		employeePayrollService.updateEmployeeSalary("Shruti Sharma", 40000.00, IOService.REST_IO);
		EmployeePayrollDataForRest employeePayrollData = employeePayrollService.getEmployeePayrollDataForRest("Shruti Sharma");
		String empJson = new Gson().toJson(employeePayrollData);
		RequestSpecification request = RestAssured.given();
		request.header("Content-Type", "application/json");
		request.body(empJson);
		Response response = request.put("/employee_payroll/"+employeePayrollData.id);
		int statusCode = response.getStatusCode();
		Assert.assertEquals(200, statusCode);
	}
	
	@Test
	public void givenEmployee_WhenDeleted_ShouldMatch200ResponseAndCount() {
		EmployeePayrollDataForRest[] arraysOfEmps = getEmployeeList();
		employeePayrollService = new EmployeePayrollService(Arrays.asList(arraysOfEmps));
		EmployeePayrollDataForRest employeePayrollData = employeePayrollService.getEmployeePayrollDataForRest("Rohan Sharma");
		RequestSpecification request = RestAssured.given();
		request.header("Content-Type", "application/json");
		Response response = request.delete("/employee_payroll/"+employeePayrollData.id);
		int statusCode = response.getStatusCode();
		Assert.assertEquals(200, statusCode);
		
		employeePayrollService.deleteEmployeePayroll(employeePayrollData.name, IOService.REST_IO);
		int entries = employeePayrollService.countEntries(IOService.REST_IO);
		Assert.assertEquals(5, entries);
	}
}
