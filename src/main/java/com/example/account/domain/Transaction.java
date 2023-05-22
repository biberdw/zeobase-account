package com.example.account.domain;

import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.*;
import lombok.experimental.SuperBuilder;

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

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "PARENT_ID")
    private Transaction parent;

    @OneToOne(fetch = LAZY, mappedBy = "parent")
    private Transaction child;

    private String transactionId;
    private LocalDateTime transactedAt;


}
