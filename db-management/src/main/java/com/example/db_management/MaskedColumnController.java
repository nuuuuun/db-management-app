package com.example.db_management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/masked-columns")
public class MaskedColumnController {

    @Autowired
    private MaskedColumnRepository repository;

    @GetMapping
    public List<MaskedColumn> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody MaskedColumn mc) {
        if (!"ADMIN".equals(SecurityUtils.getCurrentRole())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(repository.save(mc));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!"ADMIN".equals(SecurityUtils.getCurrentRole())) {
            return ResponseEntity.status(403).build();
        }
        repository.deleteById(id);
        return ResponseEntity.ok().<Void>build();
    }
}
