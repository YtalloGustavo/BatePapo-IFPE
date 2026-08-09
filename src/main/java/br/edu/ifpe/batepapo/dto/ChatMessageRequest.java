package br.edu.ifpe.batepapo.dto;

public class ChatMessageRequest {

	private int roomId;
	private String text;

	public ChatMessageRequest() {
	}

	public ChatMessageRequest(int roomId, String text) {
		this.roomId = roomId;
		this.text = text;
	}

	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}