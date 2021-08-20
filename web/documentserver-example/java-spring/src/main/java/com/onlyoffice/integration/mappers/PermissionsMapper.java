/**
 *
 * (c) Copyright Ascensio System SIA 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.onlyoffice.integration.mappers;

import com.onlyoffice.integration.entities.Permission;
import com.onlyoffice.integration.documentserver.models.filemodel.CommentGroup;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.stream.Collectors;

@Component
@Primary
public class PermissionsMapper extends AbstractMapper<Permission, com.onlyoffice.integration.documentserver.models.filemodel.Permission> {
    @Autowired
    private ModelMapper mapper;

    public PermissionsMapper() {
        super(com.onlyoffice.integration.documentserver.models.filemodel.Permission.class);
    }

    @PostConstruct
    public void configure() {
        mapper.createTypeMap(Permission.class, com.onlyoffice.integration.documentserver.models.filemodel.Permission.class)
                .setPostConverter(modelConverter());
    }

    @Override
    void handleSpecificFields(Permission source, com.onlyoffice.integration.documentserver.models.filemodel.Permission destination) {
        destination.setReviewGroups(source.getReviewGroups().stream().map(g -> g.getName()).collect(Collectors.toList()));
        destination.setCommentGroups(
                new CommentGroup(
                        source.getCommentsViewGroups().stream().map(g -> g.getName()).collect(Collectors.toList()),
                        source.getCommentsEditGroups().stream().map(g -> g.getName()).collect(Collectors.toList()),
                        source.getCommentsRemoveGroups().stream().map(g -> g.getName()).collect(Collectors.toList())
                )
        );
    }
}
