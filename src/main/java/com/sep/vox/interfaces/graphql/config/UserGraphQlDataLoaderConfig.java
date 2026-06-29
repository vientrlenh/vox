package com.sep.vox.interfaces.graphql.config;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.application.port.input.query.key.UserRolesKey;
import com.sep.vox.domain.dto.RoleDto;
import com.sep.vox.domain.mapper.RoleDtoMapper;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.UserRoleRepository;

import reactor.core.publisher.Mono;

@Configuration
public class UserGraphQlDataLoaderConfig {
    
    public UserGraphQlDataLoaderConfig(
        BatchLoaderRegistry registry, 
        UserRoleRepository userRoleRepository, 
        RoleRepository roleRepository
    ) {

        registry.<UserRolesKey, List<RoleDto>>forName("rolesByUser")
        .registerMappedBatchLoader((Set<UserRolesKey> keys, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                Map<UserRolesKey, List<RoleDto>> result = new HashMap<>();

                keys.forEach(key -> result.put(key, List.of()));

                List<UUID> userIds = keys.stream()
                    .map(k -> k.userId())
                    .toList();

                var userRoles = userRoleRepository.findByUserIdIn(userIds);
                var roleIds = userRoles.stream().map(ur -> ur.getRoleId()).distinct().toList();
                if (roleIds.isEmpty()) {
                    return result;
                }
                var roles = roleRepository.findByIdIn(roleIds)
                    .stream()
                    .collect(Collectors.toMap(r -> r.getId(), r -> r));
                
                var rolesByUserId = userRoles.stream()
                    .filter(ur -> roles.containsKey(ur.getRoleId()))
                    .collect(Collectors.groupingBy(
                        ur -> ur.getUserId(), 
                        Collectors.mapping(
                            ur -> RoleDtoMapper.toRoleDto(roles.get(ur.getRoleId())), 
                            Collectors.toList())));
                
                keys.forEach(key -> result.put(key, rolesByUserId.getOrDefault(key.userId(), List.of())));


                return result;
            })
        );

        registry.<UUID, List<RoleDto>>forName("schoolRolesByUser")
        .registerMappedBatchLoader((Set<UUID> userIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                Map<UUID, List<RoleDto>> result = new HashMap<>();
                userIds.forEach(id -> result.put(id, List.of()));

                var schoolRoles = roleRepository.findByCodeIn(SchoolRoleCodes.ALL)
                    .stream()
                    .collect(Collectors.toMap(r -> r.getId(), RoleDtoMapper::toRoleDto));
                if (schoolRoles.isEmpty()) {
                    return result;
                }

                var rolesByUserId = userRoleRepository.findByUserIdIn(userIds).stream()
                    .filter(ur -> schoolRoles.containsKey(ur.getRoleId()))
                    .collect(Collectors.groupingBy(
                        ur -> ur.getUserId(),
                        Collectors.mapping(ur -> schoolRoles.get(ur.getRoleId()), Collectors.toList())));

                rolesByUserId.forEach(result::put);
                return result;
            })
        );
    }
}
