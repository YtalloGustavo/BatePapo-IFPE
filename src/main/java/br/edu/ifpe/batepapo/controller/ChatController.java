package br.edu.ifpe.batepapo.controller;

import java.security.Principal;
import java.time.Instant;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import br.edu.ifpe.batepapo.dto.ChatMessageRequest;
import br.edu.ifpe.batepapo.dto.ChatMessageResponse;
import br.edu.ifpe.batepapo.entity.Student;
import br.edu.ifpe.batepapo.repository.StudentRepository;
import br.edu.ifpe.batepapo.service.RoomService.Room;

@Controller
public class ChatController {

	private final SimpMessagingTemplate simpMessagingTemplate;
	private final StudentRepository studentRepository;

	public ChatController(SimpMessagingTemplate simpMessagingTemplate, StudentRepository studentRepository) {
		this.simpMessagingTemplate = simpMessagingTemplate;
		this.studentRepository = studentRepository;
	}

	@MessageMapping("/chat.send")
	public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
		Student student = validSender(principal, request.getRoomId());
		if (student == null) {
			return;
		}

		String text = request.getText();
		if (text == null || text.trim().isEmpty() || text.length() > 500) {
			return;
		}

		ChatMessageResponse response = new ChatMessageResponse("CHAT", student.getName(), student.getPeriodo(),
				request.getRoomId(), text.trim(), Instant.now().toString());

		simpMessagingTemplate.convertAndSend("/topic/room." + request.getRoomId(), response);
	}

	@MessageMapping("/chat.join/{roomId}")
	public void joinRoom(@DestinationVariable int roomId, Principal principal) {
		Student student = validSender(principal, roomId);
		if (student == null) {
			return;
		}

		ChatMessageResponse response = new ChatMessageResponse("JOIN", student.getName(), student.getPeriodo(),
				roomId, student.getName() + " entrou na sala", Instant.now().toString());

		simpMessagingTemplate.convertAndSend("/topic/room." + roomId, response);
	}

	@MessageMapping("/chat.leave/{roomId}")
	public void leaveRoom(@DestinationVariable int roomId, Principal principal) {
		Student student = validSender(principal, roomId);
		if (student == null) {
			return;
		}

		ChatMessageResponse response = new ChatMessageResponse("LEAVE", student.getName(), student.getPeriodo(),
				roomId, student.getName() + " saiu da sala", Instant.now().toString());

		simpMessagingTemplate.convertAndSend("/topic/room." + roomId, response);
	}

	private Student validSender(Principal principal, int roomId) {
		if (principal == null) {
			return null;
		}

		Room room = Room.fromId(roomId);
		if (room == null) {
			return null;
		}

		Student student = studentRepository.findByUsername(principal.getName()).orElse(null);
		if (student == null) {
			return null;
		}
		if (student.getPeriodo() == null || student.getName() == null) {
			return null;
		}

		return student;
	}
}