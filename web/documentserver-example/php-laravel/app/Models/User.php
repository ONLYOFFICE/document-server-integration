<?php

namespace App\Models;

/**
 * (c) Copyright Ascensio System SIA 2025
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
final class User
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

    public ?bool $avatar;

    public ?string $image;

    public ?array $goback;

    public ?array $close;

    /**
     * Constructor
     *
     *
     * @return void
     */
    public function __construct(
        string $id,
        ?string $name,
        ?string $email,
        ?string $group,
        ?array $reviewGroups,
        ?array $commentGroups,
        ?array $userInfoGroups,
        ?bool $favorite,
        ?array $deniedPermissions,
        ?array $descriptions,
        ?bool $templates,
        ?bool $avatar,
        ?array $goback,
        ?array $close
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
        $this->avatar = $avatar;
        $this->goback = $goback;
        $this->close = $close;
    }

    public function toArray(): array
    {
        return [
            'id' => $this->id,
            'name' => $this->name,
            'email' => $this->email,
            'group' => $this->group,
            'reviewGroups' => $this->reviewGroups,
            'commentGroups' => $this->commentGroups,
            'favorite' => $this->favorite,
            'deniedPermissions' => $this->deniedPermissions,
            'descriptions' => $this->descriptions,
            'templates' => $this->templates,
            'userInfoGroups' => $this->userInfoGroups,
            'avatar' => $this->avatar,
            'goback' => $this->goback,
            'close' => $this->close,
        ];
    }
}
