package com.studyolle.modules.account;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountFactory {

    @Autowired AccountRepository accountRepository;

    public Account createAccount(String nickname) {
        Account jeong = new Account();
        jeong.setNickname(nickname);
        jeong.setEmail(nickname + "@email.com");
        accountRepository.save(jeong);
        return jeong;
    }
}