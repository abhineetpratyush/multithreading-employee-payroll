package com.capgemini.employeepayrollmultithreading;

import java.time.LocalDate;

public class EmployeePayrollDataStructrureForThread {
	public String name;
	public double salary;
	public LocalDate startDate;
	public String gender;
	public String companyName;
	public int companyId;
	public int departmentId;
	
	public EmployeePayrollDataStructrureForThread(String name, double salary, LocalDate startDate, String gender,
			String companyName, int companyId, int departmentId) {
		this.name = name;
		this.salary = salary;
		this.startDate = startDate;
		this.gender = gender;
		this.companyName = companyName;
		this.companyId = companyId;
		this.departmentId = departmentId;
	}
}
