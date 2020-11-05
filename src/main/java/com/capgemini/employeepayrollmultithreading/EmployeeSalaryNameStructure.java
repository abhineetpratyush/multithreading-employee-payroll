package com.capgemini.employeepayrollmultithreading;

public class EmployeeSalaryNameStructure {
	public String name;
	public double salary;
	public EmployeeSalaryNameStructure(String name, double salary) {
		this.name = name;
		this.salary = salary;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		long temp;
		temp = Double.doubleToLongBits(salary);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
}
