package com.example.db_management;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        if ("VIEWER".equals(SecurityUtils.getCurrentRole())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        if ("VIEWER".equals(SecurityUtils.getCurrentRole())) {
            return ResponseEntity.status(403).build();
        }
        return userRepository.findById(id)
            .map(user -> {
                user.setUsername(userDetails.getUsername());
                user.setEmail(userDetails.getEmail());
                user.setRole(userDetails.getRole());
                return ResponseEntity.ok(userRepository.save(user));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!"ADMIN".equals(SecurityUtils.getCurrentRole())) {
            return ResponseEntity.status(403).build();
        }
        return userRepository.findById(id)
            .map(user -> {
                userRepository.delete(user);
                return ResponseEntity.ok().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
