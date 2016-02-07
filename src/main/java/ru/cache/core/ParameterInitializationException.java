package ru.cache.core;

/**
 * Thrown when parameter for Caching Url object being initialized.
 * 
 * @author Kaplya Natal
 *
 */
public class ParameterInitializationException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8902017323055614688L;

	public ParameterInitializationException(String message) {
		super(message);
	}

	public ParameterInitializationException(String message, Throwable cause) {
		super(message, cause);
	}
}
