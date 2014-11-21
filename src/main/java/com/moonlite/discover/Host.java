package com.moonlite.discover;

/**
 * 
 * @author Dahai Li
 *
 * Host represent a host device 
 */
public class Host {
	String address = "";
	String name = "";
	String os = "";
	
	public Host (String address, String name, String os) {
		this.address = address;
		this.name = name;
		this.os = os;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			return true;
		}
		if (!(obj instanceof Host)) {
			return false;
		}
		Host other = (Host)obj;
		return address.equals(other.address) &&
				name.equals(other.name) &&
				os.equals(other.os);
	}
}
