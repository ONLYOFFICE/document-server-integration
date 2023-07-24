<?php

namespace Example\Helpers;

/**
 * (c) Copyright Ascensio System SIA 2023
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
 */

final class Users
{
    public string $id;
    public ?string $name;
    public ?string $email;
    public ?string $group;
    public ?array $reviewGroups;
    public ?array $commentGroups;
    public ?bool $favorite;
    public ?array $deniedPermissions;
    public ?array $descriptions;
    public ?bool $templates;
    public ?array $userInfoGroups;

    /**
     * Constructor
     *
     * @param string $id
     * @param string|null $name
     * @param string|null $email
     * @param string|null $group
     * @param array|null $reviewGroups
     * @param array|null $commentGroups
     * @param array|null $userInfoGroups
     * @param bool|null $favorite
     * @param array|null $deniedPermissions
     * @param array|null $descriptions
     * @param bool|null $templates
     *
     * @return void
     */
    public function __construct(
        string  $id,
        ?string $name,
        ?string $email,
        ?string $group,
        ?array  $reviewGroups,
        ?array  $commentGroups,
        ?array  $userInfoGroups,
        ?bool   $favorite,
        ?array  $deniedPermissions,
        ?array  $descriptions,
        ?bool   $templates
    ) {
        $this->id = $id;
        $this->name = $name;
        $this->email = $email;
        $this->group = $group;
        $this->reviewGroups = $reviewGroups;
        $this->commentGroups = $commentGroups;
        $this->favorite = $favorite;
        $this->deniedPermissions = $deniedPermissions;
        $this->descriptions = $descriptions;
        $this->templates = $templates;
        $this->userInfoGroups = $userInfoGroups;
    }
}
