package com.dhubbard.petproject.users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
public class UsersController {

	private ArrayList<User> users = new ArrayList<>();
	private SecureRandom random = new SecureRandom();
	@Autowired
	private UserRepository userRepository;

	@PostMapping("/login")
	ResponseEntity loginUser(@RequestBody UserLogin userLogin) {
		for (User user : this.userRepository.findAll()) {
			byte[] salt = user.getSalt();
			KeySpec spec = new PBEKeySpec(userLogin.getPassword().toCharArray(), salt, 65536, 128);
			byte[] userLoginHash;
			try {
				SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
				userLoginHash = factory.generateSecret(spec).getEncoded();
			} catch (Exception e) {
				return ResponseEntity.status(500).body("Error verifying user");
			}
			if (user.getEmail().equals(userLogin.getEmail()) && Arrays.equals(user.getPassword(), userLoginHash)) {
				return ResponseEntity.status(200).body("Logged in");
			}
		}
		return ResponseEntity.status(401).body("Incorrect email or password");
	}

	@PostMapping("/register")
	ResponseEntity registerUser(@RequestBody UserLogin userLogin) {
		List<User> usersWithEmail = userRepository.findByEmail(userLogin.getEmail());
		if (!usersWithEmail.isEmpty()) {
			return ResponseEntity.status(409).body("User email already exists");
		}

		byte[] salt = new byte[16];
		random.nextBytes(salt);
		KeySpec spec = new PBEKeySpec(userLogin.getPassword().toCharArray(), salt, 65536, 128);
		byte[] hash;
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			hash = factory.generateSecret(spec).getEncoded();
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Error verifying user");
		}

		User newUser = new User(userLogin.getEmail(), hash, salt);
		this.userRepository.save(newUser);
		return ResponseEntity.status(200).body("Successful registration");
	}
}