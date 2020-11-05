package com.capgemini.employeepayrollmultithreading;

import java.time.LocalDate;
import java.util.List;

public class EmployeePayrollData {
	public int id;
	public String name;
	public double salary;
	public LocalDate startDate;
	public String gender;
	public String companyName;
	public int companyId;
	public List<String> departmentNames;

	public EmployeePayrollData(Integer id, String name, Double salary) {
		this.id = id;
		this.name = name;
		this.salary = salary;
	} 

	public EmployeePayrollData(int id, String name, double salary, LocalDate startDate) {
		this(id, name, salary);
		this.startDate = startDate;
	}

	public EmployeePayrollData(int id, String name, String gender, double salary, LocalDate startDate) {
		this(id, name, salary, startDate);
		this.gender = gender;
	}

	public EmployeePayrollData(int id, String name,String gender, double salary, LocalDate startDate, String companyName, int companyId, List<String> departmentNames) {
		this(id, name, gender, salary, startDate);
		this.companyId = companyId;
		this.companyName = companyName;
		this.departmentNames = departmentNames;
	}

	public String toString() {
		return id + ", " + name + ", " + salary + ", " + startDate;
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;
		EmployeePayrollData that = (EmployeePayrollData) o;
		return id == that.id &&
				Double.compare(that.salary, salary) == 0 &&
				name.equals(that.name);		
	}
}