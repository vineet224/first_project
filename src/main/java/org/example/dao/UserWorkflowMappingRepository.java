package org.example.dao;

import java.util.HashMap;
import java.util.Optional;

import org.example.model.entites.UserWorkflowMappingEntity;
import org.springframework.stereotype.Service;

@Service
public class UserWorkflowMappingRepository {

    HashMap<String, UserWorkflowMappingEntity> userWorkflowMap = new HashMap<>();

    public Optional<UserWorkflowMappingEntity> findByMobileNo(String mobileNo) {
        return Optional.ofNullable(userWorkflowMap.get(mobileNo));
    }

    public void save(UserWorkflowMappingEntity userWorkflowMappingEntity) {
        userWorkflowMap.put(userWorkflowMappingEntity.getMobileNo(), userWorkflowMappingEntity);
    }
}
