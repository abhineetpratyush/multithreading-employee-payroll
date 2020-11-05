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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + companyId;
		result = prime * result + ((companyName == null) ? 0 : companyName.hashCode());
		result = prime * result + departmentId;
		result = prime * result + ((gender == null) ? 0 : gender.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		long temp;
		temp = Double.doubleToLongBits(salary);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((startDate == null) ? 0 : startDate.hashCode());
		return result;
	}

}
