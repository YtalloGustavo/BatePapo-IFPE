package br.edu.ifpe.batepapo.dto;

public class RoomResponse {

	private final int id;
	private final String name;

	public RoomResponse(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}