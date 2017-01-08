package core.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface OmniTransactionDAO extends JpaRepository<OmniTransaction, Long> {
    List<OmniTransaction> findFirst250ByTrackingNumberIsNotNullAndOmniTrackingIdLessThan(long omniTrackingId);
    List<OmniTransaction> findByOmniTrackingId(long omniTrackingId);
}
