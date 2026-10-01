package com.shashank.example.smartbank.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shashank.example.smartbank.model.User;
import com.shashank.example.smartbank.repository.UserRepository;
import com.shashank.example.smartbank.exception.DuplicateEmailException;
import com.shashank.example.smartbank.exception.UserNotFoundException;
import com.shashank.example.smartbank.service.UserService;

@Service
@Transactional
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;

	public UserServiceImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User createUser(String name, String email) {
		if (userRepository.existsByEmail(email)) {
			throw new DuplicateEmailException("A user with this email already exists");
		}
		return userRepository.save(new User(name, email));
	}

	@Transactional(readOnly = true)
	public User getUserById(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found: " + id));
	}

	@Transactional(readOnly = true)
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	public User updateUser(Long id, String name, String email) {
		User user = getUserById(id);
		if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
			throw new DuplicateEmailException("A user with this email already exists");
		}
		user.setName(name);
		user.setEmail(email);
		return userRepository.save(user);
	}

	public void deleteUser(Long id) {
		if (!userRepository.existsById(id)) {
			throw new UserNotFoundException("User not found: " + id);
		}
		userRepository.deleteById(id);
	}
}
