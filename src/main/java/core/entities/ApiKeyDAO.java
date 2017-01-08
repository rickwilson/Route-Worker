package core.entities;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface ApiKeyDAO extends CrudRepository<ApiKey, Long> {

    ApiKey findByApiKey(String apiKey);
    ApiKey findByApiKeyAndActive(String apiKey, boolean active);
}
