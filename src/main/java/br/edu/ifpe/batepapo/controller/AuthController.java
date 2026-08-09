package br.edu.ifpe.batepapo.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.edu.ifpe.batepapo.dto.ErrorResponse;
import br.edu.ifpe.batepapo.dto.LoginRequest;
import br.edu.ifpe.batepapo.dto.RegisterRequest;
import br.edu.ifpe.batepapo.dto.UserResponse;
import br.edu.ifpe.batepapo.entity.Role;
import br.edu.ifpe.batepapo.entity.Student;
import br.edu.ifpe.batepapo.repository.StudentRepository;

@RestController
@RequestMapping("/api")
public class AuthController {

	private final StudentRepository studentRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;

	public AuthController(StudentRepository studentRepository, PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager) {
		this.studentRepository = studentRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
	}

	@PostMapping("/register")
	public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
		if (studentRepository.existsByUsername(request.getUsername())) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(new ErrorResponse("Username já cadastrado"));
		}
		Student student = new Student(request.getUsername(), passwordEncoder.encode(request.getPassword()),
				request.getName(), request.getPeriodo(), Role.ROLE_USER);
		student = studentRepository.save(student);
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
		SecurityContextHolder.getContext().setAuthentication(authentication);
		httpRequest.getSession(true);
		return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponse(student));
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
			SecurityContextHolder.getContext().setAuthentication(authentication);
			httpRequest.getSession(true);
			httpRequest.changeSessionId();
			Student student = studentRepository.findByUsername(request.getUsername())
					.orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));
			return ResponseEntity.ok(toUserResponse(student));
		} catch (BadCredentialsException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new ErrorResponse("Credenciais inválidas"));
		}
	}

	@GetMapping("/me")
	public UserResponse me() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = authentication.getName();
		Student student = studentRepository.findByUsername(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
		return toUserResponse(student);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
		logoutHandler.logout(request, response, SecurityContextHolder.getContext().getAuthentication());
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	private UserResponse toUserResponse(Student student) {
		return new UserResponse(student.getId(), student.getUsername(), student.getName(), student.getPeriodo(),
				student.getRole());
	}
}