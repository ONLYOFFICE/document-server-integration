<?php

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

namespace App\Repositories;

class LanguageRepository
{
    private array $languages = [
        'en' => 'English',
        'sq-AL' => 'Albanian (Albania)',
        'ar-SA' => 'Arabic',
        'hy-AM' => 'Armenian',
        'az-Latn-AZ' => 'Azerbaijani',
        'eu-ES' => 'Basque',
        'be' => 'Belarusian',
        'bg-BG' => 'Bulgarian',
        'ca' => 'Catalan',
        'zh-CN' => 'Chinese (Simplified)',
        'zh-TW' => 'Chinese (Traditional)',
        'cs-CZ' => 'Czech',
        'da' => 'Danish',
        'nl-NL' => 'Dutch',
        'en-GB' => 'English (United Kingdom)',
        'fi-FI' => 'Finnish',
        'fr-FR' => 'French',
        'gl-ES' => 'Galego',
        'de-DE' => 'German',
        'el-GR' => 'Greek',
        'he-IL' => 'Hebrew (Israel)',
        'hu' => 'Hungarian',
        'id' => 'Indonesian',
        'it-IT' => 'Italian',
        'ja-JP' => 'Japanese',
        'ko-KR' => 'Korean',
        'lo' => 'Lao',
        'lv-LV' => 'Latvian',
        'ms-MY' => 'Malay (Malaysia)',
        'no' => 'Norwegian',
        'pl-PL' => 'Polish',
        'pt-BR' => 'Portuguese (Brazil)',
        'pt-PT' => 'Portuguese (Portugal)',
        'ro' => 'Romanian',
        'ru-RU' => 'Russian',
        'sr-Cyrl-RS' => 'Serbian (Cyrillic)',
        'sr-Latn-RS' => 'Serbian (Latin)',
        'si-LK' => 'Sinhala (Sri Lanka)',
        'sk-SK' => 'Slovak',
        'sl-SI' => 'Slovenian',
        'es-ES' => 'Spanish',
        'sv-SE' => 'Swedish',
        'tr-TR' => 'Turkish',
        'uk-UK' => 'Ukrainian',
        'vi-VN' => 'Vietnamese',
        'aa-AA' => 'Test Language',
    ];

    public function all(): array
    {
        return $this->languages;
    }
}
