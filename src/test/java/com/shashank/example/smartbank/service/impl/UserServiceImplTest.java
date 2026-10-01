package com.shashank.example.smartbank.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.shashank.example.smartbank.exception.DuplicateEmailException;
import com.shashank.example.smartbank.exception.UserNotFoundException;
import com.shashank.example.smartbank.model.User;
import com.shashank.example.smartbank.repository.UserRepository;

class UserServiceImplTest {

	private UserRepository userRepository;
	private UserServiceImpl userService;

	@BeforeEach
	void setUp() {
		userRepository = org.mockito.Mockito.mock(UserRepository.class);
		userService = new UserServiceImpl(userRepository);
	}

	@Test
	void createUser_whenEmailIsAvailable_savesAndReturnsUser() {
		// Arrange
		String name = "Ada Lovelace";
		String email = "ada@example.com";
		when(userRepository.existsByEmail(email)).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		User result = userService.createUser(name, email);

		// Assert
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		assertEquals(name, userCaptor.getValue().getName());
		assertEquals(email, userCaptor.getValue().getEmail());
		assertEquals(name, result.getName());
		assertEquals(email, result.getEmail());
	}

	@Test
	void createUser_whenEmailAlreadyExists_throwsDuplicateEmailExceptionWithoutSaving() {
		// Arrange
		String email = "ada@example.com";
		when(userRepository.existsByEmail(email)).thenReturn(true);

		// Act / Assert
		assertThrows(DuplicateEmailException.class, () -> userService.createUser("Ada Lovelace", email));
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void getUserById_whenUserExists_returnsUser() {
		// Arrange
		Long id = 7L;
		User expected = new User("Ada Lovelace", "ada@example.com");
		when(userRepository.findById(id)).thenReturn(Optional.of(expected));

		// Act
		User result = userService.getUserById(id);

		// Assert
		assertEquals(expected, result);
		verify(userRepository).findById(id);
	}

	@Test
	void getUserById_whenUserDoesNotExist_throwsUserNotFoundExceptionWithId() {
		// Arrange
		Long id = 7L;
		when(userRepository.findById(id)).thenReturn(Optional.empty());

		// Act
		UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> userService.getUserById(id));

		// Assert
		assertEquals("User not found: " + id, exception.getMessage());
		verify(userRepository).findById(id);
	}

	@Test
	void getAllUsers_whenUsersExist_returnsAllUsers() {
		// Arrange
		List<User> expected = List.of(new User("Ada Lovelace", "ada@example.com"),
				new User("Grace Hopper", "grace@example.com"));
		when(userRepository.findAll()).thenReturn(expected);

		// Act
		List<User> result = userService.getAllUsers();

		// Assert
		assertEquals(expected, result);
		assertEquals(2, result.size());
		verify(userRepository).findAll();
	}

	@Test
	void getAllUsers_whenNoUsersExist_returnsEmptyList() {
		// Arrange
		when(userRepository.findAll()).thenReturn(List.of());

		// Act
		List<User> result = userService.getAllUsers();

		// Assert
		assertTrue(result.isEmpty());
		verify(userRepository).findAll();
	}

	@Test
	void updateUser_whenUserExistsAndEmailIsAvailable_updatesAndSavesUser() {
		// Arrange
		Long id = 7L;
		User existing = new User("Ada", "ada@example.com");
		when(userRepository.findById(id)).thenReturn(Optional.of(existing));
		when(userRepository.existsByEmail("ada.new@example.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		User result = userService.updateUser(id, "Ada Lovelace", "ada.new@example.com");

		// Assert
		assertEquals("Ada Lovelace", existing.getName());
		assertEquals("ada.new@example.com", existing.getEmail());
		assertEquals("Ada Lovelace", result.getName());
		assertEquals("ada.new@example.com", result.getEmail());
		verify(userRepository).save(existing);
	}

	@Test
	void updateUser_whenUserDoesNotExist_throwsUserNotFoundExceptionWithoutSaving() {
		// Arrange
		Long id = 7L;
		when(userRepository.findById(id)).thenReturn(Optional.empty());

		// Act / Assert
		assertThrows(UserNotFoundException.class,
				() -> userService.updateUser(id, "Ada Lovelace", "ada.new@example.com"));
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void updateUser_whenNewEmailIsAlreadyUsed_throwsDuplicateEmailExceptionWithoutSaving() {
		// Arrange
		Long id = 7L;
		User existing = new User("Ada", "ada@example.com");
		when(userRepository.findById(id)).thenReturn(Optional.of(existing));
		when(userRepository.existsByEmail("grace@example.com")).thenReturn(true);

		// Act / Assert
		assertThrows(DuplicateEmailException.class,
				() -> userService.updateUser(id, "Ada Lovelace", "grace@example.com"));
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void updateUser_whenKeepingSameEmail_doesNotCheckForDuplicateAndSaves() {
		// Arrange
		Long id = 7L;
		String email = "ada@example.com";
		User existing = new User("Ada", email);
		when(userRepository.findById(id)).thenReturn(Optional.of(existing));
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		User result = userService.updateUser(id, "Ada Lovelace", email);

		// Assert
		assertEquals("Ada Lovelace", result.getName());
		assertEquals(email, result.getEmail());
		verify(userRepository, never()).existsByEmail(email);
		verify(userRepository).save(existing);
	}

	@Test
	void deleteUser_whenUserExists_deletesByIdOnce() {
		// Arrange
		Long id = 7L;
		when(userRepository.existsById(id)).thenReturn(true);

		// Act
		userService.deleteUser(id);

		// Assert
		verify(userRepository).deleteById(id);
	}

	@Test
	void deleteUser_whenUserDoesNotExist_throwsUserNotFoundExceptionWithoutDeleting() {
		// Arrange
		Long id = 7L;
		when(userRepository.existsById(id)).thenReturn(false);

		// Act / Assert
		assertThrows(UserNotFoundException.class, () -> userService.deleteUser(id));
		verify(userRepository, never()).deleteById(id);
	}
}
