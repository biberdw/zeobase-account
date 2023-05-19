package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.respository.AccountRepository;
import com.example.account.respository.AccountUserRepository;
import com.example.account.respository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.AccountStatus.UNREGISTERED;
import static com.example.account.type.ErrorCode.*;
import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String ACCOUNT_NUMBER = "1000000012";
    private static final long CANCEL_AMOUNT = 200L;
    private static final long BALANCE = 10000L;
    private static final long USE_AMOUNT = 1000L;
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;


    /**
     * 사용자가 없는 경우, 사용자 아이디와 계좌 소유주가 다른 경우,
     * 계좌가 이미 해지 상태인 경우, 거래금액이 잔액보다 큰 경우,
     * 거래금액이 너무 작거나 큰 경우 실패 응답
     */


    @Test
    public void successUseBalance() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();

        LocalDateTime transactedAt = LocalDateTime.now();
        String transactionId = UUID.randomUUID().toString().replace("-", "");

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .transactionType(USE)
                        .transactionResultType(S)
                        .account(account)
                        .amount(1000L)
                        .balanceSnapshot(account.getBalance() - 1000L)
                        .transactionId(transactionId)
                        .transactedAt(transactedAt)
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.useBalance(1L,
                "1000000012", 200L);
        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(200L, captor.getValue().getAmount());
        assertEquals(9800L, captor.getValue().getBalanceSnapshot());
        assertEquals(S, captor.getValue().getTransactionResultType());

        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(ACCOUNT_NUMBER, transactionDto.getAccountNumber());
        assertEquals(1000L, transactionDto.getAmount());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(transactionId, transactionDto.getTransactionId());
        assertEquals(transactedAt, transactionDto.getTransactedAt());
    }


    @Test
    @DisplayName("해당 유저 없음 잔액 사용 실패")
    public void useBalance_UserNotFound() throws Exception {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, ACCOUNT_NUMBER, 200L));


        //then
        assertEquals(USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 잔액 사용 실패")
    public void useBalance_AccountNotFound() throws Exception {
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
                () -> transactionService.useBalance(1L, ACCOUNT_NUMBER, 1000L));

        //then
        assertEquals(ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주 다름 - 잔액 사용 실패")
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
                () -> transactionService.useBalance(1L, ACCOUNT_NUMBER, 1000L));

        //then
        assertEquals(USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("해지 계좌는 해지할 수 없다")
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
                        .accountStatus(UNREGISTERED)
                        .balance(0L)
                        .build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, ACCOUNT_NUMBER, 1000L));

        //then
        assertEquals(ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액이 잔액보다 큰 경우")
    public void exceedAmount_UseBalance() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(100L)
                .accountNumber("1000000012").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, ACCOUNT_NUMBER, 1000L));
        //then
        assertEquals(AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
        verify(accountRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    public void saveFailedUseTransaction() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .transactionType(USE)
                        .transactionResultType(F)
                        .account(account)
                        .amount(1000L)
                        .balanceSnapshot(account.getBalance())
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        transactionService.saveFailedUseTransaction(ACCOUNT_NUMBER, 200L);
        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(200L, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(F, captor.getValue().getTransactionResultType());
        assertEquals(USE, captor.getValue().getTransactionType());
    }


    @Test
    public void successCancelBalance() throws Exception {
        LocalDateTime transactedAt = LocalDateTime.now();
        String transactionId = UUID.randomUUID().toString().replace("-", "");

        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(BALANCE)
                .accountNumber(ACCOUNT_NUMBER).build();

        Transaction transaction = Transaction.builder()
                .transactionType(USE)
                .transactionResultType(S)
                .account(account)
                .amount(USE_AMOUNT)
                .balanceSnapshot(BALANCE - USE_AMOUNT)
                .transactionId(transactionId)
                .transactedAt(transactedAt)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(ACCOUNT_NUMBER))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .transactionType(CANCEL)
                        .transactionResultType(S)
                        .account(account)
                        .amount(USE_AMOUNT)
                        .balanceSnapshot(BALANCE + USE_AMOUNT)
                        .transactionId(transactionId)
                        .transactedAt(transactedAt)
                        .build());

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto =
                transactionService.cancelBalance(transactionId,
                        ACCOUNT_NUMBER, USE_AMOUNT);

        //then
        verify(transactionRepository, times(1))
                .save(captor.capture());

        //실제 save 할때 당시의 값 (로직이 수행되고 난 뒤에 값을 테스트 할 수있음)
        // save 전까지의 값 (실제 로직이 잘 수행되는지)
        assertEquals(CANCEL,captor.getValue().getTransactionType());
        assertEquals(S,captor.getValue().getTransactionResultType());
        assertEquals(account,captor.getValue().getAccount());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(BALANCE + USE_AMOUNT,
                captor.getValue().getBalanceSnapshot());


        //반환 값에 대한 테스트를 할 수 있음 (예를들면 Dto에 값을 제대로 변환하고 있는지 또는 값 넣는것을 빼먹은게 있는지)
        // save 의 반환값을 받아서 dto 로 변환하고 반환하는 값
        // 반환하는 값이 잘 반환이 되는지
        assertEquals(BALANCE + USE_AMOUNT
                , transactionDto.getBalanceSnapshot());
        assertEquals(ACCOUNT_NUMBER,transactionDto.getAccountNumber());
        assertEquals(CANCEL,transactionDto.getTransactionType());
        assertEquals(S,transactionDto.getTransactionResultType());
        assertEquals(USE_AMOUNT,transactionDto.getAmount());
        assertEquals(BALANCE + USE_AMOUNT,transactionDto.getBalanceSnapshot());
        assertEquals(transactionId,transactionDto.getTransactionId());
        assertEquals(transactedAt,transactionDto.getTransactedAt());


    }

    @Test
    @DisplayName("해당 거래 없음 잔액 사용 취소 실패")
    public void cancelTransaction_TransactionNotFound() throws Exception {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", ACCOUNT_NUMBER, 1000L));

        //then
        assertEquals(TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 계좌 없음 잔액 사용 취소 실패")
    public void cancelTransaction_AccountNotFound() throws Exception {
        //given
        Transaction transaction = Transaction.builder().build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", ACCOUNT_NUMBER, 1000L));

        //then
        assertEquals(ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }


    @Test
    @DisplayName("거래와 계좌가 매칭실패 - 잔액 사용 실패")
    public void cancelTransaction_AccountUnMatch() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();

        Account account = Account.builder()
                .id(1L)
                .build();

        Account otherAccount = Account.builder()
                .id(2L)
                .build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(otherAccount));



        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", ACCOUNT_NUMBER, 1000L));

        //then
        assertEquals(TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래금액과 취소금액이 다름 -  잔액 사용 취소 실패")
    public void cancelTransaction_CancelMustFully() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(BALANCE)
                .accountNumber(ACCOUNT_NUMBER).build();

        Transaction transaction = Transaction.builder()
                .transactionType(USE)
                .transactionResultType(S)
                .account(account)
                .amount(USE_AMOUNT)
                .balanceSnapshot(BALANCE - USE_AMOUNT)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(ACCOUNT_NUMBER))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", ACCOUNT_NUMBER, 2000L));

        //then
        assertEquals(CANCEL_MUST_FULLY, exception.getErrorCode());
    }

    @Test
    @DisplayName("1년이 지난 거래 -  잔액 사용 취소 실패")
    public void cancelTransaction_TooOldOrderToCancel() throws Exception {
        //given
        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(BALANCE)
                .accountNumber(ACCOUNT_NUMBER).build();

        Transaction transaction = Transaction.builder()
                .transactionType(USE)
                .transactionResultType(S)
                .account(account)
                .amount(USE_AMOUNT)
                .balanceSnapshot(BALANCE - USE_AMOUNT)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(ACCOUNT_NUMBER))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", ACCOUNT_NUMBER, 1000L));

        //then
        assertEquals(TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());
    }



    @Test
    public void successQueryTransaction() throws Exception{
        //given
        AccountUser user = AccountUser.builder()
                .id(1L)
                .name("Pobi").build();

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(BALANCE)
                .accountNumber(ACCOUNT_NUMBER).build();

        Transaction transaction = Transaction.builder()
                .transactionType(USE)
                .transactionResultType(S)
                .account(account)
                .amount(USE_AMOUNT)
                .balanceSnapshot(BALANCE - USE_AMOUNT)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        //when
        TransactionDto transactionDto = transactionService.queryTransaction("trxId");

        //then
        assertEquals(USE,transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(USE_AMOUNT, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }


    @Test
    @DisplayName("원거래 없음 - 거래 조회 실패")
    public void queryTransaction_transactionNotFound() throws Exception{
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));
        //then
        assertEquals(TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }
}