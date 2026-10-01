package com.shashank.example.smartbank.service;

import java.util.List;

import com.shashank.example.smartbank.model.User;

public interface UserService {

	User createUser(String name, String email);

	User getUserById(Long id);

	List<User> getAllUsers();

	User updateUser(Long id, String name, String email);

	void deleteUser(Long id);
}
