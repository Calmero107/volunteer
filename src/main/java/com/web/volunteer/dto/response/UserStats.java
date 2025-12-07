package com.web.volunteer.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserStats {
    private long totalUsers;
    private long volunteers;
    private long eventManagers;
    private long admins;
}
