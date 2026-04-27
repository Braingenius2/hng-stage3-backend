package com.hng.profile.dto;

import java.util.List;
import com.hng.profile.model.Profile;

public record ProfileListResponse(String status, int count, List<Profile> data) {
}
