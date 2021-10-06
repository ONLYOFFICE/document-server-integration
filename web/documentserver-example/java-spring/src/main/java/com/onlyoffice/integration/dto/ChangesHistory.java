package com.onlyoffice.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangesHistory {
    private String created;
    private ChangesUser user;
}
