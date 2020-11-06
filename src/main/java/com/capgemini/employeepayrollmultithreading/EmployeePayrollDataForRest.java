package com.capgemini.employeepayrollmultithreading;

public class EmployeePayrollDataForRest {
	public int id;
	public String name;
	public double salary;
	public String startDate;
	public String gender;
	public EmployeePayrollDataForRest(int id, String name, double salary, String startDate, String gender) {
		this.id = id;
		this.name = name;
		this.salary = salary;
		this.startDate = startDate;
		this.gender = gender;
	}
}
