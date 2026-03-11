package com.uniclubconnect.services.followservice.repository;

import com.uniclubconnect.services.followservice.model.FollowSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowSettingRepository extends JpaRepository<FollowSetting, String> {
}
