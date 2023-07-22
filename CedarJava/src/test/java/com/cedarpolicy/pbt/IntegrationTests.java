/*
 * Copyright 2022-2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cedarpolicy.pbt;

import com.cedarpolicy.AuthorizationEngine;
import com.cedarpolicy.WrapperAuthorizationEngine;
import com.cedarpolicy.model.AuthorizationRequest;
import com.cedarpolicy.model.AuthorizationResponse;
import com.cedarpolicy.model.slice.BasicSlice;
import com.cedarpolicy.model.slice.Entity;
import com.cedarpolicy.model.slice.Policy;
import com.cedarpolicy.value.EntityUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.verifiedpermissions.VerifiedPermissionsClient;
import software.amazon.awssdk.services.verifiedpermissions.model.*;

import java.util.*;

import static com.cedarpolicy.TestUtil.loadSchemaResource;

public class IntegrationTests {

    @Test
    public void testNotionSchema() {
        AuthorizationEngine authEngine = new WrapperAuthorizationEngine();

        Set<Entity> entities = Set.of(
                // Roles
                new Entity("Notion::Role::\"7th Crew\"", Map.of(), Set.of()),
                new Entity("Notion::Role::\"Manager\"", Map.of(), Set.of("Notion::Role::\"7th Crew\"")),
                new Entity("Notion::Role::\"Admin\"", Map.of(), Set.of("Notion::Role::\"Manager\"")),

                // Users
                new Entity("Notion::User::\"김수빈\"", Map.of(), Set.of("Notion::Role::\"Admin\"")),
                new Entity("Notion::User::\"문성혁\"", Map.of(), Set.of("Notion::Role::\"Manager\"")),
                new Entity("Notion::User::\"박영진\"", Map.of(), Set.of("Notion::Role::\"7th Crew\"")),
                new Entity("Notion::User::\"김칠기\"", Map.of(), Set.of("Notion::Role::\"7th Crew\"")),

                // Workspaces
                new Entity("Notion::Workspace::\"AUSG 7기 운영진\"", Map.of(), Set.of()),
                new Entity("Notion::Workspace::\"AUSG 7기 Crew\"", Map.of(), Set.of()),

                // Pages
                new Entity("Notion::Page::\"운영진 회의록\"", Map.of("owner", new EntityUID("Notion::User::\"김수빈\"")), Set.of("Notion::Workspace::\"AUSG 7기 운영진\"")),
                new Entity("Notion::Page::\"버디 논의\"", Map.of("owner", new EntityUID("Notion::User::\"문성혁\"")), Set.of("Notion::Workspace::\"AUSG 7기 운영진\"")),
                new Entity("Notion::Page::\"박영진 자기소개\"", Map.of("owner", new EntityUID("Notion::User::\"박영진\"")), Set.of("Notion::Workspace::\"AUSG 7기 Crew\"")),
                new Entity("Notion::Page::\"김칠기 자기소개\"", Map.of("owner", new EntityUID("Notion::User::\"김칠기\"")), Set.of("Notion::Workspace::\"AUSG 7기 Crew\""))
        );

        Set<Policy> policies = Set.of(
                new Policy("""
                        permit (
                            principal in Notion::Role::"7th Crew",
                            action in [
                                Notion::Action::"VIEW_PAGE"
                            ],
                            resource in Notion::Workspace::"AUSG 7기 Crew"
                        );""", "0"),
                new Policy("""
                        permit (
                            principal in Notion::Role::"Manager",
                            action in [
                                Notion::Action::"VIEW_PAGE",
                                Notion::Action::"EDIT_PAGE"
                            ],
                            resource in Notion::Workspace::"AUSG 7기 운영진"
                        );""", "1"),
                new Policy("""
                        permit (
                            principal in Notion::Role::"Manager",
                            action in [
                                Notion::Action::"VIEW_PAGE",
                                Notion::Action::"EDIT_PAGE"
                            ],
                            resource in Notion::Workspace::"AUSG 7기 Crew"
                        );""", "2"),
                new Policy("""
                        permit (
                            principal in Notion::Role::"Admin",
                            action in [
                                Notion::Action::"TOGGLE_PUBLISH_PAGE",
                                Notion::Action::"VIEW_PAGE",
                                Notion::Action::"EDIT_PAGE"
                            ],
                            resource
                        );""", "3"),
                new Policy("""
                        permit (
                            principal in Notion::Role::"7th Crew",
                            action in [
                                Notion::Action::"EDIT_PAGE"
                            ],
                            resource
                        )
                        when { resource has owner && resource.owner == principal};""", "4")
        );

        for (String user : Arrays.asList("김수빈", "문성혁", "박영진", "김칠기")) {
            for (String action : Arrays.asList("VIEW_PAGE", "EDIT_PAGE", "TOGGLE_PUBLISH_PAGE")) {
                for (String page : Arrays.asList("운영진 회의록", "버디 논의", "박영진 자기소개", "김칠기 자기소개")) {
                    Boolean pass = isPass(authEngine, entities, policies, "Notion::User::" + "\"" + user + "\"", "Notion::Action::" + "\"" + action + "\"", "Notion::Page::" + "\"" + page + "\"");

                    System.out.println("User: " + user + ", Action: " + action + ", Page: " + page + " --> " + (pass ? "O" : "X"));
                }
            }
            System.out.println();
        }
    }

    private static Boolean isPass(AuthorizationEngine authEngine, Set<Entity> entities, Set<Policy> policies, String principalEUID, String actionEUID, String resourceEUID) {
        AuthorizationRequest query = new AuthorizationRequest(
                principalEUID,
                actionEUID,
                resourceEUID,
                new HashMap<>(),
                Optional.of(loadSchemaResource("/notion_schema.json")));

        AuthorizationResponse result =
                Assertions.assertDoesNotThrow(() -> authEngine.isAuthorized(query, new BasicSlice(policies, entities)));

        if (!result.getErrors().isEmpty()) {
            System.out.println(result.getErrors());
        }
        return result.isAllowed();
    }

    @Test
    void testWithAmazonVerifiedPermissions() {
        // 위 testNotionSchema 와 완전히 동일한 내용을 이미 AVP 에 등록해 둔 상태이다. (policy store id = CXvsHjrMN5QhHM6ZgsaWHg)
        VerifiedPermissionsClient client = VerifiedPermissionsClient.builder().build();


        var User김수빈 = EntityIdentifier.builder().entityType("Notion::User").entityId("김수빈").build();
        var User문성혁 = EntityIdentifier.builder().entityType("Notion::User").entityId("문성혁").build();
        var User박영진 = EntityIdentifier.builder().entityType("Notion::User").entityId("박영진").build();
        var User김칠기 = EntityIdentifier.builder().entityType("Notion::User").entityId("김칠기").build();

        var Role7thCrew = EntityIdentifier.builder().entityType("Notion::Role").entityId("7th Crew").build();
        var RoleManager = EntityIdentifier.builder().entityType("Notion::Role").entityId("Manager").build();
        var RoleAdmin = EntityIdentifier.builder().entityType("Notion::Role").entityId("Admin").build();

        var WorkspaceAUSG7thManager = EntityIdentifier.builder().entityType("Notion::Workspace").entityId("AUSG 7기 운영진").build();
        var WorkspaceAUSG7thCrew = EntityIdentifier.builder().entityType("Notion::Workspace").entityId("AUSG 7기 Crew").build();

        var Page운영진회의록 = EntityIdentifier.builder().entityType("Notion::Page").entityId("운영진 회의록").build();
        var Page버디논의 = EntityIdentifier.builder().entityType("Notion::Page").entityId("버디 논의").build();
        var Page박영진자기소개 = EntityIdentifier.builder().entityType("Notion::Page").entityId("박영진 자기소개").build();
        var Page김칠기자기소개 = EntityIdentifier.builder().entityType("Notion::Page").entityId("김칠기 자기소개").build();

        EntitiesDefinition entities = EntitiesDefinition.fromEntityList(Arrays.asList(
                // Roles
                EntityItem.builder().identifier(Role7thCrew).build(),
                EntityItem.builder().identifier(RoleManager).parents(Role7thCrew).build(),
                EntityItem.builder().identifier(RoleAdmin).parents(RoleManager).build(),

                // Users
                EntityItem.builder().identifier(User김수빈).parents(RoleAdmin).build(),
                EntityItem.builder().identifier(User문성혁).parents(RoleManager).build(),
                EntityItem.builder().identifier(User박영진).parents(Role7thCrew).build(),
                EntityItem.builder().identifier(User김칠기).parents(Role7thCrew).build(),

                // Workspaces
                EntityItem.builder().identifier(WorkspaceAUSG7thManager).build(),
                EntityItem.builder().identifier(WorkspaceAUSG7thCrew).build(),

                // Pages
                EntityItem.builder().identifier(Page운영진회의록).parents(WorkspaceAUSG7thManager).attributes(Map.of("owner", AttributeValue.fromEntityIdentifier(User김수빈))).build(),
                EntityItem.builder().identifier(Page버디논의).parents(WorkspaceAUSG7thManager).attributes(Map.of("owner", AttributeValue.fromEntityIdentifier(User문성혁))).build(),
                EntityItem.builder().identifier(Page박영진자기소개).parents(WorkspaceAUSG7thCrew).attributes(Map.of("owner", AttributeValue.fromEntityIdentifier(User박영진))).build(),
                EntityItem.builder().identifier(Page김칠기자기소개).parents(WorkspaceAUSG7thCrew).attributes(Map.of("owner", AttributeValue.fromEntityIdentifier(User김칠기))).build()
        ));

        var ActionViewPage = ActionIdentifier.builder().actionType("Notion::Action").actionId("VIEW_PAGE").build();
        var ActionTogglePublishPage = ActionIdentifier.builder().actionType("Notion::Action").actionId("TOGGLE_PUBLISH_PAGE").build();
        var ActionEditPage = ActionIdentifier.builder().actionType("Notion::Action").actionId("EDIT_PAGE").build();

        validate(client, entities, "김수빈", "VIEW_PAGE", "운영진 회의록", Decision.ALLOW);
        validate(client, entities, "김수빈", "VIEW_PAGE", "버디 논의", Decision.ALLOW);
        validate(client, entities, "김수빈", "VIEW_PAGE", "박영진 자기소개", Decision.ALLOW);
        validate(client, entities, "김수빈", "VIEW_PAGE", "김칠기 자기소개", Decision.ALLOW);
        validate(client, entities, "김수빈", "EDIT_PAGE", "운영진 회의록", Decision.ALLOW);
        validate(client, entities, "김수빈", "EDIT_PAGE", "버디 논의", Decision.ALLOW);
        validate(client, entities, "김수빈", "EDIT_PAGE", "박영진 자기소개", Decision.ALLOW);
        validate(client, entities, "김수빈", "EDIT_PAGE", "김칠기 자기소개", Decision.ALLOW);
        validate(client, entities, "김수빈", "TOGGLE_PUBLISH_PAGE", "운영진 회의록", Decision.ALLOW);
        validate(client, entities, "김수빈", "TOGGLE_PUBLISH_PAGE", "버디 논의", Decision.ALLOW);
        validate(client, entities, "김수빈", "TOGGLE_PUBLISH_PAGE", "박영진 자기소개", Decision.ALLOW);
        validate(client, entities, "김수빈", "TOGGLE_PUBLISH_PAGE", "김칠기 자기소개", Decision.ALLOW);

        validate(client, entities, "문성혁", "VIEW_PAGE", "운영진 회의록", Decision.ALLOW);
        validate(client, entities, "문성혁", "VIEW_PAGE", "버디 논의", Decision.ALLOW);
        validate(client, entities, "문성혁", "VIEW_PAGE", "박영진 자기소개", Decision.ALLOW);
        validate(client, entities, "문성혁", "VIEW_PAGE", "김칠기 자기소개", Decision.ALLOW);
        validate(client, entities, "문성혁", "EDIT_PAGE", "운영진 회의록", Decision.ALLOW);
        validate(client, entities, "문성혁", "EDIT_PAGE", "버디 논의", Decision.ALLOW);
        validate(client, entities, "문성혁", "EDIT_PAGE", "박영진 자기소개", Decision.ALLOW);
        validate(client, entities, "문성혁", "EDIT_PAGE", "김칠기 자기소개", Decision.ALLOW);
        validate(client, entities, "문성혁", "TOGGLE_PUBLISH_PAGE", "운영진 회의록", Decision.DENY);
        validate(client, entities, "문성혁", "TOGGLE_PUBLISH_PAGE", "버디 논의", Decision.DENY);
        validate(client, entities, "문성혁", "TOGGLE_PUBLISH_PAGE", "박영진 자기소개", Decision.DENY);
        validate(client, entities, "문성혁", "TOGGLE_PUBLISH_PAGE", "김칠기 자기소개", Decision.DENY);

        validate(client, entities, "박영진", "VIEW_PAGE", "운영진 회의록", Decision.DENY);
        validate(client, entities, "박영진", "VIEW_PAGE", "버디 논의", Decision.DENY);
        validate(client, entities, "박영진", "VIEW_PAGE", "박영진 자기소개", Decision.ALLOW);
        validate(client, entities, "박영진", "VIEW_PAGE", "김칠기 자기소개", Decision.ALLOW);
        validate(client, entities, "박영진", "EDIT_PAGE", "운영진 회의록", Decision.DENY);
        validate(client, entities, "박영진", "EDIT_PAGE", "버디 논의", Decision.DENY);
        validate(client, entities, "박영진", "EDIT_PAGE", "박영진 자기소개", Decision.ALLOW);
        validate(client, entities, "박영진", "EDIT_PAGE", "김칠기 자기소개", Decision.DENY);
        validate(client, entities, "박영진", "TOGGLE_PUBLISH_PAGE", "운영진 회의록", Decision.DENY);
        validate(client, entities, "박영진", "TOGGLE_PUBLISH_PAGE", "버디 논의", Decision.DENY);
        validate(client, entities, "박영진", "TOGGLE_PUBLISH_PAGE", "박영진 자기소개", Decision.DENY);
        validate(client, entities, "박영진", "TOGGLE_PUBLISH_PAGE", "김칠기 자기소개", Decision.DENY);

        validate(client, entities, "김칠기", "VIEW_PAGE", "운영진 회의록", Decision.DENY);
        validate(client, entities, "김칠기", "VIEW_PAGE", "버디 논의", Decision.DENY);
        validate(client, entities, "김칠기", "VIEW_PAGE", "박영진 자기소개", Decision.ALLOW);
        validate(client, entities, "김칠기", "VIEW_PAGE", "김칠기 자기소개", Decision.ALLOW);
        validate(client, entities, "김칠기", "EDIT_PAGE", "운영진 회의록", Decision.DENY);
        validate(client, entities, "김칠기", "EDIT_PAGE", "버디 논의", Decision.DENY);
        validate(client, entities, "김칠기", "EDIT_PAGE", "박영진 자기소개", Decision.DENY);
        validate(client, entities, "김칠기", "EDIT_PAGE", "김칠기 자기소개", Decision.ALLOW);
        validate(client, entities, "김칠기", "TOGGLE_PUBLISH_PAGE", "운영진 회의록", Decision.DENY);
        validate(client, entities, "김칠기", "TOGGLE_PUBLISH_PAGE", "버디 논의", Decision.DENY);
        validate(client, entities, "김칠기", "TOGGLE_PUBLISH_PAGE", "박영진 자기소개", Decision.DENY);
        validate(client, entities, "김칠기", "TOGGLE_PUBLISH_PAGE", "김칠기 자기소개", Decision.DENY);
    }

    private static void validate(VerifiedPermissionsClient client, EntitiesDefinition entities, String principal, String action, String page, Decision isAuthorized) {
        var resp = client.isAuthorized(builder -> {
            builder.policyStoreId("CXvsHjrMN5QhHM6ZgsaWHg");
            builder.principal(EntityIdentifier.builder().entityType("Notion::User").entityId(principal).build());
            builder.action(ActionIdentifier.builder().actionType("Notion::Action").actionId(action).build());
            builder.resource(EntityIdentifier.builder().entityType("Notion::Page").entityId(page).build());
            builder.entities(entities);
        });

        Assertions.assertEquals(isAuthorized, resp.decision());
    }
}
