package br.edu.ifpe.batepapo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifpe.batepapo.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

	Optional<Student> findByUsername(String username);

	boolean existsByUsername(String username);
}