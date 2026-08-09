package br.edu.ifpe.batepapo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifpe.batepapo.dto.RoomResponse;
import br.edu.ifpe.batepapo.service.RoomService;

@RestController
@RequestMapping("/api")
public class RoomController {

	private final RoomService roomService;

	public RoomController(RoomService roomService) {
		this.roomService = roomService;
	}

	@GetMapping("/rooms")
	public List<RoomResponse> listRooms() {
		return roomService.listRooms();
	}
}