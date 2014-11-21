package com.moonlite.discover;

/**
 * 
 * @author Dahai Li
 * 
 * Discover is an interface for discover network hosts.
 */
public interface Discover {
	Host[] discover (Object target);
}
