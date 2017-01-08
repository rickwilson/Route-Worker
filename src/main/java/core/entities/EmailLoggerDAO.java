package core.entities;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface EmailLoggerDAO extends CrudRepository<EmailLogger, Long> {
    List<EmailLogger> findBySentAndFailed(boolean sent, boolean failed);
}
