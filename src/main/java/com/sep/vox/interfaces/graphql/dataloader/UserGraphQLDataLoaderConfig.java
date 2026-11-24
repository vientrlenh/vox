package com.sep.vox.interfaces.graphql.dataloader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.dto.RoleDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.RoleDtoMapper;
import com.sep.vox.domain.mapper.UserDtoMapper;
import com.sep.vox.domain.model.user.Role;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserRole;
import com.sep.vox.domain.repository.RoleRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.repository.UserRoleRepository;
import com.sep.vox.interfaces.graphql.key.RoleUsersKey;
import com.sep.vox.interfaces.graphql.key.UserRolesKey;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class UserGraphQLDataLoaderConfig {

    public static final String USER_BY_USER_ID = "userByUserId";
    public static final String USERS_BY_ROLE_ID = "usersByRoleId";
    public static final String ROLES_BY_USER_ID = "rolesByUserId";

    private final BatchLoaderRegistry registry;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @PostConstruct
    void registerLoaders() {
        registerUserByUserId();
        registerUsersByRoleId();
        registerRolesByUserId();
    }

    private void registerUserByUserId() {
        registry.<UUID, UserDto>forName(USER_BY_USER_ID)
            .registerMappedBatchLoader((Set<UUID> userIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> userRepository.findByIdIn(userIds)
                    .stream()
                    .map(UserDtoMapper::toUserDto)
                    .collect(Collectors.toMap(user -> user.id(), user -> user)))
            );
    }

    private void registerUsersByRoleId() {
        registry.<RoleUsersKey, List<UserDto>>forName(USERS_BY_ROLE_ID)
            .registerMappedBatchLoader((Set<RoleUsersKey> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> loadUsersByRoleKeys(keys))
            );
    }

    private Map<RoleUsersKey, List<UserDto>> loadUsersByRoleKeys(Set<RoleUsersKey> keys) {
        Map<RoleUsersKey, List<UserDto>> result = new HashMap<>();
        keys.forEach(key -> result.put(key, List.of()));

        Map<PageKey, List<RoleUsersKey>> keysByPage = keys.stream()
            .collect(Collectors.groupingBy(key -> new PageKey(key.page(), key.size())));

        keysByPage.forEach((pageKey, groupedKeys) -> {
            Map<UUID, List<UserDto>> usersByRoleId = loadUsersByRoleId(groupedKeys, pageKey);
            groupedKeys.forEach(key -> result.put(key, usersByRoleId.getOrDefault(key.roleId(), List.of())));
        });
        return result;
    }

    private Map<UUID, List<UserDto>> loadUsersByRoleId(List<RoleUsersKey> keys, PageKey pageKey) {
        List<UUID> roleIds = keys.stream().map(key -> key.roleId()).toList();
        List<UserRole> roleUsers = userRoleRepository.findByRoleIdIn(roleIds, pageKey.page(), pageKey.size());

        List<UUID> userIds = roleUsers.stream().map(userRole -> userRole.getUserId()).distinct().toList();
        Map<UUID, User> usersById = userRepository.findByIdIn(userIds)
            .stream()
            .collect(Collectors.toMap(user -> user.getId(), user -> user));

        return roleUsers.stream()
            .filter(userRole -> usersById.containsKey(userRole.getUserId()))
            .collect(Collectors.groupingBy(
                userRole -> userRole.getRoleId(),
                Collectors.mapping(
                    userRole -> UserDtoMapper.toUserDto(usersById.get(userRole.getUserId())),
                    Collectors.toList())));
    }

    private void registerRolesByUserId() {
        registry.<UserRolesKey, List<RoleDto>>forName(ROLES_BY_USER_ID)
            .registerMappedBatchLoader((Set<UserRolesKey> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> loadRolesByUserKeys(keys))
            );
    }

    private Map<UserRolesKey, List<RoleDto>> loadRolesByUserKeys(Set<UserRolesKey> keys) {
        Map<UserRolesKey, List<RoleDto>> result = new HashMap<>();
        keys.forEach(key -> result.put(key, List.of()));

        List<UUID> userIds = keys.stream().map(key -> key.userId()).toList();
        List<UserRole> userRoles = userRoleRepository.findByUserIdIn(userIds);
        List<UUID> roleIds = userRoles.stream().map(userRole -> userRole.getRoleId()).distinct().toList();
        if (roleIds.isEmpty()) {
            return result;
        }

        Map<UUID, List<RoleDto>> rolesByUserId = groupRolesByUserId(userRoles, roleIds);
        keys.forEach(key -> result.put(key, rolesByUserId.getOrDefault(key.userId(), List.of())));
        return result;
    }

    private Map<UUID, List<RoleDto>> groupRolesByUserId(List<UserRole> userRoles, List<UUID> roleIds) {
        Map<UUID, Role> rolesById = roleRepository.findByIdIn(roleIds)
            .stream()
            .collect(Collectors.toMap(role -> role.getId(), role -> role));

        return userRoles.stream()
            .filter(userRole -> rolesById.containsKey(userRole.getRoleId()))
            .collect(Collectors.groupingBy(
                userRole -> userRole.getUserId(),
                Collectors.mapping(
                    userRole -> RoleDtoMapper.toRoleDto(rolesById.get(userRole.getRoleId())),
                    Collectors.toList())));
    }
}
