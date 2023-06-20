package com.example.alhohelp.repository;

import com.example.alhohelp.entity.GeneralAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface GeneralAccessRepository extends JpaRepository<GeneralAccess, Long> {
   GeneralAccess queryById(int id);

    @Query(value = "SELECT * FROM general_access where use_user = ?", nativeQuery = true)
    List<GeneralAccess> queryByUsUser(Long id);
    @Query(value = "SELECT * FROM general_access where user_id = ?", nativeQuery = true)
    List<GeneralAccess> queryByUserId(Long id);
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM general_access where address = ?", nativeQuery = true)
    int deleteByAddress(String address);

}
