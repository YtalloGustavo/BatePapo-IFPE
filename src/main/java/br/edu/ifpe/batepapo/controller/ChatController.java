package br.edu.ifpe.batepapo.controller;

import java.security.Principal;
import java.time.Instant;

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
		if (principal == null) {
			return;
		}

		Room room = Room.fromId(request.getRoomId());
		if (room == null) {
			return;
		}

		String text = request.getText();
		if (text == null || text.trim().isEmpty() || text.length() > 500) {
			return;
		}

		Student student = studentRepository.findByUsername(principal.getName()).orElse(null);
		if (student == null) {
			return;
		}

		ChatMessageResponse response = new ChatMessageResponse("CHAT", student.getName(), student.getPeriodo(),
				request.getRoomId(), text.trim(), Instant.now().toString());

		simpMessagingTemplate.convertAndSend("/topic/room." + request.getRoomId(), response);
	}
}