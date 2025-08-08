package fr.neocle.extensions;

import fr.neocle.flexgateway.api.extension.BaseExtension;
import fr.neocle.flexgateway.api.extension.enhancer.BaseEndpointEnhancer;
import fr.neocle.flexgateway.api.extension.handler.ActionHandler;
import fr.neocle.flexgateway.api.extension.provider.DataProvider;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class LuckPermsExtension extends BaseExtension {

    private LuckPerms luckPerms;

    @Override
    public String getName() {
        return "luckperms";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "LuckPerms integration for FlexGateway";
    }

    @Override
    public String getAuthor() {
        return "Neocle";
    }

    @Override
    public void onLoad() {
        getLogger().info("LuckPerms extension loading...");
        saveDefaultConfig();

        try {
            // Get LuckPerms API
            luckPerms = LuckPermsProvider.get();
            getLogger().info("Successfully connected to LuckPerms API");
        } catch (IllegalStateException e) {
            getLogger().severe("LuckPerms not found! Make sure LuckPerms is installed and loaded before this extension.");
            throw new RuntimeException("LuckPerms dependency not available", e);
        }
    }

    @Override
    public void onEnable() {
        getLogger().info("LuckPerms extension enabled!");

        // Register extension endpoints (/api/luckperms/...)
        registerDataProvider(new GroupsProvider());
        registerDataProvider(new GroupProvider());
        registerDataProvider(new UserPermissionsProvider());
        registerDataProvider(new UsersProvider());
        registerDataProvider(new PermissionCheckProvider());

        // Register action handlers
        registerActionHandler(new PermissionActionHandler());
        registerActionHandler(new GroupActionHandler());

        // Register base endpoint enhancers
        registerBaseEndpointEnhancer(new PlayerPermissionsEnhancer());
        registerBaseEndpointEnhancer(new PlayersGroupEnhancer());
        registerBaseEndpointEnhancer(new ServerPermissionsEnhancer());

        getLogger().info("Registered LuckPerms endpoints and enhancers");
    }

    @Override
    public void onDisable() {
        getLogger().info("LuckPerms extension disabled!");
    }

    // Extension Data Providers
    private class GroupsProvider implements DataProvider {
        @Override
        public String getEndpoint() {
            return "groups";
        }

        @Override
        public JsonElement getData(Map<String, String> parameters) {
            JsonArray groups = new JsonArray();

            boolean includePermissions = "true".equals(parameters.get("permissions"));
            boolean includeMembers = "true".equals(parameters.get("members"));

            for (Group group : luckPerms.getGroupManager().getLoadedGroups()) {
                JsonObject groupObj = new JsonObject();
                groupObj.addProperty("name", group.getName());
                groupObj.addProperty("displayName", group.getDisplayName());
                groupObj.addProperty("weight", group.getWeight().orElse(0));

                // Add parent groups
                JsonArray parents = new JsonArray();
                group.getInheritedGroups(QueryOptions.defaultContextualOptions())
                        .forEach(parent -> parents.add(parent.getName()));
                groupObj.add("parents", parents);

                if (includePermissions) {
                    JsonArray permissions = new JsonArray();
                    group.getNodes().forEach(node -> {
                        JsonObject perm = new JsonObject();
                        perm.addProperty("permission", node.getKey());
                        perm.addProperty("value", node.getValue());
                        perm.addProperty("server", node.getContexts().getAnyValue("server").orElse("global"));
                        perm.addProperty("world", node.getContexts().getAnyValue("world").orElse("global"));
                        permissions.add(perm);
                    });
                    groupObj.add("permissions", permissions);
                }

                if (includeMembers) {
                    // Count members (this might be expensive for large servers)
                    long memberCount = luckPerms.getUserManager().getLoadedUsers().stream()
                            .filter(user -> user.getInheritedGroups(QueryOptions.defaultContextualOptions())
                                    .contains(group))
                            .count();
                    groupObj.addProperty("memberCount", memberCount);
                }

                groups.add(groupObj);
            }

            return groups;
        }

        @Override
        public String getDescription() {
            return "Get all LuckPerms groups with optional permissions and member info";
        }

        @Override
        public String[] getSupportedParameters() {
            return new String[]{"permissions", "members"};
        }
    }

    private class GroupProvider implements DataProvider {
        @Override
        public String getEndpoint() {
            return "group";
        }

        @Override
        public JsonElement getData(Map<String, String> parameters) {
            String groupName = parameters.get("name");
            if (groupName == null || groupName.isEmpty()) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Group name parameter required");
                return error;
            }

            Group group = luckPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Group not found: " + groupName);
                return error;
            }

            JsonObject groupObj = new JsonObject();
            groupObj.addProperty("name", group.getName());
            groupObj.addProperty("displayName", group.getDisplayName());
            groupObj.addProperty("weight", group.getWeight().orElse(0));

            // Add detailed permissions
            JsonArray permissions = new JsonArray();
            group.getNodes().forEach(node -> {
                JsonObject perm = new JsonObject();
                perm.addProperty("permission", node.getKey());
                perm.addProperty("value", node.getValue());
                perm.addProperty("server", node.getContexts().getAnyValue("server").orElse("global"));
                perm.addProperty("world", node.getContexts().getAnyValue("world").orElse("global"));
                perm.addProperty("expiry", node.hasExpiry() ? node.getExpiry().getEpochSecond() : -1);
                permissions.add(perm);
            });
            groupObj.add("permissions", permissions);

            // Add parent groups
            JsonArray parents = new JsonArray();
            group.getInheritedGroups(QueryOptions.defaultContextualOptions())
                    .forEach(parent -> parents.add(parent.getName()));
            groupObj.add("parents", parents);

            // Add members
            JsonArray members = new JsonArray();
            luckPerms.getUserManager().getLoadedUsers().stream()
                    .filter(user -> user.getInheritedGroups(QueryOptions.defaultContextualOptions()).contains(group))
                    .forEach(user -> {
                        JsonObject member = new JsonObject();
                        member.addProperty("uuid", user.getUniqueId().toString());
                        member.addProperty("username", user.getUsername());
                        members.add(member);
                    });
            groupObj.add("members", members);

            return groupObj;
        }

        @Override
        public String getDescription() {
            return "Get detailed information about a specific LuckPerms group";
        }

        @Override
        public String[] getSupportedParameters() {
            return new String[]{"name"};
        }
    }

    private class UserPermissionsProvider implements DataProvider {
        @Override
        public String getEndpoint() {
            return "user";
        }

        @Override
        public JsonElement getData(Map<String, String> parameters) {
            String identifier = parameters.get("player");
            if (identifier == null || identifier.isEmpty()) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Player parameter required (username or UUID)");
                return error;
            }

            // Try to find user by username or UUID
            User user = null;
            try {
                UUID uuid = UUID.fromString(identifier);
                user = luckPerms.getUserManager().getUser(uuid);
            } catch (IllegalArgumentException e) {
                // Not a UUID, try username
                Player player = Bukkit.getPlayer(identifier);
                if (player != null) {
                    user = luckPerms.getUserManager().getUser(player.getUniqueId());
                }
            }

            if (user == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "User not found: " + identifier);
                return error;
            }

            JsonObject userObj = new JsonObject();
            userObj.addProperty("uuid", user.getUniqueId().toString());
            userObj.addProperty("username", user.getUsername());
            userObj.addProperty("primaryGroup", user.getPrimaryGroup());

            // Add all groups
            JsonArray groups = new JsonArray();
            user.getInheritedGroups(QueryOptions.defaultContextualOptions())
                    .forEach(group -> {
                        JsonObject groupObj = new JsonObject();
                        groupObj.addProperty("name", group.getName());
                        groupObj.addProperty("weight", group.getWeight().orElse(0));
                        groups.add(groupObj);
                    });
            userObj.add("groups", groups);

            // Add user permissions
            JsonArray permissions = new JsonArray();
            user.getNodes().forEach(node -> {
                JsonObject perm = new JsonObject();
                perm.addProperty("permission", node.getKey());
                perm.addProperty("value", node.getValue());
                perm.addProperty("server", node.getContexts().getAnyValue("server").orElse("global"));
                perm.addProperty("world", node.getContexts().getAnyValue("world").orElse("global"));
                perm.addProperty("expiry", node.hasExpiry() ? node.getExpiry().getEpochSecond() : -1);
                permissions.add(perm);
            });
            userObj.add("permissions", permissions);

            return userObj;
        }

        @Override
        public String getDescription() {
            return "Get detailed permission information for a specific user";
        }

        @Override
        public String[] getSupportedParameters() {
            return new String[]{"player"};
        }
    }

    private class UsersProvider implements DataProvider {
        @Override
        public String getEndpoint() {
            return "users";
        }

        @Override
        public JsonElement getData(Map<String, String> parameters) {
            JsonArray users = new JsonArray();

            String groupFilter = parameters.get("group");
            boolean onlineOnly = "true".equals(parameters.get("online"));

            Collection<User> userCollection = luckPerms.getUserManager().getLoadedUsers();

            for (User user : userCollection) {
                // Filter by group if specified
                if (groupFilter != null && !groupFilter.isEmpty()) {
                    boolean hasGroup = user.getInheritedGroups(QueryOptions.defaultContextualOptions())
                            .stream()
                            .anyMatch(group -> group.getName().equalsIgnoreCase(groupFilter));
                    if (!hasGroup) continue;
                }

                // Filter by online status if specified
                if (onlineOnly) {
                    Player player = Bukkit.getPlayer(user.getUniqueId());
                    if (player == null || !player.isOnline()) continue;
                }

                JsonObject userObj = new JsonObject();
                userObj.addProperty("uuid", user.getUniqueId().toString());
                userObj.addProperty("username", user.getUsername());
                userObj.addProperty("primaryGroup", user.getPrimaryGroup());

                Player player = Bukkit.getPlayer(user.getUniqueId());
                userObj.addProperty("online", player != null && player.isOnline());

                // Add group names
                JsonArray userGroups = new JsonArray();
                user.getInheritedGroups(QueryOptions.defaultContextualOptions())
                        .forEach(group -> userGroups.add(group.getName()));
                userObj.add("groups", userGroups);

                users.add(userObj);
            }

            return users;
        }

        @Override
        public String getDescription() {
            return "Get list of users with optional filtering by group or online status";
        }

        @Override
        public String[] getSupportedParameters() {
            return new String[]{"group", "online"};
        }
    }

    private class PermissionCheckProvider implements DataProvider {
        @Override
        public String getEndpoint() {
            return "check";
        }

        @Override
        public JsonElement getData(Map<String, String> parameters) {
            String playerParam = parameters.get("player");
            String permission = parameters.get("permission");

            if (playerParam == null || permission == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Both 'player' and 'permission' parameters required");
                return error;
            }

            Player player = Bukkit.getPlayer(playerParam);
            if (player == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Player not found or not online: " + playerParam);
                return error;
            }

            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "User data not found for: " + playerParam);
                return error;
            }

            JsonObject result = new JsonObject();
            result.addProperty("player", player.getName());
            result.addProperty("uuid", player.getUniqueId().toString());
            result.addProperty("permission", permission);

            // Check permission
            boolean hasPermission = user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
            result.addProperty("hasPermission", hasPermission);
            result.addProperty("checkTime", System.currentTimeMillis());

            return result;
        }

        @Override
        public String getDescription() {
            return "Check if a player has a specific permission";
        }

        @Override
        public String[] getSupportedParameters() {
            return new String[]{"player", "permission"};
        }
    }

    // Action Handlers
    private class PermissionActionHandler implements ActionHandler {
        @Override
        public String getEndpoint() {
            return "permission";
        }

        @Override
        public JsonObject handleAction(String method, Map<String, String> parameters, JsonObject body) {
            JsonObject response = new JsonObject();

            if (body == null) {
                response.addProperty("error", "Request body required");
                return response;
            }

            String playerParam = body.has("player") ? body.get("player").getAsString() : null;
            String permission = body.has("permission") ? body.get("permission").getAsString() : null;

            if (playerParam == null || permission == null) {
                response.addProperty("error", "Both 'player' and 'permission' fields required");
                return response;
            }

            Player player = Bukkit.getPlayer(playerParam);
            if (player == null) {
                response.addProperty("error", "Player not found or not online: " + playerParam);
                return response;
            }

            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                response.addProperty("error", "User data not found");
                return response;
            }

            try {
                if ("POST".equals(method)) {
                    // Add permission
                    boolean value = body.has("value") ? body.get("value").getAsBoolean() : true;
                    Node node = Node.builder(permission).value(value).build();
                    user.data().add(node);
                    luckPerms.getUserManager().saveUser(user);

                    response.addProperty("success", true);
                    response.addProperty("action", "added");
                    response.addProperty("permission", permission);
                    response.addProperty("value", value);

                } else if ("DELETE".equals(method)) {
                    // Remove permission
                    Node node = Node.builder(permission).build();
                    user.data().remove(node);
                    luckPerms.getUserManager().saveUser(user);

                    response.addProperty("success", true);
                    response.addProperty("action", "removed");
                    response.addProperty("permission", permission);
                }

                response.addProperty("player", player.getName());
                response.addProperty("timestamp", System.currentTimeMillis());

            } catch (Exception e) {
                getLogger().warning("Error managing permission: " + e.getMessage());
                response.addProperty("error", "Failed to manage permission: " + e.getMessage());
            }

            return response;
        }

        @Override
        public String[] getSupportedMethods() {
            return new String[]{"POST", "DELETE"};
        }

        @Override
        public String getDescription() {
            return "Add or remove permissions for users";
        }
    }

    private class GroupActionHandler implements ActionHandler {
        @Override
        public String getEndpoint() {
            return "group";
        }

        @Override
        public JsonObject handleAction(String method, Map<String, String> parameters, JsonObject body) {
            JsonObject response = new JsonObject();

            if (body == null) {
                response.addProperty("error", "Request body required");
                return response;
            }

            String playerParam = body.has("player") ? body.get("player").getAsString() : null;
            String groupName = body.has("group") ? body.get("group").getAsString() : null;

            if (playerParam == null || groupName == null) {
                response.addProperty("error", "Both 'player' and 'group' fields required");
                return response;
            }

            Player player = Bukkit.getPlayer(playerParam);
            if (player == null) {
                response.addProperty("error", "Player not found or not online: " + playerParam);
                return response;
            }

            Group group = luckPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                response.addProperty("error", "Group not found: " + groupName);
                return response;
            }

            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                response.addProperty("error", "User data not found");
                return response;
            }

            try {
                if ("POST".equals(method)) {
                    // Add user to group
                    Node node = Node.builder("group." + groupName).build();
                    user.data().add(node);
                    luckPerms.getUserManager().saveUser(user);

                    response.addProperty("success", true);
                    response.addProperty("action", "added");

                } else if ("DELETE".equals(method)) {
                    // Remove user from group
                    Node node = Node.builder("group." + groupName).build();
                    user.data().remove(node);
                    luckPerms.getUserManager().saveUser(user);

                    response.addProperty("success", true);
                    response.addProperty("action", "removed");
                }

                response.addProperty("player", player.getName());
                response.addProperty("group", groupName);
                response.addProperty("timestamp", System.currentTimeMillis());

            } catch (Exception e) {
                getLogger().warning("Error managing group membership: " + e.getMessage());
                response.addProperty("error", "Failed to manage group: " + e.getMessage());
            }

            return response;
        }

        @Override
        public String[] getSupportedMethods() {
            return new String[]{"POST", "DELETE"};
        }

        @Override
        public String getDescription() {
            return "Add or remove users from groups";
        }
    }

    // Base Endpoint Enhancers
    private class PlayerPermissionsEnhancer implements BaseEndpointEnhancer {
        @Override
        public String getBaseEndpoint() {
            return "player";
        }

        @Override
        public void enhanceResponse(JsonObject response, JsonObject originalData, String identifier, Map<String, String> parameters) {
            Player player = Bukkit.getPlayer(identifier);
            if (player == null) return;

            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) return;

            // Add LuckPerms data
            JsonObject luckpermsData = new JsonObject();
            luckpermsData.addProperty("primaryGroup", user.getPrimaryGroup());

            // Add groups with weights
            JsonArray groups = new JsonArray();
            user.getInheritedGroups(QueryOptions.defaultContextualOptions())
                    .stream()
                    .sorted((a, b) -> Integer.compare(b.getWeight().orElse(0), a.getWeight().orElse(0)))
                    .forEach(group -> {
                        JsonObject groupObj = new JsonObject();
                        groupObj.addProperty("name", group.getName());
                        groupObj.addProperty("displayName", group.getDisplayName());
                        groupObj.addProperty("weight", group.getWeight().orElse(0));
                        groups.add(groupObj);
                    });
            luckpermsData.add("groups", groups);

            // Add permission count
            luckpermsData.addProperty("directPermissions", user.getNodes().size());
            luckpermsData.addProperty("totalPermissions", user.resolveInheritedNodes(QueryOptions.defaultContextualOptions()).size());

            response.add("luckperms", luckpermsData);
        }

        @Override
        public String getDescription() {
            return "Adds LuckPerms permission data to player endpoint";
        }
    }

    private class PlayersGroupEnhancer implements BaseEndpointEnhancer {
        @Override
        public String getBaseEndpoint() {
            return "players";
        }

        @Override
        public void enhanceResponse(JsonObject response, JsonObject originalData, String identifier, Map<String, String> parameters) {
            // Add group distribution
            Map<String, Integer> groupCounts = new HashMap<>();
            int totalUsers = 0;

            for (Player player : Bukkit.getOnlinePlayers()) {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    totalUsers++;
                    String primaryGroup = user.getPrimaryGroup();
                    groupCounts.put(primaryGroup, groupCounts.getOrDefault(primaryGroup, 0) + 1);
                }
            }

            JsonObject groupDistribution = new JsonObject();
            for (Map.Entry<String, Integer> entry : groupCounts.entrySet()) {
                groupDistribution.addProperty(entry.getKey(), entry.getValue());
            }

            JsonObject luckpermsStats = new JsonObject();
            luckpermsStats.add("groupDistribution", groupDistribution);
            luckpermsStats.addProperty("totalUsers", totalUsers);
            luckpermsStats.addProperty("totalGroups", luckPerms.getGroupManager().getLoadedGroups().size());

            response.add("luckpermsStats", luckpermsStats);
        }

        @Override
        public String getDescription() {
            return "Adds LuckPerms group distribution to players endpoint";
        }
    }

    private class ServerPermissionsEnhancer implements BaseEndpointEnhancer {
        @Override
        public String getBaseEndpoint() {
            return "server";
        }

        @Override
        public void enhanceResponse(JsonObject response, JsonObject originalData, String identifier, Map<String, String> parameters) {
            JsonObject luckpermsInfo = new JsonObject();
            luckpermsInfo.addProperty("version", luckPerms.getPluginMetadata().getVersion());
            luckpermsInfo.addProperty("apiVersion", luckPerms.getPluginMetadata().getApiVersion());

            // Add storage info
            JsonObject storage = new JsonObject();
            storage.addProperty("type", luckPerms.getMessagingService().isPresent() ? "network" : "local");
            luckpermsInfo.add("storage", storage);

            // Add group and user counts
            JsonObject stats = new JsonObject();
            stats.addProperty("loadedGroups", luckPerms.getGroupManager().getLoadedGroups().size());
            stats.addProperty("loadedUsers", luckPerms.getUserManager().getLoadedUsers().size());
            luckpermsInfo.add("stats", stats);

            response.add("luckperms", luckpermsInfo);
        }

        @Override
        public String getDescription() {
            return "Adds LuckPerms server information to server endpoint";
        }
    }
}