package com.learn.shopapi.service;

import com.learn.shopapi.dto.LoginEventResponse;
import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.entity.LoginEvent;
import com.learn.shopapi.entity.LoginOutcome;
import com.learn.shopapi.repository.LoginEventRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Ghi/su dung su kien dang nhap.
 *
 * record(...) dung REQUIRES_NEW: phai COMMIT doc lap vi luong login co the nem loi (sai mat khau,
 * bi chan) -> neu cung transaction se bi rollback va mat ban ghi audit.
 */
@Service
public class LoginEventService {

    private final LoginEventRepository repository;

    public LoginEventService(LoginEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String username, LoginOutcome outcome) {
        repository.save(new LoginEvent(username, outcome, currentIp()));
    }

    @Transactional(readOnly = true)
    public PageResponse<LoginEventResponse> recent(Pageable pageable) {
        return PageResponse.from(repository.findAllByOrderByAtDesc(pageable), LoginEventResponse::from);
    }

    /** Lay IP nguoi goi (best-effort) tu request hien tai. */
    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRemoteAddr();
        }
        return null;
    }
}
