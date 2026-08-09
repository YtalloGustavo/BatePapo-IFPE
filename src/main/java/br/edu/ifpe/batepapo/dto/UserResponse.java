package br.edu.ifpe.batepapo.dto;

import br.edu.ifpe.batepapo.entity.Role;

public class UserResponse {

	private final Long id;
	private final String username;
	private final String name;
	private final Integer periodo;
	private final Role role;

	public UserResponse(Long id, String username, String name, Integer periodo, Role role) {
		this.id = id;
		this.username = username;
		this.name = name;
		this.periodo = periodo;
		this.role = role;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getName() {
		return name;
	}

	public Integer getPeriodo() {
		return periodo;
	}

	public Role getRole() {
		return role;
	}
}