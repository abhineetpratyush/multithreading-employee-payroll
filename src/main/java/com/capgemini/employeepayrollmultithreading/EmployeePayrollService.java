package com.capgemini.employeepayrollmultithreading;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class EmployeePayrollService {

	public enum IOService {
		CONSOLE_IO, FILE_IO, DB_IO, REST_IO
	}
	private EmployeePayrollDBService employeePayrollDBService;
	private List<EmployeePayrollData> employeePayrollList;
	private List<EmployeePayrollDataForRest> employeePayrollListForRest;
	private static final Logger log = LogManager.getLogger(EmployeePayrollService.class);
	
	public EmployeePayrollService() {
		this.employeePayrollDBService = EmployeePayrollDBService.getInstance();
	}

	public EmployeePayrollService(List<EmployeePayrollDataForRest> employeePayrollListForRest) {
		this.employeePayrollListForRest = new ArrayList<>(employeePayrollListForRest);
	}

	public List<EmployeePayrollData> readEmployeePayrollData(IOService ioService) throws CustomJDBCException {
		if(ioService.equals(IOService.DB_IO))
			this.employeePayrollList = employeePayrollDBService.readData();
		return this.employeePayrollList;
	}

	public void updateEmployeeSalary(String name, double salary) throws CustomJDBCException {
		int result = employeePayrollDBService.updateEmployeeData(name, salary);
		if (result == 0) 
			throw new CustomJDBCException(ExceptionType.RECORD_UPDATE_FAILURE);
		EmployeePayrollData employeePayrollData = this.getEmployeePayrollData(name);
		if(employeePayrollData != null) 
			employeePayrollData.salary = salary;
	}

	private EmployeePayrollData getEmployeePayrollData(String name) {
		return this.employeePayrollList.stream()
				.filter(employeePayrollDataItem -> employeePayrollDataItem.name.equals(name))
				.findFirst()
				.orElse(null);
	}

	public boolean checkEmployeePayrollInSyncWithDB(String name) throws CustomJDBCException {
		List<EmployeePayrollData> employeePayrollDataList = employeePayrollDBService.getEmployeePayrollDataFromDB(name);
		return employeePayrollDataList.get(0).equals(getEmployeePayrollData(name));
	}

	public List<EmployeePayrollData> getEmployeePayrollDataInDateRange(LocalDate startDate, LocalDate endDate) throws CustomJDBCException {
		return this.employeePayrollDBService.getEmployeePayrollDataInDateRange(startDate, endDate);
	}

	public QueryResultStructure performSQLFunction(SQLFunctionType functionType) throws CustomJDBCException {
		return this.employeePayrollDBService.performSQLFunction(functionType);
	}

	public void addEmployeeToPayroll(String name, double salary, LocalDate startDate, String gender, int companyId, String companyName, int departmentId) throws CustomJDBCException {
		employeePayrollList.add(employeePayrollDBService.addEmployeeToPayroll(name, salary, startDate, gender, companyId, companyName, departmentId));
	}
	
	public void addEmployeeToPayroll(EmployeePayrollDataForRest employeePayrollData, IOService ioService) {
		if(ioService.equals(IOService.REST_IO)) {
			this.addEmployeeToPayroll(employeePayrollData.id, employeePayrollData.name, employeePayrollData.salary, 
					employeePayrollData.startDate, employeePayrollData.gender);
		}
		
	}

	private void addEmployeeToPayroll(int id, String name, double salary, String startDate, String gender) {
		employeePayrollListForRest.add(new EmployeePayrollDataForRest(id, name, salary, startDate, gender));
	}

	public void deleteEmployeeFromPayroll(String name) throws CustomJDBCException {
		employeePayrollList.remove(employeePayrollDBService.deleteEmployeeFromPayroll(name));
	}

	public boolean checkEmployeeDeleted(String employeeName) throws CustomJDBCException {
		boolean listCheck = employeePayrollList.stream().anyMatch(employee -> employee.name.equals(employeeName));
		boolean databaseCheck = employeePayrollDBService.checkIfEmployeeActive(employeeName);
		if(listCheck == false && databaseCheck == false)
			return false;
		else 
			return true;
	}

	public void addEmployeesToPayroll(List<EmployeePayrollDataStructrureForThread> employeePayrollDataList) {
		employeePayrollDataList.forEach(
				employeePayrollData -> {
					log.info("Employee Being Added: " + employeePayrollData.name);
					try {
						this.addEmployeeToPayroll(employeePayrollData.name, employeePayrollData.salary, employeePayrollData.startDate, 
								employeePayrollData.gender, employeePayrollData.companyId, employeePayrollData.companyName, employeePayrollData.departmentId);
					} catch (CustomJDBCException e) {
						log.info("Unable to add employee to DB");
					}
					log.info("Employee Added: "+ employeePayrollData.name);
				}
				);
		log.info(this.employeePayrollList);
	}

	public int countEntries() {
		return employeePayrollList.size();
	}

	public void addEmployeesToPayrollWithThreads(List<EmployeePayrollDataStructrureForThread> employeePayrollDataList) {
		Map<Integer, Boolean> employeeAdditionStatus = new HashMap<>();
		employeePayrollDataList.forEach(employeePayrollData ->
		{
			Runnable task = () -> {
				employeeAdditionStatus.put(employeePayrollData.hashCode(), false);
				log.info("Employee Being Added: "+Thread.currentThread().getName());
				try {
					this.addEmployeeToPayroll(employeePayrollData.name, employeePayrollData.salary, employeePayrollData.startDate, 
							employeePayrollData.gender, employeePayrollData.companyId, employeePayrollData.companyName, employeePayrollData.departmentId);
				} catch (CustomJDBCException e) {
					log.info("Unable to add employee to DB");
				}
				employeeAdditionStatus.put(employeePayrollData.hashCode(), true);
				log.info("Employee Added: " + Thread.currentThread().getName());
			};
			Thread thread = new Thread(task, employeePayrollData.name);
			thread.start();
		}
				);
		while(employeeAdditionStatus.containsValue(false)) {
			try {
				Thread.sleep(10);
			} catch(InterruptedException e) {
				log.info("Unable to sleep");
			}
		}
	}

	public void updateEmployeesSalary(List<EmployeeSalaryNameStructure> employeeNameAndSalaryList) {
		Map<Integer, Boolean> employeeAdditionStatus = new HashMap<>();
		employeeNameAndSalaryList.forEach(employeeData ->
		{
			Runnable task = () -> {
				employeeAdditionStatus.put(employeeData.hashCode(), false);
				log.info("Salary being updated "+Thread.currentThread().getName());
				try {
					this.updateEmployeeSalary(employeeData.name, employeeData.salary);
				} catch (CustomJDBCException e) {
					log.info("Unable to update salary of employee");
				}
				employeeAdditionStatus.put(employeeData.hashCode(), true);
				log.info("Salary Updated: " + Thread.currentThread().getName());
			};
			Thread thread = new Thread(task, employeeData.name);
			thread.start();
		}
				);
		while(employeeAdditionStatus.containsValue(false)) {
			try {
				Thread.sleep(10);
			} catch(InterruptedException e) {
				log.info("Unable to sleep");
			}
		}
	}

	public int countEntries(IOService ioService) {
		if(ioService.equals(IOService.REST_IO))
			return employeePayrollListForRest.size();
		return 0;
	}
}