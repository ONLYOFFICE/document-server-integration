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

import com.onlyoffice.integration.entities.AbstractEntity;
import com.onlyoffice.integration.documentserver.models.AbstractModel;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

public abstract class AbstractMapper<E extends AbstractEntity, M extends AbstractModel> implements Mapper<E, M> {
    @Autowired
    ModelMapper mapper;

    private Class<M> modelClass;

    AbstractMapper(Class<M> modelClass) {
        this.modelClass = modelClass;
    }

    @Override
    public M toModel(E entity) {
        return Objects.isNull(entity)
                ? null
                : mapper.map(entity, modelClass);
    }

    Converter<E, M> modelConverter() {
        return context -> {
            E source = context.getSource();
            M destination = context.getDestination();
            handleSpecificFields(source, destination);
            return context.getDestination();
        };
    }


    void handleSpecificFields(E source, M destination) {
    }
}
