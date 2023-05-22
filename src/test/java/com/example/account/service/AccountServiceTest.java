package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.respository.AccountRepository;
import com.example.account.respository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;


    @Test
    @DisplayName("게좌 생성 성공")
    public void createAccountSuccess() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000012").build()));
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000013").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);


        //when
        AccountDto accountDto =
                accountService.createAccount(1L, 1000L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(1, accountDto.getUserId());
        assertEquals("1000000013", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("계좌 생성 - 처음 계좌를 생성하면 계좌번호는 1000000000이 돼야 한다")
    public void createFirstAccount() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
                .id(15L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000013").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);


        //when
        AccountDto accountDto =
                accountService.createAccount(1L, 1000L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(15L, accountDto.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }


    @Test
    @DisplayName("계좌 생성 - 사용자가 존재하지 않을경우 계좌생성이 실패해야 한다")
    public void createAccount_UserNotFound() throws Exception {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }


    @Test
    @DisplayName("계좌 생성 - 유저 당 최대 계좌는 10개를 넘을 수 없다")
    public void createAccount_maxAccountIs10() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }


    @Test
    @DisplayName("계좌 해지 성공")
    public void deleteAccountSuccess() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto =
                accountService.deleteAccount(1L, "1234567890");

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(1L, accountDto.getUserId());
        assertEquals("1000000012", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
    }


    @Test
    @DisplayName("계좌 해지 - 사용자가 존재하지 않을경우 계좌해지는 실패해야 한다")
    public void deleteAccount_UserNotFound() throws Exception {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 - 계좌가 존재하지 않을경우 계좌해지는 실패해야 한다")
    public void deleteAccount_AccountNotFound() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 - 계좌 소유주가 다를 경우 계좌해지는 실패해야 한다")
    public void deleteAccount_userUnMatch() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();
        AccountUser otherUser = AccountUser.builder()
                .id(13L)
                .name("Harry").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(otherUser)
                        .build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 - 해지 계좌는 잔액이 없어야 한다")
    public void deleteAccount_balanceNotEmpty() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(100L)
                        .build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 - 이미 해지 계좌는 해지할 수 없다")
    public void deleteAccount_alreadyUnregistered() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L)
                        .build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }


    @Test
    @DisplayName("계좌 확인 성공")
    public void successGetAccountsByUserId() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();

        List<Account> accounts = Arrays.asList(
                Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000000")
                        .balance(1000L)
                        .build(),
                Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000001")
                        .balance(2000L)
                        .build(),
                Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000002")
                        .balance(3000L)
                        .build()
        );

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);


        //when
        List<AccountDto> accountDtoList = accountService.getAccountsByUserId(1L);
        //then

        assertEquals(3, accountDtoList.size());
        assertEquals("1000000000", accountDtoList.get(0).getAccountNumber());
        assertEquals(1000L, accountDtoList.get(0).getBalance());
        assertEquals("1000000001", accountDtoList.get(1).getAccountNumber());
        assertEquals(2000L, accountDtoList.get(1).getBalance());
        assertEquals("1000000002", accountDtoList.get(2).getAccountNumber());
        assertEquals(3000L, accountDtoList.get(2).getBalance());

    }


    @Test
    @DisplayName("게좌 확인 - 사용자가 없는 경우 계좌 확인은 실패해야 한다")
    public void failedToGetAccounts() throws Exception {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

}