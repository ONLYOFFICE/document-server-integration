<?php

/**
 * (c) Copyright Ascensio System SIA 2024
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

namespace App\Repositories;

class LanguageRepository
{
    private array $languages = [
        'en' => 'English',
        'sq-AL' => 'Albanian (Albania)',
        'ar' => 'Arabic',
        'hy' => 'Armenian',
        'az' => 'Azerbaijani',
        'eu' => 'Basque',
        'be' => 'Belarusian',
        'bg' => 'Bulgarian',
        'ca' => 'Catalan',
        'zh' => 'Chinese (Simplified)',
        'zh-TW' => 'Chinese (Traditional)',
        'cs' => 'Czech',
        'da' => 'Danish',
        'nl' => 'Dutch',
        'en-GB' => 'English (United Kingdom)',
        'fi' => 'Finnish',
        'fr' => 'French',
        'gl' => 'Galego',
        'de' => 'German',
        'el' => 'Greek',
        'he-IL' => 'Hebrew (Israel)',
        'hu' => 'Hungarian',
        'id' => 'Indonesian',
        'it' => 'Italian',
        'ja' => 'Japanese',
        'ko' => 'Korean',
        'ku' => 'Kurdish',
        'lo' => 'Lao',
        'lv' => 'Latvian',
        'ms' => 'Malay (Malaysia)',
        'no' => 'Norwegian',
        'pl' => 'Polish',
        'pt' => 'Portuguese (Brazil)',
        'pt-PT' => 'Portuguese (Portugal)',
        'ro' => 'Romanian',
        'ru' => 'Russian',
        'sr-Latn-RS' => 'Serbian',
        'si' => 'Sinhala (Sri Lanka)',
        'sk' => 'Slovak',
        'sl' => 'Slovenian',
        'es' => 'Spanish',
        'sv' => 'Swedish',
        'tr' => 'Turkish',
        'uk' => 'Ukrainian',
        'vi' => 'Vietnamese',
        'aa-AA' => 'Test Language',
    ];

    public function all(): array
    {
        return $this->languages;
    }
}
