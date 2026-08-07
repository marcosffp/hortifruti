package com.hortifruti.sl.hortifruti.repository.googleauth;

import com.hortifruti.sl.hortifruti.model.googleauth.GoogleOAuthToken;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleOAuthTokenRepository extends JpaRepository<GoogleOAuthToken, String> {
  List<GoogleOAuthToken> findAllByStoreKeyStartingWith(String prefix);

  void deleteAllByStoreKeyStartingWith(String prefix);
}
