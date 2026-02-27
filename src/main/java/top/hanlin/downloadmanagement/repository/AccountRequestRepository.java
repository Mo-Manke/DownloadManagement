package top.hanlin.downloadmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.hanlin.downloadmanagement.domain.AccountRequest;

import java.util.List;

public interface AccountRequestRepository extends JpaRepository<AccountRequest, Long> {
	List<AccountRequest> findByStatusOrderByCreatedAtAsc(AccountRequest.Status status);
}


