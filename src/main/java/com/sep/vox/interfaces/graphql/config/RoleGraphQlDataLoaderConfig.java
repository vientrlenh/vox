package com.sep.vox.interfaces.graphql.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.application.port.input.query.key.RoleUsersKey;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.UserDtoMapper;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

import reactor.core.publisher.Mono;

@Configuration
public class RoleGraphQlDataLoaderConfig {
    
    public RoleGraphQlDataLoaderConfig(
        BatchLoaderRegistry registry,
        UserRoleRepository userRoleRepository, 
        UserRepository userRepository
    ) {
        registry.<RoleUsersKey, List<UserDto>>forName("usersByRole")
            .registerMappedBatchLoader((Set<RoleUsersKey> keys, BatchLoaderEnvironment env) -> 
            Mono.fromSupplier(() -> {
                Map<RoleUsersKey, List<UserDto>> result = new HashMap<>();

                keys.forEach(key -> result.put(key, new ArrayList<>()));

                var keysByPage = keys.stream()
                    .collect(Collectors.groupingBy(key -> new PageKey(key.page(), key.size()))
                );

                for (var entry : keysByPage.entrySet()) {
                    var pageKey = entry.getKey();
                    var groupedKeys = entry.getValue();

                    var roleIds = groupedKeys.stream()
                        .map(k -> k.roleId())
                        .toList();

                    var roleUsers = userRoleRepository.findByRoleIdIn(roleIds, pageKey.page(), pageKey.size());
                    
                    var userIds = roleUsers.stream()
                        .map(ur -> ur.getUserId())
                        .distinct()
                        .toList();
                    
                    var users = userRepository.findByIdIn(userIds)
                        .stream()
                        .collect(Collectors.toMap(u -> u.getId(), u -> u));

                    var usersByRoleId = roleUsers.stream()
                        .filter(ru -> users.containsKey(ru.getUserId()))
                        .collect(Collectors.groupingBy(
                            ur -> ur.getRoleId(), 
                            Collectors.mapping(
                                ru -> UserDtoMapper.toUserDto(users.get(ru.getUserId())), 
                                Collectors.toList())
                        ));
                    
                    groupedKeys.forEach(key -> result.put(key, usersByRoleId.getOrDefault(key.roleId(), List.of())));
                }
                
                return result;
            })
        );
    }
}
