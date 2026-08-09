package br.edu.ifpe.batepapo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import br.edu.ifpe.batepapo.dto.RoomResponse;

@Service
public class RoomService {

	public enum Room {
		PERIODO_1(1, "Período 1"),
		PERIODO_2(2, "Período 2"),
		PERIODO_3(3, "Período 3"),
		PERIODO_4(4, "Período 4"),
		PERIODO_5(5, "Período 5"),
		PERIODO_6(6, "Período 6");

		private final int id;
		private final String name;

		Room(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public static Room fromId(int id) {
			for (Room room : values()) {
				if (room.id == id) {
					return room;
				}
			}
			return null;
		}

		public static List<Room> all() {
			List<Room> rooms = new ArrayList<>();
			for (Room room : values()) {
				rooms.add(room);
			}
			return rooms;
		}
	}

	public List<RoomResponse> listRooms() {
		List<RoomResponse> responses = new ArrayList<>();
		for (Room room : Room.all()) {
			responses.add(new RoomResponse(room.getId(), room.getName()));
		}
		return responses;
	}
}