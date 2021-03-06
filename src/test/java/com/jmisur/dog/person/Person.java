package com.jmisur.dog.person;

import java.math.BigDecimal;

import com.jmisur.dog.Dto;

@Dto
public class Person {

	private String firstName;

	private String lastName;

	private Address address;

	public String getFirstName() {
		System.out.println("Getting firstName " + firstName);
		return firstName;
	}

	public void setFirstName(String firstName) {
		System.out.println("Setting firstName " + firstName);
		this.firstName = firstName;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getSomeStuff(Person p, BigDecimal o) {
		String a = "xxx" + p.getFirstName();
		return "stuff" + a + o;
	}

	public boolean isAorB() {
		if (Math.random() > 0) {
			return true;
		} else {
			return false;
		}
	}

	public boolean setSomeInt(String name, Integer what) {
		firstName = name;
		return what > 0;
	}

	public boolean setSomeInt(String name, int what) {
		firstName = name;
		return what > 0;
	}

	public boolean setSomeObject(String name, Object what) {
		firstName = name;
		return true;
	}
}
