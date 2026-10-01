package com.shashank.example.smartbank.exception;

public class DuplicateEmailException extends RuntimeException {

	public DuplicateEmailException(String message) {
		super(message);
	}
}
