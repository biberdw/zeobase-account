package com.example.account.domain;

import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

import static javax.persistence.EnumType.*;
import static javax.persistence.FetchType.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
public class Transaction extends BaseEntity {

    @Enumerated(STRING)
    private TransactionType transactionType;
    @Enumerated(STRING)
    private TransactionResultType transactionResultType;

    @ManyToOne(fetch = LAZY)
    private Account account;
    private Long amount;

    private Long balanceSnapshot;

    private String transactionId;
    private LocalDateTime transactedAt;


}
