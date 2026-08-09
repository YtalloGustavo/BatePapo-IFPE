package br.edu.ifpe.batepapo.dto;

public class ErrorResponse {

	private final String message;

	public ErrorResponse(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}