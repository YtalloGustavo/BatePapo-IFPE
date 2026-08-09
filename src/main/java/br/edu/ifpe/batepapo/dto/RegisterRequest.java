package br.edu.ifpe.batepapo.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class RegisterRequest {

	@NotBlank
	@Size(min = 3, max = 20)
	@Pattern(regexp = "[a-zA-Z0-9_]+")
	private String username;

	@NotBlank
	@Size(min = 6)
	private String password;

	@NotBlank
	@Size(max = 60)
	private String name;

	@NotNull
	@Min(1)
	@Max(6)
	private Integer periodo;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getPeriodo() {
		return periodo;
	}

	public void setPeriodo(Integer periodo) {
		this.periodo = periodo;
	}
}