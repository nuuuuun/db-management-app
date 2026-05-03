package com.example.db_management;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaskedColumnRepository extends JpaRepository<MaskedColumn, Long> {
    List<MaskedColumn> findByTableNameAndRoleIn(String tableName, List<String> roles);
}
