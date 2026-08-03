package com.hortifruti.sl.hortifruti.repository;

import com.hortifruti.sl.hortifruti.model.OAuthCredentialEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OAuthCredentialEntryRepository extends JpaRepository<OAuthCredentialEntry, Long> {
  List<OAuthCredentialEntry> findAllByDataStoreId(String dataStoreId);

  @Modifying
  @Query("DELETE FROM OAuthCredentialEntry e WHERE e.dataStoreId = :dataStoreId")
  void deleteAllByDataStoreId(@Param("dataStoreId") String dataStoreId);
}
