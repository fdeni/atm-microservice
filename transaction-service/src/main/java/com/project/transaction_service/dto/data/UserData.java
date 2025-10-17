package com.project.transaction_service.dto.data;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class UserData {
    private Long id;
    private String username;
}
