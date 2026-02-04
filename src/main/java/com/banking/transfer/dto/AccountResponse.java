package com.banking.transfer.dto;

import com.banking.transfer.entity.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {

    private Long id;
    private String username;
    private String holderName;
    private BigDecimal balance;
    private AccountStatus status;
}
