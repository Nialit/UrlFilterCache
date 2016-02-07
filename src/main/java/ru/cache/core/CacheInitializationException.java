/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.cache.core;

/**
 *
 * @author Natal Kaplya
 */
public class CacheInitializationException extends Exception {

	private static final long serialVersionUID = -5447855979286018564L;

	/**
	 * Creates a new instance of <code>CacheInitializationException</code>
	 * without detail message.
	 */
	public CacheInitializationException() {
	}

	public CacheInitializationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs an instance of <code>CacheInitializationException</code> with
	 * the specified detail message.
	 *
	 * @param msg
	 *            the detail message.
	 */
	public CacheInitializationException(String msg) {
		super(msg);
	}
}
