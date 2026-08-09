package br.edu.ifpe.batepapo.dto;

public class ChatMessageResponse {

	private final String type;
	private final String sender;
	private final int senderPeriodo;
	private final int roomId;
	private final String text;
	private final String timestamp;

	public ChatMessageResponse(String type, String sender, int senderPeriodo, int roomId, String text,
			String timestamp) {
		this.type = type;
		this.sender = sender;
		this.senderPeriodo = senderPeriodo;
		this.roomId = roomId;
		this.text = text;
		this.timestamp = timestamp;
	}

	public String getType() {
		return type;
	}

	public String getSender() {
		return sender;
	}

	public int getSenderPeriodo() {
		return senderPeriodo;
	}

	public int getRoomId() {
		return roomId;
	}

	public String getText() {
		return text;
	}

	public String getTimestamp() {
		return timestamp;
	}
}