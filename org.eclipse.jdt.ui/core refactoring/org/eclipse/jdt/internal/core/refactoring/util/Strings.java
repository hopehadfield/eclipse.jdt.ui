/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.util;

/**
 * Helper class to provide String manipulation functions not available in standard JDK.
 */
public class Strings {

	/**
	 * Returns <code>true</code> if the given two strings are equal.
	 * 
	 * @return <code>true</code> if the given two strings are equal; <code>
	 * 	false</code> otherwise
	 */
	public static boolean equals(String s1, char[] s2) {
		if (s1.length() != s2.length)
			return false;
		for (int i= 0; i < s2.length; i++) {
			if (s1.charAt(i) != s2[i])
				return false;
		}
		return true;
	}
}

