package br.edu.ifpe.batepapo.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.edu.ifpe.batepapo.entity.Student;
import br.edu.ifpe.batepapo.repository.StudentRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	private final StudentRepository studentRepository;

	public UserDetailsServiceImpl(StudentRepository studentRepository) {
		this.studentRepository = studentRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Student student = studentRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
		return new User(student.getUsername(), student.getPassword(),
				List.of(new SimpleGrantedAuthority(student.getRole().name())));
	}
}