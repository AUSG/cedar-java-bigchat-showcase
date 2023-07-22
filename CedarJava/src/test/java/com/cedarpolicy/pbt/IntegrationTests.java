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

}
