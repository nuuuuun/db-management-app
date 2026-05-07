package com.example.db_management;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        if (!"ADMIN".equals(SecurityUtils.getCurrentRole())) {
            return ResponseEntity.status(403).body("権限がありません");
        }
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body("ユーザー名は必須です");
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(409).body("ユーザー名「" + user.getUsername() + "」は既に使用されています");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("パスワードは必須です");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        if (!"ADMIN".equals(SecurityUtils.getCurrentRole())) {
            return ResponseEntity.status(403).body("権限がありません");
        }
        return userRepository.findById(id)
            .map(user -> {
                user.setUsername(userDetails.getUsername());
                user.setEmail(userDetails.getEmail());
                // ロール変更はADMINのみ許可（自分自身のロール降格は除く）
                if (userDetails.getRole() != null) {
                    user.setRole(userDetails.getRole());
                }
                // パスワードが送られてきた場合のみ更新（空の場合は変更しない）
                if (userDetails.getPassword() != null && !userDetails.getPassword().isBlank()) {
                    user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
                }
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
